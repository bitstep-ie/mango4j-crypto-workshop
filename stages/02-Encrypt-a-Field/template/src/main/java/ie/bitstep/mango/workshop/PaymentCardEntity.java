package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.annotations.Encrypt;
import ie.bitstep.mango.crypto.annotations.EncryptedData;

public class PaymentCardEntity {

    // --8<-- [start:encrypt-field]
    // TODO:START annotate-encrypt
    // TODO: Add @Encrypt above this field.
    // TODO: It marks cardNumber as confidential: mango4j-crypto will read
    // TODO: this field's value when building the ciphertext, but never
    // TODO: write to it. The field must stay `transient`, which the
    // TODO: library enforces - it's never meant to be serialized directly.
    @Encrypt
    // TODO:END annotate-encrypt
    private transient String cardNumber;
    // --8<-- [end:encrypt-field]

    // --8<-- [start:encrypted-data-field]
    // TODO:START annotate-encrypted-data
    // TODO: Add @EncryptedData above this field.
    // TODO: This is where the resulting ciphertext gets written - it's
    // TODO: the field you'd actually persist (to a database, a file,
    // TODO: wherever), never cardNumber itself.
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
