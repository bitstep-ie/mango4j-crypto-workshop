package ie.bitstep.mango.workshop.talk.naivesaveload.directfield;

import ie.bitstep.mango.workshop.talk.naivesaveload.Crypto;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the two pitfalls of a single entity that reuses cardNumber for both plaintext
 * and ciphertext (no separate transient/blob split), using real threads with
 * {@link CountDownLatch}-based ordering (deterministic, no sleeps) rather than a
 * timing-dependent race.
 */
class NaiveCardStorePitfallTest {

    private static final String CARD_NUMBER = "4111-1111-1111-1111";

    @Test
    void concurrentReaderCanObserveCiphertextDuringSave() throws InterruptedException {
        SecretKey encryptionKey = Crypto.newKey();
        SecretKey hmacKey = Crypto.newHmacKey();
        NaiveCardStore store = new NaiveCardStore();
        NaiveCardEntity entity = new NaiveCardEntity(1L, CARD_NUMBER);

        CountDownLatch ciphertextWritten = new CountDownLatch(1);
        CountDownLatch readerDone = new CountDownLatch(1);
        String[] observedByReader = new String[1];

        Thread saver = new Thread(() -> store.save(entity, encryptionKey, hmacKey, () -> {
            ciphertextWritten.countDown();
            await(readerDone);
        }));
        Thread reader = new Thread(() -> {
            await(ciphertextWritten);
            observedByReader[0] = entity.cardNumber();
            readerDone.countDown();
        });

        saver.start();
        reader.start();
        saver.join(5_000);
        reader.join(5_000);
        assertTrue(!saver.isAlive() && !reader.isAlive(), "both threads should have finished within the timeout");

        assertNotEquals(CARD_NUMBER, observedByReader[0],
                "a concurrent reader landing mid-save() should have observed ciphertext");
        assertEquals(CARD_NUMBER, entity.cardNumber(),
                "after save() completes, the field should be restored to plaintext");
    }

    @Test
    void loadOverwritesPendingInMemoryChangesWithNoWarning() {
        SecretKey encryptionKey = Crypto.newKey();
        SecretKey hmacKey = Crypto.newHmacKey();
        NaiveCardStore store = new NaiveCardStore();
        NaiveCardEntity saved = new NaiveCardEntity(1L, CARD_NUMBER);
        store.save(saved, encryptionKey, hmacKey);

        NaiveCardEntity loaded = store.load(1L, encryptionKey);
        loaded.setCardNumber("4222-2222-2222-2222");

        NaiveCardEntity reloaded = store.load(1L, encryptionKey);

        assertSame(loaded, reloaded, "load() should have returned the same managed instance");
        assertEquals(CARD_NUMBER, reloaded.cardNumber(),
                "the pending in-memory edit should have been silently discarded");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for latch");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
