package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;
import ie.bitstep.mango.crypto.core.encryption.impl.PBKDF2EncryptionService;
import ie.bitstep.mango.crypto.delegates.aws.kms.impl.service.encryption.AwsKmsEncryptionServiceDelegate;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        // The previous stage's delegate: PBKDF2EncryptionService, real
        // cryptography with no external service to talk to.
        EncryptionServiceDelegate delegate = new PBKDF2EncryptionService();

        /* TODO: Replace the delegate above with the real KMS-backed one.
         *
         * AwsKmsEncryptionServiceDelegate ships in a separate module,
         * mango4j-crypto-aws-kms-delegate, and needs a KmsClient to talk
         * to. In a real deployment that's the AWS SDK's own
         * KmsClient.builder().build() (talking to real AWS KMS). This
         * workshop supplies FakeKmsClient instead - same interface, no
         * network, no AWS account needed - so the wiring below is exactly
         * what a production deployment looks like, even though nothing
         * here actually calls AWS.
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

        PaymentCardEntity loaded = new PaymentCardEntity();
        loaded.setEncryptedData(card.getEncryptedData());

        cryptoShield.decrypt(loaded);
        System.out.println("decrypted cardNumber:         " + loaded.getCardNumber());
    }
}
