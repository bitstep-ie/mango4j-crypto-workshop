# 5. Encryption Key Rotation

*Content coming soon.*

- Why keys need to rotate: age, suspected compromise, compliance schedules, provider migration
- Phase 1 — switching new writes to a new key:
    - Pointing the alias used for new writes at a new crypto key config
    - The new key can be from a completely different provider than the old one — the structured ciphertext format (Chapter 3) and the alias indirection (Chapter 2) are what make that possible
    - Old keys continue to work for decryption, so existing data written under the old key keeps reading back correctly with no migration required yet
- Phase 2 — migrating old data to the new key:
    - Once new writes are on the new key, old rows are still encrypted under the old key
    - Migrating/rekeying existing data to the new key — the actual rekey mechanics are covered in Chapters 8–9
    - Why this happens as a later, separate pass rather than a single atomic switch
- The related failure mode: migrating an unencrypted field to encrypted (same shape of problem as phase 2, starting from no encryption at all instead of an old key):
    - Why you can't just "turn on" encryption for an existing column
    - Backfilling millions of rows without downtime
    - Dual-read/dual-write periods and their own bugs
    - The feature freeze nobody wants to announce
- When rotation/migration goes wrong in production — outages and application failure:
    - Search functionality silently degrading in production
    - Duplicate "unique" records corrupting business data
    - Emergency rollbacks and the risks they introduce
    - The incident review nobody wants to present
