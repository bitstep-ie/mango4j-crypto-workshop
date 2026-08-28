package ie.bitstep.mango.workshop.talk.naivesaveload.transientblob;

import ie.bitstep.mango.workshop.talk.naivesaveload.Crypto;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

/**
 * The same save()/load() shape as {@link ie.bitstep.mango.workshop.talk.naivesaveload.directfield.DirectFieldStore},
 * built on a transient/blob split instead of one shared column. Compare the two: this
 * fixes the concurrency window inside save(), but load()'s forced overwrite is still here.
 */
public final class TransientBlobStore {

    private final Map<Long, String> db = new HashMap<>();
    private final Map<Long, TransientBlobAccount> alreadyLoaded = new HashMap<>();

    /** Reads the transient field, writes the blob field. The transient field is
     * never touched, so there is no window where it holds anything but plaintext. */
    // --8<-- [start:transient-blob-save]
    public void save(TransientBlobAccount account, SecretKey key) {
        String blob = Crypto.encrypt(account.username(), key);
        account.setUsernameBlob(blob);
        db.put(account.id(), blob);
    }
    // --8<-- [end:transient-blob-save]

    /** Same forced-overwrite shape as {@code DirectFieldStore.load()}: an id already
     * held elsewhere gets its transient field overwritten in place, unconditionally. */
    // --8<-- [start:transient-blob-load]
    public TransientBlobAccount load(long id, SecretKey key) {
        String blob = db.get(id);
        String plaintext = Crypto.decrypt(blob, key);
        TransientBlobAccount existing = alreadyLoaded.get(id);
        if (existing != null) {
            existing.setUsername(plaintext);    // forced overwrite, same as the naive design
            existing.setUsernameBlob(blob);
            return existing;
        }
        TransientBlobAccount fresh = new TransientBlobAccount(id, plaintext);
        fresh.setUsernameBlob(blob);
        alreadyLoaded.put(id, fresh);
        return fresh;
    }
    // --8<-- [end:transient-blob-load]
}
