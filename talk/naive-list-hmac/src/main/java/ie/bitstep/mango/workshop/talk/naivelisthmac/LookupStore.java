package ie.bitstep.mango.workshop.talk.naivelisthmac;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

/**
 * The lookups table from {@code docs/talk/list-hmac.md}: one row per (record, HMAC)
 * pair, used for search. The chapter's rule is that an update must *replace* the full
 * current set of entries for a record, not append to it, "otherwise stale entries from
 * a previous version of the value would linger and stay matchable." This store offers
 * both an update that gets that wrong and one that gets it right, so the difference is
 * directly observable.
 */
public final class LookupStore {

    private record LookupEntry(long recordId, String hmac) {
    }

    private final List<LookupEntry> lookups = new ArrayList<>();

    public void createUser(long id, String username, SecretKey key) {
        lookups.add(new LookupEntry(id, HmacService.hmac(username, key)));
    }

    /** Adds a lookup entry for the new value, but never removes the entry for whatever
     * the value used to be. */
    // --8<-- [start:naive-list-hmac-update] link
    public void naiveUpdateUsername(long id, String newUsername, SecretKey key) {
        lookups.add(new LookupEntry(id, HmacService.hmac(newUsername, key)));
    }
    // --8<-- [end:naive-list-hmac-update]

    /** Removes every existing entry for this record first, then adds the one entry for
     * its current value, replacing the full set rather than appending to it. */
    // --8<-- [start:replacing-list-hmac-update] link
    public void replacingUpdateUsername(long id, String newUsername, SecretKey key) {
        lookups.removeIf(entry -> entry.recordId() == id);
        lookups.add(new LookupEntry(id, HmacService.hmac(newUsername, key)));
    }
    // --8<-- [end:replacing-list-hmac-update]

    public List<Long> findByUsername(String username, SecretKey key) {
        String target = HmacService.hmac(username, key);
        return lookups.stream()
                .filter(entry -> entry.hmac().equals(target))
                .map(LookupEntry::recordId)
                .toList();
    }
}
