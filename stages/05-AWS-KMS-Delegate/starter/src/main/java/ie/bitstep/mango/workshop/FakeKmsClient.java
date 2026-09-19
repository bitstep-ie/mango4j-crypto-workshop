package ie.bitstep.mango.workshop;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateMacRequest;
import software.amazon.awssdk.services.kms.model.GenerateMacResponse;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * A hand-written stand-in for a real AWS KMS endpoint - no network, no AWS
 * account, no Docker container. Same role {@code Base64EncryptionService}
 * played back in Encrypting a Field: real enough to prove the wiring works,
 * not real cryptographic infrastructure behind it. mango4j-crypto's
 * {@code AwsKmsEncryptionServiceDelegate} only ever talks to the
 * {@code KmsClient} interface it's given - it has no idea this one is fake.
 * <p>
 * Every KMS key id derives its own AES-256 key deterministically
 * (SHA-256 of the key id string), so the same "key" always decrypts what
 * it encrypted, and a different key id never can - without needing to
 * pre-register any keys anywhere.
 */
public class FakeKmsClient implements KmsClient {

    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    @Override
    public String serviceName() {
        return "fake-kms";
    }

    @Override
    public void close() {
        // Nothing to close - there's no real connection.
    }

    @Override
    public EncryptResponse encrypt(EncryptRequest encryptRequest) {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        byte[] cipherBytes = runCipher(Cipher.ENCRYPT_MODE, encryptRequest.keyId(), iv, encryptRequest.plaintext().asByteArray());
        byte[] blob = buildBlob(encryptRequest.keyId(), iv, cipherBytes);

        return EncryptResponse.builder()
                .keyId(encryptRequest.keyId())
                .encryptionAlgorithm(encryptRequest.encryptionAlgorithmAsString())
                .ciphertextBlob(SdkBytes.fromByteArray(blob))
                .build();
    }

    @Override
    public DecryptResponse decrypt(DecryptRequest decryptRequest) {
        // Real KMS never needs a keyId passed to Decrypt either - the
        // ciphertext blob it returns from Encrypt already carries
        // everything needed to reverse the operation. This fake follows
        // suit: the key id travels inside the blob itself, not as a
        // separate parameter mango4j-crypto's delegate would have to track.
        BlobParts parts = parseBlob(decryptRequest.ciphertextBlob().asByteArray());
        byte[] plaintext = runCipher(Cipher.DECRYPT_MODE, parts.keyId(), parts.iv(), parts.cipherBytes());

        return DecryptResponse.builder()
                .keyId(parts.keyId())
                .plaintext(SdkBytes.fromByteArray(plaintext))
                .build();
    }

    @Override
    public GenerateMacResponse generateMac(GenerateMacRequest generateMacRequest) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(deriveKeyBytes(generateMacRequest.keyId()), "HmacSHA256"));
            byte[] result = mac.doFinal(generateMacRequest.message().asByteArray());
            return GenerateMacResponse.builder()
                    .keyId(generateMacRequest.keyId())
                    .mac(SdkBytes.fromByteArray(result))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Fake KMS generateMac failed", e);
        }
    }

    private static byte[] buildBlob(String keyId, byte[] iv, byte[] cipherBytes) {
        byte[] keyIdBytes = keyId.getBytes(StandardCharsets.UTF_8);
        ByteBuffer blob = ByteBuffer.allocate(4 + keyIdBytes.length + iv.length + cipherBytes.length);
        blob.putInt(keyIdBytes.length).put(keyIdBytes).put(iv).put(cipherBytes);
        return blob.array();
    }

    private static BlobParts parseBlob(byte[] blob) {
        ByteBuffer buffer = ByteBuffer.wrap(blob);
        byte[] keyIdBytes = new byte[buffer.getInt()];
        buffer.get(keyIdBytes);
        byte[] iv = new byte[IV_LENGTH];
        buffer.get(iv);
        byte[] cipherBytes = new byte[buffer.remaining()];
        buffer.get(cipherBytes);
        return new BlobParts(new String(keyIdBytes, StandardCharsets.UTF_8), iv, cipherBytes);
    }

    private record BlobParts(String keyId, byte[] iv, byte[] cipherBytes) {
    }

    private static byte[] runCipher(int mode, String keyId, byte[] iv, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec key = new SecretKeySpec(deriveKeyBytes(keyId), "AES");
            cipher.init(mode, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return cipher.doFinal(input);
        } catch (Exception e) {
            throw new IllegalStateException("Fake KMS cipher operation failed", e);
        }
    }

    private static byte[] deriveKeyBytes(String keyId) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(keyId.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
