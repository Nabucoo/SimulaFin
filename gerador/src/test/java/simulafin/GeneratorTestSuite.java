package simulafin;

import simulafin.data.CustomerNameRepository;
import simulafin.data.LocationRepository;
import simulafin.export.CsvExporter;
import simulafin.generator.DateGenerator;
import simulafin.generator.DistributionType;
import simulafin.generator.ValueGenerator;
import simulafin.model.Category;
import simulafin.model.CityState;
import simulafin.model.TransactionType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Suíte de testes abrangente para validar:
 * 1. Unicidade e sequência rigorosa dos IDs
 * 2. Quantidade exata de registros gerados
 * 3. Respeito aos limites mínimo e máximo de valores
 * 4. Validade e formatação das datas no intervalo definido
 * 5. Catálogo de clientes com >1.000 combinações únicas
 * 6. Consistência e mapeamento correto de cidades e estados
 * 7. Integridade das categorias e tipos de pagamento
 * 8. Formatação e integridade do CSV (UTF-8, delimitadores, separador decimal)
 * 9. Comportamento estatístico das 4 distribuições (histogramas e médias)
 * 10. Desempenho e estimativa de vazão
 */
public class GeneratorTestSuite {

    private static int totalPassed = 0;
    private static int totalFailed = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("                   INICIANDO SUÍTE DE TESTES E VALIDAÇÕES                      ");
        System.out.println("===============================================================================");

        testCustomerCombinations();
        testLocationRepository();
        testValueLimitsAndDistributions();
        testDateIntervalAndRealisticTimes();
        testCsvExportIntegrityAndSequentialIds();
        testHighThroughputBenchmark();

        System.out.println("===============================================================================");
        System.out.printf("RESULTADO FINAL: %d PASSOU | %d FALHOU%n", totalPassed, totalFailed);
        System.out.println("===============================================================================");

