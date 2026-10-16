import io.LeitorCsv;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class Benchmark {

    private static final String ARQUIVO_CSV = "data/transacoes_exponencial_decrescente_50m.csv";
    private static final int REPETICOES = 3;
    private static final int[] N_THREADS = {2, 4, 6, 8};

    static class Resultado {
        final String modo;
        final int threads;
        final boolean afinidade;
        final List<Double> temposMs = new ArrayList<>();
        double tempoMedioMs;
        double speedup;
        double eficiencia;

        Resultado(String modo, int threads, boolean afinidade) {
            this.modo = modo;
            this.threads = threads;
            this.afinidade = afinidade;
        }

        void adicionarTempo(double tempoMs) {
            temposMs.add(tempoMs);
        }

        void calcularMedia() {
            double soma = 0;
            for (double tempo : temposMs) {
                soma += tempo;
            }
            tempoMedioMs = soma / temposMs.size();
        }
    }

    public static void main(String[] args) throws IOException {
        String arquivo = args.length > 0 ? args[0] : ARQUIVO_CSV;
        System.out.println("==========================================================");
        System.out.println("              BENCHMARK DO PROCESSADOR                   ");
        System.out.println("  Arquivo: " + arquivo);
        System.out.println("  Aquecimento: não");
        System.out.println("  Rodadas: " + REPETICOES + " por cenário");
        System.out.println("  Ordem: threads crescentes; afinidade alternada");
        System.out.println("  Métrica: média do tempo de leitura e processamento");
        System.out.println("==========================================================\n");

        Map<String, Resultado> porCenario = criarResultados();
        for (int repeticao = 0; repeticao < REPETICOES; repeticao++) {
            int numeroRepeticao = repeticao + 1;
            medir(porCenario.get(chave("Sequencial", 1, false)), arquivo,
                    numeroRepeticao, REPETICOES);

            for (int threads : N_THREADS) {
                boolean afinidadePrimeiro = repeticao % 2 != 0;
                medirCenario(porCenario, arquivo, threads, afinidadePrimeiro,
                        numeroRepeticao);
                medirCenario(porCenario, arquivo, threads, !afinidadePrimeiro,
                        numeroRepeticao);
            }
        }

        List<Resultado> resultados = ordenarResultados(porCenario);
        for (Resultado resultado : resultados) {
            resultado.calcularMedia();
        }

        double tempoSequencial = resultados.get(0).tempoMedioMs;
        for (int i = 1; i < resultados.size(); i++) {
            Resultado resultado = resultados.get(i);
            resultado.speedup = tempoSequencial / resultado.tempoMedioMs;
            resultado.eficiencia = (resultado.speedup / resultado.threads) * 100.0;
        }
        resultados.get(0).speedup = 1.0;
        resultados.get(0).eficiencia = 100.0;

        imprimirTabela(resultados);
        exportarCsv(resultados, "benchmark_resultados.csv");
    }

    private static Map<String, Resultado> criarResultados() {
        Map<String, Resultado> resultados = new LinkedHashMap<>();
        resultados.put(chave("Sequencial", 1, false), new Resultado("Sequencial", 1, false));
        for (int threads : N_THREADS) {
            resultados.put(chave("Paralelo", threads, false), new Resultado("Paralelo", threads, false));
        }
        for (int threads : N_THREADS) {
            resultados.put(chave("Com Afinidade", threads, true), new Resultado("Com Afinidade", threads, true));
        }
        return resultados;
    }

    private static List<Resultado> ordenarResultados(Map<String, Resultado> porCenario) {
        List<Resultado> resultados = new ArrayList<>();
        resultados.add(porCenario.get(chave("Sequencial", 1, false)));
        for (int threads : N_THREADS) {
            resultados.add(porCenario.get(chave("Paralelo", threads, false)));
        }
        for (int threads : N_THREADS) {
            resultados.add(porCenario.get(chave("Com Afinidade", threads, true)));
        }
        return resultados;
    }

    private static void medirCenario(Map<String, Resultado> resultados, String arquivo,
            int threads, boolean afinidade, int repeticao) throws IOException {
        String modo = nomeModo(afinidade);
        medir(resultados.get(chave(modo, threads, afinidade)), arquivo, repeticao, REPETICOES);
    }

    private static void medir(Resultado resultado, String arquivo, int repeticao, int totalRepeticoes)
            throws IOException {
        System.gc();
        double duracaoMs = new LeitorCsv(arquivo, modoLeitor(resultado), resultado.threads,
                resultado.afinidade).lerParaBenchmark();
        if (duracaoMs <= 0) {
            throw new IOException("A medição retornou tempo zero; verifique se o CSV contém registros.");
        }
        resultado.adicionarTempo(duracaoMs);
        System.out.printf("  %s | %d thread(s) | afinidade %s | repetição %d/%d: %,.2f ms%n",
                resultado.modo, resultado.threads, resultado.afinidade ? "sim" : "não",
                repeticao, totalRepeticoes, duracaoMs);
    }

    private static String modoLeitor(Resultado resultado) {
        return resultado.threads == 1 ? "sequencial" : "paralelo";
    }

    private static String nomeModo(boolean afinidade) {
        return afinidade ? "Com Afinidade" : "Paralelo";
    }

    private static String chave(String modo, int threads, boolean afinidade) {
        return modo + "|" + threads + "|" + afinidade;
    }

    private static void imprimirTabela(List<Resultado> resultados) {
        System.out.println("\n=========================================================================================");
        System.out.println("                               RESULTADOS DO BENCHMARK                                   ");
        System.out.println("=========================================================================================");
        System.out.printf("%-15s | %-7s | %-12s | %-16s | %-10s | %-12s%n",
                "Modo", "Threads", "Afinidade", "Média (ms)", "Speedup", "Eficiência (%)");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (Resultado resultado : resultados) {
            System.out.printf("%-15s | %-7d | %-12s | %-16.2f | %-10.2fx | %-11.2f%%%n",
                    resultado.modo, resultado.threads, resultado.afinidade ? "Sim" : "Não",
                    resultado.tempoMedioMs, resultado.speedup, resultado.eficiencia);
        }
        System.out.println("=========================================================================================\n");
    }

    private static void exportarCsv(List<Resultado> resultados, String nomeArquivo) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(nomeArquivo))) {
            writer.println("modo,threads,afinidade,tempo_ms,speedup,eficiencia_percent");
            for (Resultado resultado : resultados) {
                writer.printf(Locale.US, "%s,%d,%s,%.2f,%.4f,%.2f%n",
                        resultado.modo, resultado.threads, resultado.afinidade,
                        resultado.tempoMedioMs, resultado.speedup, resultado.eficiencia);
            }
            System.out.println("Médias exportadas para o arquivo: " + nomeArquivo);
        } catch (IOException e) {
            System.err.println("Erro ao exportar CSV: " + e.getMessage());
        }
    }
}
