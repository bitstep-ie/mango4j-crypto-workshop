#!/usr/bin/env python3
"""Generates each stage's complete/src and starter/src from its template/src.

template/src is the authoring source: not a runnable project by itself,
just source files marked up with:

  --8<-- docs markers, e.g. `// --8<-- [start:label]` /
  `// --8<-- [end:label]`, used by the docs site (pymdownx.snippets)
  to pull code samples out of complete/. Meaningless in starter/, so
  they're stripped from it.

  TODO regions, delimited by matching `// TODO:START <label>` /
  `// TODO:END <label>` comment lines, wrapping the code that's this
  stage's exercise. Inside a TODO region:
    - `// TODO: ...` lines are the instructions for the learner - keep
      writing as many as you need to explain the exercise properly.
    - every other line is the actual (hidden-from-starter) code.

From template/src we generate two runnable projects:

  complete/src - TODO markers and instruction lines are stripped,
                 leaving just the code. This is the finished, working
                 reference (and what the docs site pulls its code
                 snippets from), so it must read as normal code with
                 no leftover authoring comments.

  starter/src  - the code inside each TODO region is replaced by its
                 `// TODO: ...` instruction lines (verbatim, so they
                 can span as many lines as needed); if a region has no
                 instruction lines, a generic placeholder is used
                 instead. --8<-- docs markers are removed everywhere,
                 since starter/ isn't a docs snippet source.

Usage:
    generate_stage.py <stage-dir> [<stage-dir> ...]

Idempotent: running it again with no changes to template/ produces no
diff. CI uses this to check that committed complete/ and starter/
haven't drifted from template/ - see .github/workflows/ci.yml.
"""
import re
import shutil
import sys
from pathlib import Path

TODO_BLOCK_RE = re.compile(
    r'^[ \t]*// TODO:START (\S+)\n(.*?)\n[ \t]*// TODO:END \1\n',
    re.DOTALL | re.MULTILINE,
)
INSTRUCTION_LINE_RE = re.compile(r'^[ \t]*// TODO:(?!START\b|END\b).*$', re.MULTILINE)
SNIPPET_MARKER_RE = re.compile(r'^.*--8<--.*\n', re.MULTILINE)


def _split_block(body: str) -> tuple[str, str]:
    code_lines = []
    instruction_lines = []
    for line in body.split("\n"):
        (instruction_lines if INSTRUCTION_LINE_RE.match(line) else code_lines).append(line)
    return "\n".join(code_lines), "\n".join(instruction_lines)


def strip_todos(text: str) -> str:
    def repl(match: re.Match) -> str:
        code, _ = _split_block(match.group(2))
        code = code.strip("\n")
        return f"{code}\n" if code else ""

    return TODO_BLOCK_RE.sub(repl, text)


def collapse_todos(text: str) -> str:
    def repl(match: re.Match) -> str:
        label = match.group(1)
        _, instructions = _split_block(match.group(2))
        instructions = instructions.strip("\n")
        if not instructions:
            instructions = f"// TODO: {label} - see the stage docs"
        return f"{instructions}\n"

    text = TODO_BLOCK_RE.sub(repl, text)
    return SNIPPET_MARKER_RE.sub("", text)


def render_tree(template_src: Path, dest_src: Path, transform) -> None:
    if dest_src.exists():
        shutil.rmtree(dest_src)

    for src_path in template_src.rglob("*"):
        dest_path = dest_src / src_path.relative_to(template_src)
        if src_path.is_dir():
            dest_path.mkdir(parents=True, exist_ok=True)
            continue
        dest_path.parent.mkdir(parents=True, exist_ok=True)
        dest_path.write_text(transform(src_path.read_text()))


def generate(stage_dir: Path) -> None:
    template_src = stage_dir / "template" / "src"
    if not template_src.is_dir():
        return

    render_tree(template_src, stage_dir / "complete" / "src", strip_todos)
    render_tree(template_src, stage_dir / "starter" / "src", collapse_todos)


def main(argv: list[str]) -> None:
    if not argv:
        print(__doc__, file=sys.stderr)
        sys.exit(1)
    for arg in argv:
        generate(Path(arg))


if __name__ == "__main__":
    main(sys.argv[1:])
