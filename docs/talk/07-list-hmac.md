# 7. The List HMAC Strategy

## Two elegant concepts

The List HMAC Strategy is the design mango4j-crypto's own authors recommend by default. It rests on two ideas:

1. **A list of HMAC keys, not a single key.** During a rotation you *add* the new key rather than replacing the old one — exactly the "list of HMAC keys per tenant" idea introduced in Chapter 5, now taken all the way.
2. **Every write stores HMACs for the entire list of active keys, not just the current one.** Not one HMAC per field — one per field *per active key*.

That second point is what the Single HMAC Strategy (Chapter 6) never did, and it's what actually closes both the search-outage and unique-constraint gaps: a record written today carries HMACs under every key currently in play, so it stays findable and constraint-checkable under all of them, immediately, with no rotation window where anything is stale.

## Entity shape: three tables instead of one

This is the trade-off — it forces relational DBs into a multi-table design. Instead of one `USER_PROFILE` row with inline HMAC columns, you get:

- **`USER_PROFILE`** — the encrypted record itself, no HMAC columns at all
- **`USER_PROFILE_LOOKUPS`** — one row per (field, active HMAC key) pair, used for search
- **`USER_PROFILE_UNIQUE_VALUES`** — structurally identical to lookups, but with a compound unique constraint on `(tenant, alias, value, hmacKeyId)` — separate purely because that constraint doesn't belong on the lookup table

In code, the entity implements `Lookup` and/or `Unique` instead of exposing per-field `*Hmac` columns:

```java
@ListHmacStrategy
@Document(collection = "UserProfile")
public class UserProfileEntity implements Lookup, Unique {

    @Encrypt
    @Hmac
    private transient String pan;

    @Encrypt
    @Hmac(purposes = {Hmac.Purposes.LOOKUP, Hmac.Purposes.UNIQUE})
    private transient String userName;

    private Collection<CryptoShieldHmacHolder> lookups;
    private Collection<CryptoShieldHmacHolder> uniqueValues;

    @Override
    public void setLookups(Collection<CryptoShieldHmacHolder> lookups) { this.lookups = lookups; }
    @Override
    public List<CryptoShieldHmacHolder> getLookups() { return lookups; }
    @Override
    public void setUniqueValues(Collection<CryptoShieldHmacHolder> uniqueValues) { this.uniqueValues = uniqueValues; }
    @Override
    public List<CryptoShieldHmacHolder> getUniqueValues() { return uniqueValues; }
}
```

`@Hmac(purposes = ...)` says whether a field's HMAC is for lookup, uniqueness, or both (defaults to lookup). There's no `@HmacKeyId` here — with a list of HMACs, the key ID lives per-entry inside each `CryptoShieldHmacHolder`, not as a single field on the entity. And notably: **when updating an entity, `setLookups()`/`setUniqueValues()` must completely replace the existing collections, never append to them** — the library expects to own the full current set on every write.

On a document DB like Mongo, `lookups`/`uniqueValues` are just embedded lists on the same document — no extra tables needed, which is why the docs call this "an excellent fit for document DBs." On a relational DB you pay for it with up to three writes and a join on search.

## Why this closes both Chapter 6 gaps

- **Search**: a record written under key 2 while key 1 is still active carries lookup HMACs for *both*. Any application instance searching with either key finds it — including the multi-instance/cached-key race from Chapter 6, where one instance still thinks the old key is current.
- **Uniqueness**: the compound constraint on `(alias, value, hmacKeyId)` means a duplicate username under *any* active key gets caught by the DB, not just the current one — there's no window where a rotated key lets a duplicate slip past.

## Extra capabilities this design unlocks

- **HMAC tokenizers** — `@Hmac(HmacTokenizers = {PanTokenizer.class})` generates *additional* lookup HMACs from derived representations of a value (e.g. last-4 digits, first-6, a normalized form with dashes stripped), stored alongside the full-value HMAC — richer search without weakening the encrypted value itself.
- **`@UniqueGroup`** — a compound unique constraint spanning multiple fields (some HMAC'd, some cleartext) isn't expressible with a normal column-level constraint once one of those fields is a `@Hmac` field with no fixed column. `@UniqueGroup` lets you name a group and an order number per field so the library computes one combined unique HMAC across them.

## Verdict

| | |
|---|---|
| **Pros** | Correct under all combinations of requirements; the only design supporting passive key rotation for long-lived uniqueness data; standardized, cleaner search code; strong fit for document DBs; zero-outage search; guaranteed unique-constraint integrity; works correctly even with cached keys; easiest to rekey with no functional impact; supports as many HMAC keys as needed, added at any time |
| **Cons** | Forces relational DBs into a multi-table design; every write costs N HMACs (N = number of active keys), so a growing key list has a real performance cost until old keys are rekeyed away |

That last con is exactly why rekeying (Chapters 8–9) isn't optional infrastructure — without it, the "list" in List HMAC only ever grows.
