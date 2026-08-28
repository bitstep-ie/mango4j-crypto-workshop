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
- **A hand-rolled `save()`/`load()` pair built around that one column has its own, distinct problems** — covered separately below, because they're really about mutating a shared object in place, not about which representation a column holds at a given moment.

The transient/encrypted-blob split is what makes the first four of those non-issues: the field's role tells you what it holds, and one consistent boundary — not scattered application code — owns the only place the conversion happens.

### The naive save()/load() pattern's own problems

A typical hand-rolled implementation looks something like:

```
save(entity):
    encrypt relevant fields on entity, in place
    persist entity (the real save)
    decrypt those fields back, in place

load(id):
    entity = fetch entity from DB
    decrypt relevant fields on entity, in place
    return entity
```

This has two problems of its own, on top of (and independent of) the single-column ambiguity above:

- **A concurrency window inside `save()`.** Between the encrypt step and the decrypt step, the *same* entity instance — not a copy — sits there with ciphertext in fields the rest of the application expects to hold plaintext. In a single-threaded flow that window is invisible and harmless. In a multi-threaded application where that entity can be read concurrently — shared across requests, held in a cache, referenced from another thread — any read that lands inside the window silently sees ciphertext where it expects plaintext. There's no lock, no exception, no signal that the object is mid-mutation; it just quietly hands out the wrong representation to whoever happens to read it at the wrong moment. This is exactly the failure mode a transient/blob split removes: because `encrypt()` writes the blob field without touching the transient field, there is no window where the working representation is anything other than plaintext, so a concurrent reader sees a consistent value throughout.
- **`load()`'s decrypt is forced, and a forced decrypt can silently discard in-memory work.** `load()`'s job is "fetch from the DB, then decrypt" — but if it runs against an entity instance that already has unsaved application changes (re-loading an entity that's still being edited, refreshing a cached instance, a second `load()` call for the same id mid-request), the freshly-decrypted DB values overwrite whatever was already there. There's no conflict check, no diff, no exception — the caller just loses whatever hadn't been saved yet, with no warning, and finds out only if they happen to notice the value changed. Unlike the concurrency window above, a transient/blob split does **not** fix this on its own: `decrypt()` still writes into the transient field regardless of what was there before, whether it's mango4j-crypto's `decrypt()` or a hand-rolled one. Avoiding it is a matter of *when* the application chooses to call `decrypt()`/`load()` at all — never blindly, against an entity that might carry pending changes — not something the ciphertext representation can enforce for you.
