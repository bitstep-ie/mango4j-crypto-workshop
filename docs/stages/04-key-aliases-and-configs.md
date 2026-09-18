Key Aliases & Crypto Key Configs

!!! abstract "Overview"
    Every stage so far has had exactly one key, so `getById()` could get away with ignoring the id it was asked for and always returning that same key. This stage makes that shortcut visible by introducing a second key: `getCurrentEncryptionKey()` now answers "which key is current" by alias (an id, resolved through `getById()`) rather than by being the only option in existence. Facilitators should pair this with [Key Aliases & Key Configs](../talk/key-aliases.md), and be explicit up front that this stage is not key rotation - it's the indirection mechanism rotation depends on, covered on its own so [Key Rotation](09-key-rotation.md) can later be "just repoint the alias" instead of introducing two ideas at once.

This stage comes as two projects:

- **`starter/`** - what you work in. It compiles and runs, but both "shields" it builds point at the same key, so nothing demonstrates the alias yet. Look for the `// TODO` comments.
- **`complete/`** - the finished reference, with two independently-configured shields.

!!! tip "Follow along"
    ```bash
    cd stages/04-Key-Aliases-and-Configs/starter
    ```
    Using an IDE instead? Open `stages/04-Key-Aliases-and-Configs/starter` as its own project.

## The shortcut the previous stages got away with

`PaymentCardEntity` is untouched again - the entity never knows how many keys exist. What changes is `InMemoryCryptoKeyProvider`: instead of one hardcoded key returned unconditionally, it now holds two.

## Resolving a key by id, for real

```java
--8<-- "04-Key-Aliases-and-Configs/complete/src/main/java/ie/bitstep/mango/workshop/InMemoryCryptoKeyProvider.java:resolve-by-id"
```

`getById()` is what `decrypt()` calls with whatever key id is recorded inside the ciphertext it's reading - current or not. A provider with only one key can ignore its argument and still be correct by accident. With two keys, that stops being true, which is exactly why this stage adds a second one: it's the smallest change that turns "ignore the id" from a harmless simplification into a bug.

**Your turn:** in `starter/.../InMemoryCryptoKeyProvider.java`, make `getById()` look `cryptoKeyId` up in the key map instead of defaulting to the current key.

## "Current" is just another lookup

```java
--8<-- "04-Key-Aliases-and-Configs/complete/src/main/java/ie/bitstep/mango/workshop/InMemoryCryptoKeyProvider.java:current-by-alias"
```

`getCurrentEncryptionKey()` doesn't hold its own copy of a key - it asks `getById()` for whichever id this provider instance was configured with. That id is the alias: application code (and `CryptoShield`) never sees it, they just ask for "the current encryption key" and "a specific key by id," and both questions are answered by the same lookup.

## The two known keys

```java
--8<-- "04-Key-Aliases-and-Configs/complete/src/main/java/ie/bitstep/mango/workshop/InMemoryCryptoKeyProvider.java:key-map"
```

`workshop-archive-key` and `workshop-encryption-key` are both real, resolvable `CryptoKey`s - the only difference between them, from the provider's point of view, is which one a given instance was told is "current."

## Proving it: two shields, one archive

```java
--8<-- "04-Key-Aliases-and-Configs/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:two-shields"
```

**Your turn:** in `starter/.../Main.java`, build `lastYearsShield` with `"workshop-archive-key"` as current and `todaysShield` with `"workshop-encryption-key"` as current - two providers sharing the same known keys, disagreeing only about which one is "current."

```java
--8<-- "04-Key-Aliases-and-Configs/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:archive-then-decrypt-today"
```

`lastYearsShield` encrypts as if the archive key were still current - simulating a record written a while ago. `todaysShield`, with a completely different "current" key, decrypts it anyway: decrypt reads the key id off the ciphertext and resolves it by id, not by asking what's current right now.

```java
--8<-- "04-Key-Aliases-and-Configs/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:fresh-write"
```

New writes, in contrast, always go out under whichever key *is* current for the shield doing the writing.

## Running it

`starter/` before your changes builds two shields that are actually the same object, so the run succeeds but proves nothing about aliasing - it would succeed even if `getById()` were still broken:

```
archived encryptedData:  {"cryptoKeyId":"workshop-encryption-key",...}
decrypted by today's shield: 5111111111111111
fresh encryptedData:     {"cryptoKeyId":"workshop-encryption-key",...}
```

After both changes, the archived record is genuinely encrypted under a different key than the fresh one, and `todaysShield` still decrypts it correctly:

```
archived encryptedData:  {"cryptoKeyId":"workshop-archive-key",...}
decrypted by today's shield: 5111111111111111
fresh encryptedData:     {"cryptoKeyId":"workshop-encryption-key",...}
```

The `cryptoKeyId` on each line is the tell: two different values, one successful cross-key decrypt.
