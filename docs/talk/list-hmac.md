The List HMAC Strategy

## Two core ideas

The List HMAC Strategy is generally the recommended default for applications that can accommodate its trade-offs. It rests on two ideas:

1. **A list of HMAC keys, not a single key.** During a [rotation](key-rotation.md) you *add* the new key rather than replacing the old one, so a tenant can have several active HMAC keys at once.
2. **Every write stores HMACs for the entire list of active keys, not just the current one.** Not one HMAC per field — one per field *per active key*.

This differs from the [Single HMAC Strategy](single-hmac.md): a record written today carries HMACs under every active key. With transactional writes and correctly refreshed key configuration, that lets it remain findable and participate in uniqueness checks throughout a rotation.

## Entity shape: three tables instead of one

This is the trade-off — it forces relational DBs into a multi-table design. Instead of one record with inline HMAC columns, you get:

- **The record itself** — the encrypted data, with no HMAC columns at all
- **A lookups table** — one row per (field, active HMAC key) pair, used for search
- **A unique-values table** — structurally identical to the lookups table, but with a compound unique constraint on (tenant, field, value, key) — separate purely because that constraint doesn't belong on the lookup table

A field's HMAC is no longer "for lookup" or "for uniqueness" implicitly by which column it's in — each field is explicitly marked for one purpose, the other, or both, and the write path is responsible for populating the right table(s) accordingly. Critically, an update must *replace* the full current set of HMAC entries for a record, not append to it — otherwise stale entries from a previous version of the value would linger and stay matchable.

Here's an update that gets that wrong:

```java
--8<-- "naive-list-hmac/src/main/java/ie/bitstep/mango/workshop/talk/naivelisthmac/LookupStore.java:naive-list-hmac-update"
```
<!-- link -->

It only ever adds an entry for the new value; the entry for whatever the value used to be just sits there, still matchable, forever. Fixing it means removing every existing entry for the record before adding the new one, replacing the set rather than appending to it:

```java
--8<-- "naive-list-hmac/src/main/java/ie/bitstep/mango/workshop/talk/naivelisthmac/LookupStore.java:replacing-list-hmac-update"
```
<!-- link -->

See [`talk/naive-list-hmac/`](https://github.com/bitstep-ie/mango4j-crypto-workshop/tree/main/talk/naive-list-hmac) for the full runnable demo: renaming a user and then searching for both their old and new name against each version of `update`.

On a document DB (like MongoDB), the lookups/unique-values lists are just embedded arrays on the same document — no extra tables needed, which is part of why this strategy is a particularly good fit for document databases. On a relational DB you pay for it with up to three writes per update and a join on search.

## Why this closes both Single HMAC Strategy gaps

- **Search**: a record written under key 2 while key 1 is still active carries lookup HMACs for *both*. Any application instance searching with either key finds it — including the multi-instance/cached-key race described in [Single HMAC Strategy](single-hmac.md), where one instance still thinks the old key is current.
- **Uniqueness**: the compound constraint on (field, value, key) means a duplicate username under *any* active key gets caught by the database, not just the current one — there's no window where a rotated key lets a duplicate slip past.

## Extra capabilities this design unlocks

- **Derived-value equality lookups** — because a field can carry more than one HMAC entry, it can also store HMACs of deliberately derived representations (for example, a normalized value with punctuation stripped, or a card number's last four digits). This supports equality lookup for that specific derived value; it is not general substring or prefix search. Each derived token adds an observable equality/frequency signal, and low-entropy tokens such as four digits are easy to enumerate through a query oracle. Include only tokens justified by the threat model and access controls.
- **Compound uniqueness across multiple fields** — a unique constraint spanning several fields, where at least one is HMAC'd, isn't expressible as a normal column-level constraint once that field has no fixed column of its own. A named group with an explicit, stable ordering across the participating fields lets a single combined unique value be computed and constrained across all of them together.

## Verdict

| | |
|---|---|
| **Pros** | Supports passive key rotation for long-lived uniqueness data when the application maintains every active-key entry transactionally; a strong fit for document DBs; can preserve search availability and database-enforced uniqueness across a rotation; supports multiple active HMAC keys |
| **Cons** | Forces relational DBs into a multi-table design; every write costs N HMACs (N = number of active keys), so a growing key list has a real performance cost until old keys are rekeyed away |

That last con is exactly why rekeying ([encryption](rekeying-encryption.md), [HMACs](rekeying-hmacs.md)) isn't optional infrastructure — without it, the "list" in List HMAC only ever grows.
