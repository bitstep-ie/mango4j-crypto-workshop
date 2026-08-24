# Mango4j Crypto ALE Workshop

Welcome to the workshop for **mango4j-crypto**, an Application Level Encryption framework for Java applications. It supports pluggable encryption providers (AWS KMS, PBKDF2, wrapped keys, or your own), multiple HMAC strategies for searching and enforcing uniqueness on encrypted fields, automatic key rotation and rekeying, multi-tenant key segregation, and migrating existing unencrypted fields to encrypted ones.

**Application Level Encryption (ALE)** means your application code — not the database, not the disk — encrypts confidential fields before they're ever persisted. Done naively, it's a minefield: fields hand-encrypted straight into columns, a single HMAC column that quietly breaks unique constraints during a key rotation, a hard dependency on one cryptographic provider, no safe path from an unencrypted field to an encrypted one. All of it tends to surface as production outages, not build failures.

[mango4j-crypto](https://github.com/bitstep-ie/mango4j-crypto) is built specifically to avoid those traps. Keys are objects, not hardcoded strings, so swapping or rotating a cryptographic provider needs zero application code changes. `@Encrypt`/`@EncryptedData` annotations give you one consistent way to mark confidential fields, instead of ad hoc code scattered across the codebase. It ships multiple HMAC strategies so you can actually get searchability *and* unique constraints right, built-in rekeying support so key rotation doesn't mean a search outage or duplicate data, and `@EnableMigrationSupport` for safely moving an existing unencrypted field to encrypted. This workshop is a hands-on, step-by-step introduction to using it.

Before the hands-on stages, [The Talk](talk/01-what-is-ale.md) goes deeper: what ALE is, why it's needed, and the numerous ways the typical/naive approach fails in production.

## Prerequisites

- JDK 17+
- Maven

## How to follow along

Head straight to the [Workshop](#workshop) section below to get started.

## Workshop

To use `mango4j-crypto`, you add the dependency to your project, then annotate the entity fields you want encrypted. This workshop builds that up one stage at a time — starting with stage 1's bare dependency, so everyone begins from the same place, before annotations and everything else get added in later stages.

Each stage is a self-contained, standalone Maven project in its own folder — not something you build up with git. You progress by moving into one stage's folder at a time, trying things out, then moving on to the next.

Clone the workshop repository, then move into stage 1's folder:

```bash
git clone https://github.com/bitstep-ie/mango4j-crypto-workshop.git
cd mango4j-crypto-workshop/stages/01-Getting-Started
```

Each stage's page tells you which folder to move into next, e.g.:

```bash
cd ../02-Encrypt-a-Field
```

!!! tip "Using an IDE?"
    Each `stages/NN-Stage-Name/` folder is a standalone Maven project, so instead of `cd`-ing on the command line, you can just open that folder as its own project in your IDE (e.g. "Open" → `stages/02-Encrypt-a-Field` in IntelliJ/Eclipse/VS Code).

Because every stage is its own independent folder, anything you change while experimenting in one stage has no effect on the next — there's nothing to discard, reset, or merge.

The code samples shown on each page are pulled directly from that stage's folder, so what you read here always matches what's on disk.

1. [Getting Started](stages/01-getting-started.md)
2. [Encrypting a Field](stages/02-encrypting-a-field.md)
