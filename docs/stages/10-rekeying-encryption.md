Rekeying: Encryption

!!! abstract "Overview"
    [Key Rotation](09-key-rotation.md) switched new writes to a new key but left existing data exactly where it was: still encrypted under the old one. This stage is phase 2 - sweeping already-persisted records onto the new key - and it uses mango4j-crypto's own production `RekeyScheduler`, not a hand-rolled loop. Facilitators should pair this with [Rekeying: Encryption](../talk/rekeying-encryption.md), and set expectations up front: this is a heavier API than anything earlier in the workshop - a background scheduled job, asynchronous completion, and a forced `System.exit()` - because that's genuinely what the production mechanism looks like, not a workshop simplification.

This stage comes as two projects:

- **`starter/`** - what you work in. It compiles and runs, but the old key is never marked for retirement, so `RekeyScheduler` finds nothing to do and the wait just times out after 10 seconds. Look for the `// TODO` comment.
- **`complete/`** - the finished reference, where the scheduler actually sweeps all three records.

!!! tip "Follow along"
    ```bash
    cd stages/10-Rekeying-Encryption/starter
    ```
    Using an IDE instead? Open `stages/10-Rekeying-Encryption/starter` as its own project.

## Triggering a rekey: mark the old key `KEY_OFF`

```java
--8<-- "10-Rekeying-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/InMemoryCryptoKeyProvider.java:markForRetirement"
```

`CryptoKey` has a `rekeyMode` field for exactly this (`KEY_ON`/`KEY_OFF`, see the framework's `CryptoKey` javadoc). Marking a key `KEY_OFF` tells `RekeyScheduler` "sweep everything using this key onto whichever key is current, then tell me it's safe to delete."

**Your turn:** in `starter/.../Main.java`, mark `"workshop-encryption-key"` for retirement: `provider.markForRetirement("workshop-encryption-key")`.

!!! warning "`KEY_ON` is currently broken in mango4j-crypto"
    `RekeyScheduler` also supports the mirror image - marking a *new* key `KEY_ON` to pull every record onto it, regardless of which old key each one is currently on. While building this stage we found that path has an inverted condition and silently rekeys nothing for any entity that actually has `@Encrypt` fields (filed as [mango4j-crypto#44](https://github.com/bitstep-ie/mango4j-crypto/issues/44)). `KEY_OFF` - what this stage uses - doesn't have the same bug; it was verified working end to end while diagnosing the `KEY_ON` issue.

## Wiring up a `RekeyService`

```java
--8<-- "10-Rekeying-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/PaymentCardRekeyService.java"
```

`RekeyScheduler` doesn't know how to query your store - `RekeyService<T>` is the interface it calls to find records and save them back. `findRecordsUsingCryptoKey()` is called repeatedly, in batches, until it returns empty, so it has to check each record's *current* state every time, not a snapshot taken once. `notify()` is the only way to know the job finished - there's no return value to wait on, since the scheduler runs asynchronously.

## Wiring up key deletion

```java
--8<-- "10-Rekeying-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/RetiringKeyManager.java"
```

Once every record using the retired key has been swept, `RekeyScheduler` calls `RekeyCryptoKeyManager.markKeyForDeletion()` on its own - the manual "has the sweep reached everything yet?" bookkeeping earlier versions of this stage did by hand is now the framework's job.

## Starting the scheduler

```java
--8<-- "10-Rekeying-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:start-scheduler"
```

`RekeySchedulerConfig` is a real production configuration surface - cache duration (how long application instances might still be caching old key data, to avoid rekeying onto a key some instances don't know about yet), batch interval, failure tolerance, and the poll interval itself. This stage sets a 1-second poll interval and zero cache duration purely to make the demo finish fast; a real deployment would set these to match its own caching and traffic patterns.

## Waiting for an asynchronous job to finish

```java
--8<-- "10-Rekeying-Encryption/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:wait-and-report"
```

Every previous stage's "sweep" was a synchronous method call: `RekeySweep.run(...)` returned once it was done. `RekeyScheduler` doesn't work that way - it's a background job polling on its own schedule, so `Main` has no method call to block on. `PaymentCardRekeyService.notify()` and `RetiringKeyManager.markKeyForDeletion()` are the only signals available, so this stage uses two `CountDownLatch`es to turn "the scheduler will eventually tell us, asynchronously" into something a single-shot `Main` can wait on with a timeout.

`RekeyScheduler` also has no `shutdown()`/`close()` method - its background thread pool is non-daemon and polls forever once started, so nothing in `Main` returning normally would actually end the process. The `System.exit(0)` at the very end of `main()` is there on purpose, not a shortcut.

## Running it

`starter/` before your change: nothing is marked for retirement, so every scheduler cycle logs "No re-keying needed" and does nothing. Both waits time out after 10 seconds each:

```
rekey finished within timeout?  false
old key retired within timeout? false
  {"cryptoKeyId":"workshop-encryption-key",...}
  {"cryptoKeyId":"workshop-encryption-key",...}
  {"cryptoKeyId":"workshop-encryption-key",...}
```

After your change, the scheduler finds the marked key on its very first cycle:

```
rekey finished within timeout?  true
old key retired within timeout? true
  {"cryptoKeyId":"workshop-encryption-key-v2",...}
  {"cryptoKeyId":"workshop-encryption-key-v2",...}
  {"cryptoKeyId":"workshop-encryption-key-v2",...}
```

You'll also see `RekeyScheduler`'s own log lines above those - "Re-key complete", "All records (3) using deprecated encryption key ... have been keyed onto the current encryption key ...", and "Notifying the application to mark the following Crypto key as deleted". A recurring "0 HMAC keys were found ... skipping" line is expected noise: the scheduler always checks for HMAC rekey work too, on every cycle, whether or not this stage's entity uses any.
