Key Rotation

!!! abstract "Overview"
    This stage rotates the encryption key: introduce a new key, point new writes at it, and confirm old ciphertext still decrypts under the old key. Facilitators should present this as phase 1 of the two-phase process from [Key Rotation](../talk/key-rotation.md) - phase 2, migrating already-persisted data to the new key, is deliberately deferred to [Rekeying: Encryption](10-rekeying-encryption.md), so each phase gets its own focused exercise. Also flag what this stage deliberately leaves out: HMAC keys rotate through a list of active keys (see [List HMAC Strategy](08-list-hmac-strategy.md)), a different mechanism from the single "current" encryption key this stage covers.

This stage comes as two projects:

- **`starter/`** - what you work in. It compiles and runs, but `rotateEncryptionKeyTo()` doesn't actually change anything yet, so every write still uses the original key. Look for the `// TODO` comment.
- **`complete/`** - the finished reference, with a working rotation.

!!! tip "Follow along"
    ```bash
    cd stages/09-Key-Rotation/starter
    ```
    Using an IDE instead? Open `stages/09-Key-Rotation/starter` as its own project.

## A rotation is a live change, not a redeploy

[Key Aliases & Crypto Key Configs](04-key-aliases-and-configs.md) proved that `getById()` can resolve any known key regardless of which one is "current" - by building two separate providers, each fixed at construction to a different key, since that was the clearest way to demonstrate it. A real rotation doesn't create a new provider - it changes what one running provider's "current" points to, typically because a config value or a database row changed underneath it, not because the application redeployed.

```java
--8<-- "09-Key-Rotation/complete/src/main/java/ie/bitstep/mango/workshop/InMemoryCryptoKeyProvider.java:rotate"
```

This is the entire rotation, application-side. Nothing about `CryptoShield`, the entity, or any already-persisted ciphertext changes - only which key id `getCurrentEncryptionKey()` resolves to next.

**Your turn:** in `starter/.../InMemoryCryptoKeyProvider.java`, make `rotateEncryptionKeyTo()` actually assign `currentEncryptionKeyId`.

## Before, rotate, after

```java
--8<-- "09-Key-Rotation/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:before-rotation"
```

```java
--8<-- "09-Key-Rotation/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:rotate-and-write"
```

Same `CryptoShield`, same provider instance, same `encrypt()` call - only the key id inside the resulting `encryptedData` changes, because `rotateEncryptionKeyTo()` repointed what "current" means in between.

```java
--8<-- "09-Key-Rotation/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:old-still-decrypts"
```

`beforeRotation`'s ciphertext still decrypts correctly after the rotation: `decrypt()` reads the key id recorded inside it and resolves that specific key by id, the same mechanism [Key Aliases & Crypto Key Configs](04-key-aliases-and-configs.md) established - it never asks what's current *right now*.

## Running it

`starter/` before your change: both writes use the same key, since `rotateEncryptionKeyTo()` doesn't do anything yet.

```
before rotation encryptedData: {"cryptoKeyId":"workshop-encryption-key",...}
after rotation encryptedData:  {"cryptoKeyId":"workshop-encryption-key",...}
old record still decrypts:     5111111111111111
```

After your change:

```
before rotation encryptedData: {"cryptoKeyId":"workshop-encryption-key",...}
after rotation encryptedData:  {"cryptoKeyId":"workshop-encryption-key-v2",...}
old record still decrypts:     5111111111111111
```

The `cryptoKeyId` changing between the two writes is the rotation actually happening. The last line is the point of "phase 1 only": `beforeRotation` is never touched, re-encrypted, or migrated - it's still sitting there under the old key, and still perfectly readable. Actually moving it to the new key is [Rekeying: Encryption](10-rekeying-encryption.md), next.
