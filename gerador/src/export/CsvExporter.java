package export;

import data.CustomerNameRepository;
import data.LocationRepository;
import generator.DateGenerator;
import generator.ValueGenerator;
import model.Category;
import model.CityState;
import model.TransactionType;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Responsável por exportar transações sintéticas diretamente em arquivo CSV.
 * Projetado para suportar centenas de milhões de transações em streaming:
 * - Buffer de escrita redimensionado (1 MB a 4 MB)
 * - Zero acúmulo de objetos na Heap
 * - Callback periódico para relato de progresso e taxa de transferência
 */
public final class CsvExporter {

    public static final String HEADER = "id,cliente,data,valor,categoria,cidade,estado,tipo\n";
    private static final int DEFAULT_BUFFER_SIZE = 2 * 1024 * 1024; // 2 MB

    private final Path outputPath;
    private final DateGenerator dateGenerator;
    private final ValueGenerator valueGenerator;
    private final int bufferSize;

    public record ProgressInfo(long currentCount, long totalCount, double percent, double recordsPerSec, double elapsedSec) {
    }

    public CsvExporter(Path outputPath, DateGenerator dateGenerator, ValueGenerator valueGenerator) {
        this(outputPath, dateGenerator, valueGenerator, DEFAULT_BUFFER_SIZE);
    }

    public CsvExporter(Path outputPath, DateGenerator dateGenerator, ValueGenerator valueGenerator, int bufferSize) {
        this.outputPath = outputPath;
        this.dateGenerator = dateGenerator;
        this.valueGenerator = valueGenerator;
        this.bufferSize = bufferSize;
    }

    /**
     * Gera e grava a quantidade exata de transações diretamente no arquivo CSV.
     *
     * @param totalCount Quantidade total de registros a gerar
     * @param progressInterval Intervalo de registros para notificar o listener (0 para desativar)
     * @param progressListener Callback de progresso
     * @return Duração total em milissegundos
     */
    public long export(long totalCount, long progressInterval, Consumer<ProgressInfo> progressListener) throws IOException {
        if (totalCount <= 0) {
            throw new IllegalArgumentException("totalCount deve ser maior que zero: " + totalCount);
        }

        Category[] categories = Category.cachedValues();
        int catLen = categories.length;

        TransactionType[] types = TransactionType.cachedValues();
        int typeLen = types.length;

        String[] firstNames = CustomerNameRepository.FIRST_NAMES;
        String[] middleNames = CustomerNameRepository.MIDDLE_NAMES;
        String[] lastNames = CustomerNameRepository.LAST_NAMES;
        int fnLen = firstNames.length;
        int mnLen = middleNames.length;
        int lnLen = lastNames.length;

        long startTime = System.nanoTime();

        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw, bufferSize)) {

            // Escreve cabeçalho UTF-8
            writer.write(HEADER);

            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (long id = 1; id <= totalCount; id++) {
                // Cliente
                String cliente = firstNames[random.nextInt(fnLen)] + " " +
                                 middleNames[random.nextInt(mnLen)] + " " +
                                 lastNames[random.nextInt(lnLen)];

                // Data
                LocalDateTime data = dateGenerator.nextDateTime();

                // Valor
                double valor = valueGenerator.nextValue();

                // Categoria, Localidade e Tipo
                String categoria = categories[random.nextInt(catLen)].getDescricao();
                CityState loc = LocationRepository.getRandomCity(random);
                String tipo = types[random.nextInt(typeLen)].getDescricao();

                // Gravação direta sem criar objeto Transaction na Heap
                FastCsvFormatter.writeRow(
                        writer,
                        id,
                        cliente,
                        data,
                        valor,
                        categoria,
                        loc.cidade(),
                        loc.estado(),
                        tipo
                );

                if (progressListener != null && progressInterval > 0 && (id % progressInterval == 0 || id == totalCount)) {
                    long now = System.nanoTime();
                    double elapsedSec = (now - startTime) / 1_000_000_000.0;
                    double rate = elapsedSec > 0 ? (id / elapsedSec) : 0;
                    double percent = (id * 100.0) / totalCount;
                    progressListener.accept(new ProgressInfo(id, totalCount, percent, rate, elapsedSec));
                }
            }

            writer.flush();
        }

        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000; // ms
    }
}
