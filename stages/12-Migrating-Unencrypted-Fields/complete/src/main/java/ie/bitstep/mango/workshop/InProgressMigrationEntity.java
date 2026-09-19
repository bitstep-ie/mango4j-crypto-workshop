package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.annotations.EnableMigrationSupport;
import ie.bitstep.mango.crypto.annotations.Encrypt;
import ie.bitstep.mango.crypto.annotations.EncryptedData;

/**
 * Every previous stage's cardNumber has been transient - a source value
 * that's never itself persisted, only its ciphertext is. This entity is
 * what a field looks like mid-migration away from being plain, persisted
 * text: something else in the system (a legacy batch export, say) still
 * reads cardNumber directly and isn't ready for it to become transient
 * yet. @EnableMigrationSupport is the escape hatch that lets @Encrypt
 * accept that, for a stated deadline.
 */
public class InProgressMigrationEntity {

    // --8<-- [start:in-progress]
    @EnableMigrationSupport(
            completedBy = "2027-01-01",
            justification = "Legacy nightly export job still reads cardNumber directly; not yet updated to call decrypt()",
            ticket = "WORKSHOP-12"
    )
    @Encrypt
    private String cardNumber;
    // --8<-- [end:in-progress]

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
