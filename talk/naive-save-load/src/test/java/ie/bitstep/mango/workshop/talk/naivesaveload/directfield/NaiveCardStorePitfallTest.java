package ie.bitstep.mango.workshop.talk.naivesaveload.directfield;

import ie.bitstep.mango.workshop.talk.naivesaveload.Crypto;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the pitfalls of a single entity that reuses cardNumber for both plaintext and
 * ciphertext (no separate transient/blob split), using real threads with
 * {@link CountDownLatch}-based ordering (deterministic, no sleeps) rather than a
 * timing-dependent race.
 */
class NaiveCardStorePitfallTest {

    private static final String CARD_NUMBER = "4111-1111-1111-1111";

    @Test
    void saveRestoresCardNumberToPlaintextForContinuedUse() {
        SecretKey encryptionKey = Crypto.newKey();
        SecretKey hmacKey = Crypto.newHmacKey();
        NaiveCardStore store = new NaiveCardStore();
        NaiveCardEntity entity = new NaiveCardEntity(1L, CARD_NUMBER);

        store.save(entity, encryptionKey, hmacKey);

        assertEquals(CARD_NUMBER, entity.cardNumber(),
                "save() should have restored cardNumber back for the caller to keep using");
    }

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
    void loadRightAfterSaveIsADoubleDecryptAndThrows() {
        SecretKey encryptionKey = Crypto.newKey();
        SecretKey hmacKey = Crypto.newHmacKey();
        NaiveCardStore store = new NaiveCardStore();
        NaiveCardEntity entity = new NaiveCardEntity(1L, CARD_NUMBER);

        store.save(entity, encryptionKey, hmacKey);   // save() already restored cardNumber as its last step

        // load() blindly decrypts whatever cardNumber currently holds, with no way to
        // know save() already restored it. It's not valid ciphertext any more, so this
        // decrypt fails, the exact same root cause as a silent overwrite would have,
        // just a louder symptom of it: decrypt() has no way to know it isn't safe to run.
        assertThrows(IllegalStateException.class, () -> store.load(1L, encryptionKey));
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
