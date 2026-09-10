package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;
import ie.bitstep.mango.crypto.core.encryption.impl.PBKDF2EncryptionService;
import ie.bitstep.mango.crypto.core.encryption.impl.test.Base64EncryptionService;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        // The previous stage's delegate: Base64EncryptionService, a fake "encryption"
        // that just Base64-encodes the payload so the ciphertext is trivially readable.
        EncryptionServiceDelegate delegate = new Base64EncryptionService();

        /* TODO: Replace the delegate above with a real one.
         *
         * PBKDF2EncryptionService uses the JDK's own cryptography (AES/GCM here, with
         * an AES key derived from the passphrase in the key's configuration). It ships
         * with mango4j-crypto and needs no external service, so it's a realistic
         * delegate you can run locally. Reassign `delegate` to a new instance of it:
         */

        CryptoShield cryptoShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(new InMemoryCryptoKeyProvider())
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();

        PaymentCardEntity card = new PaymentCardEntity();
        card.setCardNumber("5111111111111111");

        cryptoShield.encrypt(card);
        System.out.println("cardNumber (still in memory): " + card.getCardNumber());
        System.out.println("encryptedData:                " + card.getEncryptedData());

        // Loading the entity back from storage: only the ciphertext is known. The
        // structured ciphertext records which key encrypted it, so decrypt() asks the
        // provider for that key by ID and gets back the config it needs - nothing here
        // has to know it's PBKDF2.
        PaymentCardEntity loaded = new PaymentCardEntity();
        loaded.setEncryptedData(card.getEncryptedData());

        cryptoShield.decrypt(loaded);
        System.out.println("decrypted cardNumber:         " + loaded.getCardNumber());
    }
}
