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
| `encrypt()` | Naive entity | No | The first call turns the field into ciphertext; a second call re-encrypts that ciphertext as if it were plaintext, corrupting the value |
| `decrypt()` | Naive entity | No | Throws rather than losing data silently, decrypting a field that's already plaintext (or holds a pending edit) fails outright, since it's no longer valid ciphertext |

This is the same forced-overwrite risk covered below for `load()`, just stated for `decrypt()` directly: `load()` is unsafe to call blindly because it calls `decrypt()`, not because of anything specific to loading.

## What happens without this discipline: encrypting fields directly into their columns

The failure mode this design specifically guards against is the naive alternative: a single entity that is both the domain object and the thing an ORM maps straight to columns, with no separate transient/blob distinction at all. Something like a `cardNumber` field, an `iv` sibling column, and a `cardNumberHmac` sibling column for search or uniqueness (the same "bolted on ad hoc" shape [Structured Ciphertext](structured-ciphertext.md) opens with) but critically, no separate field for the ciphertext: `cardNumber` itself is hand-rolled to hold plaintext most of the time and ciphertext for whichever part of the code just encrypted it. This tends to produce:

- **No single source of truth for "what's encrypted"** — you have to go read the code (or worse, ask around) to know whether a given column currently holds plaintext or ciphertext.
- **Schema churn every time a new field needs protecting** — every field that becomes confidential needs its own bespoke encrypt/decrypt wiring, instead of following one consistent pattern.
- **No consistent record of which key encrypted what** — without a [structured ciphertext](structured-ciphertext.md), there's nowhere obvious to put that metadata, so it either doesn't exist or gets tracked out-of-band.
- **Every query/repository touching that column needs bespoke logic** — because the column's meaning (plaintext or cipher) isn't guaranteed by the type system, every caller has to know the current state by convention.
- **A hand-rolled `save()`/`load()` pair built around that one column has its own, distinct problems.** Covered separately below, because they're really about mutating a shared object in place, not about which representation a column holds at a given moment.

The transient/encrypted-blob split is what makes the first four of those non-issues: the field's role tells you what it holds, and one consistent boundary, not scattered application code, owns the only place the conversion happens.

### The naive save()/load() pattern's own problems

A typical hand-rolled `save()` encrypts `cardNumber` in place, persists the entity (`iv` and `cardNumberHmac` travel along as ordinary sibling columns), then restores `cardNumber` for the caller:

```java
--8<-- "naive-save-load/src/main/java/ie/bitstep/mango/workshop/talk/naivesaveload/directfield/NaiveCardStore.java:naive-card-save"
```

That restore reassigns the plaintext `save()` already captured before encrypting, it doesn't decrypt the ciphertext it just produced. There's no reason to: the plaintext is still sitting right there in a local variable, so decrypting it back would just be `encrypt()`'s wasted work from earlier in this page, paying for a cryptographic operation to recover a value already in hand.

This has a problem the transient/blob split actually fixes:

- **A concurrency window inside `save()`.** Between the encrypt step and the decrypt step, the *same* entity instance, not a copy, sits there with ciphertext in a field the rest of the application expects to hold plaintext. In a single-threaded flow that window is invisible and harmless. In a multi-threaded application where that entity can be read concurrently (shared across requests, held in a cache, referenced from another thread) any read that lands inside the window silently sees ciphertext where it expects plaintext. There's no lock, no exception, no signal that the object is mid-mutation; it just quietly hands out the wrong representation to whoever happens to read it at the wrong moment.

Compare that to `save()` built on the transient/blob split:

```java
--8<-- "naive-save-load/src/main/java/ie/bitstep/mango/workshop/talk/naivesaveload/transientblob/TransientBlobStore.java:transient-blob-save"
```

It reads the transient field and writes the blob field, and never touches the transient field at all, so there is no window where the working representation is anything other than plaintext. A concurrent reader sees a consistent value throughout. That's the concurrency problem genuinely solved, not just made harder to trigger.

The other naive `save()`/`load()` problem isn't fixed by the split, it just changes shape. Here's the naive `load()`, on the same single entity `save()` just used:

```java
--8<-- "naive-save-load/src/main/java/ie/bitstep/mango/workshop/talk/naivesaveload/directfield/NaiveCardStore.java:naive-card-load"
```

`save()` already decrypted `cardNumber` back for the caller as its last step. This doesn't need multiple threads, a shared cache, or a session boundary to go wrong, a single method, on one thread, calling `save(entity)` and then, a few lines later in the same call, `load(entity.id(), key)` again (maybe defensively, maybe out of habit) is already a double decrypt: `cardNumber` no longer holds valid ciphertext, so the decrypt inside `load()` throws outright. It fails loudly rather than losing anything.

Here's the transient/blob version of `load()`, doing the equivalent fetch-then-decrypt:

```java
--8<-- "naive-save-load/src/main/java/ie/bitstep/mango/workshop/talk/naivesaveload/transientblob/TransientBlobStore.java:transient-blob-load"
```

- **`load()`'s decrypt is forced in both versions, but the failure looks different.** `load()`'s job is "fetch, then decrypt", but it has no way to check whether the field it's about to overwrite already holds something the caller needs, whether that's a pending edit or a value `save()` already restored a moment earlier in the exact same call path. In the naive entity, `cardNumber` is the same field decrypt() is about to overwrite, so if it isn't currently valid ciphertext, the decrypt call throws and the caller finds out immediately. In the transient/blob version, `decrypt()` reads the separate blob field (still perfectly valid ciphertext, unaffected by anything happening on the transient field) and unconditionally overwrites the transient field with whatever that decodes to, so it never throws, it just silently discards whatever was on the transient field, with no warning. Neither version is safe to call blindly; one just fails loudly and the other doesn't. Avoiding it is a matter of *when* the application chooses to call `decrypt()`/`load()` at all, not something either representation can enforce for you.

See [`talk/naive-save-load/`](https://github.com/bitstep-ie/mango4j-crypto-workshop/tree/main/talk/naive-save-load) for the full runnable demo of both variants, including a real-thread reproduction of the concurrency window.
