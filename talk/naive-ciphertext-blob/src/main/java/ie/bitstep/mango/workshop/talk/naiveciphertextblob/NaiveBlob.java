package ie.bitstep.mango.workshop.talk.naiveciphertextblob;

import java.util.Arrays;

/**
 * The naive shape: whatever AES-GCM hands back, IV included, and nothing else.
 * No key ID. No version. No provider marker. Just bytes.
 */
public final class NaiveBlob {

    // --8<-- [start:blob-shape]
    private final byte[] iv;
    private final byte[] ciphertext;
    // --8<-- [end:blob-shape]

    public NaiveBlob(byte[] iv, byte[] ciphertext) {
        this.iv = iv.clone();
        this.ciphertext = ciphertext.clone();
    }

    public byte[] iv() {
        return iv.clone();
    }

    public byte[] ciphertext() {
        return ciphertext.clone();
    }

    @Override
    public String toString() {
        return "NaiveBlob{iv=" + Arrays.toString(iv) + ", ciphertext=" + ciphertext.length + " bytes}";
    }
}
