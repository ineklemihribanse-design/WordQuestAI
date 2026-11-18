#!/usr/bin/env python3
"""Delete only old complete public APK Releases; never delete git tags or source history."""
import json
import os
import re
import sys
import urllib.request

REPOSITORY = "mihribansezik5-lgtm/WordQuestAI-Updates"
KEEP = 3
RELEASE = re.compile(r"^v(\d+)\.(\d+)\.(\d+)(?:\.(\d+))?$")


def prune(token, repository=REPOSITORY, dry_run=False, fetch=None, delete=None):
    if repository != REPOSITORY:
        raise ValueError("Unexpected public releases repository")
    if fetch is None:
        def fetch(path):
            request = urllib.request.Request("https://api.github.com" + path,
                headers={"Authorization": "Bearer " + token,
                         "Accept": "application/vnd.github+json", "User-Agent": "WQ-release-cleaner"})
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.load(response)
    if delete is None:
        def delete(path):
            request = urllib.request.Request("https://api.github.com" + path, method="DELETE",
                headers={"Authorization": "Bearer " + token,
                         "Accept": "application/vnd.github+json", "User-Agent": "WQ-release-cleaner"})
            with urllib.request.urlopen(request, timeout=30) as response:
                if response.status != 204:
                    raise RuntimeError("Unexpected delete status: " + str(response.status))
    repo = fetch("/repos/" + repository)
    if repo.get("private") is not False:
        raise RuntimeError("Updates repository must be public")
    releases = []
    for page in range(1, 6):
        batch = fetch(f"/repos/{repository}/releases?per_page=100&page={page}")
        if not isinstance(batch, list):
            raise RuntimeError("Invalid releases list")
        releases.extend(batch)
        if len(batch) < 100:
            break
    stable = []
    for release in releases:
        tag = release.get("tag_name", "")
        match = RELEASE.fullmatch(tag)
        if release.get("draft") or release.get("prerelease") or not match:
            continue
        assets = {a.get("name") for a in release.get("assets", []) if a.get("state") == "uploaded"}
        if not {"WordQuestAI.apk", "update.json"}.issubset(assets):
            # Incomplete releases are not targeted: investigate them manually.
            continue
        parts = [int(x) for x in match.groups() if x is not None]
        stable.append((tuple((parts + [0])[:4]), release))
    stable.sort(key=lambda pair: pair[0], reverse=True)
    if len(stable) <= KEEP:
        return []
    deleted = []
    for _, release in stable[KEEP:]:
        path = f"/repos/{repository}/releases/{int(release['id'])}"
        if not dry_run:
            delete(path)
        deleted.append(release["tag_name"])
    return deleted


if __name__ == "__main__":
    token = os.environ.get("WQ_UPDATES_TOKEN")
    if not token:
        sys.exit("Set WQ_UPDATES_TOKEN in GitHub Actions Secrets")
    is_dry = "--dry-run" in sys.argv
    removed = prune(token, dry_run=is_dry)
    print(("DRY RUN: " if is_dry else "Deleted old releases: ") + ", ".join(removed or ["none"]))