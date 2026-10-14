package simulafin.model;

/**
 * Meios de pagamento suportados pelo sistema de transações.
 */
public enum TransactionType {
    PIX("PIX"),
    CREDITO("CRÉDITO"),
    BOLETO("BOLETO"),
    DEBITO("DÉBITO");

    private final String descricao;

    TransactionType(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    private static final TransactionType[] VALUES = values();

    public static TransactionType[] cachedValues() {
        return VALUES;
    }
}
