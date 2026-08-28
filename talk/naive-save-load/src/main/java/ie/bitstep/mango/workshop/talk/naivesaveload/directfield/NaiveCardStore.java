package ie.bitstep.mango.workshop.talk.naivesaveload.directfield;

import ie.bitstep.mango.workshop.talk.naivesaveload.Crypto;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

/**
 * The naive hand-rolled shape from {@code docs/talk/transient-vs-encrypted-blobs.md}:
 * {@code save()} manually encrypts {@code cardNumber} in place before persisting, then
 * manually decrypts it back; {@code load()} fetches and manually decrypts. There is no
 * separate domain-level/db-level POJO split, {@code NaiveCardEntity} is both.
 *
 * <p>{@code table} models the actual persisted bytes (what a real database holds,
 * unaffected by in-memory edits until something calls {@code save()} again), while
 * {@code managed} models the kind of session-level identity map a real ORM keeps, so a
 * second {@code load()} for the same id returns the same live instance rather than a
 * disconnected copy.
 */
public final class NaiveCardStore {

    private record Row(String cardNumber, String iv, String cardNumberHmac) {
    }

    private final Map<Long, Row> table = new HashMap<>();
    private final Map<Long, NaiveCardEntity> managed = new HashMap<>();

    public void save(NaiveCardEntity entity, SecretKey encryptionKey, SecretKey hmacKey) {
        save(entity, encryptionKey, hmacKey, () -> {
        });
    }

    /**
     * Same as {@link #save(NaiveCardEntity, SecretKey, SecretKey)}, but runs
     * {@code duringCiphertextWindow} at the exact point where {@code cardNumber} holds
     * ciphertext, standing in for a concurrent reader landing in that window.
     */
    // --8<-- [start:naive-card-save] link
    public void save(NaiveCardEntity entity, SecretKey encryptionKey, SecretKey hmacKey,
                      Runnable duringCiphertextWindow) {
        String plaintext = entity.cardNumber();
        Crypto.EncryptedValue encrypted = Crypto.encryptDetached(plaintext, encryptionKey);

        entity.setCardNumber(encrypted.ciphertext());     // the SAME field now holds ciphertext
        entity.setIv(encrypted.iv());
        entity.setCardNumberHmac(Crypto.hmac(plaintext, hmacKey));

        table.put(entity.id(), new Row(entity.cardNumber(), entity.iv(), entity.cardNumberHmac()));
        managed.put(entity.id(), entity);

        duringCiphertextWindow.run();                     // a concurrent reader could land here

        entity.setCardNumber(Crypto.decryptDetached(encrypted.ciphertext(), encrypted.iv(), encryptionKey));
    }
    // --8<-- [end:naive-card-save]

    /**
     * Fetches from the persisted table and decrypts {@code cardNumber} back in place.
     * If this id is already managed (held from an earlier {@code load()}), that same
     * instance is overwritten rather than a fresh one being handed back.
     */
    // --8<-- [start:naive-card-load]
    public NaiveCardEntity load(long id, SecretKey encryptionKey) {
        Row row = table.get(id);
        String plaintext = Crypto.decryptDetached(row.cardNumber(), row.iv(), encryptionKey);

        NaiveCardEntity existing = managed.get(id);
        if (existing != null) {
            existing.setCardNumber(plaintext);   // forced overwrite, no check, no warning
            existing.setIv(row.iv());
            existing.setCardNumberHmac(row.cardNumberHmac());
            return existing;
        }

        NaiveCardEntity fresh = new NaiveCardEntity(id, plaintext);
        fresh.setIv(row.iv());
        fresh.setCardNumberHmac(row.cardNumberHmac());
        managed.put(id, fresh);
        return fresh;
    }
    // --8<-- [end:naive-card-load]
}
