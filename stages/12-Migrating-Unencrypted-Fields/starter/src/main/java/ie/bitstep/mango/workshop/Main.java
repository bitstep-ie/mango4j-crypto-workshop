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
        // no encryptedData yet. Backfilling one is exactly one encrypt()
        // call - there's no ciphertext to decrypt first, since there was
        // never any ciphertext.
        List<InProgressMigrationEntity> legacyRecords = List.of(
                newLegacyRecord("5111111111111111"),
                newLegacyRecord("5222222222222226"),
                newLegacyRecord("5333333333333335")
        );

        /* TODO: Backfill every legacy record: call cryptoShield.encrypt()
         * on each one in legacyRecords.
         */

        long backfilledCount = legacyRecords.stream().filter(record -> record.getEncryptedData() != null).count();
        System.out.println("legacy records backfilled: " + backfilledCount + " / " + legacyRecords.size());

        // Once every record - not just these three - has been backfilled,
        // the migration is done: the field goes back to being fully
        // transient, @EnableMigrationSupport comes off, and (in a system
        // backed by a real database) the now-unused plaintext column gets
        // dropped from the schema. MigratedEntity is what cardNumber looks
        // like on the other side of that cutover - identical to every
        // entity from Real Encryption onward.
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

    private static InProgressMigrationEntity newLegacyRecord(String cardNumber) {
        InProgressMigrationEntity record = new InProgressMigrationEntity();
        record.setCardNumber(cardNumber);
        return record;
    }
}
