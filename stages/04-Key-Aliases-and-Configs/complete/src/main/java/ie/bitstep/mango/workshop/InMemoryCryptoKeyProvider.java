package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.domain.CryptoKeyUsage;
import ie.bitstep.mango.crypto.core.providers.CryptoKeyProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The previous stage's provider held exactly one key, so getById() could get
 * away with ignoring the id it was asked for and always returning that one
 * key. This stage makes that shortcut visible: the provider now knows about
 * two keys, and "current" is answered by alias (an id this provider was
 * configured with) rather than by being the only option in existence.
 */
public class InMemoryCryptoKeyProvider implements CryptoKeyProvider {

    private static final Map<String, CryptoKey> KEYS_BY_ID = buildKeys();

    private final String currentEncryptionKeyId;

    public InMemoryCryptoKeyProvider(String currentEncryptionKeyId) {
        this.currentEncryptionKeyId = currentEncryptionKeyId;
    }

    // --8<-- [start:resolve-by-id]
    @Override
    public CryptoKey getById(String cryptoKeyId) {
        // The previous stage's shortcut: ignore the id asked for, just
        // return the one key that exists.
        CryptoKey resolved = KEYS_BY_ID.get(currentEncryptionKeyId);

        resolved = KEYS_BY_ID.get(cryptoKeyId);

        return resolved;
    }
    // --8<-- [end:resolve-by-id]

    // --8<-- [start:current-by-alias]
    @Override
    public CryptoKey getCurrentEncryptionKey() {
        // "Current" is answered by alias, not by a dedicated field: whichever
        // id this provider was configured with is looked up exactly the same
        // way any other key id is. Nothing about encrypt() has to know or
        // care that this particular lookup is the "current" one.
        return getById(currentEncryptionKeyId);
    }
    // --8<-- [end:current-by-alias]

    @Override
    public List<CryptoKey> getCurrentHmacKeys() {
        return List.of();
    }

    @Override
    public List<CryptoKey> getAllCryptoKeys() {
        return List.copyOf(KEYS_BY_ID.values());
    }

    // --8<-- [start:key-map]
    private static Map<String, CryptoKey> buildKeys() {
        CryptoKey archiveKey = buildEncryptionKey(
                "workshop-archive-key",
                "workshop-archive-passphrase-do-not-use-in-production",
                "workshop-archive-salt");
        CryptoKey currentKey = buildEncryptionKey(
                "workshop-encryption-key",
                "workshop-demo-passphrase-do-not-use-in-production",
                "workshop-demo-salt");
        return Map.of(archiveKey.getId(), archiveKey, currentKey.getId(), currentKey);
    }
    // --8<-- [end:key-map]

    private static CryptoKey buildEncryptionKey(String id, String passPhrase, String salt) {
        CryptoKey key = new CryptoKey();
        key.setId(id);
        key.setUsage(CryptoKeyUsage.ENCRYPTION);
        key.setCreatedDate(Instant.now());
        key.setType("PBKDF2");
        key.setConfiguration(Map.of(
                "algorithm", "AES",
                "mode", "GCM",
                "padding", "NoPadding",
                "keySize", 256,
                "iterations", 10000,
                "gcmTagLength", 128,
                "ivSize", 16,
                "passPhrase", passPhrase,
                "salt", salt
        ));
        return key;
    }
}
