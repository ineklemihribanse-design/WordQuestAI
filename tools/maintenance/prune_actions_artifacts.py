#!/usr/bin/env python3
"""Conservative WordQuestAI Actions artifact cleanup.

Runs only in the designated repo's GitHub Actions. Defaults to a read-only inventory; deletion requires explicit --apply and Actions write permission.
Never touches Git source, releases, tags, updater assets, user content or secrets.
Dry run locally: python3 tools/maintenance/prune_actions_artifacts.py --dry-run
"""
from __future__ import annotations
from collections import Counter, defaultdict
from datetime import datetime, timezone, timedelta
import json
import os
import sys
import time
import urllib.error
import urllib.request

REPO = "ineklemihribanse-design/WordQuestAI"
API = "https://api.github.com"
MIN_BYTES = 1_000_000
MAIN_AGE_HOURS = 48
EXPERIMENT_AGE_HOURS = 24
KEEP_MAIN_PER_NAME = 3
KEEP_EXPERIMENT_PER_BRANCH_NAME = 1
MAX_DELETE = 500

def iso(s):
    return datetime.fromisoformat(s.replace("Z", "+00:00"))

def request(token, path, method="GET"):
    req = urllib.request.Request(
        API + path, method=method,
        headers={"Authorization": "Bearer " + token,
                 "Accept": "application/vnd.github+json",
                 "X-GitHub-Api-Version": "2022-11-28",
                 "User-Agent": "wordquest-ai-actions-artifact-pruner"})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=35) as response:
                if method == "DELETE":
                    if response.status != 204:
                        raise RuntimeError("Unexpected GitHub DELETE status")
                    return None
                return json.load(response)
        except urllib.error.HTTPError as error:
            if error.code in (403, 429, 500, 502, 503) and attempt < 2:
                time.sleep(2 ** attempt)
                continue
            raise

def fetch_all(token):
    artifacts = []
    page = 1
    while True:
        result = request(token, f"/repos/{REPO}/actions/artifacts?per_page=100&page={page}")
        batch = result.get("artifacts", [])
        if not isinstance(batch, list):
            raise ValueError("Unexpected GitHub artifacts format")
        artifacts += batch
        if len(batch) < 100:
            break
        page += 1
        if page > 100:
            raise RuntimeError("Over 10,000 artifacts: refuse partial listing")
    return artifacts

def choose(artifacts, now):
    """Select only stale large duplicates, keeping recent samples and latest main backups."""
    valid = []
    for a in artifacts:
        try:
            created = iso(a["created_at"])
            branch = a.get("workflow_run", {}).get("head_branch")
            size = int(a["size_in_bytes"])
            artifact_id = int(a["id"])
            name = str(a["name"])
        except (KeyError, TypeError, ValueError):
            continue
        if not branch or not name or size < MIN_BYTES or not a.get("expired", False) and created > now:
            continue
        valid.append((created, artifact_id, branch, name, size))
    valid.sort(reverse=True)
    # Protect 3 most recent main artifacts of every artifact name.
    # Protect newest preview per experiment branch + artifact name.
    keep = Counter()
    delete = []
    threshold = {"main": timedelta(hours=MAIN_AGE_HOURS)}
    for created, artifact_id, branch, name, size in valid:
        key = (branch, name)
        keep[key] += 1
        if branch == "main":
            if keep[key] <= KEEP_MAIN_PER_NAME:
                continue
            age = threshold["main"]
        else:
            if keep[key] <= KEEP_EXPERIMENT_PER_BRANCH_NAME:
                continue
            age = timedelta(hours=EXPERIMENT_AGE_HOURS)
        if now - created < age:
            continue
        delete.append({"id": artifact_id, "branch": branch, "name": name,
                       "size": size, "created_at": created.isoformat()})
    # Largest duplicates first, no more than MAX_DELETE in a single job.
    return sorted(delete, key=lambda a: a["size"], reverse=True)[:MAX_DELETE]

def main():
    if os.environ.get("GITHUB_REPOSITORY") != REPO:
        sys.exit("Safety stop: this cleaner is restricted to " + REPO)
    if os.environ.get("GITHUB_EVENT_NAME") not in ("push", "schedule", "workflow_dispatch"):
        sys.exit("Safety stop: unexpected trigger")
    token = os.environ.get("GITHUB_TOKEN")
    if not token:
        sys.exit("GITHUB_TOKEN missing; unable to clean. No files modified.")
    dry_run = "--apply" not in sys.argv or "--dry-run" in sys.argv
    now = datetime.now(timezone.utc)
    all_artifacts = fetch_all(token)
    candidates = choose(all_artifacts, now)
    eligible_bytes = sum(a["size"] for a in candidates)
    print(f"Inventory: {len(all_artifacts)} artifacts. Eligible: {len(candidates)}, "
          f"{eligible_bytes/1048576:.1f} MiB (large old duplicates only).",
          flush=True)
    print(f"Protected: latest {KEEP_MAIN_PER_NAME} copies per main APK/artifact name; "
          f"newest copy of each experiment artifact and recent output. "
          "Public WordQuestAI-Updates releases are NOT accessed.", flush=True)
    deleted_bytes = 0
    deleted_count = 0
    failed_count = 0
    for candidate in candidates:
        if dry_run:
            print("DRY RUN:", candidate)
            continue
        try:
            request(token, f"/repos/{REPO}/actions/artifacts/{candidate['id']}", "DELETE")
            deleted_bytes += candidate["size"]
            deleted_count += 1
            print(f"DELETED artifact {candidate['id']}: {candidate['size']/1048576:.1f}MiB "
                  f"{candidate['branch']} {candidate['name']}", flush=True)
        except urllib.error.HTTPError as e:
            print(f"FAILED {candidate['id']}: HTTP {e.code}", flush=True)
            failed_count += 1
    print(f"RESULT: deleted={deleted_count}, freed_estimate={deleted_bytes/1048576:.1f} MiB, "
          f"failures={failed_count}. GitHub quota may update after a delay.", flush=True)
    if failed_count:
        sys.exit(1)

if __name__ == "__main__":
    main()
