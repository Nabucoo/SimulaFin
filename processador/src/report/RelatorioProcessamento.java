package report;

import java.util.Map;
import java.util.TreeMap;
import service.Analises;
import service.DetectorTransacoesSuspeitas;
import service.EstatisticasCategoria;

public class RelatorioProcessamento {

    private static final int LARGURA = 78;
    private static final String BORDA = "=".repeat(LARGURA);
    private static final String SEPARADOR = "-".repeat(LARGURA);

    public static void exibirCabecalho(String arquivo, String natureza, int threads, boolean afinidade, int tamanhoLote) {
        System.out.println("\n" + BORDA);
        titulo("CONFIGURAÇÃO DO PROCESSAMENTO");
        System.out.println(SEPARADOR);
        System.out.printf("  %-24s %s%n", "Arquivo:", arquivo);
        System.out.printf("  %-24s %s%n", "Modo:", natureza.toUpperCase());
        System.out.printf("  %-24s %d%n", "Threads:", threads);
        System.out.printf("  %-24s %s%n", "Afinidade de CPU:", afinidade ? "Sim" : "Não");
        System.out.printf("  %-24s %,d linhas%n", "Tamanho do lote:", tamanhoLote);
        System.out.println(BORDA);
        System.out.println("Leitura e processamento iniciados...\n");
    }

    public static void exibirProgresso(int numeroLote, int linhasLote, String threadName, String coreInfo) {
        if (numeroLote == 1 || numeroLote % 10 == 0) {
            System.out.printf("  [LOTE %4d] %,d linhas processadas | thread: %s%s%n",
                    numeroLote, linhasLote, threadName, coreInfo);
        }
    }

    public static void exibirResumo(long totalLinhas, int totalLotes, long tempoTotalMs, Analises analise) {
        tempoTotalMs = Math.max(1, tempoTotalMs);
        double tempoSegundos = tempoTotalMs / 1000.0;
        double vazao = (totalLinhas * 1000.0) / tempoTotalMs;

        System.out.println("\n" + BORDA);
        titulo("PROCESSAMENTO CONCLUÍDO COM SUCESSO");
        System.out.println(BORDA);
        System.out.println("RESUMO DA EXECUÇÃO");
        System.out.println(SEPARADOR);
        System.out.printf("  %-30s %,d linhas%n", "Registros lidos:", totalLinhas);
        System.out.printf("  %-30s %,d lotes%n", "Lotes processados:", totalLotes);
        System.out.printf("  %-30s %,d ms (%.2f s)%n", "Tempo total:", tempoTotalMs, tempoSegundos);
        System.out.printf("  %-30s %,.2f linhas/segundo%n", "Vazão média:", vazao);

        if (analise != null) {
            secao("MÉTRICAS CONSOLIDADAS (ANÁLISES)");
            System.out.printf("  %-30s %,d%n", "Transações analisadas:", analise.getQuantidadeTransacoes());
            System.out.printf("  %-30s R$ %,.2f%n", "Soma total das transações:", analise.getSomaTransacoes());
            System.out.printf("  %-30s R$ %,.2f%n", "Valor médio:", analise.getMediaTransacoes());
            if (analise.getMaiorTransacao() != null) {
                System.out.printf("  Maior transação: ID %d | R$ %,.2f | Cliente: %s | Categoria: %s%n",
                        analise.getMaiorTransacao().getId(),
                        analise.getMaiorTransacao().getValor(),
                        analise.getMaiorTransacao().getCliente(),
                        analise.getMaiorTransacao().getCategoria());
            }
            if (analise.getMenorTransacao() != null) {
                System.out.printf("  Menor transação: ID %d | R$ %,.2f | Cliente: %s | Categoria: %s%n",
                        analise.getMenorTransacao().getId(),
                        analise.getMenorTransacao().getValor(),
                        analise.getMenorTransacao().getCliente(),
                        analise.getMenorTransacao().getCategoria());
            }

            if (!analise.getEstatisticasPorCategoria().isEmpty()) {
                secao("ESTATÍSTICAS POR CATEGORIA");
                for (Map.Entry<String, EstatisticasCategoria> entrada
                        : new TreeMap<>(analise.getEstatisticasPorCategoria()).entrySet()) {
                    String categoria = entrada.getKey();
                    EstatisticasCategoria estatisticas = entrada.getValue();

                    System.out.printf("\n  CATEGORIA: %s%n", categoria.toUpperCase());
                    System.out.printf("    %-28s %,d%n", "Transações:",
                            estatisticas.getQuantidadeTransacoesCategoria());
                    System.out.printf("    %-28s R$ %,.2f%n", "Soma:",
                            estatisticas.getSomaValorCategoria());
                    System.out.printf("    %-28s R$ %,.2f%n", "Média:",
                            estatisticas.getMediaTransacoesCategoria());

                    if (estatisticas.getMaiorTransacaoCategoria() != null) {
                        System.out.printf("    Maior transação: ID %d | R$ %,.2f | Cliente: %s%n",
                                estatisticas.getMaiorTransacaoCategoria().getId(),
                                estatisticas.getMaiorTransacaoCategoria().getValor(),
                                estatisticas.getMaiorTransacaoCategoria().getCliente());
                    }
                    if (estatisticas.getMenorTransacaoCategoria() != null) {
                        System.out.printf("    Menor transação: ID %d | R$ %,.2f | Cliente: %s%n",
                                estatisticas.getMenorTransacaoCategoria().getId(),
                                estatisticas.getMenorTransacaoCategoria().getValor(),
                                estatisticas.getMenorTransacaoCategoria().getCliente());
                    }
                }
            }

            secao("DETECÇÃO DE TRANSAÇÕES SUSPEITAS");
            System.out.println("  REGRAS APLICADAS");
            for (String regra : DetectorTransacoesSuspeitas.getDescricoesRegras()) {
                System.out.println("    • " + regra);
            }
            System.out.println();
            System.out.printf("  %-30s %,d%n", "Transações suspeitas únicas:",
                    analise.getQuantidadeTransacoesSuspeitas());
            if (analise.getAlertasPorRegra().isEmpty()) {
                System.out.println("  Nenhuma transação suspeita encontrada.");
            } else {
                System.out.println("\n  ALERTAS POR REGRA");
                System.out.println("  Uma transação pode corresponder a várias regras.");
                for (Map.Entry<String, Long> entrada : new TreeMap<>(analise.getAlertasPorRegra()).entrySet()) {
                    System.out.printf("    %-38s %,d alertas%n", entrada.getKey(), entrada.getValue());
                }
                System.out.println("\n  AMOSTRA (até 10 transações)");
                for (String amostra : analise.getAmostrasTransacoesSuspeitas()) {
                    System.out.println("  " + amostra);
                }
            }
        }
        System.out.println("\n" + BORDA);

        ExportadorResumoCsv.exportar(totalLinhas, totalLotes, tempoTotalMs, tempoSegundos, vazao, analise);
    }

    private static void secao(String nome) {
        System.out.println("\n" + SEPARADOR);
        System.out.println(nome);
        System.out.println(SEPARADOR);
    }

    private static void titulo(String nome) {
        int espacos = Math.max(0, (LARGURA - nome.length()) / 2);
        System.out.println(" ".repeat(espacos) + nome);
    }
}
