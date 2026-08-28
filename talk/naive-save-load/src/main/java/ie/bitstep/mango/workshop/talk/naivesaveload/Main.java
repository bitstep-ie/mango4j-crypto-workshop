package ie.bitstep.mango.workshop.talk.naivesaveload;

import ie.bitstep.mango.workshop.talk.naivesaveload.directfield.DirectFieldAccount;
import ie.bitstep.mango.workshop.talk.naivesaveload.directfield.DirectFieldStore;
import ie.bitstep.mango.workshop.talk.naivesaveload.transientblob.TransientBlobAccount;
import ie.bitstep.mango.workshop.talk.naivesaveload.transientblob.TransientBlobStore;

import javax.crypto.SecretKey;
import java.util.concurrent.CountDownLatch;

/**
 * Companion demo for {@code docs/talk/transient-vs-encrypted-blobs.md}'s naive
 * save()/load() section, side by side in both variants: a naive single-column
 * design and the transient/blob split.
 *
 * <p>Run with {@code mvn -f talk/naive-save-load/pom.xml exec:java}.
 */
public final class Main {

    public static void main(String[] args) throws InterruptedException {
        SecretKey key = Crypto.newKey();

        System.out.println("== Direct field: the concurrency window inside save() ==");
        directFieldConcurrencyDemo(key);
        System.out.println();

        System.out.println("== Transient/blob: save() has no concurrency window ==");
        transientBlobConcurrencyDemo(key);
        System.out.println();

        System.out.println("== Direct field: load()'s forced overwrite ==");
        directFieldLoadDemo(key);
        System.out.println();

        System.out.println("== Transient/blob: load()'s forced overwrite (still present) ==");
        transientBlobLoadDemo(key);
    }

    private static void directFieldConcurrencyDemo(SecretKey key) throws InterruptedException {
        DirectFieldStore store = new DirectFieldStore();
        DirectFieldAccount account = new DirectFieldAccount(1L, "john.doe@test.com");

        CountDownLatch ciphertextWritten = new CountDownLatch(1);
        CountDownLatch readerDone = new CountDownLatch(1);
        String[] observedByReader = new String[1];

        Thread saver = new Thread(() -> store.save(account, key, () -> {
            ciphertextWritten.countDown();
            awaitUninterruptibly(readerDone);
        }));
        Thread reader = new Thread(() -> {
            awaitUninterruptibly(ciphertextWritten);
            observedByReader[0] = account.username();
            readerDone.countDown();
        });

        saver.start();
        reader.start();
        saver.join();
        reader.join();

        System.out.println("A second thread read the account mid-save() and saw: " + observedByReader[0]);
        System.out.println("PITFALL: that's ciphertext, not the username the rest of the app expects.");
        System.out.println("After save() finishes, the field is back to: " + account.username());
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

    private static void directFieldLoadDemo(SecretKey key) {
        DirectFieldStore store = new DirectFieldStore();
        DirectFieldAccount saved = new DirectFieldAccount(1L, "old-address@test.com");
        store.save(saved, key);

        DirectFieldAccount loaded = store.load(1L, key);
        System.out.println("First load(): " + loaded.username());

        loaded.setUsername("still-typing-a-new-address@test.com");
        System.out.println("Application makes an in-memory edit, not yet saved: " + loaded.username());

        DirectFieldAccount reloaded = store.load(1L, key);
        System.out.println("Something else in the app calls load() again for the same id.");
        System.out.println("Result: " + reloaded.username());
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
