package report;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import model.Transaction;
import service.Analises;
import service.EstatisticasCategoria;

public final class ExportadorResumoCsv {

    private static final String ARQUIVO_RESUMO_CSV = "relatorio_processamento.csv";

    private ExportadorResumoCsv() {
    }

    public static void exportar(long totalLinhas, int totalLotes, long tempoTotalMs,
            double tempoSegundos, double vazao, Analises analise) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(ARQUIVO_RESUMO_CSV), StandardCharsets.UTF_8)) {
            escreverLinhaCsv(writer, "secao", "metrica", "categoria", "quantidade", "valor",
                    "transacao_id", "cliente", "data", "detalhes");

            escreverLinhaCsv(writer, "execucao", "registros_lidos", "", totalLinhas, "", "", "", "", "linhas");
            escreverLinhaCsv(writer, "execucao", "lotes_gerados", "", totalLotes, "", "", "", "", "lotes");
            escreverLinhaCsv(writer, "execucao", "tempo_total_ms", "", "", tempoTotalMs, "", "", "", "milissegundos");
            escreverLinhaCsv(writer, "execucao", "tempo_total_segundos", "", "", tempoSegundos, "", "", "", "segundos");
            escreverLinhaCsv(writer, "execucao", "vazao_media", "", "", vazao, "", "", "", "linhas_por_segundo");

            if (analise != null) {
                escreverLinhaCsv(writer, "analise", "transacoes_analisadas", "",
                        analise.getQuantidadeTransacoes(), "", "", "", "", "transacoes");
                escreverLinhaCsv(writer, "analise", "soma_total_transacoes", "", "",
                        analise.getSomaTransacoes(), "", "", "", "reais");
                escreverLinhaCsv(writer, "analise", "valor_medio", "", "",
                        analise.getMediaTransacoes(), "", "", "", "reais");
                escreverLinhaCsv(writer, "analise", "transacoes_suspeitas_unicas", "",
                        analise.getQuantidadeTransacoesSuspeitas(), "", "", "", "", "transacoes");

                escreverTransacaoCsv(writer, "analise", "maior_transacao", analise.getMaiorTransacao());
                escreverTransacaoCsv(writer, "analise", "menor_transacao", analise.getMenorTransacao());

                for (Map.Entry<String, EstatisticasCategoria> entrada
                        : new TreeMap<>(analise.getEstatisticasPorCategoria()).entrySet()) {
                    String categoria = entrada.getKey();
                    EstatisticasCategoria estatisticas = entrada.getValue();
                    escreverLinhaCsv(writer, "categoria", "transacoes", categoria,
                            estatisticas.getQuantidadeTransacoesCategoria(), "", "", "", "", "transacoes");
                    escreverLinhaCsv(writer, "categoria", "soma_transacoes", categoria, "",
                            estatisticas.getSomaValorCategoria(), "", "", "", "reais");
                    escreverLinhaCsv(writer, "categoria", "valor_medio", categoria, "",
                            estatisticas.getMediaTransacoesCategoria(), "", "", "", "reais");
                    escreverTransacaoCategoriaCsv(writer, "maior_transacao", categoria,
                            estatisticas.getMaiorTransacaoCategoria());
                    escreverTransacaoCategoriaCsv(writer, "menor_transacao", categoria,
                            estatisticas.getMenorTransacaoCategoria());
                }

                for (Map.Entry<String, Long> entrada
                        : new TreeMap<>(analise.getTransacoesPorCidade()).entrySet()) {
                    escreverLinhaCsv(writer, "cidade", "transacoes_por_cidade", "",
                            entrada.getValue(), "", "", "", "", entrada.getKey());
                }

                for (String mes : Analises.MESES_DO_ANO) {
                    escreverLinhaCsv(writer, "mes", "transacoes_por_mes", "",
                            analise.getTransacoesPorMes().getOrDefault(mes, 0L), "", "", "", "", mes);
                }

                if (analise.getAlertasPorRegra().isEmpty()) {
                    escreverLinhaCsv(writer, "alerta", "nenhuma_transacao_suspeita", "",
                            0, "", "", "", "", "Nenhuma transacao suspeita encontrada");
                } else {
                    for (Map.Entry<String, Long> entrada
                            : new TreeMap<>(analise.getAlertasPorRegra()).entrySet()) {
                        escreverLinhaCsv(writer, "alerta", "contagem_por_regra", "",
                                entrada.getValue(), "", "", "", "", entrada.getKey());
                    }
                    for (String amostra : analise.getAmostrasTransacoesSuspeitas()) {
                        escreverLinhaCsv(writer, "alerta", "amostra_transacao_suspeita", "",
                                "", "", "", "", "", amostra);
                    }
                }
            }
            System.out.println("Resumo exportado para o arquivo: " + ARQUIVO_RESUMO_CSV);
        } catch (IOException e) {
            System.err.println("Erro ao exportar o resumo CSV: " + e.getMessage());
        }
    }

    private static void escreverTransacaoCsv(BufferedWriter writer, String secao,
            String metrica, Transaction transacao) throws IOException {
        if (transacao != null) {
            escreverLinhaCsv(writer, secao, metrica, transacao.getCategoria(), "",
                    transacao.getValor(), transacao.getId(), transacao.getCliente(),
                    transacao.getData(), "reais");
        }
    }

    private static void escreverTransacaoCategoriaCsv(BufferedWriter writer, String metrica,
            String categoria, Transaction transacao) throws IOException {
        if (transacao != null) {
            escreverLinhaCsv(writer, "categoria", metrica, categoria, "", transacao.getValor(),
                    transacao.getId(), transacao.getCliente(), transacao.getData(), "reais");
        }
    }

    private static void escreverLinhaCsv(BufferedWriter writer, Object... valores) throws IOException {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escaparCsv(valores[i] == null ? "" : valores[i].toString()));
        }
        writer.newLine();
    }

    private static String escaparCsv(String valor) {
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }
}
