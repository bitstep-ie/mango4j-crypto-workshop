# 2. Key Aliases → Crypto Key Configs

## The limits of representing a key as a string

It's tempting to represent an encryption key in your code the way you'd represent almost anything else identifying a resource: a `String`. An AWS KMS key ARN, an HSM slot label, a raw key ID — just a string your encrypt/decrypt calls pass around. That's workable until you need to do any of the things mango4j-crypto's **Key Driven Design** is built to handle:

- Switch which cryptographic provider a key uses (AWS KMS today, an HSM tomorrow)
- Run multiple providers side by side (different regions, different regulatory regimes)
- Rotate a key without touching a single line of application code

A raw string can't carry any of that. It's just an opaque label — there's nothing in it saying *how* to use it.

## A key as an object, not a string

mango4j-crypto's answer is to represent every key as a small object rather than a bare identifier — something carrying not just an ID, but what the key is used for (encryption vs. HMAC), which cryptographic provider/mechanism should handle it, and whatever configuration that provider needs to actually perform the operation (a reference to where the key lives, never the raw key material itself).

That last split — provider type separate from provider-specific configuration — is the whole point. Application code never says "call AWS KMS." It says "encrypt with this key," and underneath, the library matches the key's declared type to whichever implementation knows how to handle that type. Swap the implementation, or introduce a new one entirely, and no application code changes at all.

## The alias: how application code actually asks for a key

Application code doesn't hardcode a specific key's ID either — that would just move the stringly-typed problem up one layer, from "which provider" to "which specific key." Instead, the application answers a small set of *role*-based questions on demand: which key is currently active for new encryption, which keys are currently active for HMACs (plural — more on why in Chapters 6–7), and how to resolve any key by ID regardless of whether it's still "current."

This is the alias indirection: "the current encryption key" is a question answered dynamically by whatever component owns key configuration, not a string baked into a config file or a call site. Change what it answers, and every future write picks up the new key config automatically — nothing else in the application needs to know a change happened at all.

## Why this is the foundation for everything that follows

This indirection — alias in, concrete key config out — is what makes the rest of the talk possible:

- A structured ciphertext (Chapter 3) can record *which* key encrypted it, and that alias resolution is what turns that record back into a usable key at decrypt time.
- Key rotation (Chapter 5) is just changing what "the current key" resolves to. Old ciphertext still decrypts correctly, because the resolution mechanism still knows about old keys, not just the current one.
- The new key can be a completely different provider from the old one, with zero application code changes, because nothing in the application ever referenced the provider directly — only the alias.
