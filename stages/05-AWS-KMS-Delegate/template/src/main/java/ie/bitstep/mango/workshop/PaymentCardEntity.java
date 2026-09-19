package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.annotations.Encrypt;
import ie.bitstep.mango.crypto.annotations.EncryptedData;

/**
 * Unchanged from Real Encryption - swapping delegates never touches the
 * entity. That's the point of the annotation model: cardNumber doesn't
 * know or care whether PBKDF2EncryptionService or
 * AwsKmsEncryptionServiceDelegate is the one actually performing the
 * encryption underneath it.
 */
public class PaymentCardEntity {

    @Encrypt
    private transient String cardNumber;

    @EncryptedData
    private String encryptedData;

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(String encryptedData) {
        this.encryptedData = encryptedData;
    }
}
