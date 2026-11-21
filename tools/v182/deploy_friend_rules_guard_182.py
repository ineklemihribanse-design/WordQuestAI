#!/usr/bin/env python3
"""Merge ONLY WordQuest friendProfiles rules into LIVE RTDB, never replace room rules.

Fail-closed without valid service account or when live friendProfiles differs.
No user data is read, written, or deleted.
"""
from pathlib import Path
import copy,hashlib,json,subprocess,sys,urllib.parse,urllib.request,urllib.error
import google.auth
from google.auth.transport.requests import Request

PROJECT="wordquest-ai"
INSTANCE="wordquest-ai-default-rtdb"
URL="https://wordquest-ai-default-rtdb.firebaseio.com/.settings/rules.json"
DESIRED=Path("firebase/database.rules.json")
OUTPUT=Path("firebase/live-merged-friend-rules.json")
def digest(x):
    return hashlib.sha256(json.dumps(x,sort_keys=True,separators=(",",":")).encode()).hexdigest()
def live_rules(credentials):
    credentials.refresh(Request())
    # Never write this signed URL or OAuth token to the logs.
    uri=URL+"?access_token="+urllib.parse.quote(credentials.token,safe="")
    try:
        with urllib.request.urlopen(uri,timeout=30) as response:
            obj=json.loads(response.read().decode("utf-8"))
        if not isinstance(obj,dict) or not isinstance(obj.get("rules"),dict):
            sys.exit("Remote RTDB rules were not valid; NO DEPLOY")
        return obj
    except (urllib.error.HTTPError,urllib.error.URLError,TimeoutError,ValueError):
        sys.exit("Cannot read live Firebase rules; NO DEPLOY")

wanted=json.loads(DESIRED.read_text("utf-8"))["rules"]["friendProfiles"]
node=wanted["$uid"]
assert node[".write"]=="auth != null && auth.uid === $uid"
assert "root.child('friends').child(auth.uid).child($uid)" in node[".read"]
assert "root.child('friends').child($uid).child(auth.uid)" in node[".read"]
assert all(k in node for k in ("avatar","points","completed","lastActiveAt"))
credentials,identity_project=google.auth.default(scopes=[
    "https://www.googleapis.com/auth/cloud-platform",
    "https://www.googleapis.com/auth/firebase.database"])
if identity_project and identity_project!=PROJECT:
    sys.exit("Wrong service-account project; NO DEPLOY")
live=live_rules(credentials)
old_friend=live["rules"].get("friendProfiles")
if old_friend==wanted:
    print("PASS: live friendProfiles already matches reviewed rules; hash:",digest(live))
    sys.exit(0)
if old_friend is not None:
    sys.exit("Live friendProfiles already exists and differs; manual review needed. NO DEPLOY")
merged=copy.deepcopy(live)
merged["rules"]["friendProfiles"]=wanted
check=copy.deepcopy(merged)
del check["rules"]["friendProfiles"]
assert check==live,"Unrelated live policies unexpectedly modified"
OUTPUT.write_text(json.dumps(merged,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
Path("firebase.json").write_text(json.dumps({"database":{"rules":str(OUTPUT)}}),encoding="utf-8")
print("PASS: 1 new friendProfiles subtree; all existing live Arena and account rules untouched.")
print("Old rules SHA-256:",digest(live))
subprocess.run(["npx","--yes","firebase-tools@latest","deploy","--only","database",
                "--project",PROJECT,"--non-interactive"],check=True)
after=live_rules(credentials)
if after!=merged:
    sys.exit("DEPLOY attempted but resulting live policy differs: manual verification required.")
print("PASS: VERIFIED LIVE Firebase rules friendProfiles in",PROJECT,INSTANCE)
print("New rules SHA-256:",digest(after))
