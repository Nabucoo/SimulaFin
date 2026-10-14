package service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import model.Transaction;

public class Analises {

    private double somaTransacoes;
    private long quantidadeTransacoes;
    private Transaction maiorTransacao;
    private Transaction menorTransacao;

    private final Map<String, EstatisticasCategoria> estatisticasCategoria = new HashMap<>();

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
    }

    public void combinar(Analises outro) {
        if (outro == null || outro.quantidadeTransacoes == 0) {
            return;
        }

        this.somaTransacoes += outro.somaTransacoes;
        this.quantidadeTransacoes += outro.quantidadeTransacoes;

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
    }

    public double getSomaTransacoes() {
        return somaTransacoes;
    }

    public long getQuantidadeTransacoes() {
        return quantidadeTransacoes;
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
