package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.keyrotation.RekeyCryptoKeyManager;

import java.util.concurrent.CountDownLatch;

/**
 * RekeyScheduler calls this once it's confirmed nothing is using a
 * KEY_OFF-marked key anymore - the automatic version of what earlier
 * stages had to check for manually before removing an old key.
 */
public class RetiringKeyManager implements RekeyCryptoKeyManager {

    private final CountDownLatch keyRetiredLatch;

    public RetiringKeyManager(CountDownLatch keyRetiredLatch) {
        this.keyRetiredLatch = keyRetiredLatch;
    }

    @Override
    public void markKeyForDeletion(CryptoKey retiredKey) {
        InMemoryCryptoKeyProvider.remove(retiredKey.getId());
        keyRetiredLatch.countDown();
    }
}
