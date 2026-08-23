# AGENTS.md

Guidance for agents working in this repository.

## Project purpose

This repository is a step-by-step workshop teaching the **mango4j** crypto framework. It is intended to guide learners through progressively building up usage of mango4j, likely as a series of numbered steps.

## Current status

Docs-site scaffolding (MkDocs) and a `step-01` branch exist; most steps are not yet written. The `.gitignore` targets a Java/Maven project (`*.class`, `*.jar`, `*.war`, etc.), so workshop code is implemented in Java/Maven.

## Workshop structure: branch-per-step + MkDocs

Learners create their own working branch (`git checkout -b my-workshop`) and progress through the workshop by merging one step branch into it at a time (`git merge step-01`, then `git merge step-02`, etc.), so at every point they have real, compiling code rather than just prose, on a branch that's theirs to modify without touching the step branches themselves. The docs site (built with MkDocs + Material, deployed to GitHub Pages) narrates each step and pulls its code samples directly from that step's branch, so the two never drift apart.

Each step asks learners to make some changes themselves as an exercise. Before merging the next step, they're instructed to discard those changes (`git reset --hard && git clean -fd`) rather than commit them — this guarantees their working branch tip is always byte-identical to the step branch they last merged, so every subsequent `git merge step-0N` is conflict-free regardless of what they tried during the exercise.

**Step branches**

- Named `step-01`, `step-02`, ... (zero-padded, sorts naturally).
- Each branch is a linear increment on the previous step — `step-02` should be based on `step-01`, etc. — so `git merge step-0N` from a learner's `step-0(N-1)` checkout is a clean fast-forward/no-conflict merge.
- Contain only the actual workshop project code (currently a standalone Maven project depending on `mango4j-crypto`) — not `AGENTS.md`/`CLAUDE.md`/docs-site files, which live only on `main`.
- Mark the region of a file that a docs page should quote with pymdownx.snippets section markers, e.g. in `pom.xml`:
  ```xml
  <!-- --8<-- [start:dependency] -->
  <dependency>...</dependency>
  <!-- --8<-- [end:dependency] -->
  ```
  Pick a descriptive label per marker (`dependency`, `encrypt-annotation`, etc.) — labels only need to be unique within one file.

**Docs site (`main` branch)**

- `mkdocs.yml` — site config; theme is `material`; `pymdownx.snippets` is configured with `base_path: [docs/.snippets]` and `dedent_subsections: true`.
- `hooks.py` — an MkDocs build hook (`on_config`) that finds every `step-\d+` branch (local or `origin/`) and materializes each into a `git worktree` at `docs/.snippets/step-NN/`. This directory is build-generated and gitignored — never commit it or edit files inside it.
- `docs/steps/0N-*.md` — one page per step. Pull code into a page with:
  ```
  --8<-- "step-01/pom.xml:dependency"
  ```
  (path is relative to `docs/.snippets/`, `:label` selects the marked section; omit `:label` to include a whole file.)
- `requirements.txt` — `mkdocs`, `mkdocs-material`, `mike`, `pymdown-extensions`.
- `.github/workflows/docs.yml` — on push to `main`, fetches all branches (step branches must be fetchable — `fetch-depth: 0` plus an explicit `git fetch` of all remote heads) and deploys via `mike deploy --push --update-aliases main latest` (then `mike set-default --push latest`) to the `gh-pages` branch. `mike` rejects using the same string for both version and alias, hence version `main` / alias `latest` rather than `latest latest`.

**Adding a new step**

1. Branch `step-0N` from `step-0(N-1)`, make the code changes for that step, add/adjust `--8<-- [start:label]`/`[end:label]` markers around anything a docs page will quote, commit, push.
2. Add `docs/steps/0N-*.md` on `main`, add it to `nav:` in `mkdocs.yml`, and add its snippet includes.
3. Preview locally with `mkdocs serve` (requires the step branches to exist/be fetchable locally so `hooks.py` can create their worktrees).

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
