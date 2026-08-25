# 4. Transient vs. Encrypted Blobs

## Two representations of the same value

Every confidential field an entity carries actually has two lives:

- A **transient** representation — the plaintext value your business logic actually works with (validates, compares, displays)
- An **encrypted-blob** representation — the structured ciphertext (Chapter 3) that's what actually gets persisted

mango4j-crypto keeps these strictly separate, and requires it in the entity definition:

```java
@Encrypt
private transient String pan;

@Encrypt
private transient String userName;

@Column(name = "ENCRYPTED_DATA")
@EncryptedData
private String encryptedData;
```

The `pan` and `userName` fields are marked `transient` — a hard requirement mango4j-crypto enforces at registration time. All `@Encrypt` fields get bundled into one JSON payload and encrypted in a single operation into the one `@EncryptedData` field; the source fields themselves are never touched by that operation and never get their own column.

## Why `transient` matters

`transient` isn't decoration here — it's what stops serialization frameworks (Hibernate, Jackson, whatever your ORM is) from ever flushing the plaintext value to the database on their own. Without it, nothing stops an ORM from quietly persisting `pan` into its own column alongside the encrypted blob, defeating the entire point of ALE the moment someone adds an innocuous `@Column` to the wrong field.

Calling `cryptoShield.encrypt(entity)` doesn't clear the transient fields either — they keep their plaintext values so your code can keep working with them after encrypting. `cryptoShield.decrypt(entity)` does the reverse: it reads `encryptedData`, decrypts it, and repopulates the transient fields — it does not touch `encryptedData` itself.

## The failure mode this avoids: double encrypt/decrypt

Keeping the two representations distinct — and only ever converting between them at the `CryptoShield.encrypt()`/`decrypt()` boundary — means a value gets encrypted exactly once on the way in and decrypted exactly once on the way out. If plaintext and ciphertext shared the same field, or an application called `encrypt()` twice on data that was already ciphertext, the result would be redundant (and potentially incorrect) encrypt/decrypt passes on the same data as it moves through the system. Because mango4j-crypto requires that only the transient fields hold plaintext and only `@EncryptedData` holds ciphertext, there's no state a value can end up in that's ambiguous about which one it currently is.

## What happens without this discipline: encrypting fields directly into their columns

The failure mode this design specifically guards against is the naive alternative: encrypting values directly in place, in their own columns, with no separate transient/blob distinction at all — e.g. hand-rolling `encrypt()`/`decrypt()` calls around a `panColumn` field that's sometimes plaintext and sometimes ciphertext depending on when you look at it. This tends to produce:

- **No single source of truth for "what's encrypted"** — you have to go read the code (or worse, ask around) to know whether a given column currently holds plaintext or ciphertext.
- **Schema churn every time a new field needs protecting** — every field that becomes confidential needs its own bespoke encrypt/decrypt wiring, instead of just adding `@Encrypt`.
- **No consistent record of which key encrypted what** — without a structured ciphertext (Chapter 3), there's nowhere obvious to put that metadata, so it either doesn't exist or gets tracked out-of-band.
- **Every query/repository touching that column needs bespoke logic** — because the column's meaning (plaintext or cipher) isn't guaranteed by the type system, every caller has to know the current state by convention.

The transient/`@EncryptedData` split is what makes all four of those non-issues: the field's type tells you what it holds, and the library — not scattered application code — owns the only place the conversion happens.
