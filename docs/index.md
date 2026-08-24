# Mango4j Crypto Workshop

A hands-on, step-by-step workshop for [mango4j-crypto](https://github.com/bitstep-ie/mango4j-crypto) — a framework for implementing Application Level Encryption in Java applications.

Each stage is a self-contained, standalone Maven project in its own folder — not something you build up with git. You progress by moving into one stage's folder at a time, trying things out, then moving on to the next.

Before the hands-on stages, [The Talk](talk.md) covers what Application-Level Encryption is, why it's needed, and the numerous ways the typical/naive approach fails in production.

## Prerequisites

- JDK 17+
- Maven

## How to follow along

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

## Stages

1. [Getting Started](stages/01-getting-started.md)
2. [Encrypting a Field](stages/02-encrypting-a-field.md)
