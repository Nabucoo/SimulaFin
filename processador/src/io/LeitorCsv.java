package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import net.openhft.affinity.Affinity;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityThreadFactory;
import report.RelatorioProcessamento;
import service.Analises;
import service.ProcessadorLote;

/**
 * Orquestrador de leitura de CSV em lotes com suporte a paralelismo,
 * controle de contrapressão por semáforo e afinidade de CPU (OpenHFT).
 */
public class LeitorCsv {

    private static final int TAMANHO_LOTE = 10_000;

    private String arquivo;
    private final String natureza;
    private final int threads;
    private final boolean afinidade;

    public LeitorCsv(String arquivo, String natureza, int threads, boolean afinidade) {
        this.arquivo = arquivo;
        this.natureza = natureza;
        this.threads = threads;
        this.afinidade = afinidade;
    }

    public void ler() throws IOException {
        File file = localizarArquivo(arquivo);
        if (file == null) {
            throw new IOException("Arquivo não encontrado: " + arquivo
                    + ". Informe o caminho completo ou deixe o CSV em uma pasta data próxima ao projeto.");
        }
        arquivo = file.getAbsolutePath();

        RelatorioProcessamento.exibirCabecalho(arquivo, natureza, threads, afinidade, TAMANHO_LOTE);

        if (natureza.equalsIgnoreCase("sequencial") || threads <= 1) {
            lerSequencial();
        } else {
            lerParalelo();
        }
    }

    private File localizarArquivo(String caminho) {
        Path informado = Paths.get(caminho).toAbsolutePath().normalize();
        if (Files.isRegularFile(informado)) {
            return informado.toFile();
        }

        Path nomeArquivo = Paths.get(caminho).getFileName();
        if (nomeArquivo == null) {
            return null;
        }

        Path diretorio = Paths.get("").toAbsolutePath().normalize();
        while (diretorio != null) {
            Path candidatoData = diretorio.resolve("data").resolve(nomeArquivo);
            if (Files.isRegularFile(candidatoData)) {
                return candidatoData.toFile();
            }

            Path candidatoDireto = diretorio.resolve(nomeArquivo);
            if (Files.isRegularFile(candidatoDireto)) {
                return candidatoDireto.toFile();
            }
            diretorio = diretorio.getParent();
        }
        return null;
    }

    private void lerSequencial() throws IOException {
        long inicio = System.currentTimeMillis();
        long totalLinhas = 0;
        int totalLotes = 0;
        Analises analiseGlobal = new Analises();

        try (BufferedReader reader = new BufferedReader(new FileReader(arquivo), 1024 * 1024)) {
            if (reader.readLine() == null) return; // descarta cabeçalho

            List<String> lote = new ArrayList<>(TAMANHO_LOTE);
            String linha;
            while ((linha = reader.readLine()) != null) {
                lote.add(linha);
                if (lote.size() == TAMANHO_LOTE) {
                    totalLotes++;
                    ProcessadorLote.processar(lote, analiseGlobal);
                    totalLinhas += lote.size();
                    RelatorioProcessamento.exibirProgresso(totalLotes, lote.size(), "main", "");
                    lote = new ArrayList<>(TAMANHO_LOTE);
                }
            }

            if (!lote.isEmpty()) {
                totalLotes++;
                ProcessadorLote.processar(lote, analiseGlobal);
                totalLinhas += lote.size();
                RelatorioProcessamento.exibirProgresso(totalLotes, lote.size(), "main", "");
            }
        }

        RelatorioProcessamento.exibirResumo(totalLinhas, totalLotes, System.currentTimeMillis() - inicio, analiseGlobal);
    }

    private void lerParalelo() throws IOException {
        long inicio = System.currentTimeMillis();
        LongAdder totalLinhas = new LongAdder();
        AtomicInteger lotesProcessados = new AtomicInteger(0);
        Queue<Analises> analisesParciais = new ConcurrentLinkedQueue<>();

        ThreadFactory threadFactory = afinidade
                ? new AffinityThreadFactory("worker-affinity", AffinityStrategies.DIFFERENT_CORE)
                : new ThreadFactory() {
                    private final AtomicInteger id = new AtomicInteger(1);
                    public Thread newThread(Runnable r) { return new Thread(r, "worker-" + id.getAndIncrement()); }
                };

        ExecutorService executor = Executors.newFixedThreadPool(this.threads, threadFactory);
        Semaphore semaphore = new Semaphore(this.threads);
        int totalLotesLidos = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(arquivo), 1024 * 1024)) {
            if (reader.readLine() == null) {
                executor.shutdown();
                return;
            }

            while (true) {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Leitura interrompida", e);
                }

                List<String> lote = new ArrayList<>(TAMANHO_LOTE);
                String linha;
                while (lote.size() < TAMANHO_LOTE && (linha = reader.readLine()) != null) {
                    lote.add(linha);
                }

                if (lote.isEmpty()) {
                    semaphore.release();
                    break;
                }

                totalLotesLidos++;
                final int numLote = totalLotesLidos;
                final List<String> loteFinal = lote;

                executor.submit(() -> {
                    try {
                        Analises analiseLote = new Analises();
                        ProcessadorLote.processar(loteFinal, analiseLote);
                        analisesParciais.add(analiseLote);
                        totalLinhas.add(loteFinal.size());
                        lotesProcessados.incrementAndGet();

                        String coreInfo = afinidade ? " [CPU Core: " + Affinity.getCpu() + "]" : "";
                        RelatorioProcessamento.exibirProgresso(numLote, loteFinal.size(), Thread.currentThread().getName(), coreInfo);
                    } finally {
                        semaphore.release();
                    }
                });
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.HOURS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        Analises analiseFinal = new Analises();
        for (Analises parcial : analisesParciais) analiseFinal.combinar(parcial);

        RelatorioProcessamento.exibirResumo(totalLinhas.sum(), lotesProcessados.get(), System.currentTimeMillis() - inicio, analiseFinal);
    }
}
