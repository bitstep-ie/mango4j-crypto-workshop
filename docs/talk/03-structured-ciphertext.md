# 3. Plain Ciphertext → Structured Ciphertext

## The naive approach

The simplest possible way to implement ALE: encrypt the value, store the ciphertext. One field, one column.

```java
@Encrypt
private transient String pan;
```

That gets you *an* opaque blob. But an opaque blob on its own can't answer the questions your application will inevitably need to ask later:

- Which key encrypted this? (You need to know, in order to decrypt it.)
- Which provider handled that key? (AWS KMS? An HSM? Something else?)
- What IV was used? (Needed to reverse the encryption operation, and never safe to reuse.)

Naive implementations tend to bolt these on piecemeal and inconsistently: a key alias tacked on as a sibling column, a single HMAC field stored the same ad hoc way for search, cryptographic code scattered wherever a field happened to need it. None of it is wrong exactly — it's just uncoordinated, and it's exactly the kind of design that makes key rotation (Chapter 5) and multi-provider support (Chapter 2) painful to retrofit later.

## The fix: a structured ciphertext format

mango4j-crypto's `CryptoShield.encrypt()` doesn't write raw ciphertext bytes into your `@EncryptedData` field. It writes a JSON structure:

```json
{
  "cryptoKeyId": "someKeyId",
  "iv": "someInitializationVector",
  "data": {}
}
```

- **`cryptoKeyId`** — the `CryptoKey.id` (Chapter 2) that performed the encryption. This is how decryption always knows which key to ask `CryptoKeyProvider.getById()` for, even years after a key has stopped being "current."
- **`iv`** — the Initialization Vector used for this specific encryption operation (see Chapter 1 — this is what makes ciphertext non-deterministic, and it must travel with the ciphertext to reverse the operation).
- **`data`** — whatever the Encryption Service Delegate actually returned. This is a `Map`, not a fixed shape, because different delegates need different things here — an AWS KMS delegate's output looks nothing like a PBKDF2 delegate's.

All of your `@Encrypt`-annotated fields on an entity are bundled into a single JSON payload and encrypted together in one operation, with the result — this whole structure — written into the single field marked `@EncryptedData`:

```java
@Column(name = "ENCRYPTED_DATA")
@EncryptedData
private String encryptedData;
```

## Why this structure is the foundation for everything after it

Every capability covered later in this talk depends on the ciphertext carrying its own metadata, rather than living in a bare column with the key/provider/IV tracked (or guessed at) elsewhere:

- **Key rotation (Chapter 5)** — old records keep decrypting correctly after the current key changes, because each one's `cryptoKeyId` says exactly which key to use, permanently.
- **Multi-provider support (Chapter 2)** — a new key can point at a totally different delegate `type`, and existing ciphertext is unaffected because it already recorded which key (and therefore which delegate) it needs.
- **Rekeying (Chapters 8–9)** — a rekey job can find every record still on an old key by inspecting `cryptoKeyId` (or the optional `@EncryptionKeyId` field, kept alongside for cheap querying), decrypt with the old key, and re-encrypt with the new one.

One structured field replaces what would otherwise be several loosely-coordinated columns and a lot of implicit assumptions about "which key did we use back then."
