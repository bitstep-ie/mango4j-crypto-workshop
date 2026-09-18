package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;
import ie.bitstep.mango.crypto.core.encryption.impl.PBKDF2EncryptionService;

import java.util.List;
import java.util.Objects;

public class Main {

    public static void main(String[] args) {
        EncryptionServiceDelegate delegate = new PBKDF2EncryptionService();

        CryptoShield cryptoShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(new InMemoryCryptoKeyProvider())
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();

        // Two separate entities, same plaintext card number. encrypt() computes
        // both the ciphertext and the HMAC in one call - no separate hmac() step.
        PaymentCardEntity first = new PaymentCardEntity();
        first.setCardNumber("5111111111111111");
        cryptoShield.encrypt(first);

        PaymentCardEntity second = new PaymentCardEntity();
        second.setCardNumber("5111111111111111");
        cryptoShield.encrypt(second);

        System.out.println("first  encryptedData: " + first.getEncryptedData());
        System.out.println("second encryptedData: " + second.getEncryptedData());
        System.out.println("same ciphertext?       " + first.getEncryptedData().equals(second.getEncryptedData()));
        System.out.println();
        System.out.println("first  cardNumberHmac: " + first.getCardNumberHmac());
        System.out.println("second cardNumberHmac: " + second.getCardNumberHmac());
        System.out.println("same HMAC?              " + Objects.equals(first.getCardNumberHmac(), second.getCardNumberHmac()));
        System.out.println();
        System.out.println("hmacKeyId:             " + first.getHmacKeyId());
    }
}
