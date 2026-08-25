# 3. Plain Ciphertext → Structured Ciphertext

*Content coming soon.*

- Starting point: a naive ciphertext is just an opaque blob of bytes
- The problem: an opaque blob carries no information about which key/provider/version encrypted it
- The fix: a structured ciphertext format that carries metadata (e.g. key alias, key version, provider) alongside the raw ciphertext
- Why this structure is the foundation that key rotation (Chapter 5) and rekeying (Chapters 8–9) depend on
