# AGENTS.md

Guidance for agents working in this repository.

## Project purpose

This repository is a step-by-step workshop teaching the **mango4j** crypto framework. It is intended to guide learners through progressively building up usage of mango4j, likely as a series of numbered steps.

## Current status

Docs-site scaffolding (MkDocs), `stages/01-Getting-Started/` (a plain stage: adds the `mango4j-crypto` dependency), and `stages/02-Encrypt-a-Field/` (a starter/complete stage: encrypts a single `cardNumber` field) exist, plus `.github/workflows/ci.yml` which builds/verifies every stage; later stages are not yet written. The `.gitignore` targets a Java/Maven project (`*.class`, `*.jar`, `*.war`, `target/`, etc.), so workshop code is implemented in Java/Maven.

## Workshop structure: folder-per-stage + MkDocs

Each stage of the workshop lives in its own folder — `stages/01-Getting-Started/`, `stages/02-Encrypt-a-Field/`, ... — on `main`, all present in the checkout at once. A stage's folder is a complete, standalone, buildable Maven project, not a diff meant to be merged. Learners progress by moving into one stage's folder at a time (`cd` on the command line, or opening that folder as its own project in an IDE), trying its exercise, then moving to the next stage's folder — there is no git branching, merging, or resetting involved, so a learner's own edits can never conflict with anything: they simply don't exist in the next stage's folder. See [docs/how-it-works.md](docs/how-it-works.md) for the full explanation (also published on the docs site).

**Stage folders**

- Named `stages/NN-Stage-Name/` — zero-padded number prefix (so folders sort in workshop order) followed by a short, descriptive, hyphenated title-case name (e.g. `stages/01-Getting-Started/`, `stages/02-Encrypt-a-Field/`). Not `stage-01`/`stage-02` — the folder name itself should say what the stage is about.
- Each folder is typically authored by copying the previous stage's folder and adding that stage's changes, so it's always a complete, ready-to-build project, and e.g. `diff -r stages/01-Getting-Started stages/02-Encrypt-a-Field` shows exactly what changed between stages (an authoring detail, not something learners need to run).
- **Every stage's `pom.xml` must have a distinct `artifactId` and `name`** (e.g. `mango4j-crypto-workshop-stage-02` / "Mango4j Crypto Workshop - Stage 2: Encrypting a Field") — never copy-paste the previous stage's artifactId/name unchanged. Learners open individual stage folders as separate IDE projects, and identical artifactId/name across stages makes them indistinguishable there.
- Mark the region of a file that a docs page should quote with pymdownx.snippets section markers, e.g. in a Java file:
  ```java
  // --8<-- [start:dependency]
  ...
  // --8<-- [end:dependency]
  ```
  Pick a descriptive label per marker (`dependency`, `encrypt-annotation`, etc.) — labels only need to be unique within one file. **In XML files (`pom.xml` etc.) do not wrap the marker in an XML comment** (`<!-- --8<-- ... -->`) — XML comments cannot contain `--`, and `--8<--` contains it, so the file becomes invalid XML that Maven's parser rejects even though `mkdocs build` won't catch it (it just reads raw text). Use an XML processing instruction instead, which has no such restriction:
  ```xml
  <?mkdocs-snippet --8<-- [start:dependency]?>
  ...
  <?mkdocs-snippet --8<-- [end:dependency]?>
  ```

**Stages with a starter/complete split**

A stage can instead be three sibling folders — `template/`, `complete/`, and `starter/` — when it should give learners a partially-working exercise rather than a fully-built example (a live-led session needs devs to actually *do* something, not just read finished code). `stages/02-Encrypt-a-Field/` is the example to copy this pattern from.

- `template/src/` is the **only hand-authored source** — not itself a runnable project (no `pom.xml`). It's ordinary source with `--8<--` docs markers plus TODO regions marking the exercise:
  ```java
  // TODO:START annotate-encrypt
  @Encrypt
  // TODO:END annotate-encrypt
  ```
