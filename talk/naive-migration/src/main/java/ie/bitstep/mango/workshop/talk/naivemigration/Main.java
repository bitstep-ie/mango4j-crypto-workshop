package ie.bitstep.mango.workshop.talk.naivemigration;

import javax.crypto.SecretKey;

/**
 * Companion demo for {@code docs/talk/key-rotation.md}'s "migrating an unencrypted
 * field to encrypted" section: a table mid-backfill, some rows encrypted, some not.
 *
 * <p>Run with {@code mvn -f talk/naive-migration/pom.xml exec:java}.
 */
public final class Main {

    public static void main(String[] args) {
        SecretKey key = Crypto.newKey();

        System.out.println("== Naive load(): assumes every row is already encrypted ==");
        EmailStore naiveDemoStore = new EmailStore();
        naiveDemoStore.seedLegacyRow(1L, "already-migrated@test.com");
        naiveDemoStore.seedLegacyRow(2L, "not-yet-migrated@test.com");
        naiveDemoStore.backfill(1L, key); // the background job has reached row 1, but not row 2 yet

        System.out.println("Row 1 (backfilled): " + naiveDemoStore.loadNaive(1L, key).email());

        try {
            naiveDemoStore.loadNaive(2L, key);
            System.out.println("(this line should not be reached)");
        } catch (IllegalStateException e) {
            System.out.println("PITFALL: row 2 (not yet backfilled) blew up: " + e.getCause());
            System.out.println("It tried to decrypt plain old data as if it were ciphertext,");
            System.out.println("because nothing tracked which rows the backfill has actually reached.");
        }

        System.out.println();
        System.out.println("== Migration-aware load(): checks the tracked flag first ==");
        EmailStore awareDemoStore = new EmailStore();
        awareDemoStore.seedLegacyRow(1L, "already-migrated@test.com");
        awareDemoStore.seedLegacyRow(2L, "not-yet-migrated@test.com");
        awareDemoStore.backfill(1L, key);

        System.out.println("Row 1 (backfilled): " + awareDemoStore.loadMigrationAware(1L, key).email());
        System.out.println("Row 2 (not yet backfilled): " + awareDemoStore.loadMigrationAware(2L, key).email());
        System.out.println("Both work, because the read path knows which rows are which,");
        System.out.println("instead of assuming the whole table is in one state.");
    }
}
