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

### Chapters dropped from the original 15-chapter talk (docs/talk, pre-2026-08-25)

Not carried into the new outline in any form — set aside along with the rest of the old talk, not folded into a new chapter:

- 3. The Naive Approach: A Simple Case of Implementing ALE
- 4. Failure Mode: Encrypting Fields Directly Into Their Columns
- 11. Failure Mode: Migrating an Unencrypted Field to Encrypted
- 12. When It All Comes Together: Outages and Application Failure
- 13. What "Doing It Right" Actually Requires
- 14. Introducing mango4j-crypto
- 15. What We're Building Today

Old chapters 1 and 2 were merged into new chapter 1 (not dropped). Old chapters 5–10 were absorbed into the new outline's chapters 2–3, 5–9 (not dropped, restructured). Revisit the dropped list above if any of that material turns out to still be needed (e.g. a workshop-intro chapter like old 14/15, or the failure-mode narratives).

