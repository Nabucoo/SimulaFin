package generator;

/**
 * Tipos de distribuições estatísticas suportadas para geração dos valores das transações.
 */
public enum DistributionType {
    UNIFORME,
    NORMAL,
    EXPONENCIAL_CRESCENTE,
    EXPONENCIAL_DECRESCENTE;

    public static DistributionType fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da distribuição não pode ser vazio.");
        }
        String clean = name.trim().toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");
        for (DistributionType dt : values()) {
            if (dt.name().equals(clean)) {
                return dt;
            }
        }
        throw new IllegalArgumentException("Distribuição desconhecida: '" + name + "'. Opções válidas: UNIFORME, NORMAL, EXPONENCIAL_CRESCENTE, EXPONENCIAL_DECRESCENTE");
    }
}
