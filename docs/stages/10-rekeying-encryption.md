Rekeying: Encryption

!!! abstract "Overview"
    [Key Rotation](09-key-rotation.md) switched new writes to a new key but left existing data exactly where it was: still encrypted under the old one. This stage is phase 2 - sweeping already-persisted records, decrypting each under whichever key it was actually written with and re-encrypting it under the current one. Facilitators should pair this with [Rekeying: Encryption](../talk/rekeying-encryption.md), and be explicit that this stage covers ciphertext only: HMACs get rekeyed separately, next, because they're a distinct operation with a different retirement rule for the old key.

This stage comes as two projects:

- **`starter/`** - what you work in. It compiles and runs, and the sweep correctly rekeys every record on its first pass - but running it a second time rekeys them all over again instead of doing nothing. Look for the `// TODO` comment.
- **`complete/`** - the finished reference, where a second sweep is a safe no-op.

!!! tip "Follow along"
    ```bash
    cd stages/10-Rekeying-Encryption/starter
    ```
    Using an IDE instead? Open `stages/10-Rekeying-Encryption/starter` as its own project.

## The sweep itself

```java
--8<-- "10-Rekeying-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/RekeySweep.java:already-on-key"
```

The actual rekey - `decrypt()` then `encrypt()` again - is only two lines, given in full in `RekeySweep.run()`: nothing new, the exact same calls every previous stage has already used. What's new is deciding *whether* to bother: without a check, re-running the sweep after it's already finished would decrypt and re-encrypt every record all over again, for no reason.

**Your turn:** in `starter/.../RekeySweep.java`, make `isAlreadyOnKey()` actually check whether `record`'s `encryptedData` already carries `keyId`.

## Writing under the old key, rotating, then sweeping

```java
--8<-- "10-Rekeying-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:write-then-rotate"
```

Three records, written before the rotation - exactly `beforeRotation`'s situation from [Key Rotation](09-key-rotation.md), just three of them in a store instead of one loose variable.

```java
--8<-- "10-Rekeying-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:first-sweep"
```

The first sweep has to do real work: none of the three records are on the current key yet, so all three get decrypted and re-encrypted.

```java
--8<-- "10-Rekeying-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:second-sweep"
```

The second sweep, run immediately after with nothing else having changed, should find nothing left to do.

## Running it

`starter/` before your change: the first sweep works correctly, but the second one doesn't know it's already finished:

```
records rekeyed on first sweep:  3
  {"cryptoKeyId":"workshop-encryption-key-v2",...}
  {"cryptoKeyId":"workshop-encryption-key-v2",...}
  {"cryptoKeyId":"workshop-encryption-key-v2",...}
records rekeyed on second sweep: 3
```

After your change:

```
records rekeyed on first sweep:  3
  {"cryptoKeyId":"workshop-encryption-key-v2",...}
  {"cryptoKeyId":"workshop-encryption-key-v2",...}
  {"cryptoKeyId":"workshop-encryption-key-v2",...}
records rekeyed on second sweep: 0
```

Only the last line changes. That's the point: correctness (every record ends up on the new key) was never in question, even in `starter/`. What `isAlreadyOnKey()` buys you is being able to run the sweep on a schedule, or resume it after an interruption, without redoing - or re-paying for - work that's already done. The old key still has to stay resolvable via `getById()` for as long as any record in the store hasn't been swept yet; nothing in this stage removes it.
