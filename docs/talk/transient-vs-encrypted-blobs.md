Transient vs. Encrypted Blobs

## Two representations of the same value

Every confidential field an entity carries actually has two lives:

- A **transient** (working, in-memory) representation — the plaintext value your business logic actually works with (validates, compares, displays)
- An **encrypted-blob** representation — the [structured ciphertext](structured-ciphertext.md) that's what actually gets persisted

A sound ALE design keeps these representations separate: the plaintext value is used in memory and the encrypted blob is the persistence representation. The two are not the same field, although both can legitimately be present on an in-memory entity while it is being processed.

## Why this separation matters

If a field can hold either plaintext or ciphertext depending on when you look at it, every reader and writer has to infer its current representation. Keeping working and persisted values separate makes the intended boundary explicit and lets an ORM or persistence mapper exclude the plaintext field. That is a valuable safeguard, but it is not automatic: review serializers, ORM mappings, logs, caches, queues, traces, and diagnostic tooling as well. Any of them can still persist or expose plaintext if configured to inspect the working field.

Encrypting a value doesn't need to destroy the working copy — the application can keep using the plaintext for the rest of its current operation. Decrypting, likewise, just repopulates the working representation from the stored blob; it doesn't need to touch the blob itself.

## The failure mode this avoids: double encrypt/decrypt

Keeping the two representations distinct — and converting at a well-defined boundary — makes redundant encrypt/decrypt passes easier to prevent and test. If plaintext and ciphertext shared the same field, or an operation encrypted data that was already ciphertext, callers could apply the wrong operation. The split removes ambiguity about the intended representation, but application control flow must still ensure that encryption and decryption are called at the correct lifecycle points.

## What happens without this discipline: encrypting fields directly into their columns

The failure mode this design specifically guards against is the naive alternative: encrypting values directly in place, in their own columns, with no separate transient/blob distinction at all — hand-rolling encrypt/decrypt calls around a column that's sometimes plaintext and sometimes ciphertext depending on when you look at it. This tends to produce:

- **No single source of truth for "what's encrypted"** — you have to go read the code (or worse, ask around) to know whether a given column currently holds plaintext or ciphertext.
- **Schema churn every time a new field needs protecting** — every field that becomes confidential needs its own bespoke encrypt/decrypt wiring, instead of following one consistent pattern.
- **No consistent record of which key encrypted what** — without a [structured ciphertext](structured-ciphertext.md), there's nowhere obvious to put that metadata, so it either doesn't exist or gets tracked out-of-band.
- **Every query/repository touching that column needs bespoke logic** — because the column's meaning (plaintext or cipher) isn't guaranteed by the type system, every caller has to know the current state by convention.

The transient/encrypted-blob split is what makes all four of those non-issues: the field's role tells you what it holds, and one consistent boundary — not scattered application code — owns the only place the conversion happens.
