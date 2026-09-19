package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.domain.CryptoKeyUsage;
import ie.bitstep.mango.crypto.core.providers.CryptoKeyProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Still the simplest possible provider - one hardcoded key. As with Real
 * Encryption, the only change from the previous delegate is what this key
 * says about itself: its type now names the AWS KMS delegate, and its
 * configuration carries what that delegate needs - a KMS key id and the
 * encryption algorithm to use. Never key material: KMS never lets key
 * material leave the service, fake or real.
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

        // The previous stage's key was type "PBKDF2", needing a passphrase
        // and cipher parameters it had to carry itself.
        String keyType = "PBKDF2";
        Map<String, Object> keyConfiguration = Map.of(
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

        /* TODO: Point this key at the (fake) KMS delegate instead.
         *
         * 1. Set keyType to "AWS_KMS" - AwsKmsEncryptionServiceDelegate
         *    reports this from supportedCryptoKeyType(), and that string
         *    match is what routes this key to it (see Main).
         *
         * 2. Set keyConfiguration to what that delegate needs: an
         *    "awsKeyId" (a KMS key id or ARN in a real deployment - this
         *    fake accepts any string) and an "algorithm" -
         *    "SYMMETRIC_DEFAULT" for a standard symmetric KMS key.
         */

        key.setType(keyType);
        key.setConfiguration(keyConfiguration);

        return key;
    }
}
