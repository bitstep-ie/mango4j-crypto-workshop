package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.annotations.EnableMigrationSupport;
import ie.bitstep.mango.crypto.annotations.Encrypt;
import ie.bitstep.mango.crypto.annotations.EncryptedData;

/**
 * Same shape as InProgressMigrationEntity, but its completedBy date has
 * already passed - a migration that was supposed to be finished by now,
 * and wasn't. @EnableMigrationSupport still lets the application run; it
 * just changes what gets logged.
 */
public class OverdueMigrationEntity {

    /* TODO: Add @EnableMigrationSupport above this field too - this time
     * with a completedBy date already in the past, to see what changes
     * once the deadline has been missed.
     */
    @Encrypt
    private String cardNumber;

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
