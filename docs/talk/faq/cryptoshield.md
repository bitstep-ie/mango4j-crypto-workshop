What's `CryptoShield`?

The central class applications interact with: `cryptoShield.encrypt(entity)` and `cryptoShield.decrypt(entity)`. It's configured once, at startup, with a `CryptoKeyProvider`, the list of annotated entity classes it should know about, and the Encryption Service Delegates available to it.
