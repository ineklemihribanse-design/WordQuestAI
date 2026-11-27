#!/usr/bin/env python3
"""Fail-closed release impact classifier. Advisory until fast gates are proven.
Classify the ENTIRE diff against the last published source, not just HEAD^.
Unknown, deleted, renamed, binary or mixed infrastructure files require full QA.
"""
import argparse
import json
import re
import subprocess

FAST = re.compile(r"(?:source/app/src/main/res/(?:drawable(?:-[a-z0-9-]+)?|mipmap(?:-[a-z0-9-]+)?)/[a-z0-9_]+\.xml|assets/user_uploads/[a-zA-Z0-9_-]+\.(?:webp|png))")
STANDARD = re.compile(r"tools/v132_engine/(?:Wq150ArenaEntry|Wq150MatchSettings|Wq150HomeModesTest|Wq150ViewportTest)\.kt|tools/v150_release/qa_arena\.py")
LEVELS = {"fast": 0, "standard": 1, "full": 2}

def classify(records):
    result = "fast"
    reasons = []
    if not records:
        return {"level": "full", "reasons": ["empty or unknown diff"]}
    for status, path in records:
        if status != "M":
            level = "full"
        elif FAST.fullmatch(path):
            level = "fast"
        elif STANDARD.fullmatch(path):
            level = "standard"
        else:
            level = "full"
        if LEVELS[level] > LEVELS[result]:
            result = level
        reasons.append({"status": status, "path": path, "level": level})
    return {"level": result, "reasons": reasons}

def git_diff(base, head):
    # Never silently accept an invalid or unrelated baseline.
    for ref in (base, head):
        if not re.fullmatch(r"[0-9a-f]{40}", ref):
            raise ValueError("expected exact commit SHA")
    subprocess.run(["git", "merge-base", "--is-ancestor", base, head], check=True)
    raw = subprocess.check_output(["git", "diff", "--name-status", "-z", "--no-renames", base, head])
    fields = raw.decode("utf-8", errors="strict").split("\0")
    if fields[-1:] != [""] or (len(fields)-1)%2:
        raise ValueError("unexpected diff format")
    return list(zip(fields[:-1:2], fields[1:-1:2]))

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", required=True)
    args = parser.parse_args()
    try:
        result = classify(git_diff(args.base, args.head))
    except (ValueError, OSError, subprocess.SubprocessError) as exc:
        result = {"level": "full", "reasons": ["unverified baseline or diff: "+str(exc)]}
    print(json.dumps(result, ensure_ascii=False))
if __name__ == "__main__":
    main()
