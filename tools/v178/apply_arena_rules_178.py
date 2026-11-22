#!/usr/bin/env python3
"""Overlay strict additions to RTDB Arena v2 rules after v175 base policies."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[2]
p=ROOT/"source/database.rules.json"
s=json.loads(p.read_text("utf-8"))
r=s["rules"]["premiumRooms"]["$code"]
old="data.val() === 'lobby' && newData.val() === 'playing' && data.parent().child('guestUid').exists()"
assert old in r["phase"][".write"]
assert "ready" not in r and "hostAvatar" not in r and "guestAvatar" not in r
ids=["astra","piko","nobi","tera","lopi","tomba","foksi","moka","pera","gigi","robot","fox","owl"]
allowed="newData.isString() && ("+" || ".join("newData.val() === '"+x+"'" for x in ids)+")"
revision="data.parent().parent().child('configRevision')"
r["ready"]={"$uid":{
    ".write":"auth != null && auth.uid === $uid && (data.parent().parent().child('hostUid').val() === auth.uid || data.parent().parent().child('guestUid').val() === auth.uid) && data.parent().parent().child('phase').val() === 'lobby' && ((!newData.exists()) || (newData.isNumber() && newData.val() === ("+revision+".exists() ? "+revision+".val() : 1)))",
    ".validate":"newData.isNumber() && newData.val() === (newData.parent().parent().child('configRevision').exists() ? newData.parent().parent().child('configRevision').val() : 1)"
}}
r["hostAvatar"]={".write":"auth != null && auth.uid === data.parent().child('hostUid').val() && data.parent().child('phase').val() === 'lobby' && newData.exists()",".validate":allowed}
r["guestAvatar"]={".write":"auth != null && auth.uid === data.parent().child('guestUid').val() && data.parent().child('phase').val() === 'lobby' && newData.exists()",".validate":allowed}
r["phase"][".write"]=r["phase"][".write"].replace(old,old+" && (data.parent().child('rulesVersion').val() !== 2 || (data.parent().child('ready').child(data.parent().child('hostUid').val()).val() === data.parent().child('configRevision').val() && data.parent().child('ready').child(data.parent().child('guestUid').val()).val() === data.parent().child('configRevision').val()))",1)
official=json.loads((ROOT/"firebase/database.rules.json").read_text("utf-8"))
# The social v180 profile summary is additive and does not alter Arena policies.
# Compare every pre-existing security path EXACTLY before importing the new
# mutual-friends-only subtree into the assembled Android source.
friend_profiles=official.get("rules",{}).get("friendProfiles")
hint_policy=official["rules"]["premiumRooms"]["$code"].get("hintUsed")
# Independently emulated question-count policy only permits host changes while
# still in lobby with an atomic configRevision increment.
changes=("mode","level","topic","size","seconds","boardId","quizSeed")
live_approved=official["rules"]["premiumRooms"]["$code"]
for key in changes:
    original=s["rules"]["premiumRooms"]["$code"][key]
    current=live_approved[key]
    assert original[".write"] is False, key
    assert current[".validate"] == original[".validate"], "Unexpected validation: "+key
    assert set(current)==set(original), "Unexpected policy keys: "+key
    rule=current[".write"]
    assert isinstance(rule,str) and "auth.uid" in rule
    assert "child('hostUid').val() === auth.uid" in rule
    assert "child('phase').val() === 'lobby'" in rule
    assert "child('configRevision').val() === data.parent().child('configRevision').val() + 1" in rule
assert "configRevision" not in s["rules"]["premiumRooms"]["$code"]
revision=live_approved["configRevision"]
assert revision[".write"]=="auth != null && data.isNumber() && data.parent().child('hostUid').val() === auth.uid && data.parent().child('phase').val() === 'lobby' && newData.isNumber() && newData.val() === data.val() + 1"
assert revision[".validate"].startswith("newData.isNumber()")
verified=json.loads(json.dumps(official))
verified["rules"].pop("friendProfiles",None)
premium_verified=verified["rules"]["premiumRooms"]["$code"]
premium_verified.pop("hintUsed",None)
premium_verified.pop("configRevision",None)
# Package 6 is applied only AFTER the complete v178-v185 historical assembly.
# Ignore its separate additive subtree when checking this earlier rule revision.
premium_verified.pop("package6",None)
for key in changes:
    premium_verified[key][".write"]=False
assert verified==s,"Existing RTDB Arena rules differ from reviewed source; refuse to overwrite."
if friend_profiles is not None:
    assert friend_profiles["$uid"][".write"]=="auth != null && auth.uid === $uid"
    assert "root.child('friends')" in friend_profiles["$uid"][".read"]
    s["rules"]["friendProfiles"]=friend_profiles
# Carry forward only the exact revision-gated host settings authorized by
# the Firebase emulator test, preserving all other validation rules.
for key in changes:
    s["rules"]["premiumRooms"]["$code"][key][".write"]=live_approved[key][".write"]
s["rules"]["premiumRooms"]["$code"]["configRevision"]=revision
if hint_policy is not None:
    exact=hint_policy["$uid"]["$index"]
    expression=exact[".write"]
    assert "auth.uid === $uid" in expression
    assert "!data.exists()" in expression
    assert "child('size').val() === 20" in expression and "child('size').val() === 50" in expression
    assert "child('phase').val() === 'playing'" in expression
    assert "child('turn').child('uid').val() === auth.uid" in expression
    assert exact[".validate"]=="newData.val() === true"
    s["rules"]["premiumRooms"]["$code"]["hintUsed"]=hint_policy
p.write_text(json.dumps(s,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print("PASS: exact pre-existing RTDB rules, additive friend privacy, 10 avatars and both-ready start.")
