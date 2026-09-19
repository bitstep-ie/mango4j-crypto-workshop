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
 * Unchanged from List HMAC Strategy. Rekeying HMACs doesn't touch the
 * entity either - it's the same encrypt() call every previous stage has
 * used, just run again over records that already exist, with the active
 * HMAC key list changed underneath it.
 */
@ListHmacStrategy
public class PaymentCardEntity implements Lookup, Unique {

    @Encrypt
    @Hmac(purposes = {LOOKUP, UNIQUE})
    private transient String cardNumber;

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
