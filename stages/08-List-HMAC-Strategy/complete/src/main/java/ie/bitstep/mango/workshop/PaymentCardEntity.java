package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.annotations.Encrypt;
import ie.bitstep.mango.crypto.annotations.EncryptedData;
import ie.bitstep.mango.crypto.annotations.Hmac;
import ie.bitstep.mango.crypto.annotations.strategies.ListHmacStrategy;
import ie.bitstep.mango.crypto.domain.CryptoShieldHmacHolder;
import ie.bitstep.mango.crypto.domain.Lookup;
import ie.bitstep.mango.crypto.domain.Unique;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ie.bitstep.mango.crypto.annotations.Hmac.Purposes.LOOKUP;
import static ie.bitstep.mango.crypto.annotations.Hmac.Purposes.UNIQUE;

/**
 * Where the Single HMAC Strategy wrote one HMAC value into one plain field,
 * List HMAC Strategy writes one HMAC per currently-active key into a list -
 * hence implementing {@link Lookup} and {@link Unique} instead of declaring
 * {@code cardNumberHmac}/{@code hmacKeyId} fields directly. purposes =
 * {LOOKUP, UNIQUE} asks for both: the same field feeds search and
 * uniqueness enforcement, each tracked in its own list since an entity can
 * have fields serving only one purpose or the other.
 */
// --8<-- [start:annotate-list-hmac]
@ListHmacStrategy
public class PaymentCardEntity implements Lookup, Unique {

    @Encrypt
    @Hmac(purposes = {LOOKUP, UNIQUE})
    private transient String cardNumber;
    // --8<-- [end:annotate-list-hmac]

    @EncryptedData
    private String encryptedData;

    private List<CryptoShieldHmacHolder> lookups = new ArrayList<>();
    private List<CryptoShieldHmacHolder> uniqueValues = new ArrayList<>();

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

    @Override
    public void setLookups(Collection<CryptoShieldHmacHolder> hmacHolders) {
        this.lookups = new ArrayList<>(hmacHolders);
    }

    @Override
    public Collection<CryptoShieldHmacHolder> getLookups() {
        return lookups;
    }

    @Override
    public void setUniqueValues(Collection<CryptoShieldHmacHolder> hmacHolders) {
        this.uniqueValues = new ArrayList<>(hmacHolders);
    }

    @Override
    public List<CryptoShieldHmacHolder> getUniqueValues() {
        return uniqueValues;
    }
}
