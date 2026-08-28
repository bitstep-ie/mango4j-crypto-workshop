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
 * Proves the two pitfalls the naive single-column save()/load() shape has, using real
 * threads with {@link CountDownLatch}-based ordering (deterministic, no sleeps) rather
 * than a timing-dependent race.
 */
class DirectFieldStorePitfallTest {

    private static final String USERNAME = "john.doe@test.com";

    @Test
    void concurrentReaderCanObserveCiphertextDuringSave() throws InterruptedException {
        SecretKey key = Crypto.newKey();
        DirectFieldStore store = new DirectFieldStore();
        DirectFieldAccount account = new DirectFieldAccount(1L, USERNAME);

        CountDownLatch ciphertextWritten = new CountDownLatch(1);
        CountDownLatch readerDone = new CountDownLatch(1);
        String[] observedByReader = new String[1];

        Thread saver = new Thread(() -> store.save(account, key, () -> {
            ciphertextWritten.countDown();
            await(readerDone);
        }));
        Thread reader = new Thread(() -> {
            await(ciphertextWritten);
            observedByReader[0] = account.username();
            readerDone.countDown();
        });

        saver.start();
        reader.start();
        saver.join(5_000);
        reader.join(5_000);
        assertTrue(!saver.isAlive() && !reader.isAlive(), "both threads should have finished within the timeout");

        assertNotEquals(USERNAME, observedByReader[0],
                "a concurrent reader landing mid-save() should have observed ciphertext");
        assertEquals(USERNAME, account.username(),
                "after save() completes, the field should be restored to plaintext");
    }

    @Test
    void loadOverwritesPendingInMemoryChangesWithNoWarning() {
        SecretKey key = Crypto.newKey();
        DirectFieldStore store = new DirectFieldStore();
        DirectFieldAccount saved = new DirectFieldAccount(1L, "old-address@test.com");
        store.save(saved, key);

        DirectFieldAccount loaded = store.load(1L, key);
        loaded.setUsername("still-typing-a-new-address@test.com");

        DirectFieldAccount reloaded = store.load(1L, key);

        assertSame(loaded, reloaded, "load() should have returned the same cached instance");
        assertEquals("old-address@test.com", reloaded.username(),
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
