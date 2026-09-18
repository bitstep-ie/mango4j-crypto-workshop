package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.annotations.Encrypt;
import ie.bitstep.mango.crypto.annotations.EncryptedData;

/**
 * Unchanged from Real Encryption - this stage rotates the encryption key,
 * not the entity. HMAC fields are deliberately left out: HMAC keys rotate
 * through a list of active keys (see List HMAC Strategy), a different
 * mechanism from the single "current" encryption key this stage covers.
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
