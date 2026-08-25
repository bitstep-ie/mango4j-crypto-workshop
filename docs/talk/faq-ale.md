ALE FAQ

Quick answers to questions that come up repeatedly when discussing Application Level Encryption in general, independent of any particular framework.

- [What's a hash, and how is that different from encryption?](faq/hash-vs-encryption.md)
- [What's an IV, and why does it matter?](faq/iv.md)
- [Why do I need to HMAC data to make it searchable?](faq/hmac-for-search.md)
- [Why do I need to HMAC data to enforce a unique constraint?](faq/hmac-for-uniqueness.md)
- [What's the difference between key rotation and rekeying?](faq/rotation-vs-rekeying.md)
- [Why would a key ever need to rotate?](faq/why-rotate.md)
- [Why can there be only one active encryption key at a time, but potentially many active HMAC keys?](faq/one-encryption-key-many-hmac-keys.md)
- [If HMACs don't carry a reference to their own key, how does rekeying know what to rekey?](faq/hmac-key-id-rekeying.md)
- [What is HMAC tokenization / derived-value search?](faq/hmac-tokenization.md)
- [What's a tenant?](faq/tenant.md)
- [HMAC is deterministic by definition — does choosing a storage strategy change that?](faq/hmac-determinism.md)
