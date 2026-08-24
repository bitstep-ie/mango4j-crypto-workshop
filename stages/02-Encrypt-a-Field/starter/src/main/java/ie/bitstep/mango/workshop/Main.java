package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.encryption.impl.test.Base64EncryptionService;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        CryptoShield cryptoShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(new InMemoryCryptoKeyProvider())
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(new Base64EncryptionService()))
                .build();

        PaymentCardEntity card = new PaymentCardEntity();
        card.setCardNumber("4111111111111111");

        cryptoShield.encrypt(card);
        System.out.println("cardNumber (still in memory): " + card.getCardNumber());
        System.out.println("encryptedData:                " + card.getEncryptedData());

        // Simulate loading the entity back from storage: only the ciphertext is known.
        PaymentCardEntity loaded = new PaymentCardEntity();
        loaded.setEncryptedData(card.getEncryptedData());

        cryptoShield.decrypt(loaded);
        System.out.println("decrypted cardNumber:         " + loaded.getCardNumber());
    }
}
