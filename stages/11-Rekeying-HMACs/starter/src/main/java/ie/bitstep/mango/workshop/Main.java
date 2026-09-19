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

        // Add the new HMAC key alongside the old one - the old key stays
        // active, exactly as List HMAC Strategy's rotation scenario did.
        provider.addActiveHmacKey("workshop-hmac-key-v2");

        // The sweep only reaches the first two records, simulating a
        // rekey job that's still in progress - the third record hasn't
        // been touched yet.
        List<PaymentCardEntity> allRecords = store.all();
        RekeyHmacSweep.run(cryptoShield, allRecords.subList(0, 2));

        /* TODO: Finish the sweep before the old key gets retired below:
         * rekey the remaining record(s) too.
         */

        // Only remove the old key from the active list once every record
        // has been swept - this line assumes that's already true.
        provider.removeActiveHmacKey("workshop-hmac-key");

        PaymentCardEntity searchProbe = new PaymentCardEntity();
        searchProbe.setCardNumber("5333333333333335");
        cryptoShield.encrypt(searchProbe);
        System.out.println("previously-unswept record still findable? " + store.findByLookup(searchProbe).isPresent());
    }
}
