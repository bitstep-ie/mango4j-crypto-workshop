# How This Workshop Works

This page explains the mechanics behind the workshop, for anyone contributing a step or just curious how the pieces fit together.

## Steps are independent, complete snapshots

Each step of the workshop is a git branch named `step-01`, `step-02`, and so on. A step branch is **not a diff or a patch** meant to be merged — it's a full, standalone, buildable snapshot of the workshop project at that point in the story.

Under the hood, `step-02` is created by branching from `step-01` and adding that step's changes, so the branches form a line of history and `git diff step-01..step-02` shows exactly what changed. But that history is an authoring detail. As a learner, you never merge or diff anything — you just check out the branch for the step you're on:

```bash
git checkout step-01
# ...try the exercise...
git reset --hard      # discard your own edits
git clean -fd          # remove any new files you created
git checkout step-02   # move on
```

Because each branch is complete in itself, switching branches is always a clean operation — there's no merge step, so there's nothing to conflict. Whatever you tried during a step's exercise simply doesn't follow you into the next one once you discard it.

## Code samples are pulled live from the step branches

Every code block on a step's page in this docs site is not hand-copied — it's pulled directly out of that step's actual branch at build time, so the docs can never drift out of sync with code that really compiles.

This is done with two pieces working together:

**1. `hooks.py` materializes each step branch as a worktree.**

MkDocs supports build hooks — plain Python that runs during the build. This site's `hooks.py` runs on every build and:

1. Finds every branch matching `step-\d+` (checking both local branches and `origin/*`, so it works the same on a contributor's machine and in CI).
2. For each one, runs `git worktree add --detach docs/.snippets/step-NN <branch>` — this checks out a *second, independent copy* of that branch's files into `docs/.snippets/step-NN/`, without disturbing the branch you're actually on.

`docs/.snippets/` is gitignored and fully regenerated on each build — it's build output, never something to hand-edit or commit.

**2. `pymdownx.snippets` pulls marked sections out of those files into the page.**

`mkdocs.yml` enables the `pymdownx.snippets` Markdown extension, pointed at `docs/.snippets/` as its search path. A step's source file can mark the interesting part of itself with comments:

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
--8<-- "step-01/pom.xml:dependency"
```

The path is relative to `docs/.snippets/`, and `:dependency` selects only the text between the matching `[start:dependency]` / `[end:dependency]` markers — the rest of the file (boilerplate, unrelated config) is left out. Dropping the `:label` includes the whole file instead. Marker labels only need to be unique within their own file, so pick something descriptive per snippet (`dependency`, `encrypt-annotation`, `key-provider`, ...).

## Why this way, instead of hand-written code blocks?

A hand-copied snippet can silently drift from the real code the moment either one changes. Pulling straight from the branch means that's structurally impossible — if a step's `pom.xml` changes, its docs page picks up the new content the next time the site builds, with no separate step to remember.
