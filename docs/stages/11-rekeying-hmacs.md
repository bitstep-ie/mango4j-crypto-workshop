Rekeying: HMACs

!!! abstract "Overview"
    The final stage of the rotation story: regenerating HMACs under a new key and safely retiring the old one, using mango4j-crypto's real `RekeyScheduler` again - the HMAC side of what [Rekeying: Encryption](10-rekeying-encryption.md) already introduced for ciphertext. Facilitators should lead with [Rekeying: HMACs](../talk/rekeying-hmacs.md)'s teardown ordering rule, and note that this stage's two-phase shape (bring the new key on, *then* retire the old one, as two separate scheduler-driven passes) is the real operational sequence, not a workshop simplification.

This stage comes as two projects:

- **`starter/`** - what you work in. It compiles and runs, but neither key ever gets its `rekeyMode` set, so both scheduler passes find nothing to do and time out. Look for the `// TODO` comments.
- **`complete/`** - the finished reference, where both phases actually run.

!!! tip "Follow along"
    ```bash
    cd stages/11-Rekeying-HMACs/starter
    ```
    Using an IDE instead? Open `stages/11-Rekeying-HMACs/starter` as its own project.

## Phase one: bring the new key on

```java
--8<-- "11-Rekeying-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:bring-new-key-on"
```

Marking the new HMAC key `KEY_ON` tells `RekeyScheduler` to sweep every record onto it - additive, alongside whatever HMAC entries already exist, exactly [List HMAC Strategy](08-list-hmac-strategy.md)'s semantics, just driven by the scheduler instead of a hand-written loop.

**Your turn:** in `starter/.../Main.java`, mark `"workshop-hmac-key-v2"` `KEY_ON`.

```java
--8<-- "11-Rekeying-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:phase-one-report"
```

## Phase two: retire the old key

```java
--8<-- "11-Rekeying-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:retire-old-key"
```

Two things happen here, not one: `workshop-hmac-key-v2`'s `rekeyMode` gets cleared (it's not "being brought on" anymore, it's just the ordinary current key now), and `workshop-hmac-key` gets marked `KEY_OFF`. Only after that does `RekeyScheduler` retire it - the same "don't remove the old key until the sweep genuinely reaches everything" rule the earlier hand-rolled version of this stage enforced manually, now the framework's job.

**Your turn:** in `starter/.../Main.java`, clear `"workshop-hmac-key-v2"`'s `rekeyMode` and mark `"workshop-hmac-key"` `KEY_OFF`.

```java
--8<-- "11-Rekeying-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:phase-two-report"
```

## What doesn't happen

Running `complete/` shows something worth calling out explicitly: after phase two, every record's `lookups` list still has an entry for `workshop-hmac-key` - the key that was just retired:

```
lookups: [workshop-hmac-key-v2, workshop-hmac-key]
```

Retiring a key removes it from the `CryptoKeyProvider` (nothing can resolve it by id anymore), but it does **not** reach into every record and strip its now-stale entry. mango4j-crypto has a separate event for that - `RekeyEvent.Type.PURGE_REDUNDANT_HMACS_ASSOCIATED_WITH_KEY` - a distinct cleanup step this stage doesn't cover. The stale entry is harmless (nothing can ever compute a matching HMAC under a deleted key again, so it can never produce a false match), just not free of the storage it occupies until something purges it.

```java
--8<-- "11-Rekeying-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:search-after-rotation"
```

Search still works throughout, exactly the way [List HMAC Strategy](08-list-hmac-strategy.md) demonstrated: a probe built against whatever's current finds every record, before and after the rotation.

## Running it

`starter/` before your changes: neither key ever gets a `rekeyMode`, so both scheduler passes log "no re-keying needed" and do nothing. Both waits time out after 10 seconds each (about 20 seconds total):

```
new key brought on within timeout? false
  lookups: [workshop-hmac-key]
  lookups: [workshop-hmac-key]
  lookups: [workshop-hmac-key]
old key retired within timeout?    false
  lookups: [workshop-hmac-key]
  lookups: [workshop-hmac-key]
  lookups: [workshop-hmac-key]
search still finds the record?     false
```

The last line fails too: the search probe is built against `workshop-hmac-key-v2` (the "current" this stage's shield always uses), which nothing in the store has yet.

After all your changes:

```
new key brought on within timeout? true
  lookups: [workshop-hmac-key-v2, workshop-hmac-key]
  lookups: [workshop-hmac-key-v2, workshop-hmac-key]
  lookups: [workshop-hmac-key-v2, workshop-hmac-key]
old key retired within timeout?    true
  lookups: [workshop-hmac-key-v2, workshop-hmac-key]
  lookups: [workshop-hmac-key-v2, workshop-hmac-key]
  lookups: [workshop-hmac-key-v2, workshop-hmac-key]
search still finds the record?     true
```

That's the entire rotation story end to end, using the actual framework mechanism throughout: [Key Rotation](09-key-rotation.md) pointed new writes at a new key while old data kept working under the old one; [Rekeying: Encryption](10-rekeying-encryption.md) used `RekeyScheduler` to sweep existing ciphertext onto the new key; this stage used the same scheduler, via `RekeyListHmacFieldStrategy`, to sweep existing HMACs the same way - with the one added constraint neither of the others had: the old key can't be retired until the sweep genuinely reaches everything, or exactly the records still waiting become invisible to search.
