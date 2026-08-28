package ie.bitstep.mango.workshop.talk.naivesaveload.directfield;

/**
 * A naive entity with one column doing double duty: {@code username} holds
 * plaintext most of the time, but genuinely holds ciphertext during part of
 * {@link DirectFieldStore#save}. There is nothing on the type that tells you
 * which one it is at a given moment.
 */
public final class DirectFieldAccount {

    private final long id;
    private String username;

    public DirectFieldAccount(long id, String username) {
        this.id = id;
        this.username = username;
    }

    public long id() {
        return id;
    }

    public String username() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
