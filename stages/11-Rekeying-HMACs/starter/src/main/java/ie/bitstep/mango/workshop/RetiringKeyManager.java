package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.keyrotation.RekeyCryptoKeyManager;

import java.util.concurrent.CountDownLatch;

/**
 * RekeyScheduler calls this once it's confirmed nothing needs a retired
 * key anymore - the automatic version of the teardown-ordering check
 * earlier versions of this stage had to get right by hand.
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
