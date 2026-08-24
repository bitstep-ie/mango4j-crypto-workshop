package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.annotations.Encrypt;
import ie.bitstep.mango.crypto.annotations.EncryptedData;

public class PaymentCardEntity {

    // --8<-- [start:encrypt-field]
    // TODO:START annotate-encrypt
    @Encrypt
    // TODO:END annotate-encrypt
    private transient String cardNumber;
    // --8<-- [end:encrypt-field]

    // --8<-- [start:encrypted-data-field]
    // TODO:START annotate-encrypted-data
    @EncryptedData
    // TODO:END annotate-encrypted-data
    private String encryptedData;
    // --8<-- [end:encrypted-data-field]

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
