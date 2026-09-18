Rekeying: HMACs

!!! abstract "Overview"
    The final stage of the rotation story: regenerating HMACs under the new key and safely retiring the old one. Facilitators should lead with [Rekeying: HMACs](../talk/rekeying-hmacs.md)'s teardown ordering rule: removing the old HMAC key from the active list before every record has been swept makes unswept records silently unfindable in search, exactly what `talk/naive-hmac-rekey/` demonstrates.

!!! info "Content coming soon"
    This stage is a placeholder. It should give learners:

    - A sweep that regenerates each record's HMAC list entry for the new key, additive alongside old entries until the sweep completes (this is the List HMAC Strategy append behavior, used correctly here — unlike the single-HMAC/list-HMAC append-vs-replace pitfall from List HMAC Strategy)
    - Explicit ordering: only remove the old key from the active-keys list after the sweep has reached every record
    - A demonstration (or forward reference to `talk/naive-hmac-rekey/`) of what breaks if that ordering is violated
    - A closing note tying Key Rotation, Rekeying: Encryption, and this stage together as the complete rotation story

    Depends on List HMAC Strategy landing first, since the active-keys list and append semantics are introduced there.
