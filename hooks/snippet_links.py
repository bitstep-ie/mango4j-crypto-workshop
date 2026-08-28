"""MkDocs hook: turn a `<!-- link -->` directive into a "View on GitHub" link.

`pymdownx.snippets` pulls fenced code blocks straight from `stages/`/`talk/` via
`--8<-- [start:label]` / `[end:label]` markers (see AGENTS.md). Those markers are pure
region markers, with no opinion on how a page renders them. A doc page that wants a
"View on GitHub" link under one specific embedded snippet says so itself, by placing a
`<!-- link -->` line directly after that snippet's closing code fence:

    ```java
    --8<-- "naive-ciphertext-blob/src/.../NaiveVault.java:brute-force-decrypt"
    ```
    <!-- link -->

This is deliberately per-embed, not per-page or per-snippet-by-default: only add it
where the reader benefits from seeing the whole file for that one snippet, not to every
include. The directive itself is stripped from the rendered output either way.

Runs as an `on_page_markdown` hook (native MkDocs feature, no extra dependency) before
`pymdownx.snippets` expands the includes, so it sees the same raw
`--8<-- "path:label"` lines pymdownx does.
"""

import re
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
BASE_PATHS = ("stages", "talk")

# `(?P<indent>[ \t]*)` plus backreferences to it lets this match a fence nested inside a
# list item (indented) as well as a top-level one, as long as the fence, the include
# line, the closing fence, and the optional directive all share the same indentation.
SNIPPET_INCLUDE_RE = re.compile(
    r'(?P<indent>[ \t]*)```[a-zA-Z0-9_+-]*\n'
    r'(?P=indent)--8<-- "(?P<path>[^"\n]+)"\n'
    r'(?P=indent)```'
    r'(?P<link>\n(?P=indent)<!--\s*link\s*-->)?'
)

# Same section-marker pattern pymdownx.snippets itself matches on, so a marker line
# that pymdownx recognizes is guaranteed to be recognized here too.
SNIPPET_SECTION_RE = re.compile(
    r'''(?xi)
    ^.*?
    -{1,}8<-{1,}[ \t]+
    \[[ \t]*(?P<type>start|end)[ \t]*:[ \t]*(?P<name>[a-z][-_0-9a-z]*)[ \t]*\]
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


def _line_range(source_file, label):
    """Return (start, end) source-line numbers for `label`'s body, or None if the
    label's markers can't both be found."""
    start_line = end_line = None
    for lineno, line in enumerate(source_file.read_text(encoding="utf-8").splitlines(), start=1):
        m = SNIPPET_SECTION_RE.match(line)
        if not m or m.group("name") != label:
            continue
        if m.group("type") == "start":
            start_line = lineno
        elif start_line is not None:
            end_line = lineno
            break

    if start_line is None or end_line is None:
        return None
    return start_line + 1, end_line - 1  # the code itself, excluding the marker lines


def on_page_markdown(markdown, page, config, files):
    repo_url = (config.get("repo_url") or "").rstrip("/")
    if not repo_url:
        return markdown

    def add_link(match):
        wants_link = match.group("link")
        whole = match.group(0)
        fence_only = whole[:len(whole) - len(wants_link)] if wants_link else whole
        if not wants_link:
            return fence_only

        source_file, rel_path, label = _resolve_source(match.group("path"))
        if source_file is None or label is None:
            return fence_only

        line_range = _line_range(source_file, label)
        if line_range is None:
            return fence_only

        start, end = line_range
        anchor = f"L{start}" if start == end else f"L{start}-L{end}"
        url = f"{repo_url}/blob/main/{rel_path}#{anchor}"
        indent = match.group("indent")
        return f'{fence_only}\n{indent}<small>[View on GitHub]({url})</small>\n'

    return SNIPPET_INCLUDE_RE.sub(add_link, markdown)
