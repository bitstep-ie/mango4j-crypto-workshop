package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.domain.CryptoKeyUsage;
import ie.bitstep.mango.crypto.core.providers.CryptoKeyProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Builds on the previous stages' alias mechanism (getById() resolves any
 * key by id; "current" is just one particular id, looked up the same way)
 * by adding a second, independent key for HMACs. Encryption and HMAC keys
 * are never the same key - they answer different questions
 * (getCurrentEncryptionKey() vs getCurrentHmacKeys()) and, in a real
 * deployment, would typically come from entirely separate configuration.
 */
public class InMemoryCryptoKeyProvider implements CryptoKeyProvider {

    private static final String CURRENT_ENCRYPTION_KEY_ID = "workshop-encryption-key";
    private static final String CURRENT_HMAC_KEY_ID = "workshop-hmac-key";
    private static final Map<String, CryptoKey> KEYS_BY_ID = buildKeys();

    @Override
    public CryptoKey getById(String cryptoKeyId) {
        return KEYS_BY_ID.get(cryptoKeyId);
    }

    @Override
    public CryptoKey getCurrentEncryptionKey() {
        return getById(CURRENT_ENCRYPTION_KEY_ID);
    }

    // --8<-- [start:current-hmac-keys]
    @Override
    public List<CryptoKey> getCurrentHmacKeys() {
        // The previous stages' answer: no HMAC keys, because nothing needed one.
        List<CryptoKey> currentHmacKeys = List.of();

        currentHmacKeys = List.of(getById(CURRENT_HMAC_KEY_ID));

        return currentHmacKeys;
    }
    // --8<-- [end:current-hmac-keys]

    @Override
    public List<CryptoKey> getAllCryptoKeys() {
        return List.copyOf(KEYS_BY_ID.values());
    }

    private static Map<String, CryptoKey> buildKeys() {
        CryptoKey encryptionKey = buildKey(
                CURRENT_ENCRYPTION_KEY_ID, CryptoKeyUsage.ENCRYPTION,
                "workshop-demo-passphrase-do-not-use-in-production", "workshop-demo-salt");
        CryptoKey hmacKey = buildKey(
                CURRENT_HMAC_KEY_ID, CryptoKeyUsage.HMAC,
                "workshop-hmac-passphrase-do-not-use-in-production", "workshop-hmac-salt");
        return Map.of(encryptionKey.getId(), encryptionKey, hmacKey.getId(), hmacKey);
    }

    private static CryptoKey buildKey(String id, CryptoKeyUsage usage, String passPhrase, String salt) {
        CryptoKey key = new CryptoKey();
        key.setId(id);
        key.setUsage(usage);
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
