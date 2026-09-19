package ie.bitstep.mango.workshop;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A tiny stand-in for a real table: a flat list of already-persisted
 * records, exactly what RekeyScheduler's own RekeyService callbacks read
 * from and write back to. CopyOnWriteArrayList since RekeyScheduler reads
 * this from its own background thread.
 */
public class PaymentCardStore {

    private final List<PaymentCardEntity> records = new CopyOnWriteArrayList<>();

    public void save(PaymentCardEntity entity) {
        records.add(entity);
    }

    public List<PaymentCardEntity> all() {
        return records;
    }
}
