package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.annotations.Encrypt;
import ie.bitstep.mango.crypto.annotations.EncryptedData;

/**
 * Unchanged from Key Rotation. Rekeying doesn't touch the entity either -
 * it's a batch operation that calls the same decrypt()/encrypt() every
 * other stage has already used, just run record by record over what's
 * already persisted.
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
