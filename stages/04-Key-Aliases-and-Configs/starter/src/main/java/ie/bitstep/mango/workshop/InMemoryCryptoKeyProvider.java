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

    @Override
    public CryptoKey getById(String cryptoKeyId) {
        // The previous stage's shortcut: ignore the id asked for, just
        // return the one key that exists.
        CryptoKey resolved = KEYS_BY_ID.get(currentEncryptionKeyId);

        /* TODO: Resolve cryptoKeyId against the known keys instead of
         * always returning the current one.
         *
         * This is the other half of the alias mechanism: "current" is just
         * one particular id among several this method must be able to
         * answer for. decrypt() will ask for whichever key id is recorded
         * on the ciphertext it's reading, current or not - so getById() has
         * to actually look that id up, not shortcut back to a single key
         * the way the previous stage's did.
         */

        return resolved;
    }

    @Override
    public CryptoKey getCurrentEncryptionKey() {
        // "Current" is answered by alias, not by a dedicated field: whichever
        // id this provider was configured with is looked up exactly the same
        // way any other key id is. Nothing about encrypt() has to know or
        // care that this particular lookup is the "current" one.
        return getById(currentEncryptionKeyId);
    }

    @Override
    public List<CryptoKey> getCurrentHmacKeys() {
        return List.of();
    }

    @Override
    public List<CryptoKey> getAllCryptoKeys() {
        return List.copyOf(KEYS_BY_ID.values());
    }

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
