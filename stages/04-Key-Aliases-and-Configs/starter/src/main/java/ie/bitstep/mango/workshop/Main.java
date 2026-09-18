package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;
import ie.bitstep.mango.crypto.core.encryption.impl.PBKDF2EncryptionService;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        EncryptionServiceDelegate delegate = new PBKDF2EncryptionService();

        // Before this stage there was only ever one shield/provider - both
        // start out as the same thing, both pointed at the current key.
        CryptoShield lastYearsShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(new InMemoryCryptoKeyProvider("workshop-encryption-key"))
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();
        CryptoShield todaysShield = lastYearsShield;

        /* TODO: Build two CryptoShields whose providers each name a
         * different key as "current":
         *
         * 1. lastYearsShield - as if built back when "workshop-archive-key"
         *    was current.
         * 2. todaysShield - built today, with "workshop-encryption-key"
         *    current.
         *
         * Both providers can resolve either key by id via getById(); only
         * which one is "current" for each differs.
         */

        // Simulate a record encrypted a while ago, back when the archive key
        // was current.
        PaymentCardEntity archived = new PaymentCardEntity();
        archived.setCardNumber("5111111111111111");
        lastYearsShield.encrypt(archived);
        System.out.println("archived encryptedData:  " + archived.getEncryptedData());

        // Today's shield has a different "current" key, but can still
        // decrypt it: decrypt() reads the key id recorded on the ciphertext
        // and resolves it by id via getById(), current or not.
        PaymentCardEntity loaded = new PaymentCardEntity();
        loaded.setEncryptedData(archived.getEncryptedData());
        todaysShield.decrypt(loaded);
        System.out.println("decrypted by today's shield: " + loaded.getCardNumber());

        // New writes go out under whichever key is current for the shield
        // doing the writing.
        PaymentCardEntity fresh = new PaymentCardEntity();
        fresh.setCardNumber("5111111111111111");
        todaysShield.encrypt(fresh);
        System.out.println("fresh encryptedData:     " + fresh.getEncryptedData());
    }
}
