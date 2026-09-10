Real Encryption

!!! abstract "Overview"
    The previous stage proved the plumbing with a fake Base64 "encryption" so nothing hid the mechanics. This stage swaps that for real cryptography without touching the entity or the calling code: the only changes are the delegate handed to `CryptoShield` and the configuration carried on the key. Running it, `encryptedData` stops being readable Base64 and becomes a structured record holding the key ID, the IV, and the actual ciphertext, which is exactly the shape [Structured Ciphertext](../talk/structured-ciphertext.md) argues for. This is also the first time [Key Aliases & Key Configs](../talk/key-aliases.md) becomes concrete: the key is an object that describes how to encrypt with it, not a bare string.

This stage comes as two projects:

- **`starter/`** - what you work in. It runs, but still on the fake Base64 delegate from the last stage. Look for the `// TODO` comments.
- **`complete/`** - the finished reference, using real AES/GCM encryption.

!!! tip "Follow along"
    ```bash
    cd stages/03-Real-Encryption/starter
    ```
    Using an IDE instead? Open `stages/03-Real-Encryption/starter` as its own project.

## What does and doesn't change

`PaymentCardEntity` is untouched from the previous stage: `cardNumber` is still `@Encrypt`, `encryptedData` is still `@EncryptedData`. The annotation model is deliberately independent of which cryptography runs underneath, so moving to real encryption is a wiring change, not an entity change.

Two things carry the change:

1. The **delegate** passed to `CryptoShield`, the component that performs the actual encrypt and decrypt.
2. The **key** the `CryptoKeyProvider` hands back, which now has to describe the mechanism and its parameters.

## Choosing a delegate

```java
--8<-- "03-Real-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:choose-delegate"
```
<!-- link -->

`PBKDF2EncryptionService` ships with `mango4j-crypto`. It uses the JDK's own cryptography (AES in GCM mode here) with the AES key derived from a passphrase, so it is real encryption you can run locally with no external key service. A production system would more likely use a KMS-backed delegate, but the wiring is identical: `CryptoShield` picks a delegate per operation by matching the key's `type` against each delegate's `supportedCryptoKeyType()`.

**Your turn:** in `starter/src/main/java/ie/bitstep/mango/workshop/Main.java`, reassign `delegate` to a `new PBKDF2EncryptionService()`.

## Configuring the key

```java
--8<-- "03-Real-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/InMemoryCryptoKeyProvider.java:key-config"
```

A `CryptoKey` is an object, not an identifier. Its `type` selects the delegate, and its `configuration` map carries whatever that delegate needs: here the cipher parameters plus the passphrase and salt the key is derived from. None of that is key material, and none of it reaches application code, which only ever asks for "the current encryption key".

The passphrase here is a hardcoded literal so the stage runs anywhere. A real deployment would source it from configuration or a secret store.

**Your turn:** in `starter/src/main/java/ie/bitstep/mango/workshop/InMemoryCryptoKeyProvider.java`, set `keyType` to `"PBKDF2"` and populate `keyConfiguration` (see the `// TODO`).

## Encrypting and decrypting

The calls are unchanged:

```java
--8<-- "03-Real-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:encrypt"
```

```java
--8<-- "03-Real-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:decrypt"
```

`decrypt()` only receives the `encryptedData` blob. It reads the `cryptoKeyId` recorded inside it, asks the provider for that key by ID, and gets back the configuration it needs to reverse the operation. Nothing in the calling code has to know the encryption is PBKDF2, or that a key rotation might later make this key no longer "current".

## Running it

Run `starter/` before making changes and you get the previous stage's output: `encryptedData` is Base64 that decodes straight back to the plaintext JSON.

```
encryptedData:                {"cryptoKeyId":"workshop-encryption-key","data":{"cipherText":"eyJjYXJkTnVtYmVyIjoiNTExMTExMTExMTExMTExMSJ9"}}
```

After both changes, run it again:

```
cardNumber (still in memory): 5111111111111111
encryptedData:                {"cryptoKeyId":"workshop-encryption-key","data":{"iv":"...","cipherText":"...","algorithm":"AES","mode":"GCM","keySize":256,"gcmTagLength":128,"iterations":10000,"padding":"NoPadding"}}
decrypted cardNumber:         5111111111111111
```

The `cipherText` is now real AES/GCM output, and the `iv` sits alongside it because decryption cannot reverse the operation without it. The exact bytes change on every run, since a fresh IV is generated each time.
