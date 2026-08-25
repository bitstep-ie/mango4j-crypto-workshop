# 8. Rekeying: Encryption

## Rekeying vs. rotation: not the same step

Chapter 5 drew the line already: rotation (phase 1) is switching *new writes* to a new key. Rekeying is the separate background process that goes back over *existing* records still encrypted under an old key, decrypts them, and re-encrypts them with the new one — the mechanism that eventually lets an old key actually be retired. Without it, an old key has to stay valid and available forever, because there's no way to know you've stopped needing it.

## Two rekey modes

mango4j-crypto's built-in rekey job drives this off a `rekeyMode` field on the `CryptoKey` itself (Chapter 2), with two modes:

- **`KEY_OFF`** — rekey everything *off* this specific key and onto the current key. Used when you want to retire one particular old key. (Setting it on the *current* key is a no-op — it doesn't make sense to key something off itself.)
- **`KEY_ON`** — rekey everything currently on *any other* key onto this one. A much bigger hammer: it can touch a large fraction of your entities at once, so it needs real consideration of the performance and provider-traffic impact before triggering it. (Also a no-op if set on a key that isn't current.)

## How it actually finds and re-encrypts records

The rekey job needs to answer two questions about every entity type it manages: which records are still using a given key, and which records aren't. That's why `@EncryptionKeyId` (Chapter 3) is worth having even though it's optional for decryption — the ciphertext itself already carries the key ID, but a queryable column lets the job find "all records where `encryptionKeyId != currentKey`" cheaply, instead of decrypting every row just to check.

For each batch of matching records, the job:

1. Decrypts the record with its current (old) key
2. Re-encrypts it with the current encryption key
3. Saves it back, with `@EncryptedData` and `@EncryptionKeyId` now reflecting the new key

This runs as a background process the application configures once — a scheduler that periodically checks for keys marked `KEY_ON`/`KEY_OFF` and works through matching entities in batches, pausing between batches to avoid hammering the database or an external cryptographic provider (KMS calls aren't free, and a sudden spike in decrypt/encrypt traffic during a large rekey is exactly the kind of thing that can trip a provider's own rate limits).

## Why this is a distinct step from the HMAC rekey

Encryption rekeying only has to satisfy one property: after it runs, the record decrypts correctly under the new key. There's no search index or uniqueness constraint riding on the *encrypted* value itself — only on the HMACs, which is a separate structure with its own rekey mechanics, its own completion criteria, and (for the List HMAC Strategy) its own multi-table shape. Chapter 9 covers that half.

## How this completes rotation

Put together with Chapter 5's phase 1, the full rotation lifecycle looks like:

1. **Phase 1** — new key becomes current; new writes use it; old key still decrypts.
2. **Phase 2 / this chapter** — a rekey job (`KEY_OFF` on the old key, or `KEY_ON` on the new one) works through existing records, re-encrypting each from old key to new key.
3. Once no records remain on the old key, it can finally be deleted from the system — decryption no longer needs it, and leaving unused key material around is its own security liability.
