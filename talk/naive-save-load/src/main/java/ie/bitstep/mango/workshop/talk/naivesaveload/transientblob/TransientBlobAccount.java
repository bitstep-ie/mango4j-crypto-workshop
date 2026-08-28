package ie.bitstep.mango.workshop.talk.naivesaveload.transientblob;

/**
 * The transient/blob split from {@code docs/talk/transient-vs-encrypted-blobs.md}:
 * {@code username} is the working, always-plaintext representation; {@code usernameBlob}
 * is the persisted ciphertext. They are separate fields, so nothing that populates one
 * ever has to touch the other.
 */
public final class TransientBlobAccount {

    private final long id;
    private String username;
    private String usernameBlob;

    public TransientBlobAccount(long id, String username) {
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

    public String usernameBlob() {
        return usernameBlob;
    }

    void setUsernameBlob(String usernameBlob) {
        this.usernameBlob = usernameBlob;
    }
}
