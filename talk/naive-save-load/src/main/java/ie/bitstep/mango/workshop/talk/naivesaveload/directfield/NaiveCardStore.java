package ie.bitstep.mango.workshop.talk.naivesaveload.directfield;

import ie.bitstep.mango.workshop.talk.naivesaveload.Crypto;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

/**
 * The naive hand-rolled shape from {@code docs/talk/transient-vs-encrypted-blobs.md}:
 * {@code save()} manually encrypts {@code cardNumber} in place, persists the entity,
 * then restores {@code cardNumber} for the caller to keep using, from the plaintext it
 * already captured before encrypting, not by decrypting the ciphertext it just produced.
 * {@code load()} is simpler: fetch the managed entity, decrypt {@code cardNumber}
 * directly on it. There is no separate domain-level/db-level POJO and no DTO standing
 * in for a row, {@code table} holds {@code NaiveCardEntity} instances directly, and
 * calling {@code load()} on one that {@code save()} already restored is a double decrypt.
 */
public final class NaiveCardStore {

    private final Map<Long, NaiveCardEntity> table = new HashMap<>();

    public void save(NaiveCardEntity entity, SecretKey encryptionKey, SecretKey hmacKey) {
        save(entity, encryptionKey, hmacKey, () -> {
        });
    }

    /**
     * Same as {@link #save(NaiveCardEntity, SecretKey, SecretKey)}, but runs
     * {@code duringCiphertextWindow} at the point where {@code cardNumber} holds
     * ciphertext, standing in for a concurrent reader landing in that window.
     */
    // --8<-- [start:naive-card-save] link
    public void save(NaiveCardEntity entity, SecretKey encryptionKey, SecretKey hmacKey,
                      Runnable duringCiphertextWindow) {
        String plaintext = entity.cardNumber();     // might not actually be plaintext: save() has
                                                     // no way to verify that, so an entity that
                                                     // picked up ciphertext some other way (e.g.
                                                     // built from raw persisted bytes without going
                                                     // through load()) gets silently double-encrypted
                                                     // here, the mirror of load()'s double decrypt,
                                                     // except encrypting ciphertext-as-plaintext never
                                                     // throws, so there's no loud failure to notice
        Crypto.EncryptedValue encrypted = Crypto.encryptDetached(plaintext, encryptionKey);

        entity.setCardNumber(encrypted.ciphertext());     // the SAME field now holds ciphertext
        entity.setIv(encrypted.iv());
        entity.setCardNumberHmac(Crypto.hmac(plaintext, hmacKey));

        table.put(entity.id(), entity);                   // the real persist, same instance, no row type

        duringCiphertextWindow.run();                      // a concurrent reader could land here

        entity.setCardNumber(plaintext);                    // restore directly, no need to decrypt
                                                             // what's already sitting in a local variable
    }
    // --8<-- [end:naive-card-save]

    /**
     * Fetches the managed entity and decrypts {@code cardNumber} directly on it. Since
     * {@link #save} already leaves {@code cardNumber} decrypted, calling {@code load()}
     * on an entity {@code save()} already handled is a double decrypt, and it throws:
     * the field no longer holds valid ciphertext for this key to decrypt.
     */
    // --8<-- [start:naive-card-load] link
    public NaiveCardEntity load(long id, SecretKey encryptionKey) {
        NaiveCardEntity entity = table.get(id);
        entity.setCardNumber(Crypto.decryptDetached(entity.cardNumber(), entity.iv(), encryptionKey));
        return entity;
    }
    // --8<-- [end:naive-card-load]
}
