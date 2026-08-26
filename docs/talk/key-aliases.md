Key Aliases → Crypto Key Configs

## The limits of representing a key as a string

It's tempting to represent an encryption key in your code the way you'd represent almost anything else identifying a resource: a plain string. An AWS KMS key ARN, an HSM slot label, a raw key ID — just a string your encrypt/decrypt calls pass around. That's workable until you need to do things like:

- Switch which cryptographic provider a key uses (AWS KMS today, an HSM tomorrow)
- Run multiple providers side by side (different regions, different regulatory regimes)
- Rotate a key without touching a single line of application code

A raw string can't carry any of that. It's just an opaque label — there's nothing in it saying *how* to use it.

## A key as an object, not a string

A more durable approach is to represent every key as a small object rather than a bare identifier — something carrying not just an ID, but what the key is used for (encryption vs. HMAC), which cryptographic provider/mechanism should handle it, and whatever configuration that provider needs to actually perform the operation (a reference to where the key lives, never the raw key material itself).

That last split — provider type separate from provider-specific configuration — allows application code to ask to encrypt with a key rather than directly call a particular provider. An implementation matches the key's declared type to the delegate that knows how to handle it. Adding or changing a provider may then be a configuration and integration change rather than a change to business logic.

## The alias: how application code actually asks for a key

Application code doesn't hardcode a specific key's ID either — that would just move the stringly-typed problem up one layer, from "which provider" to "which specific key." Instead, the application answers a small set of *role*-based questions on demand: which key is currently active for new encryption, which keys are currently active for HMACs (plural — more on why in [Single HMAC Strategy](single-hmac.md) and [List HMAC Strategy](list-hmac.md)), and how to resolve any key by ID regardless of whether it's still "current."

This is the alias indirection: "the current encryption key" is a question answered by the component that owns key configuration, not a value embedded in a call site. Updating that configuration directs future writes to the new key, subject to the application's key-refresh and deployment behaviour.

## Why this is the foundation for everything that follows

This indirection — alias in, concrete key config out — is what makes the rest of the talk possible:

- A [structured ciphertext](structured-ciphertext.md) can record *which* key encrypted it, and that alias resolution is what turns that record back into a usable key at decrypt time.
- [Key rotation](key-rotation.md) is just changing what "the current key" resolves to. Old ciphertext still decrypts correctly, because the resolution mechanism still knows about old keys, not just the current one.
- The new key can use a different provider from the old one when the required delegate and configuration are available. Business logic can remain provider-agnostic because it refers to the key role rather than the provider directly.
