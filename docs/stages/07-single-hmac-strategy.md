Single HMAC Strategy

!!! abstract "Overview"
    With an HMAC column in place, this stage puts it to work: searching for a record by plaintext value, and enforcing a uniqueness constraint, both via an equality lookup against the single HMAC column. Facilitators should walk through [Single HMAC Strategy](../talk/single-hmac.md) first, especially the unique-constraint failure mode it flags: because the strategy stores exactly one HMAC per record, a value's HMAC changes the moment its key rotates, and enforcement anomalies follow directly from that.

!!! info "Content coming soon"
    This stage is a placeholder. It should give learners:

    - A search function that HMACs the search term and looks up by equality against the stored HMAC column
    - A uniqueness check (e.g. a unique DB constraint on the HMAC column) and a demonstration that it works under normal operation
    - A worked example (or forward reference) of the failure mode [Single HMAC Strategy](../talk/single-hmac.md) describes: a duplicate slipping in under a different key during rotation
    - A clear framing that this is the simpler of the two strategies, and List HMAC Strategy (next) exists specifically to fix what this one can't

    Probably reuses the entity from Introducing HMACs rather than introducing a new one — decide when drafting whether this stage needs its own starter/complete split or can stay plain.
