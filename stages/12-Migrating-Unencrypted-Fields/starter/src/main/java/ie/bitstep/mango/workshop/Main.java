package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;
import ie.bitstep.mango.crypto.core.encryption.impl.PBKDF2EncryptionService;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        EncryptionServiceDelegate delegate = new PBKDF2EncryptionService();

        // Building the shield is where @EnableMigrationSupport actually gets
        // checked. Watch the console: one WARNING (deadline still ahead) and
        // one ERROR (deadline already missed) - neither one stops the build.
        CryptoShield cryptoShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(new InMemoryCryptoKeyProvider())
                .withAnnotatedEntities(List.of(InProgressMigrationEntity.class, OverdueMigrationEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();

        // Neither field behaves any differently at runtime - transient or
        // not, encrypt()/decrypt() read and write it by reflection either way.
        InProgressMigrationEntity inProgress = new InProgressMigrationEntity();
        inProgress.setCardNumber("5111111111111111");
        cryptoShield.encrypt(inProgress);
        System.out.println("in-progress field still encrypts fine: " + inProgress.getEncryptedData());

        OverdueMigrationEntity overdue = new OverdueMigrationEntity();
        overdue.setCardNumber("5222222222222226");
        cryptoShield.encrypt(overdue);
        System.out.println("overdue field still encrypts fine:     " + overdue.getEncryptedData());

        // A handful of "legacy" records: plaintext already sitting in
        // cardNumber (as if freshly loaded from the old plaintext column),
        // no encryptedData yet.
        List<InProgressMigrationEntity> legacyRecords = List.of(
                newLegacyRecord("5111111111111111"),
                newLegacyRecord("5222222222222226"),
                newLegacyRecord("5333333333333335")
        );

        // Detect which records still need backfilling the same way a real
        // query against the table would: encryptedData stays null until
        // encrypt() has been called on a record for the first time. There's
        // no other signal to go on - mango4j-crypto doesn't track backfill
        // progress for you, and this is the *only* reliable one for a
        // record that never had any encrypted field before. (A record that
        // already has other @Encrypt fields - already CryptoShield-managed,
        // encryptedData already non-null - can't be detected this way; that
        // case rides along with an ordinary Rekeying: Encryption-style
        // sweep instead, since decrypt()/encrypt() already touch it.)
        List<InProgressMigrationEntity> stillUnmigrated = legacyRecords.stream()
                .filter(record -> record.getEncryptedData() == null)
                .toList();

        /* TODO: Backfill every unmigrated record: call
         * cryptoShield.encrypt() on each one in stillUnmigrated. There's no
         * ciphertext to decrypt first - these records only ever had a
         * plaintext value, never any ciphertext.
         */

        long remainingUnmigrated = legacyRecords.stream().filter(record -> record.getEncryptedData() == null).count();
        System.out.println("legacy records still unmigrated after this sweep: " + remainingUnmigrated + " / " + legacyRecords.size());

        // Only cut over once that count is genuinely zero - not just these
        // three records, every record. The field goes back to being fully
        // transient, @EnableMigrationSupport comes off, and (in a system
        // backed by a real database) the now-unused plaintext column gets
        // dropped from the schema. MigratedEntity is what cardNumber looks
        // like on the other side of that cutover - identical to every
        // entity from Real Encryption onward.
        if (remainingUnmigrated > 0) {
            System.out.println("cutover skipped: " + remainingUnmigrated + " record(s) still not backfilled");
        } else {
            CryptoShield cutoverShield = new CryptoShield.Builder()
                    .withCryptoKeyProvider(new InMemoryCryptoKeyProvider())
                    .withAnnotatedEntities(List.of(MigratedEntity.class))
                    .withEncryptionServiceDelegates(List.of(delegate))
                    .build();

            MigratedEntity migrated = new MigratedEntity();
            migrated.setCardNumber("5444444444444447");
            cutoverShield.encrypt(migrated);
            System.out.println("post-cutover field still encrypts fine: " + migrated.getEncryptedData());
        }
    }

    private static InProgressMigrationEntity newLegacyRecord(String cardNumber) {
        InProgressMigrationEntity record = new InProgressMigrationEntity();
        record.setCardNumber(cardNumber);
        return record;
    }
}
