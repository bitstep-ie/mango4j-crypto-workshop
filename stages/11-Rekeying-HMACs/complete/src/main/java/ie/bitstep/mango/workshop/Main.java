package ie.bitstep.mango.workshop;

import com.fasterxml.jackson.databind.ObjectMapper;
import ie.bitstep.mango.crypto.CryptoShield;
import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;
import ie.bitstep.mango.crypto.core.encryption.impl.PBKDF2EncryptionService;
import ie.bitstep.mango.crypto.domain.CryptoShieldHmacHolder;
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

        // --8<-- [start:write-old-records]
        // Three records, written while only "workshop-hmac-key" was
        // active - exactly List HMAC Strategy's setup, just driving
        // toward a real rotation this time instead of a hand-rolled one.
        CryptoShield oldKeyShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(new InMemoryCryptoKeyProvider(List.of("workshop-hmac-key")))
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
        // --8<-- [end:write-old-records]

        InMemoryCryptoKeyProvider provider = new InMemoryCryptoKeyProvider(List.of("workshop-hmac-key-v2"));
        CryptoShield cryptoShield = new CryptoShield.Builder()
                .withCryptoKeyProvider(provider)
                .withAnnotatedEntities(List.of(PaymentCardEntity.class))
                .withEncryptionServiceDelegates(List.of(delegate))
                .build();

        CountDownLatch newKeyOnLatch = new CountDownLatch(1);
        CountDownLatch oldKeyRetiredLatch = new CountDownLatch(1);

        RekeySchedulerConfig config = RekeySchedulerConfig.builder()
                .withCryptoShield(cryptoShield)
                .withRekeyServices(List.of(new PaymentCardRekeyService(store, newKeyOnLatch)))
                .withRekeyCryptoKeyManager(new RetiringKeyManager(oldKeyRetiredLatch))
                .withObjectMapper(new ObjectMapper())
                .withClock(Clock.systemUTC())
                .withCryptoKeyCachePeriod(Duration.ZERO)
                .withRekeyCheckInterval(0, 1, TimeUnit.SECONDS)
                .build();

        // --8<-- [start:bring-new-key-on]
        provider.getById("workshop-hmac-key-v2").setRekeyMode(CryptoKey.RekeyMode.KEY_ON);
        // --8<-- [end:bring-new-key-on]

        // RekeyScheduler polls on its own background thread with no
        // shutdown()/close() method - see the System.exit() at the end.
        new RekeyScheduler(config);

        // --8<-- [start:phase-one-report]
        boolean newKeyOn = newKeyOnLatch.await(10, TimeUnit.SECONDS);
        System.out.println("new key brought on within timeout? " + newKeyOn);
        for (PaymentCardEntity record : store.all()) {
            System.out.println("  lookups: " + keyIdsOf(record));
        }
        // --8<-- [end:phase-one-report]

        // --8<-- [start:retire-old-key]
        provider.getById("workshop-hmac-key-v2").setRekeyMode(null);
        provider.getById("workshop-hmac-key").setRekeyMode(CryptoKey.RekeyMode.KEY_OFF);
        // --8<-- [end:retire-old-key]

        // --8<-- [start:phase-two-report]
        boolean oldKeyRetired = oldKeyRetiredLatch.await(10, TimeUnit.SECONDS);
        System.out.println("old key retired within timeout?    " + oldKeyRetired);
        for (PaymentCardEntity record : store.all()) {
            System.out.println("  lookups: " + keyIdsOf(record));
        }
        // --8<-- [end:phase-two-report]

        // --8<-- [start:search-after-rotation]
        PaymentCardEntity searchProbe = new PaymentCardEntity();
        searchProbe.setCardNumber("5111111111111111");
        cryptoShield.encrypt(searchProbe);
        System.out.println("search still finds the record?     " + store.findByLookup(searchProbe).isPresent());
        // --8<-- [end:search-after-rotation]

        System.exit(0);
    }

    private static List<String> keyIdsOf(PaymentCardEntity record) {
        return record.getLookups().stream().map(CryptoShieldHmacHolder::getCryptoKeyId).toList();
    }
}
