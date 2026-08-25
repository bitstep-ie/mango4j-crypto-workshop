# 2. Key Aliases → Crypto Key Configs

## Strings are a trap

It's tempting to represent an encryption key in your code the way you'd represent almost anything else identifying a resource: a `String`. An AWS KMS key ARN, an HSM slot label, a raw key ID — just a string your encrypt/decrypt calls pass around. It works, right up until you need to do any of the things mango4j-crypto's **Key Driven Design** is built to handle:

- Switch which cryptographic provider a key uses (AWS KMS today, an HSM tomorrow)
- Run multiple providers side by side (different regions, different regulatory regimes)
- Rotate a key without touching a single line of application code

A raw string can't carry any of that. It's just an opaque label — there's nothing in it saying *how* to use it.

## CryptoKey: a key as an object, not a string

mango4j-crypto represents every key as a `CryptoKey` object:

```java
public class CryptoKey {
    private String id;
    private CryptoKeyUsage usage;
    private String type;
    private Map<String, Object> configuration;
    private Instant keyStartTime;
    private RekeyMode rekeyMode;
    private Instant createdDate;
    private Instant lastModifiedDate;
}
```

- **`id`** — a plain random GUID identifying this key
- **`usage`** — what the key is for: `ENCRYPTION` or `HMAC`
- **`type`** — which Encryption Service Delegate should handle this key's cryptographic operations (must match that delegate's `supportedCryptoKeyType()`)
- **`configuration`** — a `Map` of whatever information the delegate needs to do its job (e.g. an AWS KMS key ARN under a `keyArn` entry) — never the raw key bytes themselves, just a reference to where the key lives
- **`keyStartTime`** — optional, HMAC keys only; used to smooth over a caching gap in the Single HMAC Strategy (Chapter 6)
- **`rekeyMode`** — drives the built-in rekey job (Chapters 8–9)

The `type`/`configuration` split is the whole point: application code never says "call AWS KMS." It says "encrypt with this `CryptoKey`," and the library looks up the matching **Encryption Service Delegate** by `type` to do the actual cryptographic work. Swap the delegate, or introduce a new one, and no application code changes at all.

## The alias: how application code actually asks for a key

Application code doesn't hardcode a `CryptoKey.id` either — that would just move the stringly-typed problem up one layer. Instead, you implement `CryptoKeyProvider`, and mango4j-crypto asks *it* for whichever key it needs by role, not by ID:

```java
@Component
public class ApplicationCryptoKeyProvider implements CryptoKeyProvider {

    @Override
    public CryptoKey getById(String id) { ... }

    @Override
    public CryptoKey getCurrentEncryptionKey() { ... }

    @Override
    public List<CryptoKey> getCurrentHmacKeys() { ... }

    @Override
    public List<CryptoKey> getAllCryptoKeys() { ... }
}
```

- `getCurrentEncryptionKey()` — the key `CryptoShield.encrypt()` should use *right now* for new writes
- `getCurrentHmacKeys()` — every HMAC key currently in use (plural — more on why in Chapters 6–7)
- `getById(id)` — resolves any key, by ID, regardless of whether it's still "current" — this is what makes decryption of old data work after a rotation

This is the alias indirection: "the current encryption key" is a question your `CryptoKeyProvider` answers dynamically, not a string baked into a config file or a call site. Change what it answers, and every future write picks up the new key config automatically.

## Why this is the foundation for everything that follows

This indirection — alias in, `CryptoKey` config out — is what makes the rest of the talk possible:

- A structured ciphertext (Chapter 3) can record *which* key ID encrypted it, and `getById()` resolves it back to a concrete config at decrypt time
- Key rotation (Chapter 5) is just changing what `getCurrentEncryptionKey()` returns — old ciphertext still decrypts because `getById()` still knows about the old key
- The new key can be a completely different provider (`type`) from the old one, with zero application code changes, because nothing in the application ever referenced the provider directly
