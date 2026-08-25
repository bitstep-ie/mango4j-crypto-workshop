# 6. Key Rotation, Phase 1: Switching to a New Key

*Content coming soon.*

- Pointing the alias used for new writes at a new crypto key config
- The new key can be from a completely different provider than the old one — the structured ciphertext format (Chapter 3) and the alias indirection (Chapter 2) are what make that possible
- Old keys continue to work for decryption, so existing data written under the old key keeps reading back correctly with no migration required yet
