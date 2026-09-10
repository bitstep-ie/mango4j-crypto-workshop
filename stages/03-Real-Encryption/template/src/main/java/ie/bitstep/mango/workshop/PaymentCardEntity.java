package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.annotations.Encrypt;
import ie.bitstep.mango.crypto.annotations.EncryptedData;

/**
 * Unchanged from the previous stage: {@code cardNumber} is the confidential
 * working value, {@code encryptedData} is the structured ciphertext that gets
 * persisted. Swapping in a real encryption delegate doesn't touch the entity
 * at all - that's the point of the annotation model.
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
