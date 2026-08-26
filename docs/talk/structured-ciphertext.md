Plain Ciphertext → Structured Ciphertext

## The naive approach

The simplest possible way to implement ALE: encrypt the value, store the ciphertext. One field, one column, one blob of bytes.

That gets you *an* opaque blob. But an opaque blob on its own can't answer the questions your application will inevitably need to ask later:

- Which key encrypted this? (You need to know, in order to decrypt it.)
- Which provider handled that key? (AWS KMS? An HSM? Something else?)
- What IV or nonce was used? (Needed to reverse the encryption operation; the encryption scheme defines whether it must be random, unique, or both.)

Naive implementations tend to bolt these on piecemeal and inconsistently: a key alias tacked on as a sibling column, a single HMAC field stored the same ad hoc way for search, cryptographic code scattered wherever a field happened to need it. None of it is wrong exactly — it's just uncoordinated, and it's exactly the kind of design that makes [key rotation](key-rotation.md) and [multi-provider support](key-aliases.md) difficult to retrofit later.

## The fix: a structured ciphertext format

The fix is to stop treating "the ciphertext" as just the raw encrypted bytes, and instead treat it as a small structured record — commonly something like:

- **key ID** — an identifier for the key that performed the encryption. This is how decryption always knows which key to ask for, even years after a key has stopped being "current."
- **IV** — the Initialization Vector used for this specific encryption operation (see [What Is ALE, and Why Do We Still Need It?](intro.md) — this is what makes ciphertext non-deterministic, and it must travel with the ciphertext to reverse the operation).
- **the actual encrypted output** — whatever bytes the underlying cryptographic operation produced. This doesn't need to be a fixed shape, because different providers can return different kinds of output here — an HSM-backed operation's output looks nothing like a password-derived key's.

All of an entity's confidential fields can be bundled into a single payload and encrypted together in one operation, with this whole structure — key ID, IV, output — written into a single stored field, rather than encrypting each field independently and scattering key/IV metadata across several columns.

## Why this structure is the foundation for everything after it

Every capability covered later in this talk depends on the ciphertext carrying its own metadata, rather than living in a bare column with the key/provider/IV tracked (or guessed at) elsewhere:

- **[Key rotation](key-rotation.md)** — old records keep decrypting correctly after the current key changes, because each one's stored key ID says exactly which key to use, permanently.
- **[Multi-provider support](key-aliases.md)** — a new key can point at a totally different provider, and existing ciphertext is unaffected because it already recorded which key (and therefore which provider) it needs.
- **Rekeying ([encryption](rekeying-encryption.md), [HMACs](rekeying-hmacs.md))** — a rekey process can find every record still on an old key by inspecting its stored key ID, decrypt with the old key, and re-encrypt with the new one.

One structured field replaces what would otherwise be several loosely-coordinated columns and a lot of implicit assumptions about "which key did we use back then."
