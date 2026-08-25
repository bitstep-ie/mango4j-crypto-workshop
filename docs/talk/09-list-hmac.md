# 9. The List HMAC Strategy

*Content coming soon.*

- Storing a list of HMACs per value (e.g. one per active key) instead of a single HMAC
- How this fixes search across a key rotation: a search term is hashed under every active key and matched against the list
- How this fixes unique constraints: uniqueness can be enforced against all HMACs a value could produce under any currently-valid key, not just the latest one
- Trade-offs versus the single HMAC strategy from Chapter 8 (storage, constraint complexity)
