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

        // TODO:START key-type-and-config
        /* TODO: Give this key a real encryption mechanism.
         *
         * 1. Set keyType to "PBKDF2". This is the string mango4j-crypto matches
         *    against each delegate's supportedCryptoKeyType() to pick which one
         *    handles this key - it must line up with the delegate you wired up
         *    in Main.
         *
         * 2. Set keyConfiguration to the recipe that mechanism needs.
         *    PBKDF2EncryptionService derives an AES key from a passphrase, so it
         *    needs the algorithm, mode, padding, key size, iteration count, GCM tag
         *    length, IV size, passphrase and salt. None of this is key material -
         *    it's how to produce the key, plus the cipher parameters. In a real
         *    system these values (and a real passphrase) come from configuration
         *    or a secret store, never a hardcoded literal.
         */
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
        // TODO:END key-type-and-config

        key.setType(keyType);
        key.setConfiguration(keyConfiguration);
        // --8<-- [end:key-config]

        return key;
    }
}
