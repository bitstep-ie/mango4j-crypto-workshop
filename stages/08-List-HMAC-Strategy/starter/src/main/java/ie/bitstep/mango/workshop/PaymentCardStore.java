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

    private static boolean anyValueMatches(Collection<CryptoShieldHmacHolder> a, Collection<CryptoShieldHmacHolder> b) {
        // The previous stage's shortcut, degenerated to a list: nothing
        // ever matches, since there's no comparison at all yet.
        boolean matches = false;

        /* TODO: Return true if any holder in `a` has the same value as any
         * holder in `b`.
         *
         * Deliberately compare by value, not by cryptoKeyId: two holders
         * computed under the same key, from the same plaintext, will
         * always have the same value - that's the equality search relies
         * on. Two records only need to overlap on *one* active key to be
         * found as a match, even if the rest of their key lists differ.
         */

        return matches;
    }
}
