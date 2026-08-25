# 4. Transient vs. Encrypted Blobs

## Two representations of the same value

Every confidential field an entity carries actually has two lives:

- A **transient** (working, in-memory) representation — the plaintext value your business logic actually works with (validates, compares, displays)
- An **encrypted-blob** representation — the structured ciphertext (Chapter 3) that's what actually gets persisted

A sound ALE design keeps these strictly separate: the plaintext value exists only for as long as it's needed in memory, and the only thing that ever gets written to storage is the encrypted blob. The two are never the same field, and nothing is expected to hold both at once.

## Why this separation matters

If a field can hold either plaintext or ciphertext depending on when you look at it, nothing in the code guarantees which one it currently holds — every reader and writer has to know by convention, and that convention eventually gets it wrong. Keeping the working value and the persisted value as genuinely separate representations means whatever serializes data out to storage can only ever see the encrypted form; the plaintext simply isn't reachable from that path. This is usually enforced by whatever mechanism controls what a serialization layer (an ORM, a JSON mapper, whatever writes to storage) is allowed to touch — excluding the working field from that path entirely, so a plaintext value can never be accidentally flushed to disk.

Encrypting a value doesn't need to destroy the working copy — the application can keep using the plaintext for the rest of its current operation. Decrypting, likewise, just repopulates the working representation from the stored blob; it doesn't need to touch the blob itself.

## The failure mode this avoids: double encrypt/decrypt

Keeping the two representations distinct — and only ever converting between them at one well-defined boundary — means a value gets encrypted exactly once on the way in and decrypted exactly once on the way out. If plaintext and ciphertext shared the same field, or an operation encrypted data that was already ciphertext, the result would be redundant (and potentially incorrect) encrypt/decrypt passes on the same data as it moves through the system. Requiring that only the working fields hold plaintext and only the designated stored field holds ciphertext removes any state a value can end up in that's ambiguous about which one it currently is.

## What happens without this discipline: encrypting fields directly into their columns

The failure mode this design specifically guards against is the naive alternative: encrypting values directly in place, in their own columns, with no separate transient/blob distinction at all — hand-rolling encrypt/decrypt calls around a column that's sometimes plaintext and sometimes ciphertext depending on when you look at it. This tends to produce:

- **No single source of truth for "what's encrypted"** — you have to go read the code (or worse, ask around) to know whether a given column currently holds plaintext or ciphertext.
- **Schema churn every time a new field needs protecting** — every field that becomes confidential needs its own bespoke encrypt/decrypt wiring, instead of following one consistent pattern.
- **No consistent record of which key encrypted what** — without a structured ciphertext (Chapter 3), there's nowhere obvious to put that metadata, so it either doesn't exist or gets tracked out-of-band.
- **Every query/repository touching that column needs bespoke logic** — because the column's meaning (plaintext or cipher) isn't guaranteed by the type system, every caller has to know the current state by convention.

The transient/encrypted-blob split is what makes all four of those non-issues: the field's role tells you what it holds, and one consistent boundary — not scattered application code — owns the only place the conversion happens.
