# ALE Workshop

A hands-on, step-by-step workshop for [mango4j-crypto](https://github.com/bitstep-ie/mango4j-crypto) — a framework for implementing Application Level Encryption in Java applications.

## How it works

Each stage of the workshop lives in its own folder under [`stages/`](stages) — `stages/01-Getting-Started/`, `stages/02-Encrypt-a-Field/`, and so on — and each one is a complete, standalone, buildable Maven project, not something you build up with git. You work through the workshop by moving into one stage's folder at a time (`cd` on the command line, or opening it directly as its own project in an IDE), trying its exercise, then moving on to the next. Because every stage is independent, there's nothing to discard, reset, or merge between stages.

The code samples on the docs site aren't hand-copied — they're pulled directly from these stage folders at build time, so the docs and the code can never drift apart. See [How This Workshop Works](https://bitstep-ie.github.io/mango4j-crypto-workshop/latest/how-it-works/) for the full explanation.

## Prerequisites

- JDK 17+
- Maven

## Getting started

```bash
git clone https://github.com/bitstep-ie/mango4j-crypto-workshop.git
cd mango4j-crypto-workshop/stages/01-Getting-Started
```

Then **[Open the Workshop](https://bitstep-ie.github.io/mango4j-crypto-workshop/latest/)**.

## License

Apache License 2.0 (see [LICENSE](LICENSE)).
