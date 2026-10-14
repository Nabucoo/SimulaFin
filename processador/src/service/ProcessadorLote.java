package service;

import java.util.List;
import model.Transaction;
import parser.TransactionParser;

public class ProcessadorLote {

    /**
     * Processa um lote de linhas CSV:
     * - Faz o parse de cada linha em Transaction
     * - Alimenta as estatísticas no objeto Analises da thread
     * - Detecta e alerta sobre transações suspeitas
     */
    public static void processar(List<String> linhas, Analises analise) {
        if (linhas == null || analise == null) {
            return;
        }

        for (String linha : linhas) {
            Transaction transacao = TransactionParser.parse(linha);
            if (transacao != null) {
                analise.startAnalise(transacao);
            }
        }
    }
}
