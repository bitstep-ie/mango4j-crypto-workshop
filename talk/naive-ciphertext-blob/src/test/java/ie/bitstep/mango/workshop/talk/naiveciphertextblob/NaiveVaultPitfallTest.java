package ie.bitstep.mango.workshop.talk.naiveciphertextblob;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Proves the two pitfalls {@code docs/talk/structured-ciphertext.md} calls out for the
 * naive "just encrypt it" approach: no stored key ID means (1) decrypting an old record
 * costs a linear scan through every key ever issued, and (2) once the key that produced
 * a record is gone, that record is unrecoverable with no way to have found it in advance.
 */
class NaiveVaultPitfallTest {

    private final NaiveVault vault = new NaiveVault();

    @Test
    void decryptingAnOldRecordRequiresTryingEveryKnownKey() {
        SecretKey keyV1 = NaiveVault.newKey();
        SecretKey keyV2 = NaiveVault.newKey();

        NaiveBlob blob = vault.encrypt("4111-1111-1111-1111", keyV1);

        Map<String, SecretKey> knownKeysNewestFirst = new LinkedHashMap<>();
        knownKeysNewestFirst.put("keyV2", keyV2);
        knownKeysNewestFirst.put("keyV1", keyV1);

        // Works, but only because the caller happened to try every key it knows about.
        assertEquals("4111-1111-1111-1111", vault.decryptByBruteForce(blob, knownKeysNewestFirst));

        // The blob itself never says "keyV1" anywhere - there is nothing to query on.
        assertEquals(-1, blob.toString().indexOf("keyV1"));
    }

    @Test
    void destroyingTheOriginalKeyPermanentlyLosesTheRecordWithNoWayToHaveFoundItFirst() {
        SecretKey keyV1 = NaiveVault.newKey();
        SecretKey keyV2 = NaiveVault.newKey();

        NaiveBlob blob = vault.encrypt("4111-1111-1111-1111", keyV1);

        // keyV1 is destroyed (e.g. compliance-mandated key deletion). Nothing about
        // the blob could have flagged it as "still depends on keyV1" beforehand.
        Map<String, SecretKey> keysAfterDestruction = new LinkedHashMap<>();
        keysAfterDestruction.put("keyV2", keyV2);

        assertNull(vault.decryptByBruteForce(blob, keysAfterDestruction));
    }
}
