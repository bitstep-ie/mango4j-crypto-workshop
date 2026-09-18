package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;

/**
 * The batch job phase 2 of a key rotation needs: for every already-persisted
 * record still on an old key, decrypt it (using whatever key it was
 * actually written under - decrypt() resolves that by id, same as every
 * previous stage) and encrypt it again (using whichever key is current
 * right now). The old key must stay resolvable via getById() for exactly as
 * long as this sweep takes to reach every record.
 */
public class RekeySweep {

    public static int run(CryptoShield cryptoShield, InMemoryCryptoKeyProvider provider, PaymentCardStore store) {
        String currentKeyId = provider.getCurrentEncryptionKey().getId();
        int rekeyedCount = 0;

        for (PaymentCardEntity record : store.all()) {
            if (isAlreadyOnKey(record, currentKeyId)) {
                continue;
            }

            cryptoShield.decrypt(record);
            cryptoShield.encrypt(record);
            rekeyedCount++;
        }

        return rekeyedCount;
    }

    // --8<-- [start:already-on-key]
    private static boolean isAlreadyOnKey(PaymentCardEntity record, String keyId) {
        // The previous stages' sweep-free world: nothing was ever "already
        // on" a key, so this always says no, and every record gets
        // rekeyed - correct, but wasteful, and not actually idempotent.
        boolean alreadyOnKey = false;

        // TODO:START already-on-key
        /* TODO: Return true if record's encryptedData already carries
         * keyId as its cryptoKeyId.
         *
         * A structured ciphertext records which key produced it (see
         * Structured Ciphertext) - a quick text check against that is
         * enough for this workshop. A real implementation would parse the
         * structured ciphertext (or track a separate "encryption key
         * version" column) rather than pattern-match the JSON, but the
         * point is the same either way: this is what makes re-running the
         * sweep after it's already finished a safe no-op instead of
         * wasted re-encryption work.
         */
        alreadyOnKey = record.getEncryptedData().contains("\"cryptoKeyId\":\"" + keyId + "\"");
        // TODO:END already-on-key

        return alreadyOnKey;
    }
    // --8<-- [end:already-on-key]
}
