package simulafin.generator;

import simulafin.data.CustomerNameRepository;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Gerador de nomes de clientes completos compostos por:
 * [Primeiro Nome] [Segundo Nome] [Sobrenome].
 */
public final class CustomerGenerator {

    private final String[] firstNames;
    private final String[] middleNames;
    private final String[] lastNames;

    public CustomerGenerator() {
        this.firstNames = CustomerNameRepository.FIRST_NAMES;
        this.middleNames = CustomerNameRepository.MIDDLE_NAMES;
        this.lastNames = CustomerNameRepository.LAST_NAMES;
    }

    /**
     * Gera um nome completo utilizando o gerador de números aleatórios atual.
     */
    public String nextCustomerName() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String first = firstNames[random.nextInt(firstNames.length)];
        String middle = middleNames[random.nextInt(middleNames.length)];
        String last = lastNames[random.nextInt(lastNames.length)];
        return first + " " + middle + " " + last;
    }
}
