package ie.bitstep.mango.workshop.talk.naivesinglehmac;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the two pitfalls {@code docs/talk/single-hmac.md} walks through for the naive
 * "one HMAC column, one key" design: a key rotation both hides existing records from
 * search and lets a duplicate slip past the unique constraint.
 */
class UserStorePitfallTest {

    private final HmacService hmacService = new HmacService();
    private static final String USERNAME = "john.doe@test.com";

    @Test
    void rotatingTheKeyHidesExistingRecordsFromNaiveSearch() {
        UserStore store = new UserStore();
        SecretKey keyV1 = HmacService.newKey();
        store.createUser(USERNAME, hmacService.hmac(USERNAME, keyV1));

        SecretKey keyV2 = HmacService.newKey();
        String searchHmac = hmacService.hmac(USERNAME, keyV2);

        // Naive search only ever hashes with the current key.
        List<UserStore.UserRecord> results = store.findByUsernameHmac(searchHmac);

        assertTrue(results.isEmpty(), "the pre-rotation record should be invisible to a current-key-only search");
    }

    @Test
    void rotatingTheKeyLetsADuplicateUsernameBypassTheUniqueConstraint() {
        UserStore store = new UserStore();
        SecretKey keyV1 = HmacService.newKey();
        store.createUser(USERNAME, hmacService.hmac(USERNAME, keyV1));

        SecretKey keyV2 = HmacService.newKey();

        // Same logical username, hashed under the new key - the unique constraint
        // only ever sees the hash, and the two hashes genuinely differ.
        store.createUser(USERNAME, hmacService.hmac(USERNAME, keyV2));

        long rowsForThisUsername = store.allRows().stream()
                .filter(r -> r.username().equals(USERNAME))
                .count();

        assertEquals(2, rowsForThisUsername, "the naive design should have let a duplicate through");
    }
}
