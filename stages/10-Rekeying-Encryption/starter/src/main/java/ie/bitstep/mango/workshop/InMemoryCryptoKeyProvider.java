package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.domain.CryptoKeyUsage;
import ie.bitstep.mango.crypto.core.providers.CryptoKeyProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The key map is now shared and mutable (a static ConcurrentHashMap, not
 * an immutable Map.of()): RekeyScheduler runs on its own background
 * thread, mutating CryptoKey.rekeyMode directly and calling back into
 * RekeyCryptoKeyManager to remove keys once they're safe to retire, so the
 * same CryptoKey objects need to be visible everywhere, live.
 */
public class InMemoryCryptoKeyProvider implements CryptoKeyProvider {

    private static final Map<String, CryptoKey> KEYS_BY_ID = new ConcurrentHashMap<>(buildKeys());

    private final String currentEncryptionKeyId;

    public InMemoryCryptoKeyProvider(String currentEncryptionKeyId) {
        this.currentEncryptionKeyId = currentEncryptionKeyId;
    }

    @Override
    public CryptoKey getById(String cryptoKeyId) {
        return KEYS_BY_ID.get(cryptoKeyId);
    }

    @Override
    public CryptoKey getCurrentEncryptionKey() {
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

    /**
     * Marks a key for retirement: RekeyScheduler picks this up on its next
     * cycle and sweeps every record still using it onto whichever key is
     * "current" for this stage's shield.
     */
    public void markForRetirement(String keyId) {
        getById(keyId).setRekeyMode(CryptoKey.RekeyMode.KEY_OFF);
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
        CryptoKey encryptionKeyV1 = buildKey(
                "workshop-encryption-key", longAgo,
                "workshop-demo-passphrase-do-not-use-in-production", "workshop-demo-salt");
        CryptoKey encryptionKeyV2 = buildKey(
                "workshop-encryption-key-v2", longAgo.plusSeconds(60),
                "workshop-demo-passphrase-v2-do-not-use-in-production", "workshop-demo-salt-v2");
        return Map.of(encryptionKeyV1.getId(), encryptionKeyV1, encryptionKeyV2.getId(), encryptionKeyV2);
    }

    private static CryptoKey buildKey(String id, Instant createdDate, String passPhrase, String salt) {
        CryptoKey key = new CryptoKey();
        key.setId(id);
        key.setUsage(CryptoKeyUsage.ENCRYPTION);
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
