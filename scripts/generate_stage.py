#!/usr/bin/env python3
"""Generates each stage's complete/src and starter/src from its template/src.

template/src is the authoring source: not a runnable project by itself,
just source files marked up with TODO regions -
`// TODO:START <label>` ... `// TODO:END <label>` - wrapping the code
that's this stage's exercise. From it we generate two runnable
projects:

  complete/src - the TODO marker comments are stripped, leaving just
                 the wrapped code. This is the finished, working
                 reference (and what the docs site pulls its code
                 snippets from, via pymdownx.snippets), so it must
                 read as normal, unannotated-with-meta-comments code.

  starter/src  - each TODO region is collapsed into a single
                 `// TODO: <label> - see the stage docs` placeholder
                 line. This is what learners actually work in.

Usage:
    generate_starter.py <stage-dir> [<stage-dir> ...]

Idempotent: running it again with no changes to template/ produces no
diff. CI uses this to check that committed complete/ and starter/
haven't drifted from template/ - see .github/workflows/ci.yml.
"""
import re
import shutil
import sys
from pathlib import Path

TODO_RE = re.compile(
    r'^([ \t]*)// TODO:START (\S+)\n(.*?)\n[ \t]*// TODO:END \2\n',
    re.DOTALL | re.MULTILINE,
)


def strip_todos(text: str) -> str:
    return TODO_RE.sub(lambda m: f"{m.group(3)}\n", text)


def collapse_todos(text: str) -> str:
    return TODO_RE.sub(
        lambda m: f"{m.group(1)}// TODO: {m.group(2)} - see the stage docs\n",
        text,
    )


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
