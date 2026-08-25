# 8. Introducing HMACs: The Single HMAC Strategy

*Content coming soon.*

- Why encrypted values can't be searched or checked for uniqueness directly (IVs make ciphertext non-deterministic — see Chapter 1)
- Introducing a single HMAC column alongside the ciphertext, computed deterministically from the plaintext, as the thing you actually search and constrain on
- Where the single HMAC strategy breaks down:
    - Search: during key rotation, rows hashed under the old key no longer match a search term hashed under the new key
    - Unique constraints: especially problematic — a value can silently violate (or fail to enforce) uniqueness across a key rotation boundary, since the same plaintext produces two different HMACs under the two keys
