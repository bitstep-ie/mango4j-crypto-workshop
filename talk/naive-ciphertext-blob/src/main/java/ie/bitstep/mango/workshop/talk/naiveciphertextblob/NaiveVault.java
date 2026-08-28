package ie.bitstep.mango.workshop.talk.naiveciphertextblob;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;

/**
 * "Just encrypt the value" - the naive approach {@code docs/talk/structured-ciphertext.md}
 * opens with. AES-GCM is a perfectly good cipher; the pitfall isn't the algorithm, it's
 * that nothing records which key produced a given {@link NaiveBlob}.
 */
public final class NaiveVault {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static SecretKey newKey() {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        return new SecretKeySpec(raw, "AES");
    }

    public NaiveBlob encrypt(String plaintext, SecretKey key) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new NaiveBlob(iv, ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Decrypts with one specific candidate key, or returns {@code null} if this key
     * isn't the one that produced the blob. In a real system, {@code AEADBadTagException}
     * is indistinguishable from "wrong key" and "tampered ciphertext" - both just fail.
     */
    public String tryDecrypt(NaiveBlob blob, SecretKey candidateKey) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, candidateKey, new GCMParameterSpec(GCM_TAG_BITS, blob.iv()));
            byte[] plaintext = cipher.doFinal(blob.ciphertext());
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (AEADBadTagException wrongKey) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * What a naive blob forces every reader to do: since it doesn't say which key
     * encrypted it, decryption means trying every key you still happen to have,
     * until one of them works (or none do).
     */
    // --8<-- [start:brute-force-decrypt] link
    public String decryptByBruteForce(NaiveBlob blob, Map<String, SecretKey> allKnownKeysNewestFirst) {
        for (SecretKey candidate : allKnownKeysNewestFirst.values()) {
            String plaintext = tryDecrypt(blob, candidate);
            if (plaintext != null) {
                return plaintext;
            }
        }
        return null;
    }
    // --8<-- [end:brute-force-decrypt]
}
