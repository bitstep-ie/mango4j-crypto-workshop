package ie.bitstep.mango.workshop.talk.naivesaveload;

import ie.bitstep.mango.workshop.talk.naivesaveload.directfield.NaiveCardEntity;
import ie.bitstep.mango.workshop.talk.naivesaveload.directfield.NaiveCardStore;
import ie.bitstep.mango.workshop.talk.naivesaveload.transientblob.TransientBlobAccount;
import ie.bitstep.mango.workshop.talk.naivesaveload.transientblob.TransientBlobStore;

import javax.crypto.SecretKey;
import java.util.concurrent.CountDownLatch;

/**
 * Companion demo for {@code docs/talk/transient-vs-encrypted-blobs.md}'s naive
 * save()/load() section, side by side in both variants: a single naive entity that
 * reuses cardNumber for both plaintext and ciphertext, and the transient/blob split.
 *
 * <p>Run with {@code mvn -f talk/naive-save-load/pom.xml exec:java}.
 */
public final class Main {

    public static void main(String[] args) throws InterruptedException {
        SecretKey encryptionKey = Crypto.newKey();
        SecretKey hmacKey = Crypto.newHmacKey();

        System.out.println("== Naive card entity: the concurrency window inside save() ==");
        naiveCardConcurrencyDemo(encryptionKey, hmacKey);
        System.out.println();

        System.out.println("== Transient/blob: save() has no concurrency window ==");
        transientBlobConcurrencyDemo(encryptionKey);
        System.out.println();

        System.out.println("== Naive card entity: load()'s forced overwrite ==");
        naiveCardLoadDemo(encryptionKey, hmacKey);
        System.out.println();

        System.out.println("== Transient/blob: load()'s forced overwrite (still present) ==");
        transientBlobLoadDemo(encryptionKey);
    }

    private static void naiveCardConcurrencyDemo(SecretKey encryptionKey, SecretKey hmacKey) throws InterruptedException {
        NaiveCardStore store = new NaiveCardStore();
        NaiveCardEntity entity = new NaiveCardEntity(1L, "4111-1111-1111-1111");

        CountDownLatch ciphertextWritten = new CountDownLatch(1);
        CountDownLatch readerDone = new CountDownLatch(1);
        String[] observedByReader = new String[1];

        Thread saver = new Thread(() -> store.save(entity, encryptionKey, hmacKey, () -> {
            ciphertextWritten.countDown();
            awaitUninterruptibly(readerDone);
        }));
        Thread reader = new Thread(() -> {
            awaitUninterruptibly(ciphertextWritten);
            observedByReader[0] = entity.cardNumber();
            readerDone.countDown();
        });

        saver.start();
        reader.start();
        saver.join();
        reader.join();

        System.out.println("A second thread read the entity mid-save() and saw: " + observedByReader[0]);
        System.out.println("PITFALL: that's ciphertext, not the cardNumber the rest of the app expects.");
        System.out.println("After save() finishes, the field is back to: " + entity.cardNumber());
    }

    private static void transientBlobConcurrencyDemo(SecretKey key) {
        TransientBlobStore store = new TransientBlobStore();
        TransientBlobAccount account = new TransientBlobAccount(1L, "john.doe@test.com");

        store.save(account, key);

        System.out.println("username() throughout save(): " + account.username() + " (never changed)");
        System.out.println("usernameBlob() after save():  " + account.usernameBlob());
        System.out.println("There is no window here for a concurrent reader to land in: save() never");
        System.out.println("writes to the transient field at all.");
    }

    private static void naiveCardLoadDemo(SecretKey encryptionKey, SecretKey hmacKey) {
        NaiveCardStore store = new NaiveCardStore();
        NaiveCardEntity saved = new NaiveCardEntity(1L, "4111-1111-1111-1111");
        store.save(saved, encryptionKey, hmacKey);

        NaiveCardEntity loaded = store.load(1L, encryptionKey);
        System.out.println("First load(): " + loaded.cardNumber());

        loaded.setCardNumber("4222-2222-2222-2222");
        System.out.println("Application makes an in-memory edit, not yet saved: " + loaded.cardNumber());

        NaiveCardEntity reloaded = store.load(1L, encryptionKey);
        System.out.println("Something else in the app calls load() again for the same id.");
        System.out.println("Result: " + reloaded.cardNumber());
        System.out.println("PITFALL: the in-memory edit is gone, silently, no warning, no exception.");
    }

    private static void transientBlobLoadDemo(SecretKey key) {
        TransientBlobStore store = new TransientBlobStore();
        TransientBlobAccount saved = new TransientBlobAccount(1L, "old-address@test.com");
        store.save(saved, key);

        TransientBlobAccount loaded = store.load(1L, key);
        System.out.println("First load(): " + loaded.username());

        loaded.setUsername("still-typing-a-new-address@test.com");
        System.out.println("Application makes an in-memory edit, not yet saved: " + loaded.username());

        TransientBlobAccount reloaded = store.load(1L, key);
        System.out.println("Something else in the app calls load() again for the same id.");
        System.out.println("Result: " + reloaded.username());
        System.out.println("PITFALL: same as the naive design, the transient/blob split doesn't fix this one.");
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
