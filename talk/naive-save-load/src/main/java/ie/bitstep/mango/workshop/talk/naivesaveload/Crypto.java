package ie.bitstep.mango.workshop.talk.naivesaveload;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Minimal AES-GCM and HMAC-SHA256 helpers shared by both save()/load() demos in this module. */
public final class Crypto {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Crypto() {
    }

    /** A ciphertext and the IV that produced it, kept as two separate values rather
     * than bundled into one opaque blob, matching a naive sibling-column design. */
    public record EncryptedValue(String ciphertext, String iv) {
    }

    public static SecretKey newKey() {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        return new SecretKeySpec(raw, "AES");
    }

    public static String encrypt(String plaintext, SecretKey key) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String decrypt(String encoded, SecretKey key) {
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Same cipher as {@link #encrypt}, but returns the ciphertext and IV as two
     * separate values instead of one combined blob, for entities that keep the IV
     * as its own sibling column. */
    public static EncryptedValue encryptDetached(String plaintext, SecretKey key) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedValue(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(iv));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String decryptDetached(String ciphertext, String iv, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, Base64.getDecoder().decode(iv)));
            byte[] plaintext = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static SecretKey newHmacKey() {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        return new SecretKeySpec(raw, "HmacSHA256");
    }

    public static String hmac(String value, SecretKey key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
