package simulafin.model;

/**
 * Categorias de gastos permitidas para as transações financeiras.
 */
public enum Category {
    ALIMENTACAO("Alimentação"),
    TRANSPORTE("Transporte"),
    ELETRONICOS("Eletrônicos"),
    SAUDE("Saúde"),
    EDUCACAO("Educação");

    private final String descricao;

    Category(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    private static final Category[] VALUES = values();

    public static Category[] cachedValues() {
        return VALUES;
    }
}
