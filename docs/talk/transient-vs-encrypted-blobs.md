Transient vs. Encrypted Blobs

## Two representations of the same value

Every confidential field an entity carries actually has two lives:

- A **transient** (working, in-memory) representation — the plaintext value your business logic actually works with (validates, compares, displays)
- An **encrypted-blob** representation — the [structured ciphertext](structured-ciphertext.md) that's what actually gets persisted

A sound ALE design keeps these representations separate: the plaintext value is used in memory and the encrypted blob is the persistence representation. The two are not the same field, although both can legitimately be present on an in-memory entity while it is being processed.

## Why this separation matters

If a field can hold either plaintext or ciphertext depending on when you look at it, every reader and writer has to infer its current representation. Keeping working and persisted values separate makes the intended boundary explicit and lets an ORM or persistence mapper exclude the plaintext field. That is a valuable safeguard, but it is not automatic: review serializers, ORM mappings, logs, caches, queues, traces, and diagnostic tooling as well. Any of them can still persist or expose plaintext if configured to inspect the working field.

Encrypting a value doesn't need to destroy the working copy — the application can keep using the plaintext for the rest of its current operation. Decrypting, likewise, just repopulates the working representation from the stored blob; it doesn't need to touch the blob itself.

## The failure mode this avoids, and the one it doesn't: double encrypt/decrypt

To be precise about what "avoids" means here: the split does **not** stop application code from *calling* `encrypt()` or `decrypt()` more than once. Nothing about having two fields enforces a call count, that's still entirely up to the application's control flow, and calling either operation redundantly is still a lifecycle-management concern the application has to own. What the split does prevent is the *consequence* of applying the wrong operation to the wrong representation, but that consequence is not the same for both operations:

- **Calling `encrypt()` repeatedly is safe, just wasteful.** It always reads from the transient field and writes into the blob field. Call it twice in a row and you just re-encrypt the same plaintext twice, producing a fresh (if unnecessary) ciphertext each time, never ciphertext-of-ciphertext, because there's no path by which the blob field's contents feed back in as input to another `encrypt()` call. The transient field is never touched, so there is nothing for a repeated `encrypt()` call to lose.
- **Calling `decrypt()` repeatedly is not 100% safe.** It won't throw or produce garbage the way decrypting a value that isn't actually ciphertext would (that failure mode is genuinely gone), but it always overwrites the transient field with whatever the blob currently decodes to. If the application changed that field since the last `decrypt()`, for example by editing the entity in memory before saving, that change is silently discarded. The transient/blob split removes the corruption/exception risk; it does not make `decrypt()` idempotent or safe to call blindly.

| Operation | Model | Safe to call more than once? | What actually happens |
|---|---|---|---|
| `encrypt()` | Transient/blob | Yes, wasteful only | Reads the transient field, writes a fresh blob each time; the transient field is never touched, so nothing can be lost |
| `decrypt()` | Transient/blob | No | Overwrites the transient field every time; any unsaved change made to it since the last `decrypt()` is silently lost, with no error |
| `encrypt()` | Direct-field (naive) | No | The first call turns the field into ciphertext; a second call re-encrypts that ciphertext as if it were plaintext, corrupting the value |
| `decrypt()` | Direct-field (naive) | No | Same silent data loss as the transient/blob version, and can also throw or produce garbage if the field didn't actually hold ciphertext at the time |

This is the same forced-overwrite risk covered below for `load()`, just stated for `decrypt()` directly: `load()` is unsafe to call blindly because it calls `decrypt()`, not because of anything specific to loading.

## What happens without this discipline: encrypting fields directly into their columns

The failure mode this design specifically guards against is the naive alternative: encrypting values directly in place, in their own columns, with no separate transient/blob distinction at all — hand-rolling encrypt/decrypt calls around a column that's sometimes plaintext and sometimes ciphertext depending on when you look at it. This tends to produce:

