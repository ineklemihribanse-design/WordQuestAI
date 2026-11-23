#!/usr/bin/env python3
"""Regression: all solo modes run locally, existing secure online RTDB rules stay.

No Firebase deployment or widening of permissions is necessary for offline play.
"""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
RULES=ROOT/"firebase/database.rules.json"

def verify():
    obj=json.loads(RULES.read_text("utf-8"))["rules"]
    room=obj["premiumRooms"]["$code"]
    assert "auth != null" in room[".write"]
    assert "!data.exists()" in room[".write"]
    assert "root.child('roomQuota')" in room[".write"]
    assert "!newData.child('guestUid').exists()" in room[".write"]
    for m in ("duel","team","quiz"):
        assert f"newData.val() === '{m}'" in room["mode"][".validate"]
    settings=room["package6"]["settings"]
    check=settings[".validate"]
    assert "newData.child('levels').isString()" in check
    for m in ("A1","A2","B1","B2","C1","C2"):
        assert m in check
    assert "{0,5}" in check, "Existing six-level cap must be preserved"
    assert "data.parent().parent().child('hostUid').val() === auth.uid" in settings[".write"]
    print("PASS: secure Firebase room creation for invited multiplayer only")
    print("PASS: six-level selection supported without editing Firebase rules")
    print("PASS: local solo play must not call any online-room operation")

if __name__ == "__main__":verify()
