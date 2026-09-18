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
    // TODO:START annotate-hmac
    /* TODO: Add @Hmac above this field, alongside the existing @Encrypt.
     * A field can be both confidential (encrypted) and searchable (HMAC'd)
     * at once - the two annotations are independent of each other.
     */
    @Hmac
    // TODO:END annotate-hmac
    // --8<-- [end:annotate-hmac]
    private transient String cardNumber;

    @EncryptedData
    private String encryptedData;

    // --8<-- [start:hmac-fields]
    // The Single HMAC Strategy requires this field to exist, named after
    // the source field plus "Hmac" (mandatory naming convention). The
    // library writes the computed HMAC value here.
    private String cardNumberHmac;

    // TODO:START annotate-hmac-key-id
    /* TODO: Add @HmacKeyId above this field. It records which HMAC key
     * produced cardNumberHmac - the same role @EncryptionKeyId plays for
     * encryption, but tracked separately, since HMAC and encryption keys
     * are never the same key. The Single HMAC Strategy requires exactly
     * one such field, of type String.
     */
    @HmacKeyId
    // TODO:END annotate-hmac-key-id
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
