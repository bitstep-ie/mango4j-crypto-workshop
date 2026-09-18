Introducing HMACs

!!! abstract "Overview"
    Encrypted fields can't be searched or uniqueness-checked directly: the IV makes ciphertext non-deterministic, so the same plaintext encrypts differently every time. This stage introduces `@Hmac`/`@HmacKeyId`, mango4j-crypto's answer, before choosing between the two storage strategies in later stages. Facilitators should pair this with [Introducing HMACs](../talk/introducing-hmacs.md), which explains why HMAC (not encryption) is the right primitive for this: it's deterministic and doesn't use an IV.

!!! info "Content coming soon"
    This stage is a placeholder. It should give learners:

    - `@Hmac` added to `cardNumber` alongside the existing `@Encrypt`, and an `@HmacKeyId`/HMAC storage field on the entity
    - A dedicated HMAC key (separate from the encryption key) resolved through the `CryptoKeyProvider`, per [Key Aliases & Key Configs](../talk/key-aliases.md)
    - Output showing the HMAC value is stable across repeated runs for the same plaintext, unlike `encryptedData`'s ciphertext
    - A closing note that this stage stops short of *using* the HMAC for search/uniqueness — that's Single HMAC Strategy and List HMAC Strategy, next

    Likely a starter/complete stage, since it's an exercise (add the annotation, wire the key) rather than pure configuration.
