package ie.bitstep.mango.workshop.talk.naivelisthmac;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves {@code docs/talk/list-hmac.md}'s append-vs-replace rule: an update that only
 * adds a lookup entry for the new value leaves the record findable under its old value
 * too, and a replace fixes it.
 */
class LookupStorePitfallTest {

    @Test
    void naiveUpdateLeavesTheRecordFindableUnderItsOldValue() {
        SecretKey key = HmacService.newKey();
        LookupStore store = new LookupStore();
        store.createUser(1L, "old-name", key);

        store.naiveUpdateUsername(1L, "new-name", key);

        assertEquals(List.of(1L), store.findByUsername("old-name", key),
                "the stale entry for the old value should still match, this is the bug");
        assertEquals(List.of(1L), store.findByUsername("new-name", key));
    }

    @Test
    void replacingUpdateRemovesTheOldValueFromSearch() {
        SecretKey key = HmacService.newKey();
        LookupStore store = new LookupStore();
        store.createUser(1L, "old-name", key);

        store.replacingUpdateUsername(1L, "new-name", key);

        assertTrue(store.findByUsername("old-name", key).isEmpty(),
                "the old value should no longer match anything");
        assertEquals(List.of(1L), store.findByUsername("new-name", key));
    }
}
