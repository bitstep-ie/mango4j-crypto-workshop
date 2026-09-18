Key Aliases & Crypto Key Configs

!!! abstract "Overview"
    So far the workshop has used a single hardcoded key ID. This stage introduces the alias indirection from [Key Aliases & Key Configs](../talk/key-aliases.md): application code asks the `CryptoKeyProvider` for "the current encryption key" by role, not by ID, and the provider resolves that to a concrete `CryptoKey`. Facilitators should walk through why this matters before learners start: it's what makes swapping providers, and later rotating keys, a configuration change rather than a code change.

!!! info "Content coming soon"
    This stage is a placeholder. It should give learners a `CryptoKeyProvider` backed by more than one configured key, and have them:

    - Add a second key config alongside the existing one, with a distinct key ID
    - Resolve "the current encryption key" through an alias/role lookup rather than a hardcoded ID
    - Resolve an arbitrary key by ID (not just "current"), since decryption always needs this regardless of which key is current
    - Observe that switching which key is "current" doesn't require touching `Main.java`'s encrypt/decrypt calls

    Likely a plain stage (not starter/complete), since the exercise is mostly configuration rather than a TODO'd algorithm step. Decide once drafted.
