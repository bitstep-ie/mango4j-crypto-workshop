"""MkDocs hook: turn specially-flagged snippet markers into a "View on GitHub" link.

`pymdownx.snippets` pulls fenced code blocks straight from `stages/`/`talk/` via
`--8<-- [start:label]` / `[end:label]` markers (see AGENTS.md). Most of those markers
are plain - but a marker written as `--8<-- [start:label] link` opts that one snippet
into an extra "View on GitHub" link right under the rendered code block, pointing at
the exact lines the snippet was pulled from. This is deliberately per-marker, not
per-page or per-snippet-by-default: only add `link` where the snippet is worth reading
in its full surrounding context (see the `link`-flagged markers under `talk/` for
examples), not to every include.

Runs as an `on_page_markdown` hook (native MkDocs feature, no extra dependency) before
`pymdownx.snippets` expands the includes, so it sees the same raw
`--8<-- "path:label"` lines pymdownx does.
"""

import re
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
BASE_PATHS = ("stages", "talk")

SNIPPET_INCLUDE_RE = re.compile(r'```[a-zA-Z0-9_+-]*\n--8<-- "([^"\n]+)"\n```')

# Same section-marker pattern pymdownx.snippets itself matches on, so a marker line
# that pymdownx recognizes is guaranteed to be recognized here too.
SNIPPET_SECTION_RE = re.compile(
    r'''(?xi)
    ^.*?
    -{1,}8<-{1,}[ \t]+
    \[[ \t]*(?P<type>start|end)[ \t]*:[ \t]*(?P<name>[a-z][-_0-9a-z]*)[ \t]*\]
    (?P<post>.*?)$
    '''
)


def _resolve_source(path_and_label):
    """Split "path:label" (label optional) and locate the file under a base path."""
    file_part, _, label = path_and_label.rpartition(":")
    if not file_part or not re.fullmatch(r"[a-z][-_0-9a-z]*", label):
        file_part, label = path_and_label, None

    for base in BASE_PATHS:
        candidate = REPO_ROOT / base / file_part
        if candidate.is_file():
            return candidate, f"{base}/{file_part}", label
    return None, None, label


def _linked_line_range(source_file, label):
    """Return (start, end) source-line numbers for `label`'s body, only if its start
    marker carries the `link` flag - otherwise None."""
    start_line = end_line = None
    linked = False
    for lineno, line in enumerate(source_file.read_text(encoding="utf-8").splitlines(), start=1):
        m = SNIPPET_SECTION_RE.match(line)
        if not m or m.group("name") != label:
            continue
        if m.group("type") == "start":
            start_line = lineno
            linked = "link" in m.group("post").split()
        elif start_line is not None:
            end_line = lineno
            break

    if not linked or start_line is None or end_line is None:
        return None
    return start_line + 1, end_line - 1  # the code itself, excluding the marker lines


def on_page_markdown(markdown, page, config, files):
    repo_url = (config.get("repo_url") or "").rstrip("/")
    if not repo_url:
        return markdown

    def add_link(match):
        block = match.group(0)
        source_file, rel_path, label = _resolve_source(match.group(1))
        if source_file is None or label is None:
            return block

        line_range = _linked_line_range(source_file, label)
        if line_range is None:
            return block

        start, end = line_range
        anchor = f"L{start}" if start == end else f"L{start}-L{end}"
        url = f"{repo_url}/blob/main/{rel_path}#{anchor}"
        return f'{block}\n<small>[View on GitHub]({url})</small>\n'

    return SNIPPET_INCLUDE_RE.sub(add_link, markdown)
