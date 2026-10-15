package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.Transaction;

public class DetectorTransacoesSuspeitas {

    private static final double LIMITE_VALOR_MUITO_ALTO = 15_000.0;
    private static final double LIMITE_ELETRONICOS = 8_000.0;
    private static final double LIMITE_SAUDE_EDUCACAO = 2_000.0;
    private static final double LIMITE_MADRUGADA = 1_000.0;

    private static final String VALOR_MUITO_ALTO = "Valor muito alto (R$ 15.000 ou mais)";
    private static final String ELETRONICOS = "Valor alto em Eletrônicos (acima de R$ 8.000)";
    private static final String SAUDE_EDUCACAO = "Valor alto em Saúde/Educação (acima de R$ 2.000)";
    private static final String MADRUGADA = "Valor alto na madrugada (00:00–05:59, acima de R$ 1.000)";

    private DetectorTransacoesSuspeitas() {
    }

    public static List<String> detectar(Transaction transacao) {
        List<String> motivos = null;
        double valor = transacao.getValor();
        String categoria = transacao.getCategoria();

        if (valor >= LIMITE_VALOR_MUITO_ALTO) {
            motivos = new ArrayList<>(2);
            motivos.add(VALOR_MUITO_ALTO);
        }
        if (valor > LIMITE_ELETRONICOS && "Eletrônicos".equalsIgnoreCase(categoria)) {
            if (motivos == null) {
                motivos = new ArrayList<>(2);
            }
            motivos.add(ELETRONICOS);
        }
        if (valor > LIMITE_SAUDE_EDUCACAO
                && ("Saúde".equalsIgnoreCase(categoria) || "Educação".equalsIgnoreCase(categoria))) {
            if (motivos == null) {
                motivos = new ArrayList<>(2);
            }
            motivos.add(SAUDE_EDUCACAO);
        }
        if (valor > LIMITE_MADRUGADA && estaNaMadrugada(transacao.getData())) {
            if (motivos == null) {
                motivos = new ArrayList<>(2);
            }
            motivos.add(MADRUGADA);
        }

        return motivos == null ? Collections.emptyList() : motivos;
    }

    private static boolean estaNaMadrugada(String data) {
        if (data == null || data.length() < 13 || data.charAt(10) != ' '
                || !Character.isDigit(data.charAt(11)) || !Character.isDigit(data.charAt(12))) {
            return false;
        }

        int hora = (data.charAt(11) - '0') * 10 + data.charAt(12) - '0';
        return hora >= 0 && hora < 6;
    }
}