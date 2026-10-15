package parser;

import model.Transaction;

public class TransactionParser {

    /**
     * Converte uma linha CSV em uma instância de Transaction.
     * Retorna null caso a linha seja cabeçalho, esteja em branco ou inválida.
     */
    public static Transaction parse(String linha) {
        if (linha == null || linha.isBlank()) {
            return null;
        }

        String[] partes = linha.split(",");
        if (partes.length < 8) {
            return null;
        }

        try {
            int id = Integer.parseInt(partes[0].trim());
            String cliente = partes[1].trim();
            String data = partes[2].trim();
            double valor = Double.parseDouble(partes[3].trim());
            String categoria = partes[4].trim();
            String cidade = partes[5].trim();
            String estado = partes[6].trim();
            String tipo = partes[7].trim();

            return new Transaction(id, cliente, data, valor, categoria, cidade, estado, tipo);
        } catch (NumberFormatException e) {
            // Ignora linhas de cabeçalho ou mal formatadas
            return null;
        }
    }
}
