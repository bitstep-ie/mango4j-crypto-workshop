package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.keyrotation.RekeyEvent;
import ie.bitstep.mango.crypto.keyrotation.RekeyService;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * The bridge between RekeyScheduler and this workshop's in-memory store.
 * RekeyScheduler calls findRecordsUsingCryptoKey()/save() over and over,
 * in batches, until an empty list comes back - so both "which records
 * still need this?" methods have to inspect the record's *current* state
 * each time, not a snapshot taken once.
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
                .filter(record -> !isOnKey(record, cryptoKey))
                .toList();
    }

    @Override
    public List<PaymentCardEntity> findRecordsUsingCryptoKey(CryptoKey cryptoKey) {
        return store.all().stream()
                .filter(record -> isOnKey(record, cryptoKey))
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

    private static boolean isOnKey(PaymentCardEntity record, CryptoKey cryptoKey) {
        return record.getEncryptedData().contains("\"cryptoKeyId\":\"" + cryptoKey.getId() + "\"");
    }
}
