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
    }
}
