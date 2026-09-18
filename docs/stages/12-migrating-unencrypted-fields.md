Migrating Unencrypted Fields

!!! abstract "Overview"
    Every previous stage started from a field that was already encrypted. This stage covers the other direction: an existing plaintext field in production, backfilled to encrypted while the application keeps running. Facilitators should open with the naive failure mode `talk/naive-migration/` demonstrates — a table mid-backfill has rows in both states at once, and a `load()` that assumes everything's already ciphertext throws on the rows the backfill hasn't reached — before showing mango4j-crypto's migration support as the fix.

!!! info "Content coming soon"
    This stage is a placeholder. It should give learners:

    - A tracked-migration flag per record (mirroring `@EnableMigrationSupport`) so `load()` can tell which representation a given row is in
    - A backfill job that encrypts plaintext rows in place and flips the flag, safe to run incrementally/resumably
    - Confirmation the application keeps serving reads/writes correctly against both migrated and not-yet-migrated rows throughout the backfill
    - A closing note on cutover: what changes once the backfill is complete (the flag/branch can eventually be removed)

    Last stage in the "full framework usage" arc — reasonable place to also add a short closing/wrap-up page once all stages exist, similar to how the talk's Rekeying: HMACs closes out that arc.
