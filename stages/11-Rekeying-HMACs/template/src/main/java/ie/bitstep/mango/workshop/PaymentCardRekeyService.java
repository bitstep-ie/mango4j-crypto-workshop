package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.domain.CryptoShieldHmacHolder;
import ie.bitstep.mango.crypto.keyrotation.RekeyEvent;
import ie.bitstep.mango.crypto.keyrotation.RekeyService;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * The bridge between RekeyScheduler and this workshop's in-memory store.
 * findRecordsNotUsingCryptoKey()/findRecordsUsingCryptoKey() are called
 * repeatedly, in batches, until an empty list comes back, so both have to
 * inspect each record's *current* lookups, not a snapshot taken once.
 */
public class PaymentCardRekeyService implements RekeyService<PaymentCardEntity> {

    private final PaymentCardStore store;
    private final CountDownLatch rekeyFinishedLatch;

    public PaymentCardRekeyService(PaymentCardStore store, CountDownLatch rekeyFinishedLatch) {
        this.store = store;
        this.rekeyFinishedLatch = rekeyFinishedLatch;
    }

    @Override
    public Class<PaymentCardEntity> getEntityType() {
        return PaymentCardEntity.class;
    }

    @Override
    public List<PaymentCardEntity> findRecordsNotUsingCryptoKey(CryptoKey cryptoKey) {
        return store.all().stream()
                .filter(record -> !hasEntryFor(record, cryptoKey))
                .toList();
    }

    @Override
    public List<PaymentCardEntity> findRecordsUsingCryptoKey(CryptoKey cryptoKey) {
        return store.all().stream()
                .filter(record -> hasEntryFor(record, cryptoKey))
                .toList();
    }

    @Override
    public void save(List<?> records) {
        // Our records are already updated in place (no serialization round
        // trip in this workshop); a real RekeyService persists them here.
    }

    @Override
    public void notify(RekeyEvent rekeyEvent) {
        if (rekeyEvent.getType() == RekeyEvent.Type.REKEY_FINISHED) {
            rekeyFinishedLatch.countDown();
        }
    }

    private static boolean hasEntryFor(PaymentCardEntity record, CryptoKey cryptoKey) {
        return record.getLookups().stream()
                .map(CryptoShieldHmacHolder::getCryptoKeyId)
                .anyMatch(keyId -> keyId.equals(cryptoKey.getId()));
    }
}
