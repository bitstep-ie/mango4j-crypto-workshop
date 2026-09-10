package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;
import ie.bitstep.mango.crypto.core.encryption.impl.PBKDF2EncryptionService;
import ie.bitstep.mango.crypto.core.encryption.impl.test.Base64EncryptionService;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        // --8<-- [start:choose-delegate]
        // The previous stage's delegate: Base64EncryptionService, a fake "encryption"
        // that just Base64-encodes the payload so the ciphertext is trivially readable.
        EncryptionServiceDelegate delegate = new Base64EncryptionService();

        delegate = new PBKDF2EncryptionService();
        // --8<-- [end:choose-delegate]

        // --8<-- [start:build-shield]
        CryptoShield cryptoShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(new InMemoryCryptoKeyProvider())
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();
        // --8<-- [end:build-shield]

        // --8<-- [start:encrypt]
        PaymentCardEntity card = new PaymentCardEntity();
        card.setCardNumber("4111111111111111");

        cryptoShield.encrypt(card);
        System.out.println("cardNumber (still in memory): " + card.getCardNumber());
        System.out.println("encryptedData:                " + card.getEncryptedData());
        // --8<-- [end:encrypt]

        // --8<-- [start:decrypt]
        // Loading the entity back from storage: only the ciphertext is known. The
        // structured ciphertext records which key encrypted it, so decrypt() asks the
        // provider for that key by ID and gets back the config it needs - nothing here
        // has to know it's PBKDF2.
        PaymentCardEntity loaded = new PaymentCardEntity();
        loaded.setEncryptedData(card.getEncryptedData());

        cryptoShield.decrypt(loaded);
        System.out.println("decrypted cardNumber:         " + loaded.getCardNumber());
        // --8<-- [end:decrypt]
    }
}
