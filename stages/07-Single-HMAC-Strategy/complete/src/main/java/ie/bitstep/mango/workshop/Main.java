package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;
import ie.bitstep.mango.crypto.core.encryption.impl.PBKDF2EncryptionService;

import java.util.List;
import java.util.Optional;

public class Main {

    public static void main(String[] args) {
        EncryptionServiceDelegate delegate = new PBKDF2EncryptionService();

        CryptoShield originalShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(new InMemoryCryptoKeyProvider("workshop-hmac-key"))
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();

        PaymentCardStore store = new PaymentCardStore();

        // --8<-- [start:save-and-search]
        PaymentCardEntity card = new PaymentCardEntity();
        card.setCardNumber("5111111111111111");
        originalShield.encrypt(card);
        store.save(card);

        // Searching means: HMAC the search term under the same key, then
        // look it up by equality - encryptedData never enters into it.
        PaymentCardEntity searchProbe = new PaymentCardEntity();
        searchProbe.setCardNumber("5111111111111111");
        originalShield.encrypt(searchProbe);
        Optional<PaymentCardEntity> found = store.findByHmac(searchProbe.getCardNumberHmac());
        System.out.println("search found the record?               " + found.isPresent());
        // --8<-- [end:save-and-search]

        // --8<-- [start:reject-duplicate]
        // A second entity with the same card number hashes to the same
        // HMAC under the same key, so it's correctly rejected as a duplicate.
        PaymentCardEntity duplicate = new PaymentCardEntity();
        duplicate.setCardNumber("5111111111111111");
        originalShield.encrypt(duplicate);
        boolean saved = store.save(duplicate);
        System.out.println("duplicate accepted (same key)?         " + saved);
        // --8<-- [end:reject-duplicate]

        // --8<-- [start:rotation-blind-spot]
        // Now simulate an HMAC key rotation: a shield built after the HMAC
        // key changed, querying the same store, which still only holds
        // "card"'s HMAC computed under the old key - nothing has rekeyed it.
        CryptoShield rotatedShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(new InMemoryCryptoKeyProvider("workshop-hmac-key-v2"))
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();

        PaymentCardEntity duplicateAfterRotation = new PaymentCardEntity();
        duplicateAfterRotation.setCardNumber("5111111111111111");
        rotatedShield.encrypt(duplicateAfterRotation);
        boolean acceptedAfterRotation = store.save(duplicateAfterRotation);
        System.out.println("duplicate accepted (after key rotation)? " + acceptedAfterRotation);
        // --8<-- [end:rotation-blind-spot]
    }
}
