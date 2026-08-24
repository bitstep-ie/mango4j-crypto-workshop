# The Talk: Why Application-Level Encryption Is Hard

!!! note "Placeholder"
    This page is a heading-only outline for the pre-workshop talk. Content for each section is still to be written.

## 1. What Is Application-Level Encryption?

### ALE vs. encryption at rest / TDE / disk encryption

*Content coming soon.*

### Where ALE sits in the application stack

*Content coming soon.*

### Key terms: tenant, CryptoKey, ciphertext, HMAC, IV

*Content coming soon.*

## 2. Why Do We Need It?

### Regulatory and compliance drivers

*Content coming soon.*

### Reducing blast radius when a database or backup leaks

*Content coming soon.*

### Tenant / customer data segregation requirements

*Content coming soon.*

### Why "encryption at rest" alone isn't enough

*Content coming soon.*

## 3. The Naive Approach: How Most Teams First Build This

### Encrypting each field directly into its own column

*Content coming soon.*

### Ad hoc cryptographic code scattered across the codebase

*Content coming soon.*

### Hardcoding a single cryptographic provider

*Content coming soon.*

### A HMAC-in-a-column for search and uniqueness

*Content coming soon.*

## 4. Failure Mode: Encrypting Fields Directly Into Their Columns

### No single source of truth for "what's encrypted"

*Content coming soon.*

### Schema churn every time a new field needs protecting

*Content coming soon.*

### No consistent record of which key encrypted what

*Content coming soon.*

### Every query/repository touching that column needs bespoke logic

*Content coming soon.*

## 5. Failure Mode: The Single HMAC Lookup Column

### Why encrypted values never search-match themselves (IVs)

*Content coming soon.*

### Why you need a HMAC at all, for search and for uniqueness

*Content coming soon.*

### The single-column HMAC design and its hidden assumptions

*Content coming soon.*

### The race condition that silently breaks unique constraints

*Content coming soon.*

### Why "search before you write" doesn't actually fix it

*Content coming soon.*

## 6. Failure Mode: Key Rotation and the Migration Fallout

### Encryption key rotation vs. HMAC key rotation — not the same problem

*Content coming soon.*

### What happens to existing data the moment you rotate

*Content coming soon.*

### The search outage nobody planned for

*Content coming soon.*

### Multi-instance applications and cached key information

*Content coming soon.*

### How a rotated HMAC key reintroduces duplicate "unique" records

*Content coming soon.*

### Rekeying: the background job you didn't budget for

*Content coming soon.*

## 7. Failure Mode: Locked Into One Encryption Scheme or Provider

### Hardcoding a KMS/HSM/algorithm choice into application code

*Content coming soon.*

### What happens when a provider changes, is deprecated, or isn't available in a region

*Content coming soon.*

### Regulatory-driven algorithm changes

*Content coming soon.*

### The cost of a "big bang" re-encryption project

*Content coming soon.*

## 8. Failure Mode: Migrating an Unencrypted Field to Encrypted

### Why you can't just "turn on" encryption for an existing column

*Content coming soon.*

### Backfilling millions of rows without downtime

*Content coming soon.*

### Dual-read/dual-write periods and their own bugs

*Content coming soon.*

### The feature freeze nobody wants to announce

*Content coming soon.*

## 9. When It All Comes Together: Outages and Application Failure

### Search functionality silently degrading in production

*Content coming soon.*

### Duplicate "unique" records corrupting business data

*Content coming soon.*

### Emergency rollbacks and the risks they introduce

*Content coming soon.*

### The incident review nobody wants to present

*Content coming soon.*

## 10. What "Doing It Right" Actually Requires

### Decoupling application code from cryptographic providers

*Content coming soon.*

### Designing for key rotation and rekeying from day one

*Content coming soon.*

### Choosing the right HMAC strategy for your uniqueness/search needs

*Content coming soon.*

### Understanding the unencrypted → encrypted migration path up front

*Content coming soon.*

## 11. Introducing mango4j-crypto

### Key-driven design: keys as objects, not strings

*Content coming soon.*

### `CryptoShield` and annotations as the single source of truth for what's encrypted

*Content coming soon.*

### Pluggable encryption service delegates

*Content coming soon.*

### Single, Double, and List HMAC strategies

*Content coming soon.*

### Built-in rekeying support

*Content coming soon.*

### `@EnableMigrationSupport` for legacy unencrypted fields

*Content coming soon.*

## 12. What We're Building Today

### The hands-on stages, in order

*Content coming soon.*

### What each stage demonstrates from this talk

*Content coming soon.*
