Is rekeying built into mango4j-crypto, or do I have to write it myself?

Built in (currently in beta) — the actual decrypt/re-encrypt and HMAC recompute work is handled by the library. What an application has to supply is the plumbing that connects that work to its own database:

- **A `RekeyCryptoKeyManager`**, with one method: `markKeyForDeletion(cryptoKey)`. Called once a key is no longer needed; the application marks it deleted (typically so its own `CryptoKeyProvider` stops returning it) rather than being required to destroy it immediately.
- **A `RekeyService<T>` per entity type**, with a handful of methods the rekey process calls repeatedly in batches until nothing's left to do:
    - `findRecordsNotUsingCryptoKey(cryptoKey)` — for `KEY_ON` rekeys: a batch of records *not* yet on the target key.
    - `findRecordsUsingCryptoKey(cryptoKey)` — for `KEY_OFF` rekeys: a batch of records still on the key being retired.
    - `save(records)` — persist the batch after the library has already re-encrypted/re-hashed it in memory.
    - `notify(rekeyEvent)` — an optional hook for reacting to lifecycle events, e.g. a rekey finishing, or old HMACs becoming safe to purge.
- **A `RekeyScheduler`**, configured once with these plus timing settings (how often to check for pending rekeys, batching pace, etc.).

From there, setting a `CryptoKey`'s `rekeyMode` to `KEY_ON` or `KEY_OFF` is what actually triggers a rekey the next time the scheduler runs — no per-rekey code needed once the plumbing above exists. HMAC rekeying specifically is only supported for entities using the List HMAC Strategy. A later workshop stage walks through implementing this for a real entity.
