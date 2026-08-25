# 1. What Is ALE, and Why Do We Still Need It?

Application Level Encryption (ALE) means your **application code** encrypts and decrypts specific pieces of confidential data itself, before that data is ever written to (or after it's read from) storage. The database, the disk, the backup system — none of them ever see the plaintext. As far as they're concerned, a confidential field is just an opaque blob of ciphertext.

This is a deliberate contrast with encryption that happens *underneath* your application, at the infrastructure layer.

## ALE versus TDE / disk encryption

Transparent Data Encryption (TDE) and disk/volume encryption protect data **at rest** — the bytes sitting on a physical or virtual disk. They're valuable, but they solve a narrow problem: someone stealing a disk, a decommissioned drive, or an unencrypted backup file. The moment there's an authenticated connection to the database — a valid DB credential, a live query, a DBA doing routine maintenance — TDE decrypts everything transparently and hands back plaintext. That's the point of the "T" in TDE.

ALE protects against a much wider threat model, because the plaintext simply never reaches the database layer at all:

- A leaked or overly broad database credential
- A SQL injection vulnerability
- A well-meaning DBA (or a compromised one) running an ad hoc query
- A backup or replica that ends up somewhere it shouldn't
- An application bug that logs a raw query

None of these expose confidential data if the value was already ciphertext before it left your application. TDE and ALE aren't competing choices — most serious systems use both — but only ALE addresses this category of risk.

## Where ALE sits in the application stack

ALE lives in the application/service layer, as close as possible to the boundary where data crosses from your business logic into persistence:

```
Client → API → Business logic → ALE (encrypt/decrypt) → ORM / Repository → Database
```

Your business logic still works with plaintext values — a `cardNumber`, an `email` — because that's what your domain rules, validation, and application behavior actually need. The encryption step sits right before that value is handed off to be persisted, and the reverse (decryption) happens right after a record is loaded back. From the database's point of view, nothing has changed: it's still storing strings and blobs. It just never has the key, and never sees the plaintext.

## Key terms: tenant, CryptoKey, ciphertext, HMAC, IV

A handful of terms come up constantly when discussing ALE — worth having a shared, precise vocabulary before going further:

**Tenant**
:   A logical isolation boundary for a distinct customer or client entity in your system, each with its own encryption/HMAC keys — so a data breach or key compromise affecting one tenant can't expose another's data. If your application doesn't have the concept of multiple customers, you can just think of your whole application as "the tenant."

**CryptoKey**
:   A key represented as an *object* in your code — not a raw string — carrying the key's identity, what it's used for, and which cryptographic provider/mechanism should handle it. This is what lets you change *how* encryption is actually performed (a different KMS, a different algorithm, a different provider) without touching the application code that calls `encrypt()`.

**Ciphertext**
:   The encrypted output. Irreversible without the correct key — that's the entire point.

**HMAC**
:   A hash computed using a secret key. Unlike encryption, a HMAC is *deterministic*: the same input with the same key always produces the same output. That property is exactly what makes HMACs — not encrypted values — the thing you actually search and enforce uniqueness on. More on why in a later chapter.

**IV (Initialization Vector)**
:   Randomness fed into an encryption operation so that encrypting the same value twice never produces the same ciphertext. This is essential for security (it defeats pattern analysis on your data), but it has a direct consequence: you can never find a record by encrypting a search term and comparing ciphertext, because the ciphertext is different every time. That's exactly the gap HMACs exist to fill.

## Why do we still need it?

Encrypting things is extra engineering effort, so it's fair to ask why it's worth doing. The short answer: because most real-world data breaches don't involve anyone stealing a physical disk — they involve someone getting access to a live, running system, and the database sitting right there in plaintext.

### Regulatory and compliance drivers

Plenty of regulations and standards effectively require it for certain categories of data: PCI-DSS for card numbers (like the `cardNumber` field this workshop encrypts), HIPAA for health information, GDPR and similar privacy laws for personal data more broadly. These frameworks don't always say "you must implement Application Level Encryption" in those exact words, but they require controls that are, in practice, very hard to satisfy without it — data minimization, breach notification thresholds that change based on whether exposed data was encrypted, and demonstrable protection of data even from your own staff. Auditors increasingly ask specifically whether confidential fields are encrypted *before* they reach the database, not just whether the disk is encrypted.

### Reducing blast radius when a database or backup leaks

Every additional place plaintext data exists is another place it can leak from. With ALE, a leaked database dump, an exposed backup, a misconfigured replica, or a stolen snapshot all contain nothing but ciphertext — useless without the encryption keys, which live somewhere else entirely (a KMS, an HSM, a secrets manager). This is what "reducing blast radius" means in practice: a breach that would otherwise have exposed every customer's data becomes a non-event, because the thing that leaked was never usable on its own.

### Tenant / customer data segregation requirements

If your application serves multiple distinct customers or organizations — banks, enterprises, whoever — each one is a **tenant**, and it's common (and sometimes contractually required) that each tenant's confidential data be cryptographically isolated from every other tenant's. That means each tenant needs its own encryption keys, not just its own database rows. Done properly, this means a key compromise, a legal data request, or a "right to be forgotten" deletion for one tenant can be handled without touching any other tenant's data at all.

### Why "encryption at rest" alone isn't enough

As covered above, disk/volume encryption (TDE) only protects against someone stealing the physical media. The moment there's a valid, authenticated connection to the database — which describes almost every real attack path: a leaked credential, a SQL injection, an over-privileged service account, a compromised internal tool — TDE decrypts everything and hands back plaintext without a second thought. It solves a real but narrow problem. It does nothing for the much more common scenario where the *application's own access* is what's abused. ALE is the layer that still holds even when the database itself is fully compromised, because the database was never trusted with the plaintext in the first place.

## Introducing mango4j-crypto

*Content coming soon.*

The rest of this talk demonstrates the concepts above — key aliases, structured ciphertext, key rotation, HMAC strategies, rekeying — as implemented by mango4j-crypto, the framework this workshop is built around:

- Key-driven design: keys as objects, not strings
- `CryptoShield` and annotations as the single source of truth for what's encrypted
- Pluggable encryption service delegates
- Single, Double, and List HMAC strategies
- Built-in rekeying support
- `@EnableMigrationSupport` for legacy unencrypted fields
