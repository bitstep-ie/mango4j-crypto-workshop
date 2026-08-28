# Talk Notes (running)

Scratch notes from working sessions on the talk. Not part of the published talk itself.

## 2026-08-25

- Chapters need code, not just prose — currently the talk/*.md files are text-only outlines. Need to figure out where code samples come from (inline snippets? pulled from the workshop's starter/complete stages via `--8<--` markers, per [[feedback_starter_complete_pattern]]?) and which chapters need them most.
- Key aliases are replaced with crypto key configs (i.e. in mango4j-crypto, a "key alias" in code/config resolves to an actual crypto key configuration — relevant for the key rotation / multiple providers chapters, 5–6, 9–10).
- Evolution: plain ciphertext → structured ciphertext (structured format presumably carries metadata like key alias/version/provider alongside the raw ciphertext bytes — this is likely the mechanism that makes key rotation and multi-provider support possible, ties into chapters 5, 9–10).

## Decision: setting the existing 15-chapter talk (docs/talk/01–15) aside

Building a new talk instead — starting fresh rather than continuing to fill out the current outline. The notes above (code-in-talk, key aliases → crypto key configs, plain→structured ciphertext) are carried forward as input to the new talk, not discarded.

- Transient (in-memory) vs encrypted blob(s): the design ensures we never suffer from double (or more) encrypt/decrypt operations — i.e. avoid redundant re-encryption/re-decryption passes on the same data as it moves through the system.
- Encryption key rotation — topic to cover in the new talk (carries over from old chapters 5, 9–10 on multi-provider + key rotation).
  - Two-phase: (1) switch to writing new data with the new key, (2) subsequently migrate/rekey all old data still under the old key.
  - The new key can be from a different provider entirely; old keys continue to work (decrypt) so nothing breaks mid-migration.

- Next topic: introduce HMACs — single column, for search, and for unique constraints. (Carries over from old chapters 6–7.)
- Then move to list HMAC, and how it fits better for search and unique keys (vs. single HMAC). (Carries over from old chapter 8.)

- Rekeying: migrating to a new key re-encrypts the data AND regenerates the HMACs (not just the ciphertext). (Carries over from old chapters 9–10.)
  - Split into two separate rekey operations/topics: encryption rekey and HMAC rekey — treat them as distinct steps, not one combined step.

## New talk — chapter order (regenerated as docs/talk/01–09 on 2026-08-25)

1. `01-intro.md` — What is ALE, and why do we still need it (merged old chapters 1+2)
2. `02-key-aliases.md` — Key aliases → crypto key configs
3. `03-structured-ciphertext.md` — Plain ciphertext → structured ciphertext
4. `04-transient-vs-encrypted-blobs.md` — Transient vs. encrypted blobs, avoiding double encrypt/decrypt
5. `05-key-rotation.md` — Encryption key rotation as a single chapter (phase 1: switch new writes to the new key, new key can be a different provider, old keys still decrypt; phase 2: migrate old data to the new key) — originally split into 3 chapters (intro/phase1/phase2), merged into one per explicit request, no split
6. `06-single-hmac.md` — Introducing HMACs, single HMAC strategy: search and unique-constraint issues, why they break, especially unique constraints
7. `07-list-hmac.md` — List HMAC strategy: why it fits search and unique keys better than single HMAC
8. `08-rekeying-encryption.md` — Rekeying: encryption — re-encrypting data to the new key
9. `09-rekeying-hmacs.md` — Rekeying: HMACs — regenerating HMACs separately from the encryption rekey

All chapters except 1 are currently outline stubs ("Content coming soon.") with bullet points capturing what each should cover. `mkdocs.yml` nav updated to match.

### Old-talk chapters folded into the new outline (2026-08-25)

Every chapter from the original 15-chapter talk now has a home in the new 9-chapter outline — nothing was left out in the end:

- Old 1–2 (What Is ALE / Why We Need It) → new Ch 1 (merged directly)
- Old 3 (The Naive Approach) → new Ch 3 (Structured Ciphertext), as the naive/opaque-blob starting point
- Old 4 (Failure: Encrypted Columns) → new Ch 4 (Transient vs. Encrypted Blobs), as the motivating failure mode
- Old 5–10 (multi-provider/rotation, HMAC search/uniqueness, single-vs-list HMAC, rekeying) → new Ch 2–3, 5–9, restructured
- Old 11 (Failure: Migration) → new Ch 5 (Key Rotation), as the related unencrypted→encrypted migration failure mode
- Old 12 (Outages and App Failure) → new Ch 5 (Key Rotation), as the "when it goes wrong in production" section
- Old 13 (Doing It Right) → new Ch 9 (Rekeying: HMACs), as the closing wrap-up
- Old 14 (Introducing mango4j-crypto) → new Ch 1, as a closing section naming the framework the talk demonstrates
- Old 15 (What We're Building Today) → new Ch 9, as the closing transition into the hands-on workshop

All merged-in content is still outline-stub bullets/headings, not full prose (except chapter 1's original content, which was already written).

## 2026-08-26+ (chapter numbering has since moved on from the table above)

- Split "Introducing HMACs" out of the old chapter 6 (single HMAC strategy) into its own chapter: what a HMAC is, why you'd want one (search, unique constraints), independent of any specific storage strategy. The strategy-specific chapters (single HMAC, list HMAC) now assume that grounding instead of re-explaining it.
- Nav/heading numbering was later removed entirely (see mkdocs.yml). Numeric file prefixes were then dropped too — filenames are now plain descriptive slugs (`intro.md`, `key-aliases.md`, ...); order is controlled purely by nav order in mkdocs.yml.
- Reordered "Key Rotation" to come after "Introducing HMACs": Key Rotation's discussion of search/findability during a rotation depends on HMAC concepts (search, uniqueness) that hadn't been introduced yet when it came first. Current order: Intro, Key Aliases, Structured Ciphertext, Transient vs. Encrypted Blobs, Introducing HMACs, Key Rotation, Single HMAC Strategy, List HMAC Strategy, Rekeying: Encryption, Rekeying: HMACs.
- Content fix in Key Rotation's data-retention paragraph: search does NOT actually break when old HMAC keys stay in the known-keys list — a search hashing the term with every known key (old and new) still finds old records. What actually breaks under the Single HMAC Strategy is unique-constraint enforcement (a duplicate can slip in under a different key), not search. Corrected the paragraph to stop conflating the two.
- All "Chapter N" prose references replaced with markdown links to the target chapter by name (no numbers), consistent with numbering being fully removed from the talk.

## 2026-08-28

- Content fix in Transient vs. Encrypted Blobs: the "double encrypt/decrypt" claim (also in the 2026-08-25 note above) was overstated. The transient/blob split does not stop application code from *calling* `encrypt()`/`decrypt()` more than once, that's still on application control flow. What it actually prevents is the consequence of calling the wrong operation on the wrong representation: data being multiply encrypted, and exceptions (or garbage) from decrypting something that isn't actually ciphertext. Also added the naive `save()`/`load()` pitfall this doesn't solve on its own: a hand-rolled single-column `save()` leaves the entity holding ciphertext where it had plaintext, so continuing to use the value afterward means remembering to call `decrypt()` again. The split just means `encrypt()` doesn't force that by destroying the transient field, not that the application is relieved of calling `decrypt()` where it's actually needed.

- Expanded the naive `save()`/`load()` pitfall into two distinct, concrete problems (these are things that have actually been seen/tried in the past, not hypothetical): (1) a concurrency window inside `save()`, between the in-place encrypt and the in-place decrypt, a shared entity instance sits there holding ciphertext, and a concurrent reader on another thread can silently see it; the transient/blob split genuinely fixes this one, since `encrypt()` never touches the transient field. (2) `load()`'s decrypt is forced and unconditional, calling it against an entity that already has unsaved in-memory changes silently clobbers them, no warning, no conflict check. The transient/blob split does **not** fix this one: it's the same risk whether `decrypt()` is hand-rolled or mango4j-crypto's own, and avoiding it is about *when* the application chooses to call `decrypt()`/`load()`, not the ciphertext representation.
- Added `talk/naive-save-load/`, a runnable side-by-side demo of the direct-field and transient/blob variants: `DirectFieldStore`/`TransientBlobStore` and matching account classes, wired into `docs/talk/transient-vs-encrypted-blobs.md`'s new save()/load() code snippets. The concurrency window is proved with real threads and `CountDownLatch`-based ordering (deterministic, no sleeps), not a timing-dependent race.
- Content fix in Transient vs. Encrypted Blobs: the encrypt()/decrypt() safety claim needed the same correction the earlier fixes made, but for the direct calls, not just save()/load(). Repeated `encrypt()` calls really are safe in the transient/blob model (wasteful, not lossy), but repeated `decrypt()` calls are not: it always overwrites the transient field, so any unsaved in-memory edit on it is silently discarded. Added a table comparing `encrypt()`/`decrypt()` safety across the transient/blob and direct-field models, since this is the same underlying issue as `load()`'s forced overwrite, just stated for the raw operations rather than the save()/load() wrapper.
- Rebuilt the naive save()/load() example after feedback that the direct-field version (a bare `username` field, `DirectFieldAccount`/`DirectFieldStore`) wasn't a faithful naive example: too simplistic, and its separate `db: Map<Long,String>` persisted store read as "a domain pojo and a db pojo" rather than one entity doing everything. Replaced with `NaiveCardEntity`/`NaiveCardStore` (renamed from DirectFieldAccount/DirectFieldStore): a single entity with `cardNumber` (dual-purpose, plaintext most of the time, ciphertext for the part of save() where it's just been encrypted), plus `iv` and `cardNumberHmac` as ordinary sibling columns, matching the naive shape Structured Ciphertext already describes. `table`/`managed` inside NaiveCardStore model the real DB bytes and an ORM-style session identity map respectively, not a second POJO, so the forced-overwrite pitfall still demonstrates silently (not an exception), since decrypt() reads from the untouched persisted row, not from whatever the caller may have edited on the live entity. The transient/blob comparison (TransientBlobAccount/TransientBlobStore) was left unchanged; it wasn't the part called out as a bad example.
- Corrected NaiveCardStore per feedback: save() should do its own encrypt, real persist, decrypt cycle (restoring cardNumber for the caller, as originally intended), and load() should be just "fetch the managed entity, decrypt directly on it" with no Row and no branching. Consequence (also per feedback): since save() already decrypts as its last step, calling load() right after save() is a double decrypt and correctly throws (Base64/AEAD failure on a field that's no longer valid ciphertext), not a silent overwrite. This actually strengthens the comparison against TransientBlobStore, whose load() still silently overwrites the transient field (decrypt() there reads the untouched blob field, so it never has a chance to detect anything's wrong). Same root cause (decrypt() has no way to know it's unsafe to run), two different failure shapes: one throws, one doesn't.
- Added a "Gaps" section to docs/talk/faq/cryptoshield.md: no protection against concurrent encrypt()/decrypt() on the same entity instance. Verified directly against ../mango4j-crypto's real CryptoShield.java (not speculation): encrypt() never writes the source fields (so the specific ciphertext-visibility pitfall from Transient vs. Encrypted Blobs genuinely doesn't apply to it), but decrypt()'s field.set() is unconditional, no lock, no volatile, no version/dirty check anywhere in the class. Cross-linked to Transient vs. Encrypted Blobs for the underlying mechanism and the runnable naive-save-load demo of the equivalent race.
