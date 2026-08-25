Rekeying: Encryption

## Rekeying vs. rotation: not the same step

[Key Rotation](key-rotation.md) drew the line already: rotation (phase 1) is switching *new writes* to a new key. Rekeying is the separate background process that goes back over *existing* records still encrypted under an old key, decrypts them, and re-encrypts them with the new one — the mechanism that eventually lets an old key be retired. Without it, an old key has to stay valid and available forever, because there's no way to know you've stopped needing it.

## Two directions of rekey

A rekey process typically needs to support two directions, both driven by which key a given record was written under:

- **Retiring one specific key** — moving everything currently on that key onto whatever key is current, so the old one can eventually be deleted.
- **Consolidating everything onto one key** — moving everything currently on *any other* key onto a single target key. This has a much broader scope than retiring a single key: it can touch a large fraction of your entities at once, so it warrants careful consideration of the performance and provider-traffic impact before triggering it.

## How it actually finds and re-encrypts records

The rekey process needs to answer two questions about every entity type it manages: which records are still using a given key, and which records aren't. This is why it's worth storing the key ID that encrypted a record in its own queryable column (see [Structured Ciphertext](structured-ciphertext.md)), even though it's not strictly required for decryption — the ciphertext itself already carries the key ID, but a queryable column lets the process find "all records not on the current key" cheaply, instead of decrypting every row just to check.

For each batch of matching records, the process:

1. Decrypts the record with its current (old) key
2. Re-encrypts it with the current encryption key
3. Saves it back, with the stored ciphertext and key-ID column now reflecting the new key

This runs as a background process, working through matching entities in batches and pausing between batches to limit load on the database and any external cryptographic provider (calls to an external key-management service aren't free, and a sudden spike in decrypt/encrypt traffic during a large rekey can trip a provider's own rate limits).

## Why this is a distinct step from the HMAC rekey

Encryption rekeying only has to satisfy one property: after it runs, the record decrypts correctly under the new key. There's no search index or uniqueness constraint riding on the *encrypted* value itself — only on the HMACs, which is a separate structure with its own rekey mechanics, its own completion criteria, and (for the List HMAC Strategy) its own multi-table shape. [Rekeying: HMACs](rekeying-hmacs.md) covers that half.

## How this completes rotation

Put together with [Key Rotation](key-rotation.md)'s phase 1, the full rotation lifecycle looks like:

1. **Phase 1** — new key becomes current; new writes use it; old key still decrypts.
2. **Phase 2 / this chapter** — a rekey process (retiring the old key, or consolidating onto the new one) works through existing records, re-encrypting each from old key to new key.
3. Once no records remain on the old key, it can finally be deleted from the system — decryption no longer needs it, and leaving unused key material around is its own security liability.
