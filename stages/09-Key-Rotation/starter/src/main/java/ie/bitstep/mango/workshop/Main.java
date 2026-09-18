package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;
import ie.bitstep.mango.crypto.core.encryption.impl.PBKDF2EncryptionService;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        EncryptionServiceDelegate delegate = new PBKDF2EncryptionService();
        InMemoryCryptoKeyProvider provider = new InMemoryCryptoKeyProvider();

        CryptoShield cryptoShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(provider)
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();

        PaymentCardEntity beforeRotation = new PaymentCardEntity();
        beforeRotation.setCardNumber("5111111111111111");
        cryptoShield.encrypt(beforeRotation);
        System.out.println("before rotation encryptedData: " + beforeRotation.getEncryptedData());

        provider.rotateEncryptionKeyTo("workshop-encryption-key-v2");

        PaymentCardEntity afterRotation = new PaymentCardEntity();
        afterRotation.setCardNumber("5111111111111111");
        cryptoShield.encrypt(afterRotation);
        System.out.println("after rotation encryptedData:  " + afterRotation.getEncryptedData());

        // The record written before the rotation still decrypts correctly:
        // decrypt() reads its recorded key id and resolves it by id via
        // getById(), not by asking what's current right now.
        PaymentCardEntity loaded = new PaymentCardEntity();
        loaded.setEncryptedData(beforeRotation.getEncryptedData());
        cryptoShield.decrypt(loaded);
        System.out.println("old record still decrypts:     " + loaded.getCardNumber());
    }
}
