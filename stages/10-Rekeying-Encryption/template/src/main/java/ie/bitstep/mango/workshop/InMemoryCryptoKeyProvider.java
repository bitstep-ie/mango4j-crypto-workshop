package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.domain.CryptoKeyUsage;
import ie.bitstep.mango.crypto.core.providers.CryptoKeyProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Unchanged from Key Rotation: a single, live instance whose "current"
 * encryption key can be repointed mid-run via rotateEncryptionKeyTo(). That
 * stage covered new writes; this one is about everything rotateEncryptionKeyTo()
 * leaves behind - records already encrypted under whichever key used to be
 * current.
 */
public class InMemoryCryptoKeyProvider implements CryptoKeyProvider {

    private static final Map<String, CryptoKey> KEYS_BY_ID = buildKeys();

    private String currentEncryptionKeyId = "workshop-encryption-key";

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

    public void rotateEncryptionKeyTo(String newCurrentEncryptionKeyId) {
        this.currentEncryptionKeyId = newCurrentEncryptionKeyId;
    }

    private static Map<String, CryptoKey> buildKeys() {
        CryptoKey encryptionKeyV1 = buildKey(
                "workshop-encryption-key",
                "workshop-demo-passphrase-do-not-use-in-production", "workshop-demo-salt");
        CryptoKey encryptionKeyV2 = buildKey(
                "workshop-encryption-key-v2",
                "workshop-demo-passphrase-v2-do-not-use-in-production", "workshop-demo-salt-v2");
        return Map.of(encryptionKeyV1.getId(), encryptionKeyV1, encryptionKeyV2.getId(), encryptionKeyV2);
    }

    private static CryptoKey buildKey(String id, String passPhrase, String salt) {
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
