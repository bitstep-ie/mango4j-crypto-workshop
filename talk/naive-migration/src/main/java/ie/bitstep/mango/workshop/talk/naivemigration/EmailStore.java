package ie.bitstep.mango.workshop.talk.naivemigration;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

/**
 * The naive shape {@code docs/talk/key-rotation.md}'s "migrating an unencrypted field to
 * encrypted" section describes: a backfill job runs in the background, so at any given
 * moment the table has a mix of rows already encrypted and rows still plaintext, waiting
 * their turn. A read path that doesn't check which is which gets it wrong on whichever
 * rows the backfill hasn't reached yet.
 */
public final class EmailStore {

    private final Map<Long, EmailRecord> table = new HashMap<>();

    /** Simulates a row that predates ALE entirely: plain old data, never touched by
     * encryption at all, exactly what every row looked like before migration started. */
    public void seedLegacyRow(long id, String plaintextEmail) {
        table.put(id, new EmailRecord(id, plaintextEmail));
    }

    /** Simulates the backfill job reaching this one row and encrypting it. */
    public void backfill(long id, SecretKey key) {
        EmailRecord record = table.get(id);
        Crypto.EncryptedValue encrypted = Crypto.encrypt(record.email(), key);
        record.setEmail(encrypted.ciphertext());
        record.setIv(encrypted.iv());
        record.setMigrated(true);
    }

    /** Reads a row and decrypts {@code email}, unconditionally, on the assumption that
     * everything in the table is already encrypted. */
    // --8<-- [start:naive-migration-load] link
    public EmailRecord loadNaive(long id, SecretKey key) {
        EmailRecord record = table.get(id);
        record.setEmail(Crypto.decrypt(record.email(), record.iv(), key));
        return record;
    }
    // --8<-- [end:naive-migration-load]

    /** Same read, but checks the tracked migration flag first, exactly the kind of
     * explicit, tracked exception {@code @EnableMigrationSupport} records. */
    // --8<-- [start:migration-aware-load] link
    public EmailRecord loadMigrationAware(long id, SecretKey key) {
        EmailRecord record = table.get(id);
        if (record.migrated()) {
            record.setEmail(Crypto.decrypt(record.email(), record.iv(), key));
        }
        return record;
    }
    // --8<-- [end:migration-aware-load]
}
