package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.domain.CryptoShieldHmacHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The previous stage's store compared one HMAC string against one other
 * HMAC string. This store compares two lists of HMACs - one holder per
 * currently-active key - and calls it a match if any entry in one list
 * shares a value with any entry in the other. That's the mechanism that
 * lets a record survive a key rotation without ever being rewritten:
 * as long as the key it was originally written under is still active
 * somewhere in the comparison, it's still findable.
 */
public class PaymentCardStore {

    private final List<PaymentCardEntity> records = new ArrayList<>();

    public Optional<PaymentCardEntity> findByLookup(PaymentCardEntity probe) {
        return records.stream()
                .filter(record -> anyValueMatches(record.getLookups(), probe.getLookups()))
                .findFirst();
    }

    public boolean save(PaymentCardEntity entity) {
        boolean duplicate = records.stream()
                .anyMatch(record -> anyValueMatches(record.getUniqueValues(), entity.getUniqueValues()));
        if (duplicate) {
            return false;
        }
        records.add(entity);
        return true;
    }

    // --8<-- [start:any-value-matches]
    private static boolean anyValueMatches(Collection<CryptoShieldHmacHolder> a, Collection<CryptoShieldHmacHolder> b) {
        // The previous stage's shortcut, degenerated to a list: nothing
        // ever matches, since there's no comparison at all yet.
        boolean matches = false;

        matches = a.stream().anyMatch(holderA ->
                b.stream().anyMatch(holderB -> holderA.getValue().equals(holderB.getValue())));

        return matches;
    }
    // --8<-- [end:any-value-matches]
}
