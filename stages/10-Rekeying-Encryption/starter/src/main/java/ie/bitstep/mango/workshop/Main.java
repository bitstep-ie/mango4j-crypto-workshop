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

        PaymentCardStore store = new PaymentCardStore();
        for (String cardNumber : List.of("5111111111111111", "5222222222222226", "5333333333333335")) {
            PaymentCardEntity card = new PaymentCardEntity();
            card.setCardNumber(cardNumber);
            cryptoShield.encrypt(card);
            store.save(card);
        }

        // Key Rotation already happened - new writes go out under the new
        // key - but these three records are still sitting there under the
        // old one, exactly like beforeRotation was in that stage.
        provider.rotateEncryptionKeyTo("workshop-encryption-key-v2");

        int rekeyedCount = RekeySweep.run(cryptoShield, provider, store);
        System.out.println("records rekeyed on first sweep:  " + rekeyedCount);
        for (PaymentCardEntity record : store.all()) {
            System.out.println("  " + record.getEncryptedData());
        }

        // Running it again should be a safe no-op: every record is already
        // on the current key, so nothing should be decrypted/re-encrypted
        // a second time.
        int secondSweepCount = RekeySweep.run(cryptoShield, provider, store);
        System.out.println("records rekeyed on second sweep: " + secondSweepCount);
    }
}
