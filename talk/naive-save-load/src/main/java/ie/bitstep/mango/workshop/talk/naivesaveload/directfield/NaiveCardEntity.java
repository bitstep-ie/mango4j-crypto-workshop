package ie.bitstep.mango.workshop.talk.naivesaveload.directfield;

/**
 * A single entity doing every job at once: domain object, JPA-style entity, and
 * ciphertext container. {@code iv} and {@code cardNumberHmac} are separate sibling
 * columns (the naive habit {@code docs/talk/structured-ciphertext.md} opens with), but
 * {@code cardNumber} itself is the one field the naive design reuses for both
 * representations: plaintext most of the time, ciphertext for the part of
 * {@link NaiveCardStore#save} where it has just been encrypted and not yet restored.
 */
public final class NaiveCardEntity {

    private final long id;
    private String cardNumber;
    private String iv;
    private String cardNumberHmac;

    public NaiveCardEntity(long id, String cardNumber) {
        this.id = id;
        this.cardNumber = cardNumber;
    }

    public long id() {
        return id;
    }

    public String cardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String iv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String cardNumberHmac() {
        return cardNumberHmac;
    }

    public void setCardNumberHmac(String cardNumberHmac) {
        this.cardNumberHmac = cardNumberHmac;
    }
}
