# 5. Encryption Key Rotation

## Why keys rotate

Keys don't stay valid forever. A key ages out under a compliance schedule, a provider gets deprecated or migrated away from, or — in the worst case — a key is suspected compromised and needs to stop being trusted immediately. Whatever the trigger, mango4j-crypto's position is blunt: **a key rotation should be something an application can do at short notice, on the fly** — not a maintenance-window project.

## Phase 1: switching new writes to a new key

Changing an encryption key is, by itself, straightforward: add the new `CryptoKey` to your system and start using it for all writes going forward. Concretely, that means changing what your `CryptoKeyProvider.getCurrentEncryptionKey()` returns (Chapter 2) — nothing else in your application code changes.

- The new key can be from a completely different provider (`CryptoKey.type`) than the old one. The structured ciphertext format (Chapter 3) records which key encrypted each record, and the alias indirection (Chapter 2) means nothing in application code ever hardcoded "AWS KMS" — so swapping providers is just a different `CryptoKey` config, not a code change.
- Old keys keep working for **decryption**. `CryptoKeyProvider.getById()` still resolves them, so every record written under the old key keeps reading back correctly. Nothing needs to migrate yet — you just need to keep the old key around until nothing references it any more.

## Phase 2: migrating old data to the new key

Once new writes are on the new key, old rows are still sitting there encrypted under the old key. Getting them off it — so the old key can eventually be retired — is a separate, later pass: **rekeying**, covered in full in Chapters 8–9. It's a deliberately separate phase, not a single atomic switch, because:

- Rekeying potentially touches every record in the system, which takes time and costs performance/throughput you don't want to pay synchronously with the rotation itself.
- The old key has to remain valid and available for decryption for as long as any unrekeyed data still depends on it.

**Don't let a data retention window fool you into skipping this.** It's tempting to think "our key period is 10 years, but our data retention is only 5, so I'll never actually need rekey support — I'll just swap the key every 10 years." The flaw: what happens in year 11, when the key changes and you still have 5 years of records (years 6–10) sitting on the old key? You can no longer find them by search, and if any of those fields have unique constraints, you now have years of records at risk of duplication (see below). Rekey support isn't optional just because data doesn't live forever.

## The same shape of problem: migrating an *unencrypted* field to encrypted

Everything above assumes the field was already encrypted, just under an old key. But the very first time a field becomes confidential, you're migrating from *no* encryption at all — and the mechanics are strikingly similar to phase 2 above, with some extra sharp edges:

- **You can't just "turn on" encryption for an existing column.** Every existing row is plaintext; there's no ciphertext yet to decrypt, so the migration has to write encrypted values for the first time, not just re-key existing ones.
- **Backfilling millions of rows without downtime** is the same operational problem as any large-scale rekey, just with no prior art in the table to compare against as you go.
- **Dual-read/dual-write periods carry their own bugs** — while some rows are encrypted and some aren't, application code has to correctly read/write both shapes at once, which is exactly the kind of temporary complexity that outlives its "temporary" label if the migration stalls.
- **The feature freeze nobody wants to announce** — teams are often reluctant to halt other work on a table mid-migration, so the backfill either gets rushed (risking the bugs above) or drags on indefinitely.

mango4j-crypto has a purpose-built annotation for exactly this transitional state: `@EnableMigrationSupport`. It marks a field that's *not yet* fully migrated to `@Encrypt` — normally `@Encrypt` fields must be `transient` (Chapter 4), but this annotation is the one sanctioned exception, letting a field stay non-transient (i.e. still readable/writable in its old, unencrypted form) for a bounded period while the migration completes:

```java
@EnableMigrationSupport(
    completedBy = "2026-03-31",
    justification = "Backfill via crypto rekey job across large dataset",
    ticket = "OBS-1432"
)
private String email; // previously unencrypted field, non-transient
```

Before `completedBy`, its presence logs a warning at startup; after that date, it becomes an error. The deadline is a forcing function — this annotation is explicitly meant to be removed once the migration finishes, not a permanent escape hatch from the `transient` rule.

## When it goes wrong in production: outages and application failure

Rotation and migration are where the failure modes from single-key, single-HMAC designs (Chapter 6) actually show up and hurt:

- **Search functionality silently degrading in production** — with only one HMAC key active at a time, a rotation makes every existing record's HMAC stop matching new search HMACs until it's rekeyed.
- **Duplicate "unique" records corrupting business data** — the unique-constraint failure mode covered in Chapter 6: a rotated HMAC key means the same plaintext value now hashes differently, and a DB-level unique constraint on the HMAC column no longer catches the duplicate.
- **Emergency rollbacks and the risks they introduce** — rolling back a rotation after some writes have already gone out under the new key doesn't undo those writes; it just adds a second inconsistency on top of the first.
- **The incident review nobody wants to present** — the common thread in all of the above is that these failures are usually silent and gradual, not a hard crash, which means they're often caught in an incident review well after the damage is done rather than at rotation time.

This is the practical case for the List HMAC Strategy (Chapter 7) and for treating rekeying (Chapters 8–9) as a first-class, well-tested capability rather than something bolted on only once a rotation goes wrong.
