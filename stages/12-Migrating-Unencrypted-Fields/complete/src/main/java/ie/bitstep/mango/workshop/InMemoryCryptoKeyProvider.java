package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.domain.CryptoKeyUsage;
import ie.bitstep.mango.crypto.core.providers.CryptoKeyProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The simplest possible provider again - one key, nothing rotating. This
 * stage isn't about keys at all; it's about what CryptoShield requires of
 * the fields it's asked to manage.
 */
public class InMemoryCryptoKeyProvider implements CryptoKeyProvider {

    private final CryptoKey encryptionKey = buildEncryptionKey();

    @Override
    public CryptoKey getById(String cryptoKeyId) {
        return encryptionKey;
    }

    @Override
    public CryptoKey getCurrentEncryptionKey() {
        return encryptionKey;
    }

    @Override
    public List<CryptoKey> getCurrentHmacKeys() {
        return List.of();
    }

    @Override
    public List<CryptoKey> getAllCryptoKeys() {
        return List.of(encryptionKey);
    }

    private static CryptoKey buildEncryptionKey() {
        CryptoKey key = new CryptoKey();
        key.setId("workshop-encryption-key");
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
                "passPhrase", "workshop-demo-passphrase-do-not-use-in-production",
                "salt", "workshop-demo-salt"
        ));
        return key;
    }
}
