Rekeying: HMACs

!!! abstract "Overview"
    The final stage of the rotation story: regenerating HMACs under a new key and safely retiring the old one. Facilitators should lead with [Rekeying: HMACs](../talk/rekeying-hmacs.md)'s teardown ordering rule: removing the old HMAC key from the active list before every record has been swept makes unswept records silently unfindable in search - exactly what this stage's `starter/` reproduces, and what `talk/naive-hmac-rekey/` demonstrates in a hand-rolled implementation.

This stage comes as two projects:

- **`starter/`** - what you work in. It compiles and runs, but the old HMAC key is retired before the sweep reaches every record, so the last record becomes unfindable. Look for the `// TODO` comment.
- **`complete/`** - the finished reference, where the sweep finishes first and nothing is lost.

!!! tip "Follow along"
    ```bash
    cd stages/11-Rekeying-HMACs/starter
    ```
    Using an IDE instead? Open `stages/11-Rekeying-HMACs/starter` as its own project.

## Refreshing HMACs is the same call as everything else

```java
--8<-- "11-Rekeying-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/RekeyHmacSweep.java"
```

mango4j-crypto doesn't expose an "HMACs only" path - `encrypt()` is `encrypt()`, the same call every previous stage has used. Calling it again on an already-encrypted record also re-encrypts `cardNumber`, which is harmless here since this stage doesn't rotate the encryption key (fresh IV, same key, same plaintext), but it's worth knowing: Rekeying: Encryption and Rekeying: HMACs being conceptually separate doesn't mean calling one never touches what the other owns.

## Adding the new key, then a sweep that doesn't finish

```java
--8<-- "11-Rekeying-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:write-then-add-key"
```

```java
--8<-- "11-Rekeying-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:partial-sweep"
```

Three records, written while only the old key was active. The new key is added alongside it - so far, identical to [List HMAC Strategy](08-list-hmac-strategy.md)'s setup. Then the sweep runs, but only over the first two records: a stand-in for a rekey job that got interrupted, ran out of time, or simply hasn't gotten to everything yet.

**Your turn:** in `starter/.../Main.java`, finish the sweep - rekey the remaining record(s) too, before moving on.

```java
--8<-- "11-Rekeying-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:retire-old-key"
```

This line assumes the sweep is completely finished. It's given, unconditional, in both `starter/` and `complete/` - the bug isn't in this line, it's in whether the sweep actually finished before it ran.

## Running it

```java
--8<-- "11-Rekeying-HMACs/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:search-after-retirement"
```

`starter/` before your change - the third record's `lookups` only ever had an entry for the old key, which is now gone from the active list. A search probe, computed only under the remaining (new) key, has nothing to match:

```
previously-unswept record still findable? false
```

After your change - the third record picked up a new-key entry during the (now complete) sweep, before the old key was retired:

```
previously-unswept record still findable? true
```

That's the entire rotation story end to end: [Key Rotation](09-key-rotation.md) pointed new writes at a new key while old data kept working under the old one; [Rekeying: Encryption](10-rekeying-encryption.md) swept existing ciphertext onto the new key, safely re-runnable; this stage swept existing HMACs the same way, with one added constraint neither of the others had - the old key can't be retired until the sweep genuinely reaches everything, or exactly the records still waiting become invisible to search.
