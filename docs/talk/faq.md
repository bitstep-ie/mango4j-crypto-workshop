Frequently Asked Questions

Quick answers to questions that come up repeatedly when discussing ALE. Each one links to the chapter that covers the full reasoning.

## What's a hash, and how is that different from encryption?

A hash is one-way: given a value, hashing it produces an output, but that output can never be reversed back into the original value — there's no key that decrypts it, because it was never "encrypted" in the first place. Encryption is two-way: the same key that produced the ciphertext can also recover the original plaintext from it. A **HMAC** (Hash-based Message Authentication Code) is a hash computed with a secret key added into the mix — still one-way, but only someone with that key can compute the correct HMAC for a given value. See [Introducing HMACs](introducing-hmacs.md).

## HMAC is deterministic by definition — does any of this change that?

No. Determinism (same input, same key, always the same output) is a property of the HMAC algorithm itself — HMAC-SHA256 or whichever construction is in use — not something a particular design or framework adds or removes. A given (value, key) pair always produces the same HMAC, full stop. What varies between strategies isn't whether a HMAC is deterministic — it's *how many* HMACs get computed and stored for a value, and under how many different keys. The [List HMAC Strategy](list-hmac.md) stores one HMAC per active key rather than one HMAC overall, but each of those individual HMACs is still exactly as deterministic as it always was — you're just keeping several deterministic answers to "what's the HMAC of this value under key K", one per key, side by side. See [Introducing HMACs](introducing-hmacs.md).

## What's an IV, and why does it matter?

An IV (Initialization Vector) is randomness mixed into an encryption operation so that encrypting the same value twice never produces the same ciphertext, even with the same key. It travels alongside the ciphertext (in the clear — it isn't secret) so decryption can reverse the operation. This is good for security, but it has a direct consequence: you can't search for a record by re-encrypting a search term and comparing ciphertext, because the ciphertext is different every time. See [What Is ALE, and Why Do We Still Need It?](intro.md) and [Structured Ciphertext](structured-ciphertext.md).

## Why do I need to HMAC data to make it searchable?

Because ciphertext is never the same twice (see the IV question above), you can't search on it directly. A HMAC of the same value, computed with the same key, is always the same — so you store a HMAC alongside the ciphertext, and search by hashing the search term and matching against stored HMACs, the same way you'd match against a regular indexed column. See [Introducing HMACs](introducing-hmacs.md).

## Why do I need to HMAC data to enforce a unique constraint?

Same root cause: a database unique constraint on a ciphertext column is useless, because two records holding the identical plaintext value never produce identical ciphertext. Put the constraint on a HMAC column instead — the same plaintext always produces the same HMAC, so a real duplicate produces a real HMAC collision the database can catch. See [Introducing HMACs](introducing-hmacs.md).

## What's the difference between key rotation and rekeying?

Rotation is switching which key gets used for *new* writes going forward — it doesn't touch anything already written. Rekeying is the separate background process that goes back and moves *existing* records off an old key onto the new one, which is what eventually lets the old key be deleted. Rotation is quick and low-risk; rekeying is the slower, higher-effort part. See [Key Rotation](key-rotation.md), [Rekeying: Encryption](rekeying-encryption.md), and [Rekeying: HMACs](rekeying-hmacs.md).

## Why would a key ever need to rotate?

Common triggers: a compliance schedule requiring keys to age out after a fixed period, a cryptographic provider being deprecated or becoming unavailable in some region, or — the urgent case — a key suspected of being compromised, which needs to stop being trusted immediately rather than on a schedule. See [Key Rotation](key-rotation.md).

## Why can there be only one active encryption key at a time, but potentially many active HMAC keys?

Decrypting a record is only possible once you already have that record — its ciphertext carries the ID of the key that encrypted it, so decryption always knows exactly which key to use, regardless of how many keys have existed over time. Searching is the opposite problem: you don't have the record yet, that's what you're trying to find, so you don't know in advance which key was used to HMAC it. The only way to find it is to try every key that might have been used — which means every key still holding un-rekeyed data has to stay active for search, not just the newest one. See [Introducing HMACs](introducing-hmacs.md) and [List HMAC Strategy](list-hmac.md).

## If HMACs don't carry a reference to their own key, how does rekeying know what to rekey?

By the time you've found a record via search, you already know which row it is — so a stored key reference on the HMAC itself wouldn't help you find it any faster. But it's still useful for a different reason: a rekey process needs to efficiently ask "which records still have a HMAC from this old key?" without decrypting everything to check. Storing the HMAC key ID alongside each HMAC (purely as a rekeying convenience, not something search itself depends on) is what makes that query cheap. See [Rekeying: HMACs](rekeying-hmacs.md).

## What is HMAC tokenization / derived-value search?

Alongside the HMAC of a full value, you can also store HMACs of *derived* representations of it — the last four digits of a card number, a normalized form with punctuation stripped — enabling partial-match search without ever weakening or exposing the underlying encrypted value. See [List HMAC Strategy](list-hmac.md).

## What's a tenant?

A logical isolation boundary for a distinct customer or client entity in a system, each with its own encryption and HMAC keys, so a breach or key compromise affecting one tenant can't expose another's data. If an application doesn't have the concept of multiple customers, the whole application can just be thought of as a single tenant. See [What Is ALE, and Why Do We Still Need It?](intro.md).
