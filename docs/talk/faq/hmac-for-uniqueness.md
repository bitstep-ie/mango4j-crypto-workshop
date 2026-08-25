Why do I need to HMAC data to enforce a unique constraint?

Same root cause as search: a database unique constraint on a ciphertext column is useless, because two records holding the identical plaintext value never produce identical ciphertext. Put the constraint on a HMAC column instead — the same plaintext always produces the same HMAC, so a real duplicate produces a real HMAC collision the database can catch. See [Introducing HMACs](../introducing-hmacs.md).
