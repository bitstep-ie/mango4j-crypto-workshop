Is rekeying built into mango4j-crypto, or do I have to write it myself?

Built in (currently in beta). Configure a `RekeyCryptoKeyManager` and a `RekeyService` per entity, wire up a `RekeyScheduler`, and set a `CryptoKey`'s `rekeyMode` to `KEY_ON` or `KEY_OFF` to trigger a rekey the next time the scheduler runs — no additional per-rekey code required. HMAC rekeying specifically is only supported for entities using the List HMAC Strategy.
