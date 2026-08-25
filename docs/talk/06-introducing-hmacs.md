Introducing HMACs

## Why you can't search or constrain on the ciphertext itself

Chapter 1 introduced the IV: randomness fed into every encryption operation so encrypting the same value twice never produces the same ciphertext. That's essential for security, but it has a direct, unavoidable consequence — you can never find a record by encrypting a search term and comparing ciphertext, because the ciphertext is different every single time, even for the same input. The same problem applies to uniqueness: you can't put a database constraint directly on a ciphertext column, because two records holding the identical plaintext value will never produce identical ciphertext.

## What a HMAC is

**HMAC** (Hash-based Message Authentication Code) fills that gap. Unlike encryption, a HMAC — a hash computed using a secret key — is *deterministic*: the same input with the same key always produces the same output, every time. That determinism is exactly what makes HMACs — not encrypted values — the thing you actually search and enforce uniqueness on. A HMAC isn't reversible the way encryption is; you can't recover the original value from it. All it tells you is whether some other value, hashed with the same key, matches.

## Why you'd want one: search

If a field is encrypted, finding a record by that field's value isn't possible by comparing ciphertext (see above). The fix: alongside the ciphertext, store a HMAC of the plaintext value, computed with a secret key. To search, compute the HMAC of the search term with that same key and look for a match. Because the HMAC is deterministic, the same plaintext value always produces the same HMAC — so this works exactly like an index lookup on a regular column, just without ever exposing the plaintext itself in storage.

## Why you'd want one: unique constraints

The same determinism solves a second, related problem: enforcing uniqueness on an encrypted field — a username or email address, say — at the database level. A unique constraint on the ciphertext column itself is useless, since encrypting the same value twice produces two different ciphertexts. Put the constraint on the HMAC column instead: since the same plaintext always produces the same HMAC, two records with the same underlying value produce a HMAC collision, and the database's own unique constraint catches it — just as if the field had never been encrypted at all.

## Choosing a strategy

Storing HMACs sounds simple in isolation, but *how* they're stored and managed matters a great deal once key rotation (Chapter 5) enters the picture — the same HMAC key can't stay in use forever, and what happens to search and uniqueness while it changes depends heavily on the design chosen. The next two chapters cover the two main strategies: a single HMAC per field (Chapter 7), and a list of HMACs per field (Chapter 8).
