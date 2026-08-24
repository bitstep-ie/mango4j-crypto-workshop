# How This Workshop Works

This page explains the mechanics behind the workshop, for anyone contributing a stage or just curious how the pieces fit together.

## Stages are independent, standalone projects

Each stage of the workshop lives in its own folder — `stages/stage-01/`, `stages/stage-02/`, and so on — and each folder is a **complete, standalone, buildable Maven project**, not a diff or a patch on the previous stage.

As a learner, you move through the workshop by moving into one stage's folder, trying its exercise, then moving into the next stage's folder — `cd` on the command line, or just opening the folder as its own project in an IDE:

```bash
cd stages/stage-01
# ...try the exercise...
cd ../stage-02
```

Because each stage's folder is entirely separate from the others, there's no git branching, merging, or resetting involved in moving between them. Whatever you change while experimenting in `stage-01` simply has no way to reach `stage-02` — it's a different folder.

Under the hood, later stages are typically authored by copying the previous stage's folder and adding that stage's changes, so `diff -r stages/stage-01 stages/stage-02` shows exactly what changed. That's an authoring detail, not something learners need to know.

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
--8<-- "stage-01/pom.xml:dependency"
```

The path is relative to `stages/`, and `:dependency` selects only the text between the matching `[start:dependency]` / `[end:dependency]` markers — the rest of the file (boilerplate, unrelated config) is left out. Dropping the `:label` includes the whole file instead. Marker labels only need to be unique within their own file, so pick something descriptive per snippet (`dependency`, `encrypt-annotation`, `key-provider`, ...).

## Why this way, instead of hand-written code blocks?

A hand-copied snippet can silently drift from the real code the moment either one changes. Pulling straight from the stage's folder means that's structurally impossible — if a stage's `pom.xml` changes, its docs page picks up the new content the next time the site builds, with no separate step to remember.
