package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.annotations.Encrypt;
import ie.bitstep.mango.crypto.annotations.EncryptedData;

/**
 * The other side of the cutover: cardNumber is transient again, with no
 * {@code @EnableMigrationSupport} needed, because nothing outside
 * CryptoShield reads it directly anymore. This is identical in shape to
 * every entity from Real Encryption onward - successfully migrating a
 * field means ending up back at the normal, unremarkable case, not at some
 * new permanent state.
 */
public class MigratedEntity {

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
