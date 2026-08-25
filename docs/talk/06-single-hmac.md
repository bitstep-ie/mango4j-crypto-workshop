# 6. Introducing HMACs: The Single HMAC Strategy

## Why you can't search or constrain on the ciphertext itself

Chapter 1 introduced the IV: randomness fed into every encryption operation so encrypting the same value twice never produces the same ciphertext. That's essential for security, but it has a direct, unavoidable consequence — you can never find a record by encrypting a search term and comparing ciphertext, because the ciphertext is different every single time, even for the same input.

**HMAC** fills that gap. Unlike encryption, an HMAC (a hash computed using a secret key) is *deterministic*: the same input with the same key always produces the same output. That determinism is exactly what makes HMACs — not encrypted values — the thing you actually search and enforce uniqueness on.

## The Single HMAC Strategy: one column per HMAC

The simplest possible design: one HMAC column per field, holding a single HMAC value, sitting alongside the encrypted record — a `USERNAME_HMAC` column next to the `userName` ciphertext, a `PAN_HMAC` column next to the `pan` ciphertext, and typically a column recording which HMAC key produced them, which rekeying (Chapters 8–9) needs later to find what's stale.

It's the design many applications default to — simple, relational-DB-friendly, no join required — but it inherits both of the HMAC key rotation challenges head-on.

## The search problem

Rotate the HMAC key and every existing record's HMAC was computed with the *old* key. A search hashes the term with the *new* key and gets a different value — the row simply isn't found, even though it exists. Play through the sequence:

1. You change the HMAC key.
2. All searches going forward use the new key.
3. Immediately, none of your existing records are findable — their HMACs were all computed with the old key.
4. A background job starts rekeying record by record.
5. Search results gradually improve as the job progresses.
6. Only once the job finishes is search fully back online.

A single HMAC key per tenant means a rotation causes a functional search outage for however long the rekey takes. Most production systems can't tolerate this, and it's unavoidable with only one active key.

A partial fix exists: a **key start time**. When a new key is introduced, its start time is set to "now + the key cache duration," and writes never use it before that time — but *searches* immediately start hashing with every known key, old and new. Since nothing writes with the new key until every application instance has it cached, every record — old or freshly written — stays findable throughout.

## The unique constraint problem

This is the more consequential of the two. Say `userName` has a DB-level unique constraint on its HMAC column. Walk through it:

1. A user exists with username `john.doe@test.com`, HMAC'd under the old key.
2. The HMAC key changes.
3. A request comes in to create a *new* user with the same username, `john.doe@test.com`.
4. Its HMAC is computed under the new key — a different value than the existing record's HMAC.
5. The unique constraint doesn't fire, because the two HMAC values genuinely differ.
6. Two users now exist with the same username.

Adding "key start time" narrows this to a race condition rather than eliminating it, and only by paying for a search-before-every-write — itself a performance cost, and it makes the DB's own unique constraint largely redundant for the cases it's supposed to catch. Even that doesn't fully close the gap: a request arriving right at the key-start-time boundary can still slip through a race between two concurrent writes on different keys, producing the same duplicate outcome.

## Verdict

| | |
|---|---|
| **Pros** | Simplest possible design; relational-DB-friendly single-table layout; no extra write-path cost (one HMAC per attribute per write); little room for process error during a rotation |
| **Cons** | Cannot support unique constraint enforcement *and* key rotation without serious drawbacks; without key start time, rotation causes intermittent search outages; even with it, unique constraint support costs performance and still can't guarantee integrity under all circumstances |

If your application needs uniqueness enforced on any encrypted field, the Single HMAC Strategy is a design you'll eventually have to walk away from. Chapter 7 covers the strategy that actually solves both problems.
