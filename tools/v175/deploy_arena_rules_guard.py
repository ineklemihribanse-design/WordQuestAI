#!/usr/bin/env python3
"""Publish ONLY the Arena question-count rule delta, preserving live RTDB rules.

Requires GOOGLE_APPLICATION_CREDENTIALS from a Google service account with
permission to read and deploy rules for project wordquest-ai. No app data touched.
"""
import copy
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

PROJECT = "wordquest-ai"
INSTANCE = "wordquest-ai-default-rtdb"
URL = "https://wordquest-ai-default-rtdb.firebaseio.com/.settings/rules.json"
BASE = Path("source/database.rules.prearena.json")
DESIRED = Path("firebase/database.rules.json")
OUTPUT = Path("firebase/live-merged-rules.json")


def sha(document):
    return hashlib.sha256(json.dumps(document, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def arena_fields(document):
    room = document["rules"]["premiumRooms"]["$code"]
    return {
        "size": room["size"][".validate"],
        "answer": room["answers"]["$uid"]["$index"][".write"],
    }


def set_fields(document, fields):
    room = document["rules"]["premiumRooms"]["$code"]
    room["size"][".validate"] = fields["size"]
    room["answers"]["$uid"]["$index"][".write"] = fields["answer"]


def read_remote(creds):
    # Firebase REST rule-management API uses an OAuth access token. Never
    # print the authenticated URL, credentials or the unmodified live policy.
    creds.refresh(Request())
    url = URL + "?access_token=" + urllib.parse.quote(creds.token, safe="")
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        sys.exit("Firebase rules fetch failed (HTTP status " + str(exc.code) + "); NO DEPLOY")
    except (urllib.error.URLError, ValueError, TimeoutError):
        sys.exit("Firebase rules fetch failed; NO DEPLOY")


try:
    import google.auth
    from google.auth.transport.requests import Request
except ImportError:
    sys.exit("google-auth not installed; NO DEPLOY")

old = json.loads(BASE.read_text(encoding="utf-8"))
wanted = json.loads(DESIRED.read_text(encoding="utf-8"))
old_fields = arena_fields(old)
new_fields = arena_fields(wanted)
assert old_fields != new_fields, "Arena delta missing"
assert all("auth.uid === $uid" in expr for expr in (old_fields["answer"], new_fields["answer"]))
assert all("data.parent().parent().parent().child('phase').val() === 'playing'" in expr
           for expr in (old_fields["answer"], new_fields["answer"]))
assert "roomQuota" in old["rules"] and "roomQuota" in wanted["rules"]
assert new_fields["answer"].count("child('size').val() ===") == 5
assert all("child('size').val() === " + str(n) in new_fields["answer"]
           for n in (5, 10, 12, 15, 20))
assert all("newData.val() === " + str(n) in new_fields["size"]
           for n in (5, 10, 12, 15, 20))

creds, credential_project = google.auth.default(
    scopes=["https://www.googleapis.com/auth/cloud-platform",
            "https://www.googleapis.com/auth/firebase.database"])
if credential_project and credential_project != PROJECT:
    sys.exit("Authenticated service-account project differs from WordQuest; NO DEPLOY")

live = read_remote(creds)
actual_fields = arena_fields(live)
if actual_fields == new_fields:
    print("PASS: Arena count rules already deployed; live rule fingerprint:", sha(live))
    sys.exit(0)
if actual_fields != old_fields:
    sys.exit("Live Arena rules differ from reviewed v1.0.76 baseline. "
             "Refuse to overwrite a changed security policy; NO DEPLOY.")

merged = copy.deepcopy(live)
set_fields(merged, new_fields)
# Explicitly preserve all live paths except the intended two rule expressions.
test = copy.deepcopy(merged)
set_fields(test, old_fields)
assert test == live, "Unexpected live rule mutation blocked"
OUTPUT.write_text(json.dumps(merged, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
Path("firebase.json").write_text(json.dumps({"database": {"rules": str(OUTPUT)}}), encoding="utf-8")
print("Validated Arena-only policy delta; preserving all other deployed paths.")
print("Previous policy fingerprint:", sha(live))
subprocess.run(["npx", "--yes", "firebase-tools@latest", "deploy", "--only", "database",
                "--project", PROJECT, "--non-interactive"], check=True)
after = read_remote(creds)
if after != merged:
    sys.exit("Rules deployment response returned, but live rules mismatch; VERIFY MANUALLY")
print("PASS: VERIFIED LIVE ARENA RULES DEPLOYMENT to", PROJECT, INSTANCE)
print("New policy fingerprint:", sha(after))
