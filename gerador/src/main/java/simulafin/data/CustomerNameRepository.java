package simulafin.data;

/**
 * Catálogo com nomes e sobrenomes brasileiros para formação de nomes completos realistas.
 * Cada nome completo é gerado na estrutura: [Primeiro Nome] [Segundo Nome] [Sobrenome].
 * Com 40 primeiros nomes, 40 segundos nomes e 50 sobrenomes:
 * 40 * 40 * 50 = 80.000 combinações distintas (muito acima do requisito mínimo de 1.000).
 */
public final class CustomerNameRepository {

    public static final String[] FIRST_NAMES = {
            "Lucas", "Mateus", "Gabriel", "Felipe", "Guilherme",
            "Rafael", "Bruno", "Thiago", "Rodrigo", "Leonardo",
            "Gustavo", "Eduardo", "Daniel", "Vinícius", "Ryquelme",
            "Pedro", "Henrique", "Caio", "Diego", "André",
            "Ana", "Beatriz", "Camila", "Carolina", "Daniela",
            "Fernanda", "Gabriela", "Isabela", "Juliana", "Larissa",
            "Mariana", "Natália", "Patrícia", "Rafaela", "Letícia",
            "Bianca", "Vanessa", "Bruna", "Aline", "Jéssica"
    };

    public static final String[] MIDDLE_NAMES = {
            "Henrique", "Felipe", "Eduardo", "Augusto", "Miguel",
            "Alexandre", "Fernando", "Antônio", "Carlos", "Roberto",
            "Luiz", "Paulo", "Victor", "César", "Vinícius",
            "Gabriel", "Lucas", "Bernardo", "Emanuel", "Vicente",
            "Beatriz", "Vitória", "Luiza", "Helena", "Cristina",
            "Maria", "Sofia", "Clara", "Alice", "Laura",
            "Lorena", "Cecília", "Valentina", "Emanuelle", "Aparecida",
            "Inês", "Tereza", "Regina", "Marta", "Joana"
    };

    public static final String[] LAST_NAMES = {
            "Silva", "Santos", "Oliveira", "Souza", "Rodrigues",
            "Ferreira", "Alves", "Pereira", "Lima", "Gomes",
            "Costa", "Ribeiro", "Martins", "Carvalho", "Almeida",
            "Lopes", "Soares", "Fernandes", "Vieira", "Barbosa",
            "Rocha", "Dias", "Nascimento", "Andrade", "Moreira",
            "Nunes", "Marques", "Machado", "Mendes", "Freitas",
            "Cardoso", "Ramos", "Gonçalves", "Santana", "Teixeira",
            "Moura", "Cavalcanti", "Castro", "Campos", "Barros",
            "Duarte", "Pinto", "Batista", "Pacheco", "Monteiro",
            "Borges", "Carneiro", "Guimarães", "Farias", "Coelho"
    };

    private CustomerNameRepository() {
    }

    public static int getTotalPossibleCombinations() {
        return FIRST_NAMES.length * MIDDLE_NAMES.length * LAST_NAMES.length;
    }
}
