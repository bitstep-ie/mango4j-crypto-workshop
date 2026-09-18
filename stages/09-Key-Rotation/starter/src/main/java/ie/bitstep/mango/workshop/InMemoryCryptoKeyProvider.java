package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.domain.CryptoKeyUsage;
import ie.bitstep.mango.crypto.core.providers.CryptoKeyProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Key Aliases & Crypto Key Configs built two providers, each fixed at
 * construction to a different "current" key, to prove getById() resolves
 * either one. This provider is a single instance whose "current" key can
 * change while the application keeps running - the realistic shape a
 * rotation actually takes: the same CryptoKeyProvider, mid-lifetime,
 * repointed at a new key.
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

    /**
     * The entire rotation, application-side: repoint "current" at a
     * different, already-known key id. Nothing about CryptoShield, the
     * entity, or any already-persisted ciphertext has to change.
     */
    public void rotateEncryptionKeyTo(String newCurrentEncryptionKeyId) {
        /* TODO: Assign currentEncryptionKeyId to newCurrentEncryptionKeyId.
         */
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
