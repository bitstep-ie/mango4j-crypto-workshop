package ie.bitstep.mango.workshop.talk.naivesinglehmac;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Deterministic HMAC-SHA256, exactly the primitive {@code docs/talk/introducing-hmacs.md} describes. */
public final class HmacService {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static SecretKey newKey() {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        return new SecretKeySpec(raw, "HmacSHA256");
    }

    // --8<-- [start:hmac]
    public String hmac(String value, SecretKey key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    // --8<-- [end:hmac]
}
