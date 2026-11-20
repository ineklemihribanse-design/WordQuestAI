#!/usr/bin/env python3
"""Check the staged Firebase security policy is limited by selected quiz size."""
from pathlib import Path
import json
p=Path(__file__).resolve().parents[2]/"source/database.rules.json"
rules=json.loads(p.read_text("utf-8"))["rules"]
room=rules["premiumRooms"]["$code"]
size=room["size"][".validate"]
expr=room["answers"]["$uid"]["$index"][".write"]
assert "mode').val() !== 'quiz'" in size
assert "newData.val() === 12" in size
for n in (5,10,15,20): assert "newData.val() === "+str(n) in size
assert "newData.val() === 30" in size and "newData.val() === 50" in size
for n in (5,10,12,15,20):
    assert "child('size').val() === "+str(n) in expr
assert "auth.uid === $uid" in expr
assert "data.parent().parent().parent().child('phase').val() === 'playing'" in expr
assert "roomQuota" in rules
print("PASS: staged Firebase rules enforce real question bounds while retaining UID, phase and quota security.")
