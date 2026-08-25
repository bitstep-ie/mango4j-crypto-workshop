# 4. Transient vs. Encrypted Blobs

*Content coming soon.*

- Distinguishing the transient (in-memory, plaintext or working) representation of a value from its encrypted-blob (persisted) representation
- The failure mode this guards against: redundant encrypt/decrypt passes on the same data as it moves through the system
- How keeping the two representations distinct ensures data is encrypted once and decrypted once per boundary crossing, never twice or more
- The failure mode when you don't do this — encrypting fields directly into their columns, with no distinct representations:
    - No single source of truth for "what's encrypted"
    - Schema churn every time a new field needs protecting
    - No consistent record of which key encrypted what
    - Every query/repository touching that column needs bespoke logic
