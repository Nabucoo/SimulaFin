import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import io.LeitorCsv;

public class Benchmark {

    // Configurações do experimento
    private static final String ARQUIVO_CSV = "data/transacoes_50M.csv";
    private static final int REPETICOES = 3; // Executa 3 vezes cada cenário e tira a média
    private static final int[] N_THREADS = {2, 4, 6, 8};

    static class Resultado {
        String modo;
        int threads;
        boolean afinidade;
        double tempoMedioMs;
        double speedup;
        double eficiencia;

        public Resultado(String modo, int threads, boolean afinidade, double tempoMedioMs) {
            this.modo = modo;
            this.threads = threads;
            this.afinidade = afinidade;
            this.tempoMedioMs = tempoMedioMs;
        }
    }

    public static void main(String[] args) {
        String arquivo = args.length > 0 ? args[0] : ARQUIVO_CSV;
        System.out.println("==========================================================");
        System.out.println("    INICIANDO MÓDULO DE EXPERIMENTAÇÃO E BENCHMARK        ");
        System.out.println("    Arquivo: " + arquivo);
        System.out.println("    Repetições por configuração: " + REPETICOES);
        System.out.println("==========================================================\n");

        List<Resultado> resultados = new ArrayList<>();

        // 1. MEDIR TEMPO SEQUENCIAL (Base para Speedup e Eficiência)
        System.out.println(">>> Executando modo SEQUENCIAL...");
        double tempoSequencial = executarCenario(arquivo, "sequencial", 1, false);
        Resultado resSeq = new Resultado("Sequencial", 1, false, tempoSequencial);
        resSeq.speedup = 1.0;
        resSeq.eficiencia = 100.0;
        resultados.add(resSeq);

        // 2. MEDIR PARALELO SEM AFINIDADE (2, 4, 6, 8 threads)
        for (int th : N_THREADS) {
            System.out.println(">>> Executando PARALELO SEM AFINIDADE (" + th + " threads)...");
            double tempo = executarCenario(arquivo, "paralelo", th, false);
            Resultado r = new Resultado("Paralelo", th, false, tempo);
            r.speedup = tempoSequencial / tempo;
            r.eficiencia = (r.speedup / th) * 100.0;
            resultados.add(r);
        }

        // 3. MEDIR PARALELO COM AFINIDADE (2, 4, 6, 8 threads)
        for (int th : N_THREADS) {
            System.out.println(">>> Executando PARALELO COM AFINIDADE (" + th + " threads)...");
            double tempo = executarCenario(arquivo, "paralelo", th, true);
            Resultado r = new Resultado("Com Afinidade", th, true, tempo);
            r.speedup = tempoSequencial / tempo;
            r.eficiencia = (r.speedup / th) * 100.0;
            resultados.add(r);
        }

        // 4. EXIBIR TABELA FORMATADA NO TERMINAL
        imprimirTabela(resultados);

        // 5. EXPORTAR CSV PARA GERAÇÃO DOS GRÁFICOS
        exportarCsv(resultados, "benchmark_resultados.csv");
    }

    private static double executarCenario(String arquivo, String modo, int threads, boolean afinidade) {
        long somaTempos = 0;

        for (int i = 1; i <= REPETICOES; i++) {
            System.gc(); // Sugere limpeza de memória antes de cada bateria
            long inicio = System.currentTimeMillis();
            try {
                LeitorCsv leitor = new LeitorCsv(arquivo, modo, threads, afinidade);
                leitor.ler();
            } catch (Exception e) {
                System.err.println("Erro durante a execução: " + e.getMessage());
            }
            long duracao = System.currentTimeMillis() - inicio;
            somaTempos += duracao;
            System.out.printf("   Repetição %d/%d: %,d ms\n", i, REPETICOES, duracao);
        }

        double media = (double) somaTempos / REPETICOES;
        System.out.printf("   -> Tempo Médio: %,.2f ms (%.2f s)\n\n", media, media / 1000.0);
        return media;
    }

    private static void imprimirTabela(List<Resultado> resultados) {
        System.out.println("\n=========================================================================================");
        System.out.println("                               TABELA DE RESULTADOS DO BENCHMARK                          ");
        System.out.println("=========================================================================================");
        System.out.printf("%-15s | %-7s | %-12s | %-15s | %-10s | %-12s\n", 
                          "Modo", "Threads", "Afinidade", "Tempo Médio (ms)", "Speedup", "Eficiência (%)");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (Resultado r : resultados) {
            System.out.printf("%-15s | %-7d | %-12s | %-15.2f | %-10.2fx | %-11.2f%%\n",
                    r.modo, r.threads, r.afinidade ? "Sim" : "Não", r.tempoMedioMs, r.speedup, r.eficiencia);
        }
        System.out.println("=========================================================================================\n");
    }

    private static void exportarCsv(List<Resultado> resultados, String nomeArquivo) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(nomeArquivo))) {
            writer.println("modo,threads,afinidade,tempo_ms,speedup,eficiencia_percent");
            for (Resultado r : resultados) {
                writer.printf("%s,%d,%s,%.2f,%.4f,%.2f\n",
                        r.modo, r.threads, r.afinidade, r.tempoMedioMs, r.speedup, r.eficiencia);
            }
            System.out.println("Resultados exportados para o arquivo: " + nomeArquivo);
        } catch (Exception e) {
            System.err.println("Erro ao exportar CSV: " + e.getMessage());
        }
    }
}