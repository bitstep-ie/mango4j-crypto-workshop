# Introduction

Welcome to the workshop for **mango4j-crypto**, an Application Level Encryption framework for Java applications. It supports pluggable encryption providers (AWS KMS, PBKDF2, wrapped keys, or your own), multiple HMAC strategies for searching and enforcing uniqueness on encrypted fields, automatic key rotation and rekeying, multi-tenant key segregation, and migrating existing unencrypted fields to encrypted ones.

**Application Level Encryption (ALE)** means your application code — not the database, not the disk — encrypts confidential fields before they're ever persisted. Done naively, it's a minefield: fields hand-encrypted straight into columns, a single HMAC column that quietly breaks unique constraints during a key rotation, a hard dependency on one cryptographic provider, no safe path from an unencrypted field to an encrypted one. All of it tends to surface as production outages, not build failures.

[mango4j-crypto](https://github.com/bitstep-ie/mango4j-crypto) is built specifically to avoid those traps. Keys are objects, not hardcoded strings, so swapping or rotating a cryptographic provider needs zero application code changes. `@Encrypt`/`@EncryptedData` annotations give you one consistent way to mark confidential fields, instead of ad hoc code scattered across the codebase. It ships multiple HMAC strategies so you can actually get searchability *and* unique constraints right, built-in rekeying support so key rotation doesn't mean a search outage or duplicate data, and `@EnableMigrationSupport` for safely moving an existing unencrypted field to encrypted. This workshop is a hands-on, step-by-step introduction to using it.

Before the hands-on stages, [The Talk](../talk/01-intro.md) goes deeper: what ALE is, why it's needed, and the numerous ways the typical/naive approach fails in production.

## Prerequisites

- JDK 17+
- Maven

## How to follow along

Head straight to [Getting Started](01-getting-started.md) to get started.
