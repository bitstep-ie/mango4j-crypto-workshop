package ie.bitstep.mango.workshop.talk.naiveciphertextblob;

import javax.crypto.SecretKey;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Companion demo for {@code docs/talk/structured-ciphertext.md}'s "naive approach"
 * section: encrypt the value, store the ciphertext, nothing else.
 *
 * <p>Run with {@code mvn -f talk/naive-ciphertext-blob/pom.xml exec:java}.
 */
public final class Main {

    public static void main(String[] args) {
        NaiveVault vault = new NaiveVault();

        System.out.println("== Naive ciphertext blob demo ==");
        System.out.println();

        // Year one: everything is encrypted with keyV1. Nobody thinks about rotation yet.
        SecretKey keyV1 = NaiveVault.newKey();
        NaiveBlob cardNumberRecord = vault.encrypt("4111-1111-1111-1111", keyV1);
        System.out.println("Encrypted a card number under keyV1: " + cardNumberRecord);
        System.out.println("Stored in the database: just the blob above. No key ID next to it.");
        System.out.println();

        // Time passes. The org rotates its encryption key, as every mature key
        // management program eventually must.
        SecretKey keyV2 = NaiveVault.newKey();
        System.out.println("-- Key rotation happens: keyV2 is now current, keyV1 is retained for decrypt --");
        System.out.println();

        // Reading the old record back still works today, but only because the
        // application still happens to have keyV1 lying around, and is willing to
        // pay for a linear scan through every key it has ever used.
        Map<String, SecretKey> knownKeysNewestFirst = new LinkedHashMap<>();
        knownKeysNewestFirst.put("keyV2", keyV2);
        knownKeysNewestFirst.put("keyV1", keyV1);

        String recovered = vault.decryptByBruteForce(cardNumberRecord, knownKeysNewestFirst);
        System.out.println("Decrypted the old record by trying every known key, newest first: " + recovered);
        System.out.println("(That's O(number of keys ever issued) per read, forever.)");
        System.out.println();

        // The real pitfall: without a stored key ID, there is no query that answers
        // "which records still need to be rekeyed off keyV1?" You'd have to decrypt
        // every single record in the table with keyV1 just to find out which ones
        // it belongs to.
        System.out.println("-- Now suppose keyV1 must be destroyed (e.g. a compliance-mandated key deletion) --");
        boolean keyV1Destroyed = true;
        Map<String, SecretKey> keysAfterDestruction = new LinkedHashMap<>();
        keysAfterDestruction.put("keyV2", keyV2);
        // keyV1 intentionally absent - it has been destroyed.

        String afterDestruction = vault.decryptByBruteForce(cardNumberRecord, keysAfterDestruction);
        System.out.println("Decrypt attempt after keyV1 is destroyed: " + afterDestruction);
        System.out.println();
        System.out.println("PITFALL: that record is now permanently unreadable, and because the blob");
        System.out.println("never recorded which key encrypted it, there was no way to run a targeted");
        System.out.println("'find every record still on keyV1' migration before deleting the key -");
        System.out.println("only a full-table decrypt-and-check sweep, which most teams never run in time.");

        if (keyV1Destroyed && afterDestruction == null) {
            System.exit(0);
        }
    }
}
