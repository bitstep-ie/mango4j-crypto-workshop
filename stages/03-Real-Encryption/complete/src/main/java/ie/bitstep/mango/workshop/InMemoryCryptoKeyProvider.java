package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.domain.CryptoKeyUsage;
import ie.bitstep.mango.crypto.core.providers.CryptoKeyProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Still the simplest possible CryptoKeyProvider - one hardcoded key kept in memory.
 * The only change from the previous stage is what that key <em>says about itself</em>:
 * its {@code type} now names a real encryption mechanism, and its {@code configuration}
 * carries everything that mechanism needs to operate. Application code never sees any
 * of this - it just asks for "the current encryption key" and gets an object back.
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

        // --8<-- [start:key-config]
        // The previous stage's key was just this: a type of "BASE_64" and no
        // configuration at all, because the fake Base64 delegate needs none.
        String keyType = "BASE_64";
        Map<String, Object> keyConfiguration = Map.of();

        keyType = "PBKDF2";
        keyConfiguration = Map.of(
                "algorithm", "AES",
                "mode", "GCM",
                "padding", "NoPadding",
                "keySize", 256,
                "iterations", 10000,
                "gcmTagLength", 128,
                "ivSize", 16,
                "passPhrase", "workshop-demo-passphrase-do-not-use-in-production",
                "salt", "workshop-demo-salt"
        );

        key.setType(keyType);
        key.setConfiguration(keyConfiguration);
        // --8<-- [end:key-config]

        return key;
    }
}
