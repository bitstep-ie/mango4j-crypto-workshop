package ie.bitstep.mango.workshop.talk.naivesaveload.directfield;

import ie.bitstep.mango.workshop.talk.naivesaveload.Crypto;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

/**
 * The naive hand-rolled shape from {@code docs/talk/transient-vs-encrypted-blobs.md}:
 * {@code save()} encrypts the single column in place, persists, then decrypts it back;
 * {@code load()} fetches and decrypts. Both operations mutate one shared field.
 */
public final class DirectFieldStore {

    private final Map<Long, String> db = new HashMap<>();
    private final Map<Long, DirectFieldAccount> alreadyLoaded = new HashMap<>();

    public void save(DirectFieldAccount account, SecretKey key) {
        save(account, key, () -> {
        });
    }

    /**
     * Same as {@link #save(DirectFieldAccount, SecretKey)}, but runs
     * {@code duringCiphertextWindow} at the exact point where {@code username} holds
     * ciphertext, standing in for a concurrent reader landing in that window.
     */
    // --8<-- [start:direct-field-save] link
    public void save(DirectFieldAccount account, SecretKey key, Runnable duringCiphertextWindow) {
        String plaintext = account.username();
        String ciphertext = Crypto.encrypt(plaintext, key);
        account.setUsername(ciphertext);       // the SAME field now holds ciphertext
        db.put(account.id(), ciphertext);       // the real persist
        duringCiphertextWindow.run();           // a concurrent reader could land here
        account.setUsername(Crypto.decrypt(ciphertext, key));  // restore plaintext
    }
    // --8<-- [end:direct-field-save]

    /**
     * Fetches and decrypts. If this id is already held elsewhere (a cache, a session,
     * anything that kept the object from an earlier {@code load()}), that same instance
     * is overwritten in place rather than a fresh one being handed back.
     */
    // --8<-- [start:direct-field-load]
    public DirectFieldAccount load(long id, SecretKey key) {
        String plaintext = Crypto.decrypt(db.get(id), key);
        DirectFieldAccount existing = alreadyLoaded.get(id);
        if (existing != null) {
            existing.setUsername(plaintext);    // forced overwrite, no check, no warning
            return existing;
        }
        DirectFieldAccount fresh = new DirectFieldAccount(id, plaintext);
        alreadyLoaded.put(id, fresh);
        return fresh;
    }
    // --8<-- [end:direct-field-load]
}
