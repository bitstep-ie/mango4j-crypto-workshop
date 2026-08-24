# 2. Why Do We Need It?

Encrypting things is extra engineering effort, so it's fair to ask why it's worth doing. The short answer: because most real-world data breaches don't involve anyone stealing a physical disk — they involve someone getting access to a live, running system, and the database sitting right there in plaintext.

## Regulatory and compliance drivers

Plenty of regulations and standards effectively require it for certain categories of data: PCI-DSS for card numbers (like the `cardNumber` field this workshop encrypts), HIPAA for health information, GDPR and similar privacy laws for personal data more broadly. These frameworks don't always say "you must implement Application Level Encryption" in those exact words, but they require controls that are, in practice, very hard to satisfy without it — data minimization, breach notification thresholds that change based on whether exposed data was encrypted, and demonstrable protection of data even from your own staff. Auditors increasingly ask specifically whether confidential fields are encrypted *before* they reach the database, not just whether the disk is encrypted.

## Reducing blast radius when a database or backup leaks

Every additional place plaintext data exists is another place it can leak from. With ALE, a leaked database dump, an exposed backup, a misconfigured replica, or a stolen snapshot all contain nothing but ciphertext — useless without the encryption keys, which live somewhere else entirely (a KMS, an HSM, a secrets manager). This is what "reducing blast radius" means in practice: a breach that would otherwise be catastrophic (every customer's data, exposed) becomes a non-event, because the thing that leaked was never usable on its own.

## Tenant / customer data segregation requirements

If your application serves multiple distinct customers or organizations — banks, enterprises, whoever — each one is a **tenant**, and it's common (and sometimes contractually required) that each tenant's confidential data be cryptographically isolated from every other tenant's. That means each tenant needs its own encryption keys, not just its own database rows. Done properly, this means a key compromise, a legal data request, or a "right to be forgotten" deletion for one tenant can be handled without touching any other tenant's data at all. If your application only has one "customer" — itself — you can just think of the whole application as a single tenant, but the same principle still applies: the keys are a first-class concept, not an afterthought.

## Why "encryption at rest" alone isn't enough

As covered in [Chapter 1](01-what-is-ale.md), disk/volume encryption (TDE) only protects against someone stealing the physical media. The moment there's a valid, authenticated connection to the database — which describes almost every real attack path: a leaked credential, a SQL injection, an over-privileged service account, a compromised internal tool — TDE decrypts everything and hands back plaintext without a second thought. It solves a real but narrow problem. It does nothing for the much more common scenario where the *application's own access* is what's abused. ALE is the layer that still holds even when the database itself is fully compromised, because the database was never trusted with the plaintext in the first place.
