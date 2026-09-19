package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.domain.CryptoKeyUsage;
import ie.bitstep.mango.crypto.core.providers.CryptoKeyProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The key map is shared and mutable (a static ConcurrentHashMap, not an
 * immutable Map.of()): RekeyScheduler mutates CryptoKey.rekeyMode and
 * calls back to remove keys, from its own background thread, so the same
 * CryptoKey objects need to be visible everywhere, live. "Current" HMAC
 * keys are still constructor-configured per instance, the same pattern
 * List HMAC Strategy introduced.
 */
public class InMemoryCryptoKeyProvider implements CryptoKeyProvider {

    private static final String CURRENT_ENCRYPTION_KEY_ID = "workshop-encryption-key";
    private static final Map<String, CryptoKey> KEYS_BY_ID = new ConcurrentHashMap<>(buildKeys());

    private final List<String> currentHmacKeyIds;

    public InMemoryCryptoKeyProvider(List<String> currentHmacKeyIds) {
        this.currentHmacKeyIds = currentHmacKeyIds;
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
        return currentHmacKeyIds.stream().map(this::getById).toList();
    }

    @Override
    public List<CryptoKey> getAllCryptoKeys() {
        return List.copyOf(KEYS_BY_ID.values());
    }

    /**
     * Called by RetiringKeyManager once RekeyScheduler confirms nothing is
     * using a retired key anymore.
     */
    static void remove(String keyId) {
        KEYS_BY_ID.remove(keyId);
    }

    private static Map<String, CryptoKey> buildKeys() {
        Instant longAgo = Instant.now().minusSeconds(3600);
        CryptoKey encryptionKey = buildKey(
                CURRENT_ENCRYPTION_KEY_ID, CryptoKeyUsage.ENCRYPTION, longAgo,
                "workshop-demo-passphrase-do-not-use-in-production", "workshop-demo-salt");
        CryptoKey hmacKeyV1 = buildKey(
                "workshop-hmac-key", CryptoKeyUsage.HMAC, longAgo,
                "workshop-hmac-passphrase-do-not-use-in-production", "workshop-hmac-salt");
        CryptoKey hmacKeyV2 = buildKey(
                "workshop-hmac-key-v2", CryptoKeyUsage.HMAC, longAgo.plusSeconds(60),
                "workshop-hmac-passphrase-v2-do-not-use-in-production", "workshop-hmac-salt-v2");
        return Map.of(
                encryptionKey.getId(), encryptionKey,
                hmacKeyV1.getId(), hmacKeyV1,
                hmacKeyV2.getId(), hmacKeyV2
        );
    }

    private static CryptoKey buildKey(String id, CryptoKeyUsage usage, Instant createdDate, String passPhrase, String salt) {
        CryptoKey key = new CryptoKey();
        key.setId(id);
        key.setUsage(usage);
        key.setCreatedDate(createdDate);
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
