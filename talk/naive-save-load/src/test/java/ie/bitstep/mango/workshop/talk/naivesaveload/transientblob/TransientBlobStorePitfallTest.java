package ie.bitstep.mango.workshop.talk.naivesaveload.transientblob;

import ie.bitstep.mango.workshop.talk.naivesaveload.Crypto;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Proves the transient/blob split's two outcomes for the save()/load() pitfalls: the
 * concurrency window inside save() is gone (the transient field is never written), but
 * load()'s forced overwrite is unchanged, since it doesn't depend on which representation
 * a column holds.
 */
class TransientBlobStorePitfallTest {

    @Test
    void saveNeverChangesTheTransientFieldSoThereIsNoConcurrencyWindow() {
        SecretKey key = Crypto.newKey();
        TransientBlobStore store = new TransientBlobStore();
        TransientBlobAccount account = new TransientBlobAccount(1L, "john.doe@test.com");

        store.save(account, key);

        assertEquals("john.doe@test.com", account.username(),
                "the transient field should never have been touched by save()");
        assertNotNull(account.usernameBlob(), "the blob field should hold the persisted ciphertext");
    }

    @Test
    void loadStillOverwritesPendingInMemoryChangesWithNoWarning() {
        SecretKey key = Crypto.newKey();
        TransientBlobStore store = new TransientBlobStore();
        TransientBlobAccount saved = new TransientBlobAccount(1L, "old-address@test.com");
        store.save(saved, key);

        TransientBlobAccount loaded = store.load(1L, key);
        loaded.setUsername("still-typing-a-new-address@test.com");

        TransientBlobAccount reloaded = store.load(1L, key);

        assertSame(loaded, reloaded, "load() should have returned the same cached instance");
        assertEquals("old-address@test.com", reloaded.username(),
                "the transient/blob split does not fix this pitfall: the pending edit is still discarded");
    }
}
