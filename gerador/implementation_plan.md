# Plano de Implementação: Gerador de Transações Financeiras Sintéticas em Java

## 1. Visão Geral e Arquitetura Proposta

O projeto tem como objetivo desenvolver um gerador robusto e de altíssimo desempenho de transações financeiras sintéticas em Java 21, capaz de escalar de pequenas amostras de validação até centenas de milhões de registros (ex.: 500M) sem estouro de memória (Zero/Low Allocation Streaming).

### Estrutura de Pacotes e Componentes

Organização sugerida em padrão de pacotes limpo (`simulafin`):

```
simulafin/
├── Main.java                        // Ponto de entrada CLI, parsing de argumentos e orquestração
├── model/
│   ├── Transaction.java             // POJO / Record ou estrutura de dados da transação
│   ├── TransactionType.java         // Enum: PIX, CREDITO, BOLETO, DEBITO
│   ├── Category.java                // Enum: Alimentacao, Transporte, Eletronicos, Saude, Educacao
│   └── CityState.java               // Record (cidade, uf)
├── data/
│   ├── LocationRepository.java      // Repositório imutável das 500 maiores cidades do Brasil (Censo IBGE 2022)
│   └── CustomerNameRepository.java  // Listas de nomes, segundos nomes e sobrenomes (>1.000 combinações)
├── generator/
│   ├── DistributionType.java        // Enum: UNIFORME, NORMAL, EXPONENCIAL_CRESCENTE, EXPONENCIAL_DECRESCENTE
│   ├── ValueGenerator.java          // Implementação das 4 distribuições estatísticas de valores
│   ├── CustomerGenerator.java       // Geração determinística ou estocástica de clientes válidos
│   ├── DateGenerator.java           // Geração temporal (intervalo configurável + horário realista/pico)
│   └── TransactionGenerator.java    // Coordena a criação dos registros
├── export/
│   ├── CsvExporter.java             // Exportador em streaming com BufferedWriter e buffer dimensionado
│   └── FastCsvFormatter.java        // Utilitário de formatação de números e datas sem overhead excessivo
└── test/
    └── GeneratorTestSuite.java      // Suíte de testes automatizados e validação estatística
```

---

## 2. Escolhas Estatísticas e Fórmulas Matemáticas

### 2.1 Distribuições de Valor (`ValueGenerator`)
Dado o intervalo de valor $[V_{min}, V_{max}]$, definimos o intervalo $\Delta = V_{max} - V_{min}$ e $U \sim \text{Uniforme}(0, 1)$.

1. **UNIFORME**:
   $$V = V_{min} + U \cdot \Delta$$
   - Todos os pontos têm probabilidade teórica uniforme no intervalo.

2. **NORMAL**:
   - Média $\mu = \frac{V_{min} + V_{max}}{2}$.
   - Desvio-padrão padrão documentado: $\sigma = \frac{\Delta}{6}$ (pela regra empírica $3\sigma$, $\approx 99.73\%$ dos valores recaem naturalmente em $[V_{min}, V_{max}]$).
   - Para valores fora do intervalo (caudas $< 0.27\%$), aplica-se **rejeição com reamostragem** (truncated normal) para preservar a forma exata da curva gaussiana sem criar picos espúrios nos limites.
   - Algoritmo: Box-Muller ou `ThreadLocalRandom.current().nextGaussian()`.

3. **EXPONENCIAL DECRESCENTE (Concentração no valor mínimo)**:
   - Para evitar rejeição infinita da cauda longa da distribuição exponencial pura, utilizamos a transformada inversa truncada em $[0, 1]$ com parâmetro de taxa $\lambda$ (ex: $\lambda = 3.0$ ou configurável):
     $$U \sim \text{Uniforme}(0, 1)$$
     $$F(x) = \frac{1 - e^{-\lambda x}}{1 - e^{-\lambda}} \implies x = -\frac{1}{\lambda} \ln(1 - U \cdot (1 - e^{-\lambda}))$$
     $$V = V_{min} + x \cdot \Delta$$
   - Garante que $x \in [0, 1]$, com densidade estritamente decrescente de $V_{min}$ para $V_{max}$, sem perda de amostras nem distorção.

4. **EXPONENCIAL CRESCENTE (Concentração no valor máximo)**:
   - Reflexão simétrica da exponencial decrescente:
     $$V = V_{max} - x \cdot \Delta$$
     onde $x$ é a variável exponencial normalizada em $[0, 1]$ calculada acima.
   - Densidade estritamente crescente rumo a $V_{max}$.

