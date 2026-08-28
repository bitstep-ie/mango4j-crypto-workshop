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

To be precise about what "avoids" means here: the split does **not** stop application code from *calling* `encrypt()` or `decrypt()` more than once. Nothing about having two fields enforces a call count — that's still entirely up to the application's control flow, and calling either operation redundantly is still a lifecycle-management concern the application has to own. What the split does prevent is the *consequence* of applying the wrong operation to the wrong representation:

- **Data being multiply encrypted.** `encrypt()` always reads from the transient field and writes into the blob field. Call it twice in a row and you just re-encrypt the same plaintext twice, producing a fresh (if wasteful) ciphertext each time — never ciphertext-of-ciphertext, because there's no path by which the blob field's contents feed back in as input to another `encrypt()` call.
- **Exceptions (or silent garbage) from decrypting something that isn't ciphertext.** `decrypt()` always reads from the blob field and writes into the transient field. Call it twice and you just repopulate the same plaintext twice — never an attempt to decrypt a value that was already plaintext, which is exactly the call that throws (or produces garbage) in the naive single-column design below.

## What happens without this discipline: encrypting fields directly into their columns

The failure mode this design specifically guards against is the naive alternative: encrypting values directly in place, in their own columns, with no separate transient/blob distinction at all — hand-rolling encrypt/decrypt calls around a column that's sometimes plaintext and sometimes ciphertext depending on when you look at it. This tends to produce:

- **No single source of truth for "what's encrypted"** — you have to go read the code (or worse, ask around) to know whether a given column currently holds plaintext or ciphertext.
- **Schema churn every time a new field needs protecting** — every field that becomes confidential needs its own bespoke encrypt/decrypt wiring, instead of following one consistent pattern.
- **No consistent record of which key encrypted what** — without a [structured ciphertext](structured-ciphertext.md), there's nowhere obvious to put that metadata, so it either doesn't exist or gets tracked out-of-band.
- **Every query/repository touching that column needs bespoke logic** — because the column's meaning (plaintext or cipher) isn't guaranteed by the type system, every caller has to know the current state by convention.
- **A `save()`/`load()` pair fights its own state.** The typical hand-rolled shape is a `save(entity)` that encrypts the column in place before persisting, and a `load(id)` that decrypts it after fetching. Because there's only one column, `save()` leaves the entity holding ciphertext where it previously held plaintext — so any code that needs to keep working with the value right after saving (rendering a confirmation, continuing the current request) has to remember to call `decrypt()` again just to get the entity back to a state the rest of the application can use. That's an easy step to forget, and it's exactly the kind of call application control flow has to get right on its own, even with a clean transient/blob split — the split removes the corruption/exception failure mode above, not the responsibility to call `decrypt()` wherever plaintext is actually needed again.

The transient/encrypted-blob split is what makes the first four of those non-issues: the field's role tells you what it holds, and one consistent boundary — not scattered application code — owns the only place the conversion happens. It doesn't, by itself, save you from the last one: `encrypt()` doesn't destroy the transient field's plaintext (see above), precisely so that a `save()` built on it can keep using the value afterward without a follow-up `decrypt()` call — but only if `save()` is actually written that way.
