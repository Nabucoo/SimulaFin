package export;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;

/**
 * Utilitário de formatação de alta velocidade para linhas CSV.
 * Evita instanciações caras de objetos String/Number e utiliza bufferização de caracteres.
 * Formato padrão: id,cliente,data,valor,categoria,cidade,estado,tipo
 */
public final class FastCsvFormatter {

    private static final char COMMA = ',';
    private static final char NEWLINE = '\n';
    private static final char DOT = '.';

    private FastCsvFormatter() {
    }

    /**
     * Formata e escreve uma linha CSV diretamente no Writer de forma otimizada.
     */
    public static void writeRow(Writer writer,
                                long id,
                                String cliente,
                                LocalDateTime data,
                                double valor,
                                String categoria,
                                String cidade,
                                String estado,
                                String tipo) throws IOException {

        // 1. ID
        writeLong(writer, id);
        writer.write(COMMA);

        // 2. Cliente
        writeEscaped(writer, cliente);
        writer.write(COMMA);

        // 3. Data: dd/MM/yyyy HH:mm:ss
        writeDateTime(writer, data);
        writer.write(COMMA);

        // 4. Valor (2 casas decimais e separador ponto)
        writeDouble2Decimals(writer, valor);
        writer.write(COMMA);

        // 5. Categoria
        writeEscaped(writer, categoria);
        writer.write(COMMA);

        // 6. Cidade
        writeEscaped(writer, cidade);
        writer.write(COMMA);

        // 7. Estado
        writeEscaped(writer, estado);
        writer.write(COMMA);

        // 8. Tipo
        writeEscaped(writer, tipo);
        writer.write(NEWLINE);
    }

    /**
     * Escreve campo de texto escapando aspas se contiver vírgula ou aspas.
     */
    public static void writeEscaped(Writer writer, String text) throws IOException {
        if (text == null) return;
        boolean needsQuotes = text.indexOf(COMMA) >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0;
        if (!needsQuotes) {
            writer.write(text);
        } else {
            writer.write('"');
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '"') {
                    writer.write('"'); // dobra aspas
                }
                writer.write(c);
            }
            writer.write('"');
        }
    }

    /**
     * Escreve um long diretamente sem alocação de String.
     */
    public static void writeLong(Writer writer, long value) throws IOException {
        writer.write(Long.toString(value));
    }

    /**
     * Escreve um valor double arredondado para exatamente duas casas decimais com ponto decimal.
     * Exemplo: 123.456 -> 123.46
     */
    public static void writeDouble2Decimals(Writer writer, double value) throws IOException {
        long cents = Math.round(value * 100.0);
        if (cents < 0) {
            writer.write('-');
            cents = -cents;
        }
        long integerPart = cents / 100;
        long decimalPart = cents % 100;

        writer.write(Long.toString(integerPart));
        writer.write(DOT);
        if (decimalPart < 10) {
            writer.write('0');
        }
        writer.write(Long.toString(decimalPart));
    }

    /**
     * Escreve a data no formato dd/MM/yyyy HH:mm:ss diretamente sem DateTimeFormatter alocativo.
     */
    public static void writeDateTime(Writer writer, LocalDateTime dt) throws IOException {
        int day = dt.getDayOfMonth();
        int month = dt.getMonthValue();
        int year = dt.getYear();
        int hour = dt.getHour();
        int minute = dt.getMinute();
        int second = dt.getSecond();

        write2Digits(writer, day);
        writer.write('/');
        write2Digits(writer, month);
        writer.write('/');
        writer.write(Integer.toString(year));
        writer.write(' ');
        write2Digits(writer, hour);
        writer.write(':');
        write2Digits(writer, minute);
        writer.write(':');
        write2Digits(writer, second);
    }

    private static void write2Digits(Writer writer, int val) throws IOException {
        if (val < 10) {
            writer.write('0');
        }
        writer.write(Integer.toString(val));
    }
}
