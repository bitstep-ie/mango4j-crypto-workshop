package ie.bitstep.mango.workshop.talk.naivelisthmac;

import javax.crypto.SecretKey;

/**
 * Companion demo for {@code docs/talk/list-hmac.md}'s append-vs-replace rule.
 *
 * <p>Run with {@code mvn -f talk/naive-list-hmac/pom.xml exec:java}.
 */
public final class Main {

    public static void main(String[] args) {
        SecretKey key = HmacService.newKey();

        System.out.println("== Naive update: appends instead of replacing ==");
        LookupStore naiveStore = new LookupStore();
        naiveStore.createUser(1L, "old-name", key);
        naiveStore.naiveUpdateUsername(1L, "new-name", key);

        System.out.println("Search for 'old-name': " + naiveStore.findByUsername("old-name", key));
        System.out.println("Search for 'new-name': " + naiveStore.findByUsername("new-name", key));
        System.out.println("PITFALL: the record is now findable under BOTH the old and new value,");
        System.out.println("because the old value's lookup entry was never removed.");

        System.out.println();
        System.out.println("== Replacing update: removes the old entries first ==");
        LookupStore replacingStore = new LookupStore();
        replacingStore.createUser(1L, "old-name", key);
        replacingStore.replacingUpdateUsername(1L, "new-name", key);

        System.out.println("Search for 'old-name': " + replacingStore.findByUsername("old-name", key));
        System.out.println("Search for 'new-name': " + replacingStore.findByUsername("new-name", key));
        System.out.println("Only the current value matches, as expected.");
    }
}
