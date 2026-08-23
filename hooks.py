"""MkDocs build hook.

Materializes each `step-NN` git branch into a worktree under
`docs/.snippets/step-NN/`, so step pages can pull real, compiling
code straight from the branches via pymdownx.snippets section includes
(`--8<-- "step-01/pom.xml:dependency"`, matching `[start:dependency]`
/ `[end:dependency]` marker comments in the source) instead of
hand-copied snippets that can drift from the branch.
"""

import re
import subprocess
from pathlib import Path

STEP_BRANCH_RE = re.compile(r"^step-\d+$")
SNIPPETS_DIR = Path("docs/.snippets")


def _run(args):
    return subprocess.run(args, capture_output=True, text=True, check=True).stdout


def _branch_name(ref):
    ref = ref.strip()
    if ref.startswith("origin/"):
        ref = ref[len("origin/") :]
    return ref


def _list_step_branches():
    # Maps step name -> ref to check out. Prefers a local branch (dev
    # machines) over the remote-tracking ref (CI, where only `origin/*`
    # exists) when both are present.
    branches = {}
    for line in _run(["git", "branch", "-r", "--format=%(refname:short)"]).splitlines():
        name = _branch_name(line)
        if STEP_BRANCH_RE.match(name):
            branches[name] = line.strip()
    for line in _run(["git", "branch", "--format=%(refname:short)"]).splitlines():
        name = _branch_name(line)
        if STEP_BRANCH_RE.match(name):
            branches[name] = name
    return dict(sorted(branches.items()))


def on_config(config):
    try:
        branches = _list_step_branches()
    except subprocess.CalledProcessError as exc:
        print(f"[hooks.py] could not list git branches, skipping snippet worktrees: {exc.stderr}")
        return config

    SNIPPETS_DIR.mkdir(parents=True, exist_ok=True)

    try:
        existing = _run(["git", "worktree", "list", "--porcelain"])
    except subprocess.CalledProcessError:
        existing = ""

    for branch, ref in branches.items():
        worktree_path = SNIPPETS_DIR / branch
        if str(worktree_path) in existing or worktree_path.exists():
            continue
        result = subprocess.run(
            ["git", "worktree", "add", "--detach", str(worktree_path), ref],
            capture_output=True,
            text=True,
        )
        if result.returncode != 0:
            print(f"[hooks.py] failed to add worktree for '{branch}' ({ref}): {result.stderr.strip()}")

    return config
