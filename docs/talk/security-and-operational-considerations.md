# Security and Operational Considerations

Application Level Encryption is a useful control, but it is not a complete security architecture. This chapter makes the assumptions and operational responsibilities around an ALE design explicit, so that encryption, HMAC indexing, key rotation, and migration are evaluated as parts of one system.

## Start with the boundary you are protecting

ALE is most effective when the risk is direct exposure of a persistence layer: a database dump, a replica, a backup, a storage snapshot, or credentials that permit direct database queries. When protected fields are encrypted before persistence, those systems can store ciphertext rather than plaintext.

That boundary matters. ALE does not, by itself, protect against an attacker or insider who can:

- run arbitrary code in the application process;
- invoke an application endpoint that decrypts or returns confidential data;
- read the encryption keys or use the key-management service with the application's authority; or
- obtain plaintext from logs, traces, caches, queues, metrics, error reports, or client responses.

Document the assets, actors, and systems in scope before deciding what ALE is expected to achieve. It complements controls such as access control, network segmentation, database encryption, secrets management, monitoring, and secure software development; it does not replace them.

## Choose confidentiality *and* integrity

Encryption provides confidentiality: it prevents an unauthorized reader from interpreting ciphertext as plaintext. It does not necessarily detect alteration, substitution, or truncation of ciphertext. For production data, choose an encryption delegate and configuration that provide authenticated encryption (for example, an AEAD construction), or an equally robust, reviewed integrity mechanism.

Where the encryption design supports additional authenticated data, bind stable context that should not be transferable between records — for example, an entity type, tenant identifier, field-set version, or record identifier. Do this only where that context is stable across legitimate reads and migrations. The exact capability and data format are properties of the selected delegate, so verify them before designing storage around them.

The HMACs used in this workshop for lookup and uniqueness are not ciphertext authentication tags. They are deterministic indexes over selected plaintext values. A lookup HMAC must not be treated as proof that an encrypted payload has not been changed.

## Handle IVs and nonces according to the selected algorithm

An IV or nonce is per-operation input to an encryption scheme. It is usually stored with the ciphertext and normally is not secret. Its safety requirement depends on the algorithm and mode: some schemes require unpredictable random input, some require uniqueness under a key, and some require both.

Do not invent nonce generation rules from a generic example. Reuse of a nonce where the scheme forbids it can disclose relationships between plaintexts and, for some authenticated modes, can compromise confidentiality and integrity. Keep the nonce, authentication data, authentication tag, ciphertext, algorithm/version metadata, and key identifier in a format the chosen delegate can validate during decryption.

## Treat HMAC search as an equality index with leakage

A HMAC lets the application calculate the same protected index for the same normalized input and key. It is therefore suitable for equality lookup and, with the right storage strategy, uniqueness enforcement. It is not a general substring, prefix, fuzzy, or range-search mechanism.

This convenience has a cost: matching HMAC values reveal that the underlying values match. An observer of the index can see frequency and equality patterns even without knowing the plaintext. Derived tokens create additional patterns. For example, a HMAC of the last four digits of a card number supports equality lookup on those four digits, but the input domain is small and can be easy to enumerate through a search or write oracle.

Before adding an HMAC index, specify:

- the exact normalized input (case folding, whitespace, punctuation, Unicode handling, and locale);
- which readers may perform lookups and whether they can enumerate candidate values;
- the expected input entropy and whether derived tokens are acceptable;
- the key scope, tenant scope, and retention period; and
- the database constraint and transaction that preserve uniqueness under concurrent writes.

Version normalization rules deliberately. A silent normalization change can make existing values unfindable or weaken a uniqueness check until old entries are reindexed.

## Make persistence boundaries enforceable and testable

Keeping a transient plaintext field separate from its encrypted persistence blob is a strong modelling practice. It does not by itself prove that plaintext cannot leave the process. Review every integration that can inspect an entity or its fields:

- ORM mappings and database serializers;
- JSON/XML serialization and API response models;
- application, audit, SQL, and error logs;
- distributed tracing, metrics tags, crash reporting, and debugger tooling;
- caches, message queues, search indexes, and temporary files; and
- test fixtures, sample data, support exports, and backup procedures.

Use explicit DTOs and serialization rules where appropriate, and write integration tests that assert the persisted and emitted representations. Test the failure paths too: validation errors, exceptions, retries, and dead-letter handling often follow different logging and serialization paths.

## Operate key changes as a lifecycle, not a configuration edit

Changing which key is current affects more than new encryption calls. A safe rotation plan identifies the old key, the new key, the key provider and delegates used by both, cache-refresh behaviour across application instances, the records that still depend on the old key, and the conditions for retirement.

For encryption, old ciphertext must remain decryptable until rekeying or retention expiry has removed every dependency. For HMACs, search and uniqueness behaviour depends on the storage strategy. The List HMAC Strategy can preserve those properties across a rotation when writes create the required entries for all active keys and the persistence changes are transactional. A Single HMAC column cannot provide equivalent database-enforced uniqueness across key changes.

Before rotation, rehearse the operation in a representative environment. Monitor progress, failures, throughput, provider rate limits, and the remaining count of records on each key. Define rollback behaviour in advance: a rollback cannot make writes already created under a new key disappear, so both key sets may need to remain usable while the state is reconciled.

## Treat migration as a controlled temporary exception

Encrypting an existing plaintext column requires a backfill and, often, a period in which the application can correctly handle both old and new representations. `@EnableMigrationSupport` records the temporary exception to the normal `transient`-field rule, but it does not perform the migration or decide the application's read/write semantics.

Give each migration an owner, completion date, observability, and a removal criterion. Define how new writes are protected while the backfill runs, how retries avoid data loss or stale overwrites, and how the application detects records that have not yet migrated. Remove the compatibility path and migration annotation after verification; leaving them indefinitely expands the plaintext attack surface.

## Keep compliance claims scoped

Encryption may help satisfy contractual, regulatory, or risk-management requirements, but it is not a universal compliance conclusion. PCI DSS, HIPAA, GDPR, local law, data residency commitments, and customer contracts have different scopes and may require controls beyond encryption. Obtain appropriate legal, compliance, and security review for the system being deployed.

## A practical review checklist

Before putting an ALE design into production, confirm that:

- the threat model states what ALE protects and what it does not;
- the selected encryption implementation provides appropriate confidentiality and integrity properties;
- key material, nonces, ciphertext metadata, and authentication data are managed according to the implementation's requirements;
- plaintext is excluded from persistence, observability, and messaging paths;
- HMAC indexes have documented normalization, leakage, authorization, and concurrency behaviour;
- rotation and rekeying are tested with old and new keys, multiple application instances, and failure recovery; and
- migration and key retirement have measurable completion criteria.
