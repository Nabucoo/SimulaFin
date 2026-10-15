package simulafin.model;

/**
 * Representa uma localidade brasileira composta pelo nome da cidade e sua Unidade Federativa (UF).
 * 
 * @param cidade Nome do município
 * @param estado Sigla da Unidade Federativa (UF)
 * @param populacao População residente aferida pelo Censo IBGE 2022
 */
public record CityState(String cidade, String estado, int populacao) {
    public CityState(String cidade, String estado) {
        this(cidade, estado, 0);
    }
}
