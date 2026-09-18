Single HMAC Strategy

!!! abstract "Overview"
    With an HMAC field in place, this stage puts it to work: searching for a record by plaintext value, and enforcing a uniqueness constraint, both via an equality lookup against the stored HMAC. Facilitators should walk through [Single HMAC Strategy](../talk/single-hmac.md) first, especially the unique-constraint failure mode it flags: because the strategy stores exactly one HMAC per record, a value's HMAC changes the moment its key rotates, and enforcement anomalies follow directly from that. This stage doesn't just describe that failure - it reproduces it, in code, on the last line of `Main.java`.

This stage comes as two projects:

- **`starter/`** - what you work in. It compiles and runs, but `PaymentCardStore.findByHmac()` always reports no match, so search fails and nothing is ever treated as a duplicate. Look for the `// TODO` comment.
- **`complete/`** - the finished reference, including the rotation failure mode reproduced live.

!!! tip "Follow along"
    ```bash
    cd stages/07-Single-HMAC-Strategy/starter
    ```
    Using an IDE instead? Open `stages/07-Single-HMAC-Strategy/starter` as its own project.

## The entity hasn't changed

`PaymentCardEntity` is exactly what [Introducing HMACs](06-introducing-hmacs.md) left it: `cardNumber` is `@Encrypt` and `@Hmac`, `cardNumberHmac` and `hmacKeyId` carry the computed value and the key that produced it. The entity never knows what its HMAC gets used for - that's entirely up to the code around it, which is this stage's actual subject.

## A store that never sees plaintext

```java
--8<-- "07-Single-HMAC-Strategy/complete/src/main/java/ie/bitstep/mango/workshop/PaymentCardStore.java:find-by-hmac"
```

`PaymentCardStore` stands in for a real table/column. It never touches `cardNumber` or `encryptedData` - only `cardNumberHmac`. Because that value is deterministic, an equality filter against it is enough to find a match. The same filter against `encryptedData` could never work: two encryptions of the identical card number produce different ciphertext every time.

**Your turn:** in `starter/.../PaymentCardStore.java`, make `findByHmac()` actually search `records` instead of always returning empty.

```java
--8<-- "07-Single-HMAC-Strategy/complete/src/main/java/ie/bitstep/mango/workshop/PaymentCardStore.java:save"
```

Uniqueness reuses the exact same lookup: `save()` rejects anything whose HMAC is already present. Search and uniqueness aren't two mechanisms here, they're one mechanism used twice.

## Search and duplicate rejection, working correctly

```java
--8<-- "07-Single-HMAC-Strategy/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:save-and-search"
```

```java
--8<-- "07-Single-HMAC-Strategy/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:reject-duplicate"
```

Under a single, unchanging HMAC key, both work exactly as expected: the search probe finds the saved record, and a genuine duplicate is rejected before it's ever added to the store.

## Where Single HMAC Strategy breaks

```java
--8<-- "07-Single-HMAC-Strategy/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:rotation-blind-spot"
```

`rotatedShield` is built with a different "current" HMAC key - simulating a key rotation that's happened since `card` was first saved. The duplicate encrypted under the new key hashes to a completely different `cardNumberHmac`, so `findByHmac()` genuinely finds nothing: as far as the store can tell, this is a brand new value, not a repeat of one it already has.

This isn't a bug in `PaymentCardStore` - it's the structural limit [Single HMAC Strategy](../talk/single-hmac.md) describes. The strategy stores exactly one HMAC per record, computed under whatever key was current at the time. Checking uniqueness against records written under an older key would mean recomputing the candidate's HMAC under every key that's ever been active and comparing each - something this strategy has no mechanism for. [List HMAC Strategy](08-list-hmac-strategy.md), next, stores one HMAC per active key instead of one HMAC per record, closing exactly this gap.

## Running it

`starter/` before your change:

```
search found the record?               false
duplicate accepted (same key)?         true
duplicate accepted (after key rotation)? true
```

Nothing is ever found, and nothing is ever rejected - `findByHmac()` always reporting "no match" makes every save look like the first one.

After your change:

```
search found the record?               true
duplicate accepted (same key)?         false
duplicate accepted (after key rotation)? true
```

The middle line is the fix working: a genuine duplicate, same key, correctly rejected. The last line is the point of this stage: the exact same duplicate, encrypted under a rotated key, is silently accepted.