        if (totalFailed > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition, String errorMsg) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            totalPassed++;
        } else {
            System.err.println("  [FAIL] " + testName + " -> " + errorMsg);
            totalFailed++;
        }
    }

    // 1. Validação de Combinações de Nomes (> 1.000)
    private static void testCustomerCombinations() {
        System.out.println("\n--- 1. Teste de Nomes e Sobrenomes ---");
        int combs = CustomerNameRepository.getTotalPossibleCombinations();
        assertTrue("Total de combinações de nomes >= 1000", combs >= 1000, "Encontradas apenas " + combs);
        System.out.printf("      Total de combinações possíveis: %,d%n", combs);

        Set<String> generatedNames = new HashSet<>();
        simulafin.generator.CustomerGenerator gen = new simulafin.generator.CustomerGenerator();
        for (int i = 0; i < 5000; i++) {
            String name = gen.nextCustomerName();
            String[] parts = name.split(" ");
            if (parts.length < 3) {
                assertTrue("Nome possui ao menos 3 partes", false, "Nome inválido: " + name);
                return;
            }
            generatedNames.add(name);
        }
        assertTrue("Diversidade de nomes gerados em 5000 amostras (>1000 distintos)",
                generatedNames.size() > 1000, "Distintos: " + generatedNames.size());
    }

    // 2. Validação da Base de Cidades e Estados
    private static void testLocationRepository() {
        System.out.println("\n--- 2. Teste da Base de Cidades e Estados (IBGE 2022) ---");
        CityState[] cities = LocationRepository.getAllCities();
        assertTrue("Quantidade de cidades == 500", cities.length == 500, "Cidades carregadas: " + cities.length);

        Set<String> validUfs = Set.of(
                "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
                "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
                "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
        );

        boolean allValidUfs = true;
        for (CityState cs : cities) {
            if (!validUfs.contains(cs.estado())) {
                allValidUfs = false;
                break;
            }
        }
        assertTrue("Todas as UFs são unidades federativas válidas do Brasil", allValidUfs, "UF inválida encontrada");
        assertTrue("Maior cidade é São Paulo, SP",
                cities[0].cidade().equals("São Paulo") && cities[0].estado().equals("SP"),
                "Primeira cidade: " + cities[0]);
    }

    // 3. Validação das Distribuições de Valores e Limites
    private static void testValueLimitsAndDistributions() {
        System.out.println("\n--- 3. Teste das Distribuições Estatísticas de Valores ---");
        double min = 100.0;
        double max = 1000.0;
        int sampleSize = 100_000;

        for (DistributionType dt : DistributionType.values()) {
            ValueGenerator vg = new ValueGenerator(min, max, dt);
            double sum = 0.0;
            double actualMin = Double.MAX_VALUE;
            double actualMax = -Double.MAX_VALUE;

            // Histograma com 10 intervalos (bins)
            int numBins = 10;
            int[] bins = new int[numBins];
            double binWidth = (max - min) / numBins;

            for (int i = 0; i < sampleSize; i++) {
                double val = vg.nextValue();
                if (val < min || val > max) {
                    assertTrue(dt.name() + " respeita limites rigorosamente", false, "Valor fora: " + val);
                    return;
                }
                if (val < actualMin) actualMin = val;
                if (val > actualMax) actualMax = val;
                sum += val;

                int binIdx = (int) ((val - min) / binWidth);
                if (binIdx >= numBins) binIdx = numBins - 1;
                bins[binIdx]++;
            }

            double mean = sum / sampleSize;
            double mid = (min + max) / 2.0;

            assertTrue(dt.name() + ": limites respeitados [min=" + min + ", max=" + max + "]",
                    actualMin >= min && actualMax <= max, "Min=" + actualMin + ", Max=" + actualMax);

            switch (dt) {
                case UNIFORME -> {
                    // Média deve ser próxima do ponto médio
                    boolean meanOk = Math.abs(mean - mid) < 20.0;
                    assertTrue("UNIFORME: média próxima do ponto médio (" + mean + " vs " + mid + ")", meanOk, "Média: " + mean);
                }
                case NORMAL -> {
                    boolean meanOk = Math.abs(mean - mid) < 15.0;
                    // Bins centrais (bins 4 e 5) devem ter muito mais amostras que as pontas (0 e 9)
                    boolean bellShape = (bins[4] + bins[5]) > (bins[0] + bins[9]) * 3;
                    assertTrue("NORMAL: média gaussiana simétrica (" + mean + ")", meanOk, "Média: " + mean);
                    assertTrue("NORMAL: formato em sino preservado", bellShape, "Centro=" + (bins[4] + bins[5]) + ", Pontas=" + (bins[0] + bins[9]));
                }
                case EXPONENCIAL_DECRESCENTE -> {
                    // Média deve ser substancialmente menor que o ponto médio
                    boolean meanOk = mean < mid - 50.0;
                    // Bin 0 deve ser muito maior que Bin 9
                    boolean decreasing = bins[0] > bins[9] * 3;
                    assertTrue("EXPONENCIAL_DECRESCENTE: média concentrada no mínimo (" + mean + " < " + mid + ")", meanOk, "Média: " + mean);
                    assertTrue("EXPONENCIAL_DECRESCENTE: frequência decresce até o máximo (Bin0=" + bins[0] + " > Bin9=" + bins[9] + ")", decreasing, "Bin0=" + bins[0] + ", Bin9=" + bins[9]);
                }
                case EXPONENCIAL_CRESCENTE -> {
                    // Média deve ser substancialmente maior que o ponto médio
                    boolean meanOk = mean > mid + 50.0;
                    // Bin 9 deve ser muito maior que Bin 0
                    boolean increasing = bins[9] > bins[0] * 3;
                    assertTrue("EXPONENCIAL_CRESCENTE: média concentrada no máximo (" + mean + " > " + mid + ")", meanOk, "Média: " + mean);
                    assertTrue("EXPONENCIAL_CRESCENTE: frequência cresce até o máximo (Bin9=" + bins[9] + " > Bin0=" + bins[0] + ")", increasing, "Bin9=" + bins[9] + ", Bin0=" + bins[0]);
                }
            }
        }
    }

    // 4. Validação de Intervalo e Distribuição de Horários
    private static void testDateIntervalAndRealisticTimes() {
        System.out.println("\n--- 4. Teste de Datas e Distribuição Realista de Horários ---");
        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 8, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 6, 30, 22, 0, 0);
        DateGenerator dg = new DateGenerator(start, end);

        int sampleSize = 50_000;
        int morningNight = 0; // 00h às 06h
        int daytime = 0;      // 09h às 21h

        for (int i = 0; i < sampleSize; i++) {
            LocalDateTime dt = dg.nextDateTime();
            if (dt.isBefore(start) || dt.isAfter(end)) {
                assertTrue("Data dentro do intervalo", false, "Data fora: " + dt);
                return;
            }
            int hour = dt.getHour();
            if (hour >= 0 && hour < 6) {
                morningNight++;
            } else if (hour >= 9 && hour <= 21) {
                daytime++;
            }
        }

        assertTrue("Todas as 50.000 datas dentro do intervalo estrito", true, "");
        assertTrue("Concentração realista de horários (diurno > madrugada: " + daytime + " vs " + morningNight + ")",
                daytime > morningNight * 4, "Diurno=" + daytime + ", Madrugada=" + morningNight);
    }

    // 5. Validação da Exportação de CSV e Sequenciamento de IDs
    private static void testCsvExportIntegrityAndSequentialIds() {
        System.out.println("\n--- 5. Teste de Integridade do CSV e Unicidade/Sequência dos IDs ---");
        try {
            Path tempCsv = Files.createTempFile("test_transactions_", ".csv");
            tempCsv.toFile().deleteOnExit();

            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59, 59);
            DateGenerator dg = new DateGenerator(start, end);
            ValueGenerator vg = new ValueGenerator(10.0, 500.0, DistributionType.UNIFORME);

            CsvExporter exporter = new CsvExporter(tempCsv, dg, vg);
            long count = 25_000L;
            exporter.export(count, 0, null);

            // Validação das linhas
            long expectedId = 1;
            int totalLines = 0;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            Set<String> validCategories = new HashSet<>();
            for (Category c : Category.values()) validCategories.add(c.getDescricao());

            Set<String> validTypes = new HashSet<>();
            for (TransactionType t : TransactionType.values()) validTypes.add(t.getDescricao());

            try (BufferedReader reader = new BufferedReader(new FileReader(tempCsv.toFile(), StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                assertTrue("Cabeçalho CSV idêntico ao padrão",
                        header.equals("id,cliente,data,valor,categoria,cidade,estado,tipo"),
                        "Header: " + header);

                String line;
                while ((line = reader.readLine()) != null) {
                    totalLines++;
                    String[] parts = line.split(",");
                    if (parts.length != 8) {
                        assertTrue("Linha possui 8 campos exatos", false, "Campos=" + parts.length + " na linha: " + line);
                        return;
                    }

                    long id = Long.parseLong(parts[0]);
                    if (id != expectedId) {
                        assertTrue("ID sequencial estrito", false, "Esperado " + expectedId + ", obtido " + id);
                        return;
                    }
                    expectedId++;

                    // Cliente com ao menos 3 nomes
                    String[] nameParts = parts[1].split(" ");
                    if (nameParts.length < 3) {
                        assertTrue("Nome cliente com 3 partes", false, "Nome: " + parts[1]);
                        return;
                    }

                    // Data válida
                    LocalDateTime.parse(parts[2], formatter);

                    // Valor com ponto decimal e duas casas
                    String valStr = parts[3];
                    if (!valStr.contains(".") || valStr.substring(valStr.indexOf('.')).length() != 3) {
                        assertTrue("Valor financeiro possui 2 casas e ponto", false, "Valor: " + valStr);
                        return;
                    }
                    double val = Double.parseDouble(valStr);
                    if (val < 10.0 || val > 500.0) {
                        assertTrue("Valor dentro dos limites", false, "Valor: " + val);
                        return;
                    }

                    // Categoria e Tipo
                    if (!validCategories.contains(parts[4])) {
                        assertTrue("Categoria permitida", false, "Categoria: " + parts[4]);
                        return;
                    }
                    if (!validTypes.contains(parts[7])) {
                        assertTrue("Tipo permitido", false, "Tipo: " + parts[7]);
                        return;
                    }
                }
            }

            assertTrue("Quantidade exata de registros gerados == " + count, totalLines == count, "Gerados: " + totalLines);
            assertTrue("IDs iniciam em 1 e são estritamente sequenciais até " + count, expectedId == count + 1, "Último ID: " + (expectedId - 1));

        } catch (Exception e) {
            assertTrue("Execução sem exceções", false, e.getMessage());
            e.printStackTrace();
        }
    }

    // 6. Teste de Desempenho e Vazão (Benchmark)
    private static void testHighThroughputBenchmark() {
        System.out.println("\n--- 6. Teste de Desempenho e Vazão em Streaming ---");
        try {
            Path tempCsv = Files.createTempFile("benchmark_transactions_", ".csv");
            tempCsv.toFile().deleteOnExit();

            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
            DateGenerator dg = new DateGenerator(start, end);
            ValueGenerator vg = new ValueGenerator(1.0, 10000.0, DistributionType.NORMAL);

            CsvExporter exporter = new CsvExporter(tempCsv, dg, vg);
            long count = 2_000_000L; // 2 milhões de registros
            System.out.println("      Gerando 2.000.000 registros para aferição de taxa real...");

            long ms = exporter.export(count, 500_000L, p -> {
                System.out.printf(Locale.US, "      [Progresso] %5.1f%% | Taxa: %,9.0f reg/s%n", p.percent(), p.recordsPerSec());
            });

            double sec = ms / 1000.0;
            double rate = count / sec;
            System.out.printf(Locale.US, "      Tempo Total: %.2fs | Taxa Média: %,.0f registros/segundo%n", sec, rate);

            assertTrue("Taxa de geração superior a 500.000 registros/segundo", rate > 500_000, "Taxa: " + rate);

        } catch (Exception e) {
            assertTrue("Benchmark concluído com sucesso", false, e.getMessage());
            e.printStackTrace();
        }
    }
}
