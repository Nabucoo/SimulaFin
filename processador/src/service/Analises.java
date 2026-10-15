package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Transaction;

public class Analises {

    private static final int LIMITE_AMOSTRAS_SUSPEITAS = 10;

    private double somaTransacoes;
    private long quantidadeTransacoes;
    private long quantidadeTransacoesSuspeitas;
    private Transaction maiorTransacao;
    private Transaction menorTransacao;

    private final Map<String, EstatisticasCategoria> estatisticasCategoria = new HashMap<>();
    private final Map<String, Long> alertasPorRegra = new HashMap<>();
    private final List<String> amostrasTransacoesSuspeitas = new ArrayList<>();

    public Analises() {
    }

    public void startAnalise(Transaction t) {
        if (t == null) {
            return;
        }

        somaTransacoes += t.getValor();
        quantidadeTransacoes++;

        if (maiorTransacao == null || t.getValor() > maiorTransacao.getValor()) {
            maiorTransacao = t;
        }

        if (menorTransacao == null || t.getValor() < menorTransacao.getValor()) {
            menorTransacao = t;
        }

        EstatisticasCategoria estatistica = estatisticasCategoria.computeIfAbsent(t.getCategoria(), 
        k -> new EstatisticasCategoria());

        estatistica.startAnalise(t);

        List<String> motivos = DetectorTransacoesSuspeitas.detectar(t);
        if (!motivos.isEmpty()) {
            quantidadeTransacoesSuspeitas++;
            for (String motivo : motivos) {
                alertasPorRegra.merge(motivo, 1L, Long::sum);
            }
            if (amostrasTransacoesSuspeitas.size() < LIMITE_AMOSTRAS_SUSPEITAS) {
                amostrasTransacoesSuspeitas.add(String.format(
                        "ID %d | R$ %.2f | %s | %s | %s",
                        t.getId(), t.getValor(), t.getData(), t.getCategoria(), String.join("; ", motivos)));
            }
        }
    }

    public void combinar(Analises outro) {
        if (outro == null || outro.quantidadeTransacoes == 0) {
            return;
        }

        this.somaTransacoes += outro.somaTransacoes;
        this.quantidadeTransacoes += outro.quantidadeTransacoes;
        this.quantidadeTransacoesSuspeitas += outro.quantidadeTransacoesSuspeitas;

        if (this.maiorTransacao == null || 
            (outro.maiorTransacao != null && outro.maiorTransacao.getValor() > this.maiorTransacao.getValor())) {
            this.maiorTransacao = outro.maiorTransacao;
        }

        if (this.menorTransacao == null || 
            (outro.menorTransacao != null && outro.menorTransacao.getValor() < this.menorTransacao.getValor())) {
            this.menorTransacao = outro.menorTransacao;
        }
    
        for (Map.Entry<String, EstatisticasCategoria> entrada : outro.estatisticasCategoria.entrySet()) {
            this.estatisticasCategoria
                    .computeIfAbsent(entrada.getKey(), categoria -> new EstatisticasCategoria())
                    .combinar(entrada.getValue());
        }

        for (Map.Entry<String, Long> entrada : outro.alertasPorRegra.entrySet()) {
            this.alertasPorRegra.merge(entrada.getKey(), entrada.getValue(), Long::sum);
        }
        for (String amostra : outro.amostrasTransacoesSuspeitas) {
            if (amostrasTransacoesSuspeitas.size() == LIMITE_AMOSTRAS_SUSPEITAS) {
                break;
            }
            amostrasTransacoesSuspeitas.add(amostra);
        }
    }

    public double getSomaTransacoes() {
        return somaTransacoes;
    }

    public long getQuantidadeTransacoes() {
        return quantidadeTransacoes;
    }

    public long getQuantidadeTransacoesSuspeitas() {
        return quantidadeTransacoesSuspeitas;
    }

    public Map<String, Long> getAlertasPorRegra() {
        return Collections.unmodifiableMap(alertasPorRegra);
    }

    public List<String> getAmostrasTransacoesSuspeitas() {
        return Collections.unmodifiableList(amostrasTransacoesSuspeitas);
    }

    public double getMediaTransacoes() {
        if (quantidadeTransacoes == 0) {
            return 0.0;
        }
        return somaTransacoes / quantidadeTransacoes;
    }

    public Transaction getMaiorTransacao() {
        return maiorTransacao;
    }

    public Transaction getMenorTransacao() {
        return menorTransacao;
    }

    public Map<String, EstatisticasCategoria> getEstatisticasPorCategoria() {
        return Collections.unmodifiableMap(estatisticasCategoria);
    }
}
