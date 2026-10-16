package generator;

import data.LocationRepository;
import model.Category;
import model.CityState;
import model.Transaction;
import model.TransactionType;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Coordenador da geração de transações sintéticas.
 */
public final class TransactionGenerator {

    private final CustomerGenerator customerGenerator;
    private final DateGenerator dateGenerator;
    private final ValueGenerator valueGenerator;
    private final Category[] categories;
    private final TransactionType[] transactionTypes;

    private long currentId;

    public TransactionGenerator(DateGenerator dateGenerator, ValueGenerator valueGenerator) {
        this.customerGenerator = new CustomerGenerator();
        this.dateGenerator = dateGenerator;
        this.valueGenerator = valueGenerator;
        this.categories = Category.cachedValues();
        this.transactionTypes = TransactionType.cachedValues();
        this.currentId = 1L;
    }

    public synchronized void resetId() {
        this.currentId = 1L;
    }

    /**
     * Gera a próxima transação como objeto Transaction.
     */
    public Transaction nextTransaction() {
        long id = currentId++;
        String cliente = customerGenerator.nextCustomerName();
        LocalDateTime data = dateGenerator.nextDateTime();
        double valor = valueGenerator.nextValue();

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Category cat = categories[random.nextInt(categories.length)];
        CityState cs = LocationRepository.getRandomCity(random);
        TransactionType tipo = transactionTypes[random.nextInt(transactionTypes.length)];

        return new Transaction(
                id,
                cliente,
                data,
                valor,
                cat.getDescricao(),
                cs.cidade(),
                cs.estado(),
                tipo.getDescricao()
        );
    }
}
