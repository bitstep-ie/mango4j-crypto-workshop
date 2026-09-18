package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.annotations.Encrypt;
import ie.bitstep.mango.crypto.annotations.EncryptedData;
import ie.bitstep.mango.crypto.annotations.Hmac;
import ie.bitstep.mango.crypto.annotations.HmacKeyId;
import ie.bitstep.mango.crypto.annotations.strategies.SingleHmacStrategy;

/**
 * Unchanged from the previous stage - the entity itself doesn't know or
 * care whether its HMAC ends up used for search, uniqueness, both, or
 * neither. That decision lives entirely in what application code does
 * with cardNumberHmac after encrypt() sets it, which is this stage's
 * subject.
 */
@SingleHmacStrategy
public class PaymentCardEntity {

    @Encrypt
    @Hmac
    private transient String cardNumber;

    @EncryptedData
    private String encryptedData;

    private String cardNumberHmac;

    @HmacKeyId
    private String hmacKeyId;

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
