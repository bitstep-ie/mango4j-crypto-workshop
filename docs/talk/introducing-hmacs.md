Introducing HMACs

## Why you can't search or constrain on the ciphertext itself

[What Is ALE, and Why Do We Still Need It?](intro.md) introduced IV/nonce handling. With the usual randomized or nonce-based encryption schemes, encrypting the same value twice produces different ciphertext. As a result, equality search and a unique constraint cannot normally operate on ciphertext values themselves.

## What a HMAC is

**HMAC** (Hash-based Message Authentication Code) fills that gap. Unlike encryption, a HMAC — a hash computed using a secret key — is *deterministic*: the same input with the same key always produces the same output, every time. That determinism is exactly what makes HMACs — not encrypted values — the thing you actually search and enforce uniqueness on. A HMAC isn't reversible the way encryption is; you can't recover the original value from it. All it tells you is whether some other value, hashed with the same key, matches.

## Why you'd want one: search

If a field is encrypted, finding a record by that field's value is not normally possible by comparing ciphertext (see above). One approach is to store a HMAC of the plaintext alongside the ciphertext. To search, compute the HMAC of the normalized search term with the same key and look for a match. This supports equality lookup without storing the plaintext, but it does reveal that matching stored values are equal and requires deliberate normalization, access control, and key management.

## Why you'd want one: unique constraints

The same determinism solves a second, related problem: enforcing uniqueness on an encrypted field — a username or email address, say — at the database level. A unique constraint on the ciphertext column itself is useless, since encrypting the same value twice produces two different ciphertexts. Put the constraint on the HMAC column instead: since the same plaintext always produces the same HMAC, two records with the same underlying value produce a HMAC collision, and the database's own unique constraint catches it — just as if the field had never been encrypted at all.

## Choosing a strategy

Storing HMACs sounds simple in isolation, but *how* they're stored and managed matters a great deal once [key rotation](key-rotation.md) enters the picture — the same HMAC key can't stay in use forever, and what happens to search and uniqueness while it changes depends heavily on the design chosen. The next chapter covers that; after it, the two main storage strategies: a single HMAC per field ([Single HMAC Strategy](single-hmac.md)), and a list of HMACs per field ([List HMAC Strategy](list-hmac.md)).
