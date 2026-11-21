#!/usr/bin/env python3
"""Safe live RTDB merge: ONLY 5-option Arena host lobby settings permissions.

No gameplay/user data operations. All other live account, Arena, and friend
rules are taken from the *currently deployed* Firebase policy verbatim.
"""
import copy,json,hashlib,subprocess,sys,urllib.request,urllib.parse,urllib.error
from pathlib import Path
import google.auth
from google.auth.transport.requests import Request

PROJECT="wordquest-ai"
INSTANCE="wordquest-ai-default-rtdb"
URL="https://wordquest-ai-default-rtdb.firebaseio.com/.settings/rules.json"
TARGET=json.loads(Path("firebase/database.rules.json").read_text("utf-8"))
CODE=TARGET["rules"]["premiumRooms"]["$code"]
FIELDS=("mode","level","topic","size","seconds","boardId","quizSeed")
def hashdoc(doc):
    return hashlib.sha256(json.dumps(doc,sort_keys=True,separators=(",",":")).encode()).hexdigest()
def remote(auth):
    auth.refresh(Request())
    uri=URL+"?access_token="+urllib.parse.quote(auth.token,safe="")
    try:
        with urllib.request.urlopen(uri,timeout=35) as response:
            doc=json.loads(response.read().decode("utf-8"))
    except (urllib.error.HTTPError,urllib.error.URLError,TimeoutError,ValueError):
        sys.exit("Cannot securely fetch current Firebase rules. Nothing published.")
    if not isinstance(doc,dict) or not isinstance(doc.get("rules"),dict):
        sys.exit("Current Firebase rules malformed. Nothing published.")
    return doc

credentials,project=google.auth.default(scopes=[
    "https://www.googleapis.com/auth/cloud-platform",
    "https://www.googleapis.com/auth/firebase.database"
])
if project and project!=PROJECT:sys.exit("Wrong Firebase project. Nothing published.")
live=remote(credentials)
room=live.get("rules",{}).get("premiumRooms",{}).get("$code",{})
if not room:sys.exit("Live Arena rules missing. Manual review required; NO DEPLOY.")
check=room.get("size",{}).get(".validate","")
if not all("newData.val() === "+str(n) in check for n in (5,10,12,15,20)):
    sys.exit("Live size validator does not allow all quiz sizes. Manual review required; NO DEPLOY.")
def applied(room):
    return all(room.get(k,{}).get(".write")==CODE[k][".write"] for k in FIELDS) and room.get("configRevision")==CODE["configRevision"]
if applied(room):
    print("VERIFIED: live question-count rules already deployed; SHA-256",hashdoc(live))
    sys.exit(0)
if any(room.get(k,{}).get(".write") is not False for k in FIELDS):
    sys.exit("Live room-setting permissions differ from expected baseline. NO DEPLOY.")
if "configRevision" in room:
    sys.exit("Live configRevision rules already exist and differ. NO DEPLOY.")
merged=copy.deepcopy(live)
newroom=merged["rules"]["premiumRooms"]["$code"]
for key in FIELDS:newroom[key][".write"]=CODE[key][".write"]
newroom["configRevision"]=CODE["configRevision"]
# Ensure any other LIVE policy fields remain EXACTLY untouched.
checkcopy=copy.deepcopy(merged)
restore=checkcopy["rules"]["premiumRooms"]["$code"]
for key in FIELDS:restore[key][".write"]=False
del restore["configRevision"]
assert checkcopy==live
out=Path("firebase/live-merged-quizcount-183.json")
out.write_text(json.dumps(merged,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
Path("firebase.json").write_text(json.dumps({"database":{"rules":str(out)}}),encoding="utf-8")
print("Validated merge, all other live rules preserved. Before SHA-256:",hashdoc(live))
subprocess.run(["npx","--yes","firebase-tools@latest","deploy","--only","database","--project",PROJECT,"--non-interactive"],check=True)
now=remote(credentials)
if now!=merged:sys.exit("Rules upload attempted but live policy differs. Manual verification needed.")
print("PASS VERIFIED LIVE Arena question-count edit permission:",PROJECT,INSTANCE)
print("After SHA-256:",hashdoc(now))
