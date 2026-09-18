package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.annotations.Encrypt;
import ie.bitstep.mango.crypto.annotations.EncryptedData;
import ie.bitstep.mango.crypto.annotations.Hmac;
import ie.bitstep.mango.crypto.annotations.HmacKeyId;
import ie.bitstep.mango.crypto.annotations.strategies.SingleHmacStrategy;

/**
 * cardNumber now carries a second annotation alongside @Encrypt: @Hmac,
 * which asks mango4j-crypto to also compute a deterministic HMAC of the
 * plaintext value. @SingleHmacStrategy (class-level) selects which of the
 * library's HMAC storage strategies to use for this entity - the simplest
 * one, one HMAC value per field. What that strategy is good for (and where
 * it falls short) is this stage's follow-on, not this one.
 */
@SingleHmacStrategy
public class PaymentCardEntity {

    @Encrypt
    // --8<-- [start:annotate-hmac]
    @Hmac
    // --8<-- [end:annotate-hmac]
    private transient String cardNumber;

    @EncryptedData
    private String encryptedData;

    // --8<-- [start:hmac-fields]
    // The Single HMAC Strategy requires this field to exist, named after
    // the source field plus "Hmac" (mandatory naming convention). The
    // library writes the computed HMAC value here.
    private String cardNumberHmac;

    @HmacKeyId
    private String hmacKeyId;
    // --8<-- [end:hmac-fields]

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

    public String getCardNumberHmac() {
        return cardNumberHmac;
    }

    public void setCardNumberHmac(String cardNumberHmac) {
        this.cardNumberHmac = cardNumberHmac;
    }

    public String getHmacKeyId() {
        return hmacKeyId;
    }

    public void setHmacKeyId(String hmacKeyId) {
        this.hmacKeyId = hmacKeyId;
    }
}