- **No single source of truth for "what's encrypted"** — you have to go read the code (or worse, ask around) to know whether a given column currently holds plaintext or ciphertext.
- **Schema churn every time a new field needs protecting** — every field that becomes confidential needs its own bespoke encrypt/decrypt wiring, instead of following one consistent pattern.
- **No consistent record of which key encrypted what** — without a [structured ciphertext](structured-ciphertext.md), there's nowhere obvious to put that metadata, so it either doesn't exist or gets tracked out-of-band.
- **Every query/repository touching that column needs bespoke logic** — because the column's meaning (plaintext or cipher) isn't guaranteed by the type system, every caller has to know the current state by convention.
- **A hand-rolled `save()`/`load()` pair built around that one column has its own, distinct problems.** Covered separately below, because they're really about mutating a shared object in place, not about which representation a column holds at a given moment.

The transient/encrypted-blob split is what makes the first four of those non-issues: the field's role tells you what it holds, and one consistent boundary, not scattered application code, owns the only place the conversion happens.

### The naive save()/load() pattern's own problems

A typical hand-rolled `save()` encrypts the column in place, persists, then decrypts it back:

```java
--8<-- "naive-save-load/src/main/java/ie/bitstep/mango/workshop/talk/naivesaveload/directfield/DirectFieldStore.java:direct-field-save"
```

This has a problem the transient/blob split actually fixes:

- **A concurrency window inside `save()`.** Between the encrypt step and the decrypt step, the *same* entity instance, not a copy, sits there with ciphertext in a field the rest of the application expects to hold plaintext. In a single-threaded flow that window is invisible and harmless. In a multi-threaded application where that entity can be read concurrently (shared across requests, held in a cache, referenced from another thread) any read that lands inside the window silently sees ciphertext where it expects plaintext. There's no lock, no exception, no signal that the object is mid-mutation; it just quietly hands out the wrong representation to whoever happens to read it at the wrong moment.

Compare that to `save()` built on the transient/blob split:

```java
--8<-- "naive-save-load/src/main/java/ie/bitstep/mango/workshop/talk/naivesaveload/transientblob/TransientBlobStore.java:transient-blob-save"
```

It reads the transient field and writes the blob field, and never touches the transient field at all, so there is no window where the working representation is anything other than plaintext. A concurrent reader sees a consistent value throughout. That's the concurrency problem genuinely solved, not just made harder to trigger.

The other naive `save()`/`load()` problem isn't fixed by the split at all. Here's the naive `load()`:

```java
--8<-- "naive-save-load/src/main/java/ie/bitstep/mango/workshop/talk/naivesaveload/directfield/DirectFieldStore.java:direct-field-load"
```

And here's the transient/blob version, doing the same thing:

```java
--8<-- "naive-save-load/src/main/java/ie/bitstep/mango/workshop/talk/naivesaveload/transientblob/TransientBlobStore.java:transient-blob-load"
```

- **`load()`'s decrypt is forced, and a forced decrypt can silently discard in-memory work, in both versions.** `load()`'s job is "fetch from the DB, then decrypt", but if it runs against an entity instance that already has unsaved application changes (re-loading an entity that's still being edited, refreshing a cached instance, a second `load()` call for the same id mid-request) the freshly-decrypted DB values overwrite whatever was already there. There's no conflict check, no diff, no exception; the caller just loses whatever hadn't been saved yet, with no warning, and finds out only if they happen to notice the value changed. `decrypt()` writes into the transient field regardless of what was there before, whether it's mango4j-crypto's `decrypt()` or a hand-rolled one, so having a separate transient field doesn't change this outcome at all. Avoiding it is a matter of *when* the application chooses to call `decrypt()`/`load()` at all, never blindly, against an entity that might carry pending changes, not something the ciphertext representation can enforce for you.

See [`talk/naive-save-load/`](https://github.com/bitstep-ie/mango4j-crypto-workshop/tree/main/talk/naive-save-load) for the full runnable demo of both variants, including a real-thread reproduction of the concurrency window.