- `scripts/generate_stage.py <stage-dir>` generates both runnable projects from `template/src/`:
  - `complete/src/` — TODO marker comments stripped, leaving just the wrapped code (e.g. `@Encrypt` alone). This is the finished reference project, and what the docs site pulls its snippets from — it must never show TODO clutter, since it's presented as "the finished version."
  - `starter/src/` — each TODO region collapsed to one `// TODO: <label> - see the stage docs` placeholder line. This is what learners actually work in; it compiles and runs as-is (functionally incomplete, not broken).
  - Running it again with no template changes is a no-op (idempotent) — this is what lets CI detect drift (see below).
- `complete/pom.xml` and `starter/pom.xml` are hand-maintained separately (not generated) — each needs its own distinct `artifactId`/`name` per the rule above, and TODO markers rarely belong in build files anyway.
- After editing `template/src/`, always rerun `python3 scripts/generate_stage.py stages/NN-Stage-Name` and commit the regenerated `complete/`/`starter/` alongside it — never hand-edit files under `complete/src/` or `starter/src/` directly, they'll just get overwritten and drift will fail CI.

**Docs site**

- `mkdocs.yml` — site config; theme is `material`; `pymdownx.snippets` is configured with `base_path: [stages]` and `dedent_subsections: true` — it reads straight from the `stages/` folders already in the checkout, no build hook or git worktree needed.
- `docs/stages/0N-*.md` — one page per stage. Pull code into a page with:
  ```
  --8<-- "01-Getting-Started/pom.xml:dependency"
  ```
  (path is relative to `stages/`, `:label` selects the marked section; omit `:label` to include a whole file.)
- `requirements.txt` — `mkdocs`, `mkdocs-material`, `mike`, `pymdown-extensions`.
- `.github/workflows/docs.yml` — on push to `main`, deploys via `mike deploy --push --update-aliases main latest` (then `mike set-default --push latest`) to the `gh-pages` branch. `mike` rejects using the same string for both version and alias, hence version `main` / alias `latest` rather than `latest latest`.

**CI**

`.github/workflows/ci.yml` runs on every push and pull request (unlike `docs.yml`, which only deploys on push to `main`) and discovers stages generically by globbing, so adding a new stage never requires editing the workflow itself:

- `stages/*/pom.xml` (plain stages) — must `mvn compile`.
- `stages/*/complete/pom.xml` — must `mvn compile`, and if it declares `exec-maven-plugin`, must also `mvn exec:java` successfully — this is what proves a stage actually *works*, not just compiles.
- `stages/*/starter/pom.xml`, where present — must `mvn compile` (only compile, since it's deliberately unfinished).
- `stages/*/template/` — for every stage with one, reruns `scripts/generate_stage.py` and then `git diff --exit-code`, failing the build if `complete/`/`starter/` don't exactly match what the template generates (i.e. someone edited a generated file directly, or edited `template/` without regenerating).

**Adding a new stage**

1. Either:
   - **Plain stage** (nothing to leave unfinished): copy the previous stage's folder to `stages/NN-Stage-Name/`, update `pom.xml`'s `artifactId`/`name`, make the code changes, add/adjust `--8<-- [start:label]`/`[end:label]` markers around anything a docs page will quote, verify it actually compiles/runs (`mvn compile`, or `mvn exec:java` where applicable).
   - **Starter/complete stage** (an exercise for learners to do): create `stages/NN-Stage-Name/template/src/` with `--8<--` docs markers and `TODO:START`/`TODO:END` regions around the exercise, hand-write `complete/pom.xml` and `starter/pom.xml` with distinct artifactIds/names, run `python3 scripts/generate_stage.py stages/NN-Stage-Name`, then verify both `complete/` (compiles and runs correctly) and `starter/` (compiles, and demonstrates the "before" state) actually work.
2. Commit.
3. Add `docs/stages/0N-*.md`, add it to `nav:` in `mkdocs.yml`, and add its snippet includes (pointing at `complete/` for a split stage). Start the page with an `!!! abstract "Overview"` admonition — 2-3 sentences on what problem this stage solves and why, written so a workshop facilitator can talk through it before releasing learners to work through the rest of the page themselves (see stages 1 and 2 for examples). This is what makes the docs usable for both self-serve reading and live-led sessions from the same page, without needing a separate facilitator guide.
4. Preview locally with `mkdocs serve`.

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
