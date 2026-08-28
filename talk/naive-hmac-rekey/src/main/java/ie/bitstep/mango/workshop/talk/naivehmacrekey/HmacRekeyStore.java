package ie.bitstep.mango.workshop.talk.naivehmacrekey;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The rekey teardown sequence from {@code docs/talk/rekeying-hmacs.md}: add the new key,
 * sweep existing records to add their new-key entries, only then remove the old key from
 * the active list. This store lets that ordering be violated on purpose, to show what
 * happens to a record the sweep hasn't reached yet if the old key is removed too soon.
 */
public final class HmacRekeyStore {

    private record LookupEntry(long recordId, String keyLabel, String hmac) {
    }

    private final Map<String, SecretKey> keysByLabel = new LinkedHashMap<>();
    private final List<String> activeKeyLabels = new ArrayList<>();
    private final List<LookupEntry> lookups = new ArrayList<>();

    public void addActiveKey(String label, SecretKey key) {
        keysByLabel.put(label, key);
        activeKeyLabels.add(label);
    }

    /** Step 5 of the process: remove a key from the active list, e.g. once its rekey is
     * believed complete. Nothing here checks whether every record has actually been
     * swept yet, exactly like a naive implementation of that step wouldn't either. */
    // --8<-- [start:remove-active-key] link
    public void removeActiveKey(String label) {
        activeKeyLabels.remove(label);
    }
    // --8<-- [end:remove-active-key]

    /** A record written while only "old" is active, so it only ever gets a lookup entry
     * under that one key. */
    public void createUser(long id, String username) {
        for (String label : activeKeyLabels) {
            lookups.add(new LookupEntry(id, label, HmacService.hmac(username, keysByLabel.get(label))));
        }
    }

    /** One unit of rekey-sweep work: add this one record's lookup entry for the new key,
     * strictly additive, existing entries for other keys are left untouched. */
    public void rekeySweepOneRecord(long id, String username, String newKeyLabel) {
        lookups.add(new LookupEntry(id, newKeyLabel, HmacService.hmac(username, keysByLabel.get(newKeyLabel))));
    }

    /** Search hashes the term with every *currently active* key. A key that's been
     * removed from the active list is never tried, even if some record's only lookup
     * entry is still sitting there under it. */
    // --8<-- [start:hmac-rekey-search] link
    public List<Long> search(String username) {
        List<Long> matches = new ArrayList<>();
        for (String label : activeKeyLabels) {
            String target = HmacService.hmac(username, keysByLabel.get(label));
            lookups.stream()
                    .filter(entry -> entry.keyLabel().equals(label) && entry.hmac().equals(target))
                    .map(LookupEntry::recordId)
                    .forEach(matches::add);
        }
        return matches;
    }
    // --8<-- [end:hmac-rekey-search]
}
