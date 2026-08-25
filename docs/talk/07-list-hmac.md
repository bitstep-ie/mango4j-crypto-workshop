The List HMAC Strategy

## Two core ideas

The List HMAC Strategy is generally the recommended default for applications that can accommodate its trade-offs. It rests on two ideas:

1. **A list of HMAC keys, not a single key.** During a rotation you *add* the new key rather than replacing the old one — exactly the "list of HMAC keys per tenant" idea introduced in Chapter 5, now taken all the way.
2. **Every write stores HMACs for the entire list of active keys, not just the current one.** Not one HMAC per field — one per field *per active key*.

That second point is what the Single HMAC Strategy (Chapter 6) never did, and it's what actually closes both the search-outage and unique-constraint gaps: a record written today carries HMACs under every key currently in play, so it stays findable and constraint-checkable under all of them, immediately, with no rotation window where anything is stale.

## Entity shape: three tables instead of one

This is the trade-off — it forces relational DBs into a multi-table design. Instead of one record with inline HMAC columns, you get:

- **The record itself** — the encrypted data, with no HMAC columns at all
- **A lookups table** — one row per (field, active HMAC key) pair, used for search
- **A unique-values table** — structurally identical to the lookups table, but with a compound unique constraint on (tenant, field, value, key) — separate purely because that constraint doesn't belong on the lookup table

A field's HMAC is no longer "for lookup" or "for uniqueness" implicitly by which column it's in — each field is explicitly marked for one purpose, the other, or both, and the write path is responsible for populating the right table(s) accordingly. Critically, an update must *replace* the full current set of HMAC entries for a record, not append to it — otherwise stale entries from a previous version of the value would linger and stay matchable.

On a document DB (like MongoDB), the lookups/unique-values lists are just embedded arrays on the same document — no extra tables needed, which is part of why this strategy is a particularly good fit for document databases. On a relational DB you pay for it with up to three writes per update and a join on search.

## Why this closes both Chapter 6 gaps

- **Search**: a record written under key 2 while key 1 is still active carries lookup HMACs for *both*. Any application instance searching with either key finds it — including the multi-instance/cached-key race from Chapter 6, where one instance still thinks the old key is current.
- **Uniqueness**: the compound constraint on (field, value, key) means a duplicate username under *any* active key gets caught by the database, not just the current one — there's no window where a rotated key lets a duplicate slip past.

## Extra capabilities this design unlocks

- **Derived-value search terms** — because a field can carry more than one HMAC entry, it's straightforward to also store HMACs of *derived* representations of a value (e.g. the last four digits of a card number, a normalized form with punctuation stripped), enabling richer partial-match search without weakening the encrypted value itself.
- **Compound uniqueness across multiple fields** — a unique constraint spanning several fields, where at least one is HMAC'd, isn't expressible as a normal column-level constraint once that field has no fixed column of its own. A named group with an explicit, stable ordering across the participating fields lets a single combined unique value be computed and constrained across all of them together.

## Verdict

| | |
|---|---|
| **Pros** | Correct under all combinations of requirements; the only design supporting passive key rotation for long-lived uniqueness data; standardized, cleaner search code; strong fit for document DBs; zero-outage search; guaranteed unique-constraint integrity; works correctly even with cached keys; easiest to rekey with no functional impact; supports as many HMAC keys as needed, added at any time |
| **Cons** | Forces relational DBs into a multi-table design; every write costs N HMACs (N = number of active keys), so a growing key list has a real performance cost until old keys are rekeyed away |

That last con is exactly why rekeying (Chapters 8–9) isn't optional infrastructure — without it, the "list" in List HMAC only ever grows.
