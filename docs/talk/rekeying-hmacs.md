Rekeying: HMACs

## Why HMACs need their own rekey process

[Rekeying: Encryption](rekeying-encryption.md) covered re-encrypting a record's ciphertext from an old key to a new one. HMACs need the same eventual outcome — get everything off old keys — but the process looks different, because a HMAC isn't just replaced in place. Under the [List HMAC Strategy](list-hmac.md), a record can legitimately have HMACs from *multiple* keys stored simultaneously, and the rekey job has to *add* new entries without disturbing old ones that other in-flight operations might still depend on.

This additive requirement is why HMAC rekeying, in practice, is only well-supported for entities using the List HMAC Strategy, unlike encryption-only rekeying ([Rekeying: Encryption](rekeying-encryption.md)), which works regardless of HMAC strategy.

## The process

1. **Add the new HMAC key** to the active key list — this is rotation phase 1 from [Key Rotation](key-rotation.md), nothing new here.
2. **Wait for the key cache expiry** to pass, so every application instance is actually using the new key for writes (the same caching hazard [Single HMAC Strategy](single-hmac.md) described for the multi-instance search race).
3. **Kick off the rekey job.** It selects records by one of two criteria, depending on why you're rekeying:
    - Any record *missing* HMACs for the new key — the common case for a full sweep after adding a key.
    - Any record that *has* HMACs for a key you're trying to remove but *lacks* them for the new key — the common case for a targeted, passive rotation.
4. **For each matching record**: decrypt it, compute HMAC(s) under the new key, and *add* those entries to the lookup/unique-value collections, leaving existing entries for other keys untouched.
5. **Wait for the sweep to finish**, then remove the old key from the active key list.

    This step is where getting the ordering wrong bites. Removing a key is a one-liner:

    ```java
    --8<-- "naive-hmac-rekey/src/main/java/ie/bitstep/mango/workshop/talk/naivehmacrekey/HmacRekeyStore.java:remove-active-key"
    ```
    <!-- link -->

    and search only ever hashes with whatever's currently in the active list:

    ```java
    --8<-- "naive-hmac-rekey/src/main/java/ie/bitstep/mango/workshop/talk/naivehmacrekey/HmacRekeyStore.java:hmac-rekey-search"
    ```
    <!-- link -->

    Nothing in either method checks whether the sweep has finished. Remove the key while even one record is still unswept, and its only lookup entry becomes permanently untried, silently dropping it from every future search, with no error anywhere. See [`talk/naive-hmac-rekey/`](https://github.com/bitstep-ie/mango4j-crypto-workshop/tree/main/talk/naive-hmac-rekey) for the full runnable demo, comparing both orderings.

    This wait is also an ongoing cost, not a one-time rule: the old key must stay valid and loaded on every instance ([the same caching hazard step 2 and Single HMAC Strategy described](single-hmac.md), now for *keeping* a key rather than *picking up* one) for as long as the sweep takes, and for a large database that sweep can be the biggest task in the whole rotation.
6. **Wait for cache expiry again**, so no instance is still generating HMACs — or worse, still trying to *validate* against — the old key.
7. **Clean up**: remove the now-orphaned old-key HMAC entries from the lookup/unique-value tables. Leaving them doesn't break anything functionally, but stale HMAC material sitting in your database is itself a security exposure worth avoiding, especially for long-lived data.
8. **Only now** delete the old HMAC key from wherever key material is stored.

## Why this is split from the encryption rekey

Encryption rekeying ([Rekeying: Encryption](rekeying-encryption.md)) is a straight swap: decrypt with the old key, re-encrypt with the new one, done. HMAC rekeying can't be, because HMACs stay load-bearing for search and uniqueness *while the rotation is still in progress*, and replacing one in place, even briefly, would reopen the search-outage and duplicate-record problems from [Key Rotation](key-rotation.md) and [Single HMAC Strategy](single-hmac.md). Keeping the two rekeys distinct, each addressing its own failure mode, is what lets them run independently and safely overlap in time.

## How this interacts with the List HMAC Strategy

This is the mechanism that makes the [List HMAC Strategy](list-hmac.md) straightforward to rekey with no impact to application functionality. Because every active key's HMAC coexists in the same lookup/unique-value collections, adding a new key's entries and later removing an old key's entries are both just additive/subtractive operations on those collections — never a moment where a record is only findable under one key or the other. It's also the answer to the "list only ever grows" cost flagged at the end of [List HMAC Strategy](list-hmac.md): rekeying is what shrinks it back down once a rotation completes.

---

## Closing: what "doing it right" actually requires

Two things cover almost everything this talk has walked through:

- **Decoupling application code from cryptographic providers.** Every mechanism covered (structured ciphertext, the key alias indirection, pluggable providers) exists so a provider change, rotation, or rekey never touches business logic; HMAC strategies and rekey processes are just what it takes to keep search and uniqueness correct while that happens.
- **Understanding the unencrypted → encrypted migration path *up front***, before you need it. [Key Rotation](key-rotation.md) covered the cost of addressing this after the fact: dual-read/dual-write periods, unplanned pauses on other work. Deciding early, with tracked, temporary guardrails from day one, is generally cheaper than retrofitting it later under time pressure.

## What we're building today

The rest of this session moves from theory to hands-on practice: the workshop's stages build up from a plain project with no encryption to full encryption with multiple keys and providers, HMAC search and unique constraints, and key rotation with both re-encryption and rehashing. Each stage maps back to a concept from this talk, roughly in the order covered here.
