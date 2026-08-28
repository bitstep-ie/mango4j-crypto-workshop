package ie.bitstep.mango.workshop.talk.naivehmacrekey;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the teardown-ordering pitfall {@code docs/talk/rekeying-hmacs.md} describes:
 * removing the old HMAC key before the rekey sweep has reached every record makes the
 * unswept records silently unfindable.
 */
class HmacRekeyStorePitfallTest {

    @Test
    void removingTheOldKeyBeforeTheSweepFinishesHidesUnsweptRecords() {
        HmacRekeyStore store = new HmacRekeyStore();
        store.addActiveKey("old", HmacService.newKey());
        store.createUser(1L, "john");
        store.createUser(2L, "jane");

        store.addActiveKey("new", HmacService.newKey());
        store.rekeySweepOneRecord(1L, "john", "new"); // only john has been swept

        store.removeActiveKey("old");

        assertEquals(1, store.search("john").size(), "john was swept, so should still be findable");
        assertTrue(store.search("jane").isEmpty(),
                "jane was never swept, and her only entry is under the now-removed key");
    }

    @Test
    void removingTheOldKeyAfterTheSweepFinishesKeepsEveryoneFindable() {
        HmacRekeyStore store = new HmacRekeyStore();
        store.addActiveKey("old", HmacService.newKey());
        store.createUser(1L, "john");
        store.createUser(2L, "jane");

        store.addActiveKey("new", HmacService.newKey());
        store.rekeySweepOneRecord(1L, "john", "new");
        store.rekeySweepOneRecord(2L, "jane", "new");

        store.removeActiveKey("old");

        assertEquals(1, store.search("john").size());
        assertEquals(1, store.search("jane").size());
    }
}
