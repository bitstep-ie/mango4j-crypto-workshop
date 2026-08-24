# How This Workshop Works

This page explains the mechanics behind the workshop, for anyone contributing a stage or just curious how the pieces fit together.

## Stages are independent, standalone projects

Each stage of the workshop lives in its own folder — `stages/01-Getting-Started/`, `stages/02-Encrypt-a-Field/`, and so on — and each folder is a **complete, standalone, buildable Maven project**, not a diff or a patch on the previous stage. The `NN-` prefix keeps the folders sorted in workshop order; the rest of the name says what the stage is about.

As a learner, you move through the workshop by moving into one stage's folder, trying its exercise, then moving into the next stage's folder:

```bash
cd stages/01-Getting-Started
# ...try the exercise...
cd ../02-Encrypt-a-Field
```

!!! tip "Using an IDE?"
    `cd` is just the command-line way to do it. Each stage folder is a standalone Maven project, so you can equally open it directly as its own project in an IDE (e.g. "Open" → `stages/02-Encrypt-a-Field` in IntelliJ/Eclipse/VS Code) instead of `cd`-ing.

Because each stage's folder is entirely separate from the others, there's no git branching, merging, or resetting involved in moving between them. Whatever you change while experimenting in `01-Getting-Started` simply has no way to reach `02-Encrypt-a-Field` — it's a different folder.

Under the hood, later stages are typically authored by copying the previous stage's folder and adding that stage's changes, so `diff -r stages/01-Getting-Started stages/02-Encrypt-a-Field` shows exactly what changed. That's an authoring detail, not something learners need to know.

## Code samples are pulled live from the stage folders

Every code block on a stage's page in this docs site is not hand-copied — it's pulled directly out of that stage's actual folder at build time, so the docs can never drift out of sync with code that really compiles.

This is done with the `pymdownx.snippets` Markdown extension, configured in `mkdocs.yml` with `stages/` as its search path. A stage's source file can mark the interesting part of itself with comments:

```xml
<!-- --8<-- [start:dependency] -->
<dependency>
    <groupId>ie.bitstep.mango</groupId>
    <artifactId>mango4j-crypto</artifactId>
    <version>1.0.0</version>
</dependency>
<!-- --8<-- [end:dependency] -->
```

And a docs page includes just that section with:

```
--8<-- "01-Getting-Started/pom.xml:dependency"
```

The path is relative to `stages/`, and `:dependency` selects only the text between the matching `[start:dependency]` / `[end:dependency]` markers — the rest of the file (boilerplate, unrelated config) is left out. Dropping the `:label` includes the whole file instead. Marker labels only need to be unique within their own file, so pick something descriptive per snippet (`dependency`, `encrypt-annotation`, `key-provider`, ...).

## Why this way, instead of hand-written code blocks?

A hand-copied snippet can silently drift from the real code the moment either one changes. Pulling straight from the stage's folder means that's structurally impossible — if a stage's `pom.xml` changes, its docs page picks up the new content the next time the site builds, with no separate step to remember.
