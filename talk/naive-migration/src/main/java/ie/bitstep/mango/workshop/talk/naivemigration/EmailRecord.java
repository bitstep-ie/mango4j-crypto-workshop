package ie.bitstep.mango.workshop.talk.naivemigration;

/**
 * One row in a table where an {@code email} column is being migrated from plaintext to
 * encrypted. {@code migrated} is a genuine, tracked flag (matching mango4j-crypto's
 * {@code @EnableMigrationSupport}), not something inferred from the field's shape:
 * during a backfill, some rows are already ciphertext and some are still the original
 * plaintext, and there is no reliable way to tell which just by looking at the bytes.
 */
public final class EmailRecord {

    private final long id;
    private String email;
    private String iv;
    private boolean migrated;

    public EmailRecord(long id, String email) {
        this.id = id;
        this.email = email;
        this.migrated = false;
    }

    public long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String iv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public boolean migrated() {
        return migrated;
    }

    public void setMigrated(boolean migrated) {
        this.migrated = migrated;
    }
}
