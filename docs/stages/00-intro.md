# Introduction

Welcome to the workshop for **mango4j-crypto**, an Application Level Encryption framework for Java applications. It supports pluggable encryption providers (AWS KMS, PBKDF2, wrapped keys, or your own), multiple HMAC strategies for searching and enforcing uniqueness on encrypted fields, rekeying support, multi-tenant key segregation, and migration of existing unencrypted fields to encrypted ones.

**Application Level Encryption (ALE)** means your application code encrypts confidential fields before persistence. It introduces design choices around where ciphertext is stored, how keys rotate, how encrypted fields are searched, and how existing plaintext data is migrated. This workshop uses mango4j-crypto to make those choices explicit and repeatable.

[mango4j-crypto](https://github.com/bitstep-ie/mango4j-crypto) provides the abstractions needed to address those choices. Keys are objects rather than hardcoded strings, so provider changes can usually be made through key and delegate configuration instead of business-logic changes. `@Encrypt`/`@EncryptedData` annotations provide a consistent field model, and the available HMAC strategies let an application choose the storage model appropriate to its search and uniqueness requirements. Rekeying and migration still require application-owned configuration, persistence integration, scheduling, and operational monitoring; this workshop introduces those responsibilities step by step.

Before the hands-on stages, [The Talk](../talk/intro.md) goes deeper: what ALE is, why it's needed, and the numerous ways the typical/naive approach fails in production.

## Prerequisites

- JDK 17+
- Maven

## How to follow along

Head straight to [Getting Started](01-getting-started.md) to get started.
