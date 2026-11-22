#!/usr/bin/env python3
"""Guarded, friendProfiles-ONLY Firebase RTDB rule update for WordQuest AI 1.0.85.

Requires an already authenticated project service account. Fails without
altering Firebase if any unexpected live rule drift is found. Does not touch
existing data, users, game rooms or the user's private mascot ZIP files.
"""
from pathlib import Path
import copy, hashlib, json, subprocess, sys, urllib.parse, urllib.request, urllib.error
import google.auth
from google.auth.transport.requests import Request

PROJECT = "wordquest-ai"
URL = "https://wordquest-ai-default-rtdb.firebaseio.com/.settings/rules.json"
HERE = Path(__file__).resolve().parents[2]
CURRENT = HERE / "firebase/database.rules.v183.json"
DESIRED = HERE / "firebase/database.rules.v185.json"
OUTPUT = HERE / "firebase/live-merged-friend-maskot-v185.json"

def digest(data):
    return hashlib.sha256(json.dumps(data,sort_keys=True,separators=(",",":")).encode()).hexdigest()

def fetch_live(credentials):
    credentials.refresh(Request())
    url=URL+"?access_token="+urllib.parse.quote(credentials.token,safe="")
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            data=json.loads(response.read().decode("utf-8"))
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError, ValueError) as exc:
        sys.exit("Cannot read live Firebase rules. NO DEPLOY")
    if not isinstance(data,dict) or not isinstance(data.get("rules"),dict):
        sys.exit("Invalid live Firebase rules. NO DEPLOY")
    return data

old = json.loads(CURRENT.read_text("utf-8"))["rules"]["friendProfiles"]
new = json.loads(DESIRED.read_text("utf-8"))["rules"]["friendProfiles"]
old_node=old["$uid"]
new_node=new["$uid"]
assert set(new_node)==set(old_node)|{"avatarPreview"}
assert {k:v for k,v in new_node.items() if k!="avatarPreview"}==old_node
assert new_node[".read"]==old_node[".read"]
assert new_node[".write"]==old_node[".write"]
assert "root.child('friends').child(auth.uid).child($uid)" in new_node[".read"]
assert "root.child('friends').child($uid).child(auth.uid)" in new_node[".read"]
assert "18050" in new_node["avatarPreview"][".validate"]
assert "custom_[a-f0-9]{32}" in new_node["avatarPreview"][".validate"]
assert new_node["$other"]=={".validate":False}
credentials, project=google.auth.default(scopes=[
    "https://www.googleapis.com/auth/cloud-platform",
    "https://www.googleapis.com/auth/firebase.database"])
if project and project!=PROJECT: sys.exit("Wrong authenticated project. NO DEPLOY")
live=fetch_live(credentials)
live_node=live["rules"].get("friendProfiles")
if live_node==new:
    print("PASS: live friend-only mascot rules already deployed:",digest(live))
    sys.exit(0)
if live_node is not None and live_node!=old:
    sys.exit("Live friendProfiles rule differs from audited baseline. NO DEPLOY")
merged=copy.deepcopy(live)
merged["rules"]["friendProfiles"]=new
check=copy.deepcopy(merged)
if live_node is None: del check["rules"]["friendProfiles"]
else: check["rules"]["friendProfiles"]=old
assert check==live,"Unrelated Firebase rules would change; NO DEPLOY"
OUTPUT.write_text(json.dumps(merged,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
(HERE/"firebase.json").write_text(json.dumps({"database":{"rules":str(OUTPUT)}},
    ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print("PASS: only friendProfiles subtree changed, old hash:",digest(live))
subprocess.run(["npx","--yes","firebase-tools@latest","deploy",
    "--only","database","--project",PROJECT,"--non-interactive"],check=True)
after=fetch_live(credentials)
if after!=merged:
    sys.exit("Unexpected deployed rules; check live project urgently")
print("PASS: deployed exact friend-only private thumbnail rules; hash:",digest(after))
