package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;
import ie.bitstep.mango.crypto.core.encryption.impl.PBKDF2EncryptionService;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        EncryptionServiceDelegate delegate = new PBKDF2EncryptionService();

        CryptoShield originalShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(new InMemoryCryptoKeyProvider(List.of("workshop-hmac-key")))
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();

        PaymentCardStore store = new PaymentCardStore();

        PaymentCardEntity card = new PaymentCardEntity();
        card.setCardNumber("5111111111111111");
        originalShield.encrypt(card);
        store.save(card);

        PaymentCardEntity searchProbe = new PaymentCardEntity();
        searchProbe.setCardNumber("5111111111111111");
        originalShield.encrypt(searchProbe);
        System.out.println("search found the record?                       " + store.findByLookup(searchProbe).isPresent());

        // Rotate the HMAC key the way Key Rotation recommends: add the new
        // key while the old one stays active, rather than swapping outright
        // the way the previous stage's shield did.
        CryptoShield rotatingShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(new InMemoryCryptoKeyProvider(List.of("workshop-hmac-key", "workshop-hmac-key-v2")))
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();

        PaymentCardEntity duplicateDuringRotation = new PaymentCardEntity();
        duplicateDuringRotation.setCardNumber("5111111111111111");
        rotatingShield.encrypt(duplicateDuringRotation);
        boolean acceptedDuringRotation = store.save(duplicateDuringRotation);
        System.out.println("duplicate accepted while old key still active? " + acceptedDuringRotation);
    }
}
