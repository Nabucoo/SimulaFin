package model;

import java.time.LocalDateTime;

/**
 * Representa uma transação financeira sintética completa.
 *
 * @param id Identificador único e sequencial da transação (iniciado em 1)
 * @param cliente Nome completo do cliente (Primeiro Nome + Segundo Nome + Sobrenome)
 * @param data Data e hora da ocorrência
 * @param valor Valor financeiro com 2 casas decimais
 * @param categoria Categoria da despesa
 * @param cidade Nome do município
 * @param estado Sigla da Unidade Federativa (UF)
 * @param tipo Meio de pagamento utilizado
 */
public record Transaction(
        long id,
        String cliente,
        LocalDateTime data,
        double valor,
        String categoria,
        String cidade,
        String estado,
        String tipo
) {
}
