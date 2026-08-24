package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.domain.CryptoKeyUsage;
import ie.bitstep.mango.crypto.core.providers.CryptoKeyProvider;

import java.time.Instant;
import java.util.List;

/**
 * The simplest possible CryptoKeyProvider: a single hardcoded Base64 "encryption" key,
 * kept in memory. A real application would look its keys up from wherever it stores them
 * (a database, a KMS, ...) instead.
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
        key.setType("BASE_64");
        key.setUsage(CryptoKeyUsage.ENCRYPTION);
        key.setCreatedDate(Instant.now());
        return key;
    }
}
