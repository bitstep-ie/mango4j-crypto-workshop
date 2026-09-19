package ie.bitstep.mango.workshop;

import ie.bitstep.mango.crypto.CryptoShield;

import java.util.List;

/**
 * Refreshing a record's HMACs is the same encrypt() call every previous
 * stage has already used - mango4j-crypto doesn't expose a separate
 * "HMACs only" path, so calling encrypt() again also re-encrypts
 * cardNumber. That's harmless here, since this stage doesn't rotate the
 * encryption key (just a fresh IV and ciphertext bytes for the same
 * plaintext under the same key) - but it's worth knowing that Rekeying:
 * Encryption and Rekeying: HMACs being conceptually separate operations
 * doesn't mean calling one never touches what the other is responsible
 * for.
 */
public class RekeyHmacSweep {

    public static void run(CryptoShield cryptoShield, List<PaymentCardEntity> records) {
        records.forEach(cryptoShield::encrypt);
    }
}
