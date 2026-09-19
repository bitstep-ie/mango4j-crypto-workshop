package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.domain.CryptoShieldHmacHolder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A flat list of records, same as List HMAC Strategy's store -
 * CopyOnWriteArrayList since RekeyScheduler reads this from its own
 * background thread.
 */
public class PaymentCardStore {

    private final List<PaymentCardEntity> records = new CopyOnWriteArrayList<>();

    public void save(PaymentCardEntity entity) {
        records.add(entity);
    }

    public List<PaymentCardEntity> all() {
        return records;
    }

    public Optional<PaymentCardEntity> findByLookup(PaymentCardEntity probe) {
        return records.stream()
                .filter(record -> anyValueMatches(record.getLookups(), probe.getLookups()))
                .findFirst();
    }

    private static boolean anyValueMatches(Collection<CryptoShieldHmacHolder> a, Collection<CryptoShieldHmacHolder> b) {
        return a.stream().anyMatch(holderA ->
                b.stream().anyMatch(holderB -> holderA.getValue().equals(holderB.getValue())));
    }
}
