package ie.bitstep.mango.workshop.talk.naivemigration;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Proves the mid-migration pitfall {@code docs/talk/key-rotation.md} describes: a naive
 * read that assumes every row is already encrypted breaks on rows the backfill job
 * hasn't reached yet, while a migration-aware read handles both correctly.
 */
class EmailStorePitfallTest {

    @Test
    void naiveLoadThrowsOnARowTheBackfillHasNotReachedYet() {
        SecretKey key = Crypto.newKey();
        EmailStore store = new EmailStore();
        store.seedLegacyRow(1L, "not-yet-migrated@test.com");

        assertThrows(IllegalStateException.class, () -> store.loadNaive(1L, key));
    }

    @Test
    void naiveLoadWorksOnceARowHasBeenBackfilled() {
        SecretKey key = Crypto.newKey();
        EmailStore store = new EmailStore();
        store.seedLegacyRow(1L, "already-migrated@test.com");
        store.backfill(1L, key);

        assertEquals("already-migrated@test.com", store.loadNaive(1L, key).email());
    }

    @Test
    void migrationAwareLoadHandlesBothMigratedAndUnmigratedRows() {
        SecretKey key = Crypto.newKey();
        EmailStore store = new EmailStore();
        store.seedLegacyRow(1L, "already-migrated@test.com");
        store.seedLegacyRow(2L, "not-yet-migrated@test.com");
        store.backfill(1L, key);

        assertEquals("already-migrated@test.com", store.loadMigrationAware(1L, key).email());
        assertEquals("not-yet-migrated@test.com", store.loadMigrationAware(2L, key).email());
    }
}
