Why do I need to HMAC data to make it searchable?

Because ciphertext is never the same twice (see [What's an IV, and why does it matter?](iv.md)), you can't search on it directly. A HMAC of the same value, computed with the same key, is always the same — so you store a HMAC alongside the ciphertext, and search by hashing the search term and matching against stored HMACs, the same way you'd match against a regular indexed column. See [Introducing HMACs](../introducing-hmacs.md).
