package ie.bitstep.mango.workshop.talk.naivehmacrekey;

/**
 * Companion demo for {@code docs/talk/rekeying-hmacs.md}'s teardown ordering: what
 * happens to a record the sweep hasn't reached yet if the old key is removed too soon.
 *
 * <p>Run with {@code mvn -f talk/naive-hmac-rekey/pom.xml exec:java}.
 */
public final class Main {

    public static void main(String[] args) {
        System.out.println("== Removing the old key before the sweep finishes ==");
        HmacRekeyStore early = new HmacRekeyStore();
        early.addActiveKey("old", HmacService.newKey());
        early.createUser(1L, "john");
        early.createUser(2L, "jane"); // both written back when "old" was the only active key

        early.addActiveKey("new", HmacService.newKey()); // rotation phase 1
        early.rekeySweepOneRecord(1L, "john", "new"); // sweep has only reached john so far

        early.removeActiveKey("old"); // done too soon: jane hasn't been swept yet

        System.out.println("Search for 'john': " + early.search("john") + " (swept, still findable)");
        System.out.println("Search for 'jane': " + early.search("jane") + " (PITFALL: not swept, now invisible)");
        System.out.println("Jane's only lookup entry is still under the old key, which search no longer tries.");

        System.out.println();
        System.out.println("== Removing the old key only after the sweep finishes ==");
        HmacRekeyStore correct = new HmacRekeyStore();
        correct.addActiveKey("old", HmacService.newKey());
        correct.createUser(1L, "john");
        correct.createUser(2L, "jane");

        correct.addActiveKey("new", HmacService.newKey());
        correct.rekeySweepOneRecord(1L, "john", "new");
        correct.rekeySweepOneRecord(2L, "jane", "new"); // sweep reaches everyone first

        correct.removeActiveKey("old"); // now safe

        System.out.println("Search for 'john': " + correct.search("john"));
        System.out.println("Search for 'jane': " + correct.search("jane") + " (still findable)");
    }
}
