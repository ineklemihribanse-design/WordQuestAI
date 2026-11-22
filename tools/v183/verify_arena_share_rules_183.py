#!/usr/bin/env python3
"""Fail-closed diff: only per-match custom IDs and opt-in previews may change RTDB rules.
Never deploy Firebase automatically and never weaken existing friend/quiz policies.
"""
import copy, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
old=json.loads((ROOT/"firebase/database.rules.json").read_text("utf-8"))
new=json.loads((ROOT/"firebase/database.rules.v183.json").read_text("utf-8"))
a=old["rules"]["premiumRooms"]["$code"]
b=new["rules"]["premiumRooms"]["$code"]
for key in ("hostAvatar","guestAvatar"):
    allow=" || (newData.isString() && newData.val().matches(/^custom_[a-f0-9]{32}$/))"
    assert b[key][".validate"]==a[key][".validate"]+allow, key
    b[key][".validate"]=a[key][".validate"]
    lobby=" && data.parent().child('phase').val() === 'lobby' && newData.exists()"
    during_game=" && (data.parent().child('phase').val() === 'lobby' || data.parent().child('phase').val() === 'playing') && newData.exists()"
    assert a[key][".write"].count(lobby)==1, key
    assert b[key][".write"]==a[key][".write"].replace(lobby,during_game), key
    b[key][".write"]=a[key][".write"]
for side in ("host","guest"):
    k=side+"MascotPreview"
    assert k not in a and k in b
    node=b.pop(k)
    assert set(node)=={".write",".validate"}
    assert "auth.uid === data.parent().child('"+side+"Uid').val()" in node[".write"]
    assert "data.parent().child('"+side+"Avatar')" in node[".write"]
    assert "!newData.exists()" in node[".write"]
    assert "newData.val().length <= 18050" in node[".validate"]
    assert "custom_[a-f0-9]{32}" in node[".validate"]
    assert "newData.val().beginsWith(data.parent().child('"+side+"Avatar').val() + ':')" in node[".validate"]
assert new==old, "Unexpected existing Firebase policy change"
print("PASS: only 2 custom-avatar validations, owner-only in-game avatar edits and 2 participant preview nodes added")
print("NOT DEPLOYED: owner must separately test/publish firebase/database.rules.v183.json")
