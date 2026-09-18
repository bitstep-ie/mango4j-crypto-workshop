List HMAC Strategy

!!! abstract "Overview"
    This stage replaces the previous stage's single HMAC field with a list of HMACs per record - one entry per currently-active key - and shows it closing the exact gap [Single HMAC Strategy](07-single-hmac-strategy.md) left open: a duplicate submitted while a key rotation is in progress is now correctly rejected. Facilitators should lead with [List HMAC Strategy](../talk/list-hmac.md), and be clear about scope: mango4j-crypto's own `ListHmacFieldStrategy` manages the list correctly (it replaces each active key's entry on every write and only ever retains entries for keys no longer current), so the append-vs-replace bug [List HMAC Strategy](../talk/list-hmac.md) warns about isn't something this stage's exercise can reproduce - that's what `talk/naive-list-hmac/` is for, a hand-rolled implementation that gets it wrong on purpose. This stage's exercise is the comparison logic an application still has to write: matching across a list.

This stage comes as two projects:

- **`starter/`** - what you work in. It compiles and runs, but `PaymentCardStore`'s list comparison always reports no match, so nothing is ever found and nothing is ever rejected. Look for the `// TODO` comment.
- **`complete/`** - the finished reference, including the rotation fix working live.

!!! tip "Follow along"
    ```bash
    cd stages/08-List-HMAC-Strategy/starter
    ```
    Using an IDE instead? Open `stages/08-List-HMAC-Strategy/starter` as its own project.

## From one field to two lists

`cardNumberHmac`/`hmacKeyId` are gone. In their place, `PaymentCardEntity` implements `Lookup` and `Unique` - the interfaces `ListHmacFieldStrategy` requires - and `@Hmac`'s `purposes` says which list `cardNumber`'s HMACs feed:

```java
--8<-- "08-List-HMAC-Strategy/complete/src/main/java/ie/bitstep/mango/workshop/PaymentCardEntity.java:annotate-list-hmac"
```

`setLookups()`/`setUniqueValues()` are called by the library, not application code - on every `encrypt()`, it computes one `CryptoShieldHmacHolder` per currently-active HMAC key and hands you the list. Compare that to the previous stage's single `cardNumberHmac`: this is a list because [`getCurrentHmacKeys()`](06-introducing-hmacs.md) can now return more than one key at once, and every one of them gets represented.

## A comparison across two lists, not two strings

```java
--8<-- "08-List-HMAC-Strategy/complete/src/main/java/ie/bitstep/mango/workshop/PaymentCardStore.java:any-value-matches"
```

This is the actual new mechanic. The previous stage's store compared `cardNumberHmac.equals(otherCardNumberHmac)` - one value against one value. Here, two records only need to share *one* matching entry across their lists to count as the same value, regardless of whether their lists are otherwise different lengths or cover different keys.

**Your turn:** in `starter/.../PaymentCardStore.java`, make `anyValueMatches()` actually compare the two collections instead of always returning `false`.

## Proving the fix

```java
--8<-- "08-List-HMAC-Strategy/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:save-and-search"
```

Search still works exactly as before - HMAC the term, look for a match.

```java
--8<-- "08-List-HMAC-Strategy/complete/src/main/java/ie/bitstep/mango/workshop/Main.java:rotation-with-old-key-still-active"
```

`rotatingShield` is configured the way [Key Rotation](09-key-rotation.md) recommends: the new key is added *alongside* the old one, not swapped in outright the way the previous stage's `rotatedShield` was. `card`'s stored `uniqueValues` only has an entry for `workshop-hmac-key` (it was written before the rotation), but `duplicateDuringRotation`'s `uniqueValues` has entries for *both* keys - including one computed under `workshop-hmac-key`, which matches `card`'s entry exactly. One shared key in the comparison is enough.

## Running it

`starter/` before your change:

```
search found the record?                       false
duplicate accepted while old key still active? true
```

`anyValueMatches()` always returning `false` makes every comparison look like "no relationship at all" - search finds nothing, and nothing is ever a duplicate.

After your change:

```
search found the record?                       true
duplicate accepted while old key still active? false
```

The second line is the point of this stage: the exact scenario that slipped through in [Single HMAC Strategy](07-single-hmac-strategy.md) - a duplicate submitted mid-rotation - is now correctly caught, because keeping the old key active kept the original record findable.
