Introducing HMACs

!!! abstract "Overview"
    Encrypted fields can't be searched or uniqueness-checked directly: the IV makes ciphertext non-deterministic, so the same plaintext encrypts differently every time. This stage introduces `@Hmac`/`@HmacKeyId`, mango4j-crypto's answer, before choosing between the two storage strategies in later stages. Facilitators should pair this with [Introducing HMACs](../talk/introducing-hmacs.md), which explains why HMAC (not encryption) is the right primitive for this: it's deterministic and doesn't use an IV. Be explicit that this stage stops at proving determinism - it doesn't yet *use* the HMAC for search or uniqueness. That's [Single HMAC Strategy](07-single-hmac-strategy.md), next.

This stage comes as two projects:

- **`starter/`** - what you work in. It compiles and runs, but `cardNumber` isn't `@Hmac`'d yet, so both HMAC fields come back `null`. Look for the `// TODO` comments.
- **`complete/`** - the finished reference, with a working HMAC.

!!! tip "Follow along"
    ```bash
    cd stages/06-Introducing-HMACs/starter
    ```
    Using an IDE instead? Open `stages/06-Introducing-HMACs/starter` as its own project.

## A field can be both confidential and searchable

`@Hmac` is independent of `@Encrypt` - a field can carry both. Where `@Encrypt` says "hide this," `@Hmac` says "also compute a deterministic fingerprint of this," and mango4j-crypto happily does both in the same `encrypt()` call.

```java
--8<-- "06-Introducing-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/PaymentCardEntity.java:annotate-hmac"
```

**Your turn:** in `starter/.../PaymentCardEntity.java`, add `@Hmac` above `cardNumber`, alongside the existing `@Encrypt`.

## What the Single HMAC Strategy needs on the entity

The class-level `@SingleHmacStrategy` annotation (already on `PaymentCardEntity`) picks the simplest of mango4j-crypto's HMAC storage strategies - what it's good for, and where it falls short, is next stage's topic. For now, it requires two things per `@Hmac` field:

```java
--8<-- "06-Introducing-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/PaymentCardEntity.java:hmac-fields"
```

`cardNumberHmac` is already there - a plain persisted field, named after the source field plus `Hmac`, where the library writes the computed value. `hmacKeyId` needs its `@HmacKeyId` annotation, playing the same role for HMACs that `@EncryptionKeyId`-style tracking plays for encryption: recording which key produced the value.

**Your turn:** add `@HmacKeyId` above `hmacKeyId`.

## A key that's just for HMACs

```java
--8<-- "06-Introducing-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/InMemoryCryptoKeyProvider.java:current-hmac-keys"
```

`getCurrentHmacKeys()` is resolved the exact same way `getCurrentEncryptionKey()` was in the previous stage: by id, through `getById()`. It returns a `List` - not because this stage uses more than one, but because [List HMAC Strategy](08-list-hmac-strategy.md) will. The key itself (`workshop-hmac-key`) is entirely separate from `workshop-encryption-key`: encryption and HMAC keys are never the same key.

**Your turn:** in `starter/.../InMemoryCryptoKeyProvider.java`, return `List.of(getById(CURRENT_HMAC_KEY_ID))` instead of the empty list.

## Running it

`starter/` before your changes compiles and runs, but with `cardNumber` not yet `@Hmac`'d, `PaymentCardEntity` has no HMAC field at all as far as the library can see - both `cardNumberHmac` and `hmacKeyId` stay `null`:

```
first  cardNumberHmac: null
second cardNumberHmac: null
same HMAC?              true
hmacKeyId:             null
```

After all three changes:

```
first  cardNumberHmac: vqGr/6T0RMdQlvu2bP9FTH9nx2vqNW6Nh2NaKHLnbmQ=
second cardNumberHmac: vqGr/6T0RMdQlvu2bP9FTH9nx2vqNW6Nh2NaKHLnbmQ=
same HMAC?              true
hmacKeyId:             workshop-hmac-key
```

The interesting comparison is against `encryptedData`, printed just above: two entities built from the identical card number get two different ciphertexts (fresh IV each time) but the exact same HMAC, every run. That determinism is the whole reason HMAC - not encryption - is what search and uniqueness end up built on.
