package report;

import java.util.Map;
import java.util.TreeMap;
import service.Analises;
import service.EstatisticasCategoria;

public class RelatorioProcessamento {

    public static void exibirCabecalho(String arquivo, String natureza, int threads, boolean afinidade, int tamanhoLote) {
        System.out.println("==================================================");
        System.out.println("Iniciando leitura do arquivo: " + arquivo);
        System.out.println("Modo: " + natureza.toUpperCase() + " | Threads: " + threads + " | Afinidade: " + afinidade);
        System.out.println("Tamanho do lote: " + tamanhoLote + " linhas");
        System.out.println("==================================================");
    }

    public static void exibirProgresso(int numeroLote, int linhasLote, String threadName, String coreInfo) {
        if (numeroLote == 1 || numeroLote % 10 == 0) {
            System.out.printf("-> Lote %4d processado (%d linhas) pela %s%s%n",
                    numeroLote, linhasLote, threadName, coreInfo);
        }
    }

    public static void exibirResumo(long totalLinhas, int totalLotes, long tempoTotalMs, Analises analise) {
        tempoTotalMs = Math.max(1, tempoTotalMs);
        double tempoSegundos = tempoTotalMs / 1000.0;
        double vazao = (totalLinhas * 1000.0) / tempoTotalMs;

        System.out.println("==================================================");
        System.out.println("PROCESSAMENTO CONCLUÍDO COM SUCESSO");
        System.out.println("--------------------------------------------------");
        System.out.printf("Total de registros lidos: %,d linhas%n", totalLinhas);
        System.out.printf("Total de lotes gerados:   %,d lotes%n", totalLotes);
        System.out.printf("Tempo total de execução:  %,d ms (%.2f s)%n", tempoTotalMs, tempoSegundos);
        System.out.printf("Vazão média:              %,.2f linhas/segundo%n", vazao);

        if (analise != null) {
            System.out.println("--------------------------------------------------");
            System.out.println("MÉTRICAS CONSOLIDADAS (ANALISES)");
            System.out.println("--------------------------------------------------");
            System.out.printf("Transações analisadas:    %,d%n", analise.getQuantidadeTransacoes());
            System.out.printf("Soma total das transações: R$ %,.2f%n", analise.getSomaTransacoes());
            System.out.printf("Valor médio:              R$ %,.2f%n", analise.getMediaTransacoes());
            if (analise.getMaiorTransacao() != null) {
                System.out.printf("Maior transação:          ID %d | R$ %,.2f | %s | %s%n",
                        analise.getMaiorTransacao().getId(),
                        analise.getMaiorTransacao().getValor(),
                        analise.getMaiorTransacao().getCliente(),
                        analise.getMaiorTransacao().getCategoria());
            }
            if (analise.getMenorTransacao() != null) {
                System.out.printf("Menor transação:          ID %d | R$ %,.2f | %s | %s%n",
                        analise.getMenorTransacao().getId(),
                        analise.getMenorTransacao().getValor(),
                        analise.getMenorTransacao().getCliente(),
                        analise.getMenorTransacao().getCategoria());
            }

            if (!analise.getEstatisticasPorCategoria().isEmpty()) {
                System.out.println("--------------------------------------------------");
                System.out.println("ESTATÍSTICAS POR CATEGORIA");
                System.out.println("--------------------------------------------------");
                for (Map.Entry<String, EstatisticasCategoria> entrada
                        : new TreeMap<>(analise.getEstatisticasPorCategoria()).entrySet()) {
                    String categoria = entrada.getKey();
                    EstatisticasCategoria estatisticas = entrada.getValue();

                    System.out.printf("Categoria: %s%n", categoria);
                    System.out.printf("  Transações:             %,d%n",
                            estatisticas.getQuantidadeTransacoesCategoria());
                    System.out.printf("  Soma:                   R$ %,.2f%n",
                            estatisticas.getSomaValorCategoria());
                    System.out.printf("  Média:                  R$ %,.2f%n",
                            estatisticas.getMediaTransacoesCategoria());

                    if (estatisticas.getMaiorTransacaoCategoria() != null) {
                        System.out.printf("  Maior transação:        ID %d | R$ %,.2f | %s%n",
                                estatisticas.getMaiorTransacaoCategoria().getId(),
                                estatisticas.getMaiorTransacaoCategoria().getValor(),
                                estatisticas.getMaiorTransacaoCategoria().getCliente());
                    }
                    if (estatisticas.getMenorTransacaoCategoria() != null) {
                        System.out.printf("  Menor transação:        ID %d | R$ %,.2f | %s%n",
                                estatisticas.getMenorTransacaoCategoria().getId(),
                                estatisticas.getMenorTransacaoCategoria().getValor(),
                                estatisticas.getMenorTransacaoCategoria().getCliente());
                    }
                }
            }

            System.out.println("--------------------------------------------------");
            System.out.println("DETECÇÃO DE TRANSAÇÕES SUSPEITAS");
            System.out.println("--------------------------------------------------");
            System.out.printf("Transações suspeitas únicas: %,d%n",
                    analise.getQuantidadeTransacoesSuspeitas());
            if (analise.getAlertasPorRegra().isEmpty()) {
                System.out.println("Nenhuma transação suspeita encontrada.");
            } else {
                System.out.println("Alertas por regra (uma transação pode corresponder a várias regras):");
                for (Map.Entry<String, Long> entrada : new TreeMap<>(analise.getAlertasPorRegra()).entrySet()) {
                    System.out.printf("  %s: %,d%n", entrada.getKey(), entrada.getValue());
                }
                System.out.println("Amostra (até 10 transações):");
                for (String amostra : analise.getAmostrasTransacoesSuspeitas()) {
                    System.out.println("  " + amostra);
                }
            }
        }
        System.out.println("==================================================");
    }
}
