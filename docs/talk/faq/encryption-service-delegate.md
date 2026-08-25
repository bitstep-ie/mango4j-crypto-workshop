What's an Encryption Service Delegate?

mango4j-crypto's pluggable abstraction over an actual cryptographic provider. The framework ships several built in — production ones like PBKDF2, wrapped-key, and AWS KMS delegates, and test-only ones like Base64 and Identity — and applications can supply their own by subclassing `EncryptionServiceDelegate`. A `CryptoKey`'s `type` field is what tells the framework which delegate to route a given key's operations through.
