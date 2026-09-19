package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;
import ie.bitstep.mango.crypto.core.encryption.impl.PBKDF2EncryptionService;
import ie.bitstep.mango.crypto.delegates.aws.kms.impl.service.encryption.AwsKmsEncryptionServiceDelegate;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        // --8<-- [start:choose-delegate]
        // The previous stage's delegate: PBKDF2EncryptionService, real
        // cryptography with no external service to talk to.
        EncryptionServiceDelegate delegate = new PBKDF2EncryptionService();

        delegate = new AwsKmsEncryptionServiceDelegate(new FakeKmsClient());
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
        card.setCardNumber("5111111111111111");

        cryptoShield.encrypt(card);
        System.out.println("cardNumber (still in memory): " + card.getCardNumber());
        System.out.println("encryptedData:                " + card.getEncryptedData());
        // --8<-- [end:encrypt]

        // --8<-- [start:decrypt]
        PaymentCardEntity loaded = new PaymentCardEntity();
        loaded.setEncryptedData(card.getEncryptedData());

        cryptoShield.decrypt(loaded);
        System.out.println("decrypted cardNumber:         " + loaded.getCardNumber());
        // --8<-- [end:decrypt]
    }
}
