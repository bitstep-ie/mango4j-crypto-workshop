# 5. Encryption Key Rotation

## Why keys rotate

Keys don't stay valid forever. A key ages out under a compliance schedule, a provider gets deprecated or migrated away from, or a key is suspected compromised and needs to stop being trusted. Whatever the trigger, the design goal worth aiming for is that **a key rotation should be something an application can do at short notice, on the fly** — not something that requires a maintenance window.

## Phase 1: switching new writes to a new key

Changing an encryption key is, by itself, straightforward: add the new key to your system and start using it for all writes going forward. Concretely, that means changing what "the current encryption key" resolves to (Chapter 2) — nothing else in your application code changes.

- The new key can be from a completely different provider than the old one. The structured ciphertext format (Chapter 3) records which key encrypted each record, and the alias indirection (Chapter 2) means nothing in application code ever hardcoded a specific provider — so swapping providers is just a different key configuration, not a code change.
- Old keys keep working for **decryption**. Resolving a key by ID still works for any key the application has ever used, so every record written under the old key keeps reading back correctly. Nothing needs to migrate yet — you just need to keep the old key around until nothing references it any more.

## Phase 2: migrating old data to the new key

Once new writes are on the new key, old rows are still sitting there encrypted under the old key. Getting them off it — so the old key can eventually be retired — is a separate, later pass: **rekeying**, covered in full in Chapters 8–9. It's a deliberately separate phase, not a single atomic switch, because:

- Rekeying potentially touches every record in the system, which takes time and costs performance/throughput you don't want to pay synchronously with the rotation itself.
- The old key has to remain valid and available for decryption for as long as any unrekeyed data still depends on it.

**A data retention window doesn't remove the need for this.** It's tempting to reason that if the key period is 10 years but data retention is only 5, rekey support is unnecessary — the key could just be swapped every 10 years. Consider what happens in year 11: the key changes, and 5 years of records (years 6–10) are still sitting on the old key. Those records are no longer findable by search, and if any of those fields have unique constraints, that many years of records are exposed to potential duplication (see below). Rekey support is needed regardless of how long data is retained, not only when retention exceeds the key period.

## The same shape of problem: migrating an *unencrypted* field to encrypted

Everything above assumes the field was already encrypted, just under an old key. But the very first time a field becomes confidential, you're migrating from *no* encryption at all — and the mechanics are similar to phase 2 above, with a few additional considerations:

- **Encryption for an existing column can't just be switched on.** Every existing row is plaintext; there's no ciphertext yet to decrypt, so the migration has to write encrypted values for the first time, not just re-key existing ones.
- **Backfilling millions of rows without downtime** is the same operational problem as any large-scale rekey, just with no prior art in the table to compare against as you go.
- **Dual-read/dual-write periods introduce their own complexity** — while some rows are encrypted and some aren't, application code has to correctly read/write both shapes at once, and that temporary complexity can persist longer than planned if the migration stalls.
- **Pausing other work on the table for the duration of the migration** is often unpopular, so teams sometimes rush the backfill instead (risking the issues above) or let it run indefinitely in the background.

This transitional state deserves explicit support rather than being handled ad hoc. A field mid-migration is deliberately *not yet* meeting the normal rule that a confidential field only ever exists as a working (Chapter 4) value — it's still readable and writable in its old, unencrypted, persisted form while the backfill completes. Treating that as a tracked, temporary exception — with an owner, a justification, and a target date by which it should be gone — rather than a silent gap in the data model, is what keeps a migration from quietly becoming permanent. Making the exception loud (a startup warning that escalates to an error past the target date, for example) is what turns the deadline into an actual forcing function rather than a comment nobody revisits.

## Production impact of single-key, single-HMAC designs

Rotation and migration are where the limitations of single-key, single-HMAC designs (Chapter 6) become visible in production:

- **Search functionality degrades gradually** — with only one HMAC key active at a time, a rotation makes every existing record's HMAC stop matching new search HMACs until it's rekeyed.
- **Duplicate records can appear in fields meant to be unique** — the unique-constraint failure mode covered in Chapter 6: a rotated HMAC key means the same plaintext value now hashes differently, and a DB-level unique constraint on the HMAC column no longer catches the duplicate.
- **Rolling back a rotation doesn't undo its effects** — writes that already went out under the new key stay as they are, so a rollback adds a second inconsistency rather than removing the first.
- **These failures tend to be gradual rather than immediate** — they're rarely a hard crash, which means they're often identified well after the rotation, during a later investigation, rather than at the time it happened.

This is the practical case for the List HMAC Strategy (Chapter 7) and for treating rekeying (Chapters 8–9) as a first-class, tested capability rather than something addressed only after a rotation causes a problem.
