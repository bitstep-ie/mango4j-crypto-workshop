package ie.bitstep.mango.workshop;

import java.util.ArrayList;
import java.util.List;

/**
 * A tiny stand-in for a real table: a flat list of already-persisted
 * records, exactly what a rekey sweep has to iterate over in a real system.
 */
public class PaymentCardStore {

    private final List<PaymentCardEntity> records = new ArrayList<>();

    public void save(PaymentCardEntity entity) {
        records.add(entity);
    }

    public List<PaymentCardEntity> all() {
        return records;
    }
}
