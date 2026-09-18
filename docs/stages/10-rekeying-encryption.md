Rekeying: Encryption

!!! abstract "Overview"
    Key Rotation switched new writes to a new key but left existing data under the old one. This stage is phase 2: sweeping existing records, decrypting under their recorded key and re-encrypting under the current one. Facilitators should pair this with [Rekeying: Encryption](../talk/rekeying-encryption.md), and be explicit that this stage covers ciphertext only — HMACs are rekeyed separately, next, because they're a distinct operation with a different retirement rule for the old key.

!!! info "Content coming soon"
    This stage is a placeholder. It should give learners:

    - A sweep/batch job over existing records that decrypts each with its recorded `cryptoKeyId` and re-encrypts with the current key
    - Confirmation that the old key must stay resolvable throughout the sweep (nothing about the sweep itself removes the old key from the provider)
    - A record-level check for "already on the current key" so the sweep is safely re-runnable/idempotent
    - A forward note that removing the old key entirely is a later, separate decision, not part of this stage

    Likely a starter/complete stage: the sweep loop is a natural TODO exercise.
