# 2. Encrypting a Field

!!! abstract "Overview"
    This is the first stage that actually encrypts something. The goal is the minimum set of moving parts needed to do that: one field marked `@Encrypt`, one field marked `@EncryptedData` for the ciphertext to land in, a key source (`CryptoKeyProvider`), and something to do the actual encrypting (`EncryptionServiceDelegate`). We deliberately use a fake Base64 "encryption" delegate here so the plumbing is visible without any real cryptography or KMS setup getting in the way — real encryption comes in a later stage.

!!! tip "Follow along"
    ```bash
    cd stages/02-Encrypt-a-Field
    ```
    Using an IDE instead? Just open `stages/02-Encrypt-a-Field` as its own project.

## The entity

`mango4j-crypto` works by annotating fields on a plain Java object. `PaymentCardEntity` has two fields that matter:

```java
--8<-- "02-Encrypt-a-Field/src/main/java/ie/bitstep/mango/workshop/PaymentCardEntity.java:encrypt-field"
```

`@Encrypt` marks `cardNumber` as confidential — note it must be `transient`, which the library enforces. Its plaintext value is never written to `encryptedData` or stored anywhere by the library itself; it just stays as a normal, in-memory value on the object you're holding.

```java
--8<-- "02-Encrypt-a-Field/src/main/java/ie/bitstep/mango/workshop/PaymentCardEntity.java:encrypted-data-field"
```

`@EncryptedData` marks where the resulting ciphertext goes. This is the field you'd actually persist (to a database, a file, wherever) — never `cardNumber` itself.

## Wiring up CryptoShield

`CryptoShield` is the object you call `encrypt()`/`decrypt()` on. Building one needs two things: something that supplies cryptographic keys (a `CryptoKeyProvider`), and something that does the actual encrypting (an `EncryptionServiceDelegate`).

For this stage, [`InMemoryCryptoKeyProvider`](https://github.com/bitstep-ie/mango4j-crypto-workshop/blob/main/stages/02-Encrypt-a-Field/src/main/java/ie/bitstep/mango/workshop/InMemoryCryptoKeyProvider.java) hands back one hardcoded key — a real application would look keys up from wherever it stores them. And rather than wiring up real encryption (KMS, a cipher, ...), we use the library's built-in `Base64EncryptionService`, which just Base64-encodes data — it exists specifically so you can learn and test the mechanics without any real cryptographic setup.

```java
--8<-- "02-Encrypt-a-Field/src/main/java/ie/bitstep/mango/workshop/Main.java:build-shield"
```

## Encrypting and decrypting

```java
--8<-- "02-Encrypt-a-Field/src/main/java/ie/bitstep/mango/workshop/Main.java:encrypt"
```

`encrypt()` reads the `@Encrypt` field(s), builds the ciphertext, and writes it to the `@EncryptedData` field — `cardNumber` itself is left untouched, so you can keep using it in your code right after encrypting.

```java
--8<-- "02-Encrypt-a-Field/src/main/java/ie/bitstep/mango/workshop/Main.java:decrypt"
```

`decrypt()` does the reverse: given only the ciphertext, it reconstructs `cardNumber`. This is what "loading an entity back from storage" looks like in practice — you'd load a row containing only `encryptedData`, then call `decrypt()` to get the real value back.

Run it and you'll see something like:

```
cardNumber (still in memory): 4111111111111111
encryptedData:                {"cryptoKeyId":"workshop-encryption-key","data":{"cipherText":"eyJjYXJkTnVtYmVyIjoiNDExMTExMTExMTExMTExMSJ9"}}
decrypted cardNumber:         4111111111111111
```

Next: *3. Coming soon*.
