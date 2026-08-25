# 9. Rekeying: HMACs

## Why HMACs need their own rekey process

Chapter 8 covered re-encrypting a record's ciphertext from an old key to a new one. HMACs need the same eventual outcome — get everything off old keys — but the process looks different, because a HMAC isn't just replaced in place. Under the List HMAC Strategy (Chapter 7), a record can legitimately have HMACs from *multiple* keys stored simultaneously, and the rekey job has to *add* new entries without disturbing old ones that other in-flight operations might still depend on.

**mango4j-crypto's built-in rekey job currently only supports HMAC rekeying for entities using the List HMAC Strategy.** Encryption-only rekeying (Chapter 8) works for any entity; HMAC rekeying specifically needs the list shape to do this safely.

## The process

1. **Add the new HMAC key** to the active key list — this is rotation phase 1 from Chapter 5, nothing new here.
2. **Wait for the key cache expiry** to pass, so every application instance is actually using the new key for writes (the same caching hazard Chapter 6 described for the multi-instance search race).
3. **Kick off the rekey job.** It selects records by one of two criteria, depending on why you're rekeying:
    - Any record *missing* HMACs for the new key — the common case for a full sweep after adding a key.
    - Any record that *has* HMACs for a key you're trying to remove but *lacks* them for the new key — the common case for a targeted, passive rotation.
4. **For each matching record**: decrypt it, compute HMAC(s) under the new key, and *add* those entries to the lookup/unique-value collections. Existing HMAC entries for other keys are left completely untouched — this is strictly additive, never a replace.
5. **Wait for the sweep to finish**, then remove the old key from the active key list.
6. **Wait for cache expiry again**, so no instance is still generating HMACs — or worse, still trying to *validate* against — the old key.
7. **Clean up**: remove the now-orphaned old-key HMAC entries from the lookup/unique-value tables. Leaving them doesn't break anything functionally, but stale HMAC material sitting in your database is itself a security exposure worth avoiding, especially for long-lived data.
8. **Only now** delete the old HMAC key from wherever key material is stored.

## Why this is split from the encryption rekey

Encryption rekeying (Chapter 8) is a straight swap: decrypt with the old key, re-encrypt with the new one, done. HMAC rekeying can't be a swap, because — unlike ciphertext, which only ever needs to satisfy "decrypts correctly under its recorded key" — HMACs are load-bearing for search and uniqueness *while the rotation is still in progress*. Replacing a HMAC in place, even briefly, would reopen exactly the search-outage and duplicate-record windows Chapters 5–6 spent so much time on. Keeping the encryption and HMAC rekeys as two distinct steps, each addressing a different failure mode, is what lets them run independently and safely overlap in time.

## How this interacts with the List HMAC Strategy

This is precisely the process Chapter 7 gestured at when it said the List HMAC Strategy is "the easiest strategy for supporting rekeying with no impact to application functionality." Because every active key's HMAC coexists in the same lookup/unique-value collections, adding a new key's entries and later removing an old key's entries are both just additive/subtractive operations on those collections — never a moment where a record is only findable under one key or the other. It's also the answer to the "list only ever grows" cost flagged at the end of Chapter 7: rekeying is what actually shrinks it back down once a rotation completes.

---

## Closing: what "doing it right" actually requires

Two things, in the end, cover almost everything this talk has walked through:

- **Decoupling application code from cryptographic providers.** Every mechanism covered — structured ciphertext, the key alias indirection, pluggable delegates — exists so that a provider change, a key rotation, or a rekey never requires touching business logic. That decoupling is the actual deliverable; HMAC strategies and rekey jobs are just what it takes to keep search and uniqueness correct while it happens.
- **Understanding the unencrypted → encrypted migration path *up front***, before you need it. Chapter 5 covered why bolting this on after the fact is painful — dual-read/dual-write periods, unannounced feature freezes, the works. Deciding early how a field moves from plaintext to encrypted, and building `@EnableMigrationSupport`-style guardrails into your process from day one, is far cheaper than retrofitting it under pressure later.

## What we're building today

The rest of this session moves from theory to hands-on practice, working through the numbered stages in `stages/` in this workshop's repository — starting from a plain project with the `mango4j-crypto` dependency added, and building up field encryption, HMAC search, and (as stages are added) key rotation and rekeying, one exercise at a time. Each stage maps back to a concept from this talk: expect to recognize `@Encrypt`/`@EncryptedData` from Chapters 3–4 first, HMAC strategies from Chapters 6–7 next, and rotation/rekeying from Chapters 5, 8–9 as the workshop progresses.
