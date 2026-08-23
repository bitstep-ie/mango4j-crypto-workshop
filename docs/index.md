# Mango4j Crypto Workshop

A hands-on, step-by-step workshop for [mango4j-crypto](https://github.com/bitstep-ie/mango4j-crypto) — a framework for implementing Application Level Encryption in Java applications.

Each step is a self-contained branch with real, compiling code — not just prose. You progress by checking out one step branch at a time, rather than merging anything into a branch of your own.

Each step's page will ask you to make some changes yourself as an exercise. Before moving to the next step, **discard those changes** and switch branches — this avoids any risk of merge conflicts between your own edits and the next step's code.

## Prerequisites

- JDK 17+
- Maven

## How to follow along

Clone the workshop repository, then check out step 1:

```bash
git clone https://github.com/bitstep-ie/mango4j-crypto-workshop.git
cd mango4j-crypto-workshop
git checkout step-01
```

Once you've tried a step's exercise, discard your changes and switch to the next step:

```bash
git reset --hard
git clean -fd
git checkout step-02
```

The code samples shown on each page are pulled directly from that step's branch, so what you read here always matches what you'll have locally.

## Steps

1. [Getting Started](steps/01-getting-started.md)
