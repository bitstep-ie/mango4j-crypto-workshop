# 3. Plain Ciphertext → Structured Ciphertext

*Content coming soon.*

- Starting point: the naive approach — encrypting with just the ciphertext, nothing else
- The problem: an opaque blob carries no information about which key/provider/version encrypted it, so there's nowhere to put a key alias, and no way to know which key to decrypt with later
- Naive attempts to work around it: bolting the key alias on alongside the ciphertext as a separate field; a single HMAC field for search, stored the same ad hoc way; cryptographic code scattered across the codebase with no shared structure
- The fix: a structured ciphertext format that carries metadata (e.g. key alias, key version, provider) alongside the raw ciphertext, in one place
- Why this structure is the foundation that key rotation (Chapter 5) and rekeying (Chapters 8–9) depend on
