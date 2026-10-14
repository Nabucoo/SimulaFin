# SimulaFin

SimulaFin é um projeto Java com dois programas: um gerador de transações financeiras sintéticas em CSV e um processador que calcula análises sobre esses arquivos, em modo sequencial ou paralelo.

Os registros são artificiais e servem para desenvolvimento, demonstrações e benchmarks. Não representam clientes nem transações reais.

## Requisitos

- JDK 21 ou superior (`java` e `javac` disponíveis no terminal).
- Espaço em disco de acordo com o tamanho do CSV gerado ou processado.

O processador usa bibliotecas Java incluídas em `processador/lib/`, incluindo OpenHFT Affinity para a opção de afinidade de CPU.

## Estrutura

```text
SimulaFin/
├── gerador/
│   └── src/
│       ├── main/java/       # Código do gerador
│       ├── main/resources/  # Lista de cidades baseada no IBGE
│       └── test/java/       # Suíte de testes do gerador
├── processador/
│   ├── src/                 # Código do processador de CSV
│   └── lib/                 # Dependências Java do processador
├── data/                    # CSVs locais de exemplo/benchmark (ignorados pelo Git)
└── README.md
```

## Gerador de transações

Compile a partir da raiz do repositório:

```bash
mkdir -p gerador/bin
javac -encoding UTF-8 -d gerador/bin $(find gerador/src/main/java -name '*.java')
cp gerador/src/main/resources/cidades_ibge_top500.csv gerador/bin/
```

Gere 100 mil registros usando os valores padrão:

```bash
java -cp gerador/bin simulafin.Main --count=100000 --out=data/transacoes.csv
```

O arquivo terá o cabeçalho `id,cliente,data,valor,categoria,cidade,estado,tipo`. A saída usa UTF-8. Crie a pasta de destino antes de executar, se ela ainda não existir.

### Opções do gerador

| Opção | Descrição | Padrão |
|---|---|---|
| `--count=N` | Quantidade de transações (deve ser maior que zero) | `1000000` |
| `--min=V` | Valor mínimo | `10.0` |
| `--max=V` | Valor máximo | `5000.0` |
| `--dist=TIPO` | `UNIFORME`, `NORMAL`, `EXPONENCIAL_CRESCENTE` ou `EXPONENCIAL_DECRESCENTE` | `UNIFORME` |
| `--out=ARQUIVO` | Caminho do CSV de saída | `transacoes.csv` |
| `--start="dd/MM/yyyy HH:mm:ss"` | Data e hora iniciais | `01/01/2024 00:00:00` |
| `--end="dd/MM/yyyy HH:mm:ss"` | Data e hora finais | `31/12/2024 23:59:59` |
| `--progress=N` | Intervalo de registros entre atualizações de progresso | Automático |
| `--help` | Exibe a ajuda | — |

Exemplo com distribuição normal e período definido:

```bash
java -cp gerador/bin simulafin.Main \
  --count=1000000 \
  --dist=NORMAL \
  --min=20 \
  --max=2500 \
  --start="01/01/2025 00:00:00" \
  --end="31/12/2025 23:59:59" \
  --out=data/transacoes_normal.csv
```

As datas incluem horários distribuídos com maior frequência durante o dia. A distribuição `NORMAL` é limitada ao intervalo de valores configurado; as distribuições exponenciais concentram valores em direção ao mínimo ou ao máximo.

## Processador de CSV

Compile o segundo programa a partir da raiz:

```bash
mkdir -p processador/bin
javac -encoding UTF-8 -cp 'processador/lib/*' -d processador/bin $(find processador/src -name '*.java')
```

Execute informando o arquivo CSV:

```bash
java -cp 'processador/bin:processador/lib/*' Main data/transacoes.csv
```

Sem um argumento, o programa tenta abrir `data/transactions_10M.csv`; se não encontrar o arquivo, solicita um caminho pelo terminal. Em seguida, escolha o processamento sequencial ou paralelo. No modo paralelo, informe 2, 4, 6 ou 8 threads e escolha se deseja ativar a afinidade de CPU.

O processador lê o cabeçalho e processa os registros em lotes. Use um CSV produzido pelo gerador ou com o mesmo formato de colunas. Em sistemas Windows, substitua `:` no classpath pelo separador `;`.

## Testes do gerador

Compile e execute a suíte incluída:

```bash
javac -encoding UTF-8 -cp gerador/bin -d gerador/bin $(find gerador/src/test/java -name '*.java')
java -cp gerador/bin simulafin.GeneratorTestSuite
```

## Arquivos de dados e Git

Os CSVs gerados e os arquivos de benchmark em `data/` podem ocupar muito espaço e são ignorados pelo `.gitignore`. Gere-os localmente conforme necessário; eles não são necessários para compilar o gerador. A base compacta de cidades usada pelo programa fica versionada em `gerador/src/main/resources/cidades_ibge_top500.csv`.

Os diretórios `bin/` contêm arquivos compilados e também são ignorados. As bibliotecas em `processador/lib/` são dependências usadas pelo processador.
