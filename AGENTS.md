# AGENTS.md

Guidance for agents working in this repository.

## Project purpose

This repository is a step-by-step workshop teaching the **mango4j** crypto framework. It is intended to guide learners through progressively building up usage of mango4j, likely as a series of numbered steps.

## Current status

Docs-site scaffolding (MkDocs) and `stages/stage-01/` (adds the `mango4j-crypto` dependency) and `stages/stage-02/` (encrypts a single `cardNumber` field, verified by actually compiling/running against the real published `mango4j-crypto` 1.0.0) exist; later stages are not yet written. The `.gitignore` targets a Java/Maven project (`*.class`, `*.jar`, `*.war`, `target/`, etc.), so workshop code is implemented in Java/Maven.

## Workshop structure: folder-per-stage + MkDocs

Each stage of the workshop lives in its own folder — `stages/stage-01/`, `stages/stage-02/`, ... — on `main`, all present in the checkout at once. A stage's folder is a complete, standalone, buildable Maven project, not a diff meant to be merged. Learners progress by moving into one stage's folder at a time (`cd` on the command line, or opening that folder as its own project in an IDE), trying its exercise, then moving to the next stage's folder — there is no git branching, merging, or resetting involved, so a learner's own edits can never conflict with anything: they simply don't exist in the next stage's folder. See [docs/how-it-works.md](docs/how-it-works.md) for the full explanation (also published on the docs site).

**Stage folders**

- Named `stages/stage-01/`, `stages/stage-02/`, ... (zero-padded, sorts naturally).
- Each folder is typically authored by copying the previous stage's folder and adding that stage's changes, so it's always a complete, ready-to-build project, and `diff -r stages/stage-01 stages/stage-02` shows exactly what changed between stages (an authoring detail, not something learners need to run).
- **Every stage's `pom.xml` must have a distinct `artifactId` and `name`** (e.g. `mango4j-crypto-workshop-stage-02` / "Mango4j Crypto Workshop - Stage 2: Encrypting a Field") — never copy-paste the previous stage's artifactId/name unchanged. Learners open individual stage folders as separate IDE projects, and identical artifactId/name across stages makes them indistinguishable there.
- Mark the region of a file that a docs page should quote with pymdownx.snippets section markers, e.g. in `pom.xml`:
  ```xml
  <!-- --8<-- [start:dependency] -->
  <dependency>...</dependency>
  <!-- --8<-- [end:dependency] -->
  ```
  Pick a descriptive label per marker (`dependency`, `encrypt-annotation`, etc.) — labels only need to be unique within one file.

**Docs site**

- `mkdocs.yml` — site config; theme is `material`; `pymdownx.snippets` is configured with `base_path: [stages]` and `dedent_subsections: true` — it reads straight from the `stages/` folders already in the checkout, no build hook or git worktree needed.
- `docs/stages/0N-*.md` — one page per stage. Pull code into a page with:
  ```
  --8<-- "stage-01/pom.xml:dependency"
  ```
  (path is relative to `stages/`, `:label` selects the marked section; omit `:label` to include a whole file.)
- `requirements.txt` — `mkdocs`, `mkdocs-material`, `mike`, `pymdown-extensions`.
- `.github/workflows/docs.yml` — on push to `main`, deploys via `mike deploy --push --update-aliases main latest` (then `mike set-default --push latest`) to the `gh-pages` branch. `mike` rejects using the same string for both version and alias, hence version `main` / alias `latest` rather than `latest latest`.

**Adding a new stage**

1. Copy `stages/stage-0(N-1)/` to `stages/stage-0N/`, update `pom.xml`'s `artifactId`/`name` to be stage-specific, make the code changes for that stage, add/adjust `--8<-- [start:label]`/`[end:label]` markers around anything a docs page will quote, verify it actually compiles/runs (`mvn compile`, or `mvn exec:java` where applicable), commit.
2. Add `docs/stages/0N-*.md`, add it to `nav:` in `mkdocs.yml`, and add its snippet includes.
3. Preview locally with `mkdocs serve`.

## License

Apache License 2.0 (see `LICENSE`).

## Sibling repository: mango4j-crypto

The mango4j-crypto framework source code that this workshop teaches lives in a **sibling repository**, checked out at `../mango4j-crypto` relative to this repo (i.e. `bitstep.ie/mango4j-crypto` next to `bitstep.ie/mango4j-crypto-workshop`).

It is a Maven multi-module project (`pom.xml`) with three modules:

- `mango4j-crypto-core` — core domain types (e.g. `CryptoKey`) and SPIs (e.g. `CryptoKeyProvider`)
- `mango4j-crypto` — the main framework: annotations (`@Encrypt`, `@Hmac`, `@EncryptedData`, `@EncryptionKeyId`, `@HmacKeyId`), `CryptoShield`, encryption service delegates
- `mango4j-crypto-aws-kms-delegate` — an AWS KMS-backed encryption service delegate

mango4j-crypto is a framework for implementing **Application Level Encryption** (data at rest) in Java applications via annotations on entity fields, rather than an encryption provider itself — it lets you plug in your own `CryptoKeyProvider` and encryption service delegates.

When writing workshop steps in this repo, treat `../mango4j-crypto` as read-only reference/dependency source (its own README, javadocs, and `documentation/` folder are the source of truth for API details) rather than something to edit from here.

## Sibling repository: mango4j-examples

A working example application lives in another sibling repository, checked out at `../mango4j-examples`. It's a Maven project (`pom.xml`) with a single module, `mango4j-crypto-example` — a Spring Boot app demonstrating `mango4j-crypto` usage with each HMAC strategy. Like `../mango4j-crypto`, treat it as read-only reference when authoring workshop stages, not something to edit from here.
