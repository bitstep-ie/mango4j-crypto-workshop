package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.domain.CryptoKeyUsage;
import ie.bitstep.mango.crypto.core.providers.CryptoKeyProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Same alias mechanism as the previous stages, but now with two HMAC keys
 * on file: "workshop-hmac-key" and "workshop-hmac-key-v2", simulating an
 * HMAC key rotation. Which one is "current" is decided per provider
 * instance, the same way Key Aliases & Key Configs varied which encryption
 * key was current - here it's what lets Main build a shield "from before
 * the rotation" and a shield "from after" it.
 */
public class InMemoryCryptoKeyProvider implements CryptoKeyProvider {

    private static final String CURRENT_ENCRYPTION_KEY_ID = "workshop-encryption-key";
    private static final Map<String, CryptoKey> KEYS_BY_ID = buildKeys();

    private final String currentHmacKeyId;

    public InMemoryCryptoKeyProvider(String currentHmacKeyId) {
        this.currentHmacKeyId = currentHmacKeyId;
    }

    @Override
    public CryptoKey getById(String cryptoKeyId) {
        return KEYS_BY_ID.get(cryptoKeyId);
    }

    @Override
    public CryptoKey getCurrentEncryptionKey() {
        return getById(CURRENT_ENCRYPTION_KEY_ID);
    }

    @Override
    public List<CryptoKey> getCurrentHmacKeys() {
        return List.of(getById(currentHmacKeyId));
    }

    @Override
    public List<CryptoKey> getAllCryptoKeys() {
        return List.copyOf(KEYS_BY_ID.values());
    }

    private static Map<String, CryptoKey> buildKeys() {
        CryptoKey encryptionKey = buildKey(
                CURRENT_ENCRYPTION_KEY_ID, CryptoKeyUsage.ENCRYPTION,
                "workshop-demo-passphrase-do-not-use-in-production", "workshop-demo-salt");
        CryptoKey hmacKeyV1 = buildKey(
                "workshop-hmac-key", CryptoKeyUsage.HMAC,
                "workshop-hmac-passphrase-do-not-use-in-production", "workshop-hmac-salt");
        CryptoKey hmacKeyV2 = buildKey(
                "workshop-hmac-key-v2", CryptoKeyUsage.HMAC,
                "workshop-hmac-passphrase-v2-do-not-use-in-production", "workshop-hmac-salt-v2");
        return Map.of(
                encryptionKey.getId(), encryptionKey,
                hmacKeyV1.getId(), hmacKeyV1,
                hmacKeyV2.getId(), hmacKeyV2
        );
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
