package service;

import model.Transaction;

public class EstatisticasCategoria {
    private long quantidadeTransacoesCategoria;
    private double somaValorCategoria;
    private Transaction maiorTransacaoCategoria;
    private Transaction menorTransacaoCategoria;

    public EstatisticasCategoria() {

    }

    public void startAnalise(Transaction t) {
        if (t == null) {
            return;
        }

        this.somaValorCategoria += t.getValor();
        this.quantidadeTransacoesCategoria++;

        if (this.maiorTransacaoCategoria == null || this.maiorTransacaoCategoria.getValor() < t.getValor()) {
            this.maiorTransacaoCategoria = t;
        }

        if (this.menorTransacaoCategoria == null || this.menorTransacaoCategoria.getValor() > t.getValor()) {
            this.menorTransacaoCategoria = t;
        }

    }

    public void combinar(EstatisticasCategoria outro) {
        if (outro == null || outro.quantidadeTransacoesCategoria == 0) {
            return;
        }

        this.somaValorCategoria += outro.getSomaValorCategoria();
        this.quantidadeTransacoesCategoria += outro.getQuantidadeTransacoesCategoria();

        if (this.maiorTransacaoCategoria == null ||
            (outro.maiorTransacaoCategoria != null && outro.maiorTransacaoCategoria.getValor() > this.maiorTransacaoCategoria.getValor())) {
                this.maiorTransacaoCategoria = outro.maiorTransacaoCategoria;
        }

        if (this.menorTransacaoCategoria == null ||
            (outro.menorTransacaoCategoria != null && outro.menorTransacaoCategoria.getValor() < this.menorTransacaoCategoria.getValor())) {
                this.menorTransacaoCategoria = outro.menorTransacaoCategoria;
        }
    }

    public long getQuantidadeTransacoesCategoria() {
        return this.quantidadeTransacoesCategoria;
    }

    public double getSomaValorCategoria() {
        return this.somaValorCategoria;
    }

    public double getMediaTransacoesCategoria() {
        if (quantidadeTransacoesCategoria == 0) {
            return 0.0;
        }
        return somaValorCategoria / quantidadeTransacoesCategoria;
    }

    public Transaction getMaiorTransacaoCategoria() {
        return this.maiorTransacaoCategoria;
    }

    public Transaction getMenorTransacaoCategoria() {
        return this.menorTransacaoCategoria;
    }

}
