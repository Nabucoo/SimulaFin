package simulafin;

import simulafin.data.CustomerNameRepository;
import simulafin.data.LocationRepository;
import simulafin.export.CsvExporter;
import simulafin.generator.DateGenerator;
import simulafin.generator.DistributionType;
import simulafin.generator.ValueGenerator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Ponto de entrada CLI do SimulaFin - Gerador de Transações Financeiras Sintéticas.
 */
public class Main {

    private static final DateTimeFormatter DATE_INPUT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void main(String[] args) {
        // Configuração padrão
        double minValue = 10.0;
        double maxValue = 5000.0;
        long totalCount = 1_000_000L;
        DistributionType distribution = DistributionType.UNIFORME;
        Path outputFile = Paths.get("transacoes.csv");
        LocalDateTime startDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        long progressInterval = 0; // Calculado dinamicamente

        // Parsing de argumentos CLI
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--help") || arg.equals("-h")) {
                printHelp();
                return;
            } else if (arg.startsWith("--min=")) {
                minValue = Double.parseDouble(arg.substring(6));
            } else if (arg.startsWith("--max=")) {
                maxValue = Double.parseDouble(arg.substring(6));
            } else if (arg.startsWith("--count=")) {
                totalCount = Long.parseLong(arg.substring(8));
            } else if (arg.startsWith("--dist=")) {
                distribution = DistributionType.fromString(arg.substring(7));
            } else if (arg.startsWith("--out=")) {
                outputFile = Paths.get(arg.substring(6));
            } else if (arg.startsWith("--start=")) {
                startDateTime = LocalDateTime.parse(arg.substring(8), DATE_INPUT_FORMAT);
            } else if (arg.startsWith("--end=")) {
                endDateTime = LocalDateTime.parse(arg.substring(6), DATE_INPUT_FORMAT);
            } else if (arg.startsWith("--progress=")) {
                progressInterval = Long.parseLong(arg.substring(11));
            }
        }

        if (progressInterval <= 0) {
            progressInterval = Math.max(100_000L, totalCount / 20);
        }

        System.out.println("===============================================================================");
        System.out.println("                   SIMULAFIN - GERADOR DE TRANSAÇÕES SINTÉTICAS                ");
        System.out.println("===============================================================================");
        System.out.printf(Locale.US, "Registros a gerar   : %,d%n", totalCount);
        System.out.printf(Locale.US, "Faixa de Valores    : R$ %.2f a R$ %.2f%n", minValue, maxValue);
        System.out.printf("Distribuição        : %s%n", distribution.name());
        System.out.printf("Período das Datas   : %s até %s%n",
                startDateTime.format(DATE_INPUT_FORMAT), endDateTime.format(DATE_INPUT_FORMAT));
        System.out.printf("Arquivo de Saída    : %s%n", outputFile.toAbsolutePath());
        System.out.printf("Base de Cidades     : %d municípios carregados (%s)%n",
                LocationRepository.size(), LocationRepository.getDataSourceInfo());
        System.out.printf(Locale.US, "Combinações Nomes   : %,d combinações disponíveis%n",
                CustomerNameRepository.getTotalPossibleCombinations());
        System.out.println("-------------------------------------------------------------------------------");

        DateGenerator dateGen = new DateGenerator(startDateTime, endDateTime);
        ValueGenerator valueGen = new ValueGenerator(minValue, maxValue, distribution);
        CsvExporter exporter = new CsvExporter(outputFile, dateGen, valueGen);

        System.out.println("Iniciando geração e exportação contínua...");
        try {
            long durationMs = exporter.export(totalCount, progressInterval, p -> {
                System.out.printf(Locale.US, "Progresso: %6.2f%% | %,11d / %,11d | Taxa: %,10.0f reg/s | Tempo: %.1fs%n",
                        p.percent(), p.currentCount(), p.totalCount(), p.recordsPerSec(), p.elapsedSec());
            });

            System.out.println("-------------------------------------------------------------------------------");
            double totalSec = durationMs / 1000.0;
            double avgRate = totalSec > 0 ? (totalCount / totalSec) : totalCount;
            System.out.printf(Locale.US, "Concluído com sucesso!%n");
            System.out.printf(Locale.US, "Tempo Total         : %.2f segundos (%,d ms)%n", totalSec, durationMs);
            System.out.printf(Locale.US, "Taxa Média          : %,10.0f registros/segundo%n", avgRate);
            long fileBytes = outputFile.toFile().length();
            double fileMb = fileBytes / (1024.0 * 1024.0);
            System.out.printf(Locale.US, "Tamanho do Arquivo  : %.2f MB (%,d bytes)%n", fileMb, fileBytes);
            System.out.println("===============================================================================");

        } catch (Exception e) {
            System.err.println("Erro durante a execução: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printHelp() {
        System.out.println("Uso do SimulaFin:");
        System.out.println("  java -cp <classpath> simulafin.Main [opções]");
        System.out.println();
        System.out.println("Opções disponíveis:");
        System.out.println("  --count=<N>         Quantidade de transações a gerar (padrão: 100000)");
        System.out.println("  --min=<V>           Valor mínimo das transações (padrão: 10.0)");
        System.out.println("  --max=<V>           Valor máximo das transações (padrão: 5000.0)");
        System.out.println("  --dist=<TIPO>       UNIFORME | NORMAL | EXPONENCIAL_CRESCENTE | EXPONENCIAL_DECRESCENTE");
        System.out.println("  --out=<ARQUIVO>     Caminho do arquivo CSV de saída (padrão: transacoes.csv)");
        System.out.println("  --start=\"dd/MM/yyyy HH:mm:ss\" Data inicial (padrão: 01/01/2024 00:00:00)");
        System.out.println("  --end=\"dd/MM/yyyy HH:mm:ss\"   Data final (padrão: 31/12/2024 23:59:59)");
        System.out.println("  --progress=<N>      Intervalo de exibição de progresso");
        System.out.println("  -h, --help          Exibe esta mensagem de ajuda");
    }
}