### 2.2 Distribuição Temporal e Horários Realistas (`DateGenerator`)
- Intervalo configurável: Data Inicial a Data Final (ex: 2024-01-01 00:00:00 a 2024-12-31 23:59:59).
- Dia selecionado uniformemente no intervalo.
- **Horário do dia (0h às 23:59)**: Comportamento comercial realista (baixa madrugada, picos comerciais às 11h-14h e 17h-20h).
  - Modelado por distribuição mista gaussiana (bimodal diurna em torno de 12h e 18h) com piso de ruído uniforme para transações 24h (PIX/Internet), garantindo que nenhum horário fique fora de $[0, 24)$ horas.

---

## 3. Base das 500 Cidades e Estados (`LocationRepository`)

- **Fonte Oficial**: Instituto Brasileiro de Geografia e Estatística (IBGE) - Censo Demográfico 2022 (Resultados da População por Municípios).
- As 500 cidades mais populosas do Brasil serão incluídas em arquivo embutido no repositório (`cities_ibge_top500.csv` ou array estático otimizado de registros `CityState`).
- Cada cidade mapeada com sua respectiva UF exata (ex.: `São Paulo, SP`, `Rio de Janeiro, RJ`, `Belo Horizonte, MG`, etc.).
- Carregamento único em memória (vetor estático imutável de 500 elementos $\approx$ alguns kilobytes de RAM).

---

## 4. Estratégia para Volumes Extremos (até 500 Milhões de Registros)

Gerar 500 milhões de registros em CSV ($\approx 40$ a 50 GB de arquivo) impõe gargalos severos de I/O e Garbage Collection (GC) se implementado de forma ingênua:

1. **Streaming / Zero Buffer de Objeto**:
   - Jamais manter listas de transações em memória.
   - O loop de geração formata diretamente para o buffer de saída ou reutiliza um objeto mutável `Transaction`/registro de linha.
2. **I/O Otimizado**:
   - `BufferedWriter` envelopando `OutputStreamWriter` (UTF-8) com `FileOutputStream`, usando buffer configurado para 1MB a 4MB (reduz chamadas de sistema syscalls de escrita).
   - Formatação customizada rápida de números (`long` para ASCII, decimais com divisão e resto evitando `String.format` pesado).
3. **Geradores de Números Pseudoaleatórios (PRNG)**:
   - Uso de `java.util.concurrent.ThreadLocalRandom` ou `SplittableRandom` (extremamente rápido e sem overhead de sincronização).
4. **Relatório de Progresso**:
   - Feedback visual a cada N transações (ex.: a cada 5.000.000 ou configurável via parâmetro), exibindo taxa de geração (transações/segundo) e percentual concluído.

---

## 5. Plano de Testes Automatizados (`TestSuite`)

1. **Unicidade e Sequência de IDs**: Testar que IDs iniciam em 1 e terminam em $N$ sem gaps ou duplicatas.
2. **Respeito aos Limites de Valor**: Validar que nenhum valor cai fora de $[V_{min}, V_{max}]$ nas 4 distribuições.
3. **Distribuições Estatísticas**:
   - Teste de média e mediana:
     - Uniforme: média $\approx (V_{min} + V_{max}) / 2$.
     - Normal: média $\approx \mu$, variância condizente.
     - Exponencial Crescente: média $> (V_{min} + V_{max}) / 2$.
     - Exponencial Decrescente: média $< (V_{min} + V_{max}) / 2$.
4. **Validade de Datas**: Todas as datas pertencem ao intervalo estipulado e formato `dd/MM/yyyy HH:mm:ss`.
5. **Nomes de Clientes**: Verificação de catálogo de nomes com $> 1.000$ combinações únicas possíveis.
6. **Integridade de Cidades/UF**: Validação contra a tabela oficial das 500 cidades.
7. **Formatação do CSV**: Validação de cabeçalho, vírgulas, ponto como separador decimal e codificação UTF-8.

---

## 6. Verificação e Entregáveis

- Código-fonte modular compilável com `javac` padrão (Java 21 sem dependências externas obrigatórias).
- Execução de testes de unidade e estatísticos.
- Geração de um arquivo CSV de exemplo (`amostra_transacoes.csv`) com relatório de benchmark.
- Documentação completa em markdown detalhando fórmulas, execução e medições de desempenho.
