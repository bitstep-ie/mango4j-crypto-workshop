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

        alreadyOnKey = record.getEncryptedData().contains("\"cryptoKeyId\":\"" + keyId + "\"");

        return alreadyOnKey;
    }
    // --8<-- [end:already-on-key]
}
