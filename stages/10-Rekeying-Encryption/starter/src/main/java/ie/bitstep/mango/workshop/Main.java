package ie.bitstep.mango.workshop;

import com.fasterxml.jackson.databind.ObjectMapper;
import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;
import ie.bitstep.mango.crypto.core.encryption.impl.PBKDF2EncryptionService;
import ie.bitstep.mango.crypto.keyrotation.RekeyScheduler;
import ie.bitstep.mango.crypto.keyrotation.RekeySchedulerConfig;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        EncryptionServiceDelegate delegate = new PBKDF2EncryptionService();

        // Three records, written while "workshop-encryption-key" was
        // current - exactly Key Rotation's beforeRotation scenario, just
        // three of them in a store this time.
        CryptoShield oldKeyShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(new InMemoryCryptoKeyProvider("workshop-encryption-key"))
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();

        PaymentCardStore store = new PaymentCardStore();
        for (String cardNumber : List.of("5111111111111111", "5222222222222226", "5333333333333335")) {
            PaymentCardEntity card = new PaymentCardEntity();
            card.setCardNumber(cardNumber);
            oldKeyShield.encrypt(card);
            store.save(card);
        }

        InMemoryCryptoKeyProvider provider = new InMemoryCryptoKeyProvider("workshop-encryption-key-v2");
        CryptoShield cryptoShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(provider)
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();

        CountDownLatch rekeyFinishedLatch = new CountDownLatch(1);
        CountDownLatch keyRetiredLatch = new CountDownLatch(1);

        /* TODO: Mark "workshop-encryption-key" for retirement, so
         * RekeyScheduler picks it up on its next cycle:
         * provider.markForRetirement("workshop-encryption-key")
         */

        RekeySchedulerConfig config = RekeySchedulerConfig.builder()
                .withCryptoShield(cryptoShield)
                .withRekeyServices(List.of(new PaymentCardRekeyService(store, rekeyFinishedLatch)))
                .withRekeyCryptoKeyManager(new RetiringKeyManager(keyRetiredLatch))
                .withObjectMapper(new ObjectMapper())
                .withClock(Clock.systemUTC())
                .withCryptoKeyCachePeriod(Duration.ZERO)
                .withRekeyCheckInterval(0, 1, TimeUnit.SECONDS)
                .build();

        // RekeyScheduler polls on its own background thread with no
        // shutdown()/close() method exposed anywhere - once started, it
        // never stops on its own. See System.exit() below.
        new RekeyScheduler(config);

        boolean rekeyed = rekeyFinishedLatch.await(10, TimeUnit.SECONDS);
        boolean retired = rekeyed && keyRetiredLatch.await(10, TimeUnit.SECONDS);

        System.out.println("rekey finished within timeout?  " + rekeyed);
        System.out.println("old key retired within timeout? " + retired);
        for (PaymentCardEntity record : store.all()) {
            System.out.println("  " + record.getEncryptedData());
        }

        // Nothing above stops the scheduler's background thread - without
        // this, the JVM would just hang here forever.
        System.exit(0);
    }
}
