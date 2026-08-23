# Mango4j Crypto Workshop

A hands-on, step-by-step workshop for [mango4j-crypto](https://github.com/bitstep-ie/mango4j-crypto) — a framework for implementing Application Level Encryption in Java applications.

Each step builds on the last. Rather than just reading, you'll progress your own local copy of the project step by step, merging in each step's changes as you go — so at every point you have real, compiling code, not just snippets on a page.

Each step's page will ask you to make some changes yourself as an exercise. Before moving to the next step, **discard those changes** — this keeps your working branch identical to the step branch you last merged, so merging the next step is always clean, with no conflicts against your own edits.

## Prerequisites

- JDK 17+
- Maven

## How to follow along

Clone the workshop repository, then create and switch to your own working branch:

```bash
git clone https://github.com/bitstep-ie/mango4j-crypto-workshop.git
cd mango4j-crypto-workshop
git checkout -b my-workshop
```

Merge in step 1 to get started:

```bash
git merge step-01
```

Once you've tried a step's exercise, discard your changes and merge the next step:

```bash
git reset --hard
git clean -fd
git merge step-02
```

The code samples shown on each page are pulled directly from that step's branch, so what you read here always matches what you'll have locally.

## Steps

1. [Getting Started](steps/01-getting-started.md)
