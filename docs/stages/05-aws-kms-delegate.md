AWS KMS Delegate

!!! abstract "Overview"
    Stage 3 swapped the fake Base64 delegate for `PBKDF2EncryptionService`, a real but local-only cryptographic provider. This stage swaps again, this time for `mango4j-crypto-aws-kms-delegate`, showing that moving to a production key management service is the same shape of change: a different delegate, a different `CryptoKey.type`/`configuration`, and nothing else. Facilitators should frame this as proof that the delegate abstraction from [Key Aliases & Key Configs](../talk/key-aliases.md) holds up against a real external provider, not just the local PBKDF2 example.

!!! info "Content coming soon"
    This stage is a placeholder. It should give learners:

    - A local or mocked AWS KMS setup runnable without real AWS credentials (e.g. LocalStack, or a documented "point this at your own KMS key" fallback) so CI and self-serve learners aren't blocked on cloud access
    - The `mango4j-crypto-aws-kms-delegate` dependency added alongside the existing ones
    - A `CryptoKey` configured with the KMS key ARN/ID instead of a PBKDF2 passphrase
    - Confirmation that `encrypt()`/`decrypt()` call sites in `Main.java` are unchanged from stage 3

    Needs a decision on the CI story (LocalStack container vs. skip in CI) before this can move from placeholder to real content — check with the maintainers of `../mango4j-crypto-aws-kms-delegate` for the recommended local testing setup.
