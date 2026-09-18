Key Rotation

!!! abstract "Overview"
    This stage rotates the encryption key: introduce a new key (optionally from a different provider), point new writes at it, and confirm old ciphertext still decrypts under the old key. Facilitators should present this as phase 1 of the two-phase process from [Key Rotation](../talk/key-rotation.md) — phase 2, migrating existing data to the new key, is deliberately deferred to Rekeying: Encryption so each phase gets its own focused exercise.

!!! info "Content coming soon"
    This stage is a placeholder. It should give learners:

    - A second encryption key added to the `CryptoKeyProvider`, with "current" repointed to it via the alias mechanism from Key Aliases & Crypto Key Configs
    - Confirmation that data encrypted before the rotation still decrypts correctly (the old key must remain resolvable by ID even though it's no longer "current")
    - New writes after the rotation observably using the new key ID inside the structured ciphertext
    - A note that HMAC keys rotate the same way but aren't touched by this stage (kept separate on purpose, since HMAC uses a list of active keys rather than one "current" key)

    Natural follow-on from Introducing HMACs / Single or List HMAC Strategy if those land first — decide stage ordering once all are drafted, since Key Rotation's data-retention discussion assumes HMAC concepts per the talk's own reordering (see `docs/talk/notes.md`).
