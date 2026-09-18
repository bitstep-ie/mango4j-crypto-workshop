List HMAC Strategy

!!! abstract "Overview"
    This stage replaces the single HMAC column from the previous stage with a list of HMACs per record, one per active/known key, fixing the rotation-time uniqueness gap that motivated it. Facilitators should lead with [List HMAC Strategy](../talk/list-hmac.md)'s core rule: updates must *replace* the list of HMACs for a value, never just append to it, or old entries make a value findable under a stale value forever (the exact pitfall `talk/naive-list-hmac/` demonstrates).

!!! info "Content coming soon"
    This stage is a placeholder. It should give learners:

    - The single HMAC column replaced by a list-valued HMAC store (one entry per known HMAC key)
    - A search function that checks the search term's HMAC under any known key, not just the current one
    - A uniqueness check across the full list, showing it doesn't have Single HMAC Strategy's rotation gap
    - An update path exercise that deliberately breaks the append-only version first (mirroring `talk/naive-list-hmac/`) before fixing it to replace-on-update

    Consider whether this should be a starter/complete stage specifically so the append-vs-replace bug is something learners fix themselves, not just read about.
