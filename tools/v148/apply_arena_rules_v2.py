#!/usr/bin/env python3
"""Prepare Firebase Realtime Database rules for Arena v2.

This only generates the correct staged JSON. Publishing production Firebase rules
is a separate, privileged deployment and must not be pretended to have happened.
"""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[2]
RULES=ROOT/"source/database.rules.json"

def main():
    data=json.loads(RULES.read_text(encoding="utf-8"))
    room=data["rules"]["premiumRooms"]["$code"]
    if "rulesVersion" in room:
        raise RuntimeError("Arena v2 rules were already applied")
    room["rulesVersion"]={
        ".write":False,
        ".validate":"newData.isNumber() && newData.val() === 2"
    }
    mascot_valid="newData.val() === 'robot' || newData.val() === 'fox' || newData.val() === 'owl'"
    room["hostAvatar"]={".write":False,".validate":mascot_valid}
    room["guestAvatar"]={
        ".write":(
            "auth != null && auth.uid === data.parent().child('guestUid').val() && "
            "data.parent().child('phase').val() === 'lobby' && "
            "!data.exists()"),
        ".validate":mascot_valid
    }
    base="data.parent().parent().parent()"
    is_member=(
        "(auth.uid === "+base+".child('hostUid').val() || "
        "auth.uid === "+base+".child('guestUid').val())")
    allowed_turn=(
        "("+base+".child('turn').child('uid').val() === auth.uid && "
        "("+base+".child('turn').child('until').val() === 0 || "
        "now <= "+base+".child('turn').child('until').val()))")
    match_conditions=(
        "auth != null && "+is_member+
        " && "+base+".child('phase').val() === 'playing'"+
        " && "+base+".child('mode').val() === 'duel'"+
        " && "+base+".child('rulesVersion').val() === 2"+
        " && !"+base+".child('solved').child($index).exists()"+
        " && "+allowed_turn)
    first_failed=lambda who: (
        "("+base+".child('unknown').child($index).child("+who+").val() === true || "
        +base+".child('mistakes').child($index).child("+who+").val() >= 1)")
    index_rule="$index.matches(/^([0-9]|[1-4][0-9])$/)"
    room["replay"]={"$index":{"$uid":{
        ".write":(match_conditions+
            " && "+index_rule+
            " && auth.uid === $uid && !data.exists() && "+
            first_failed(base+".child('hostUid').val()")+" && "+
            first_failed(base+".child('guestUid').val()")),
        ".validate":"newData.isString() && (newData.val() === 'wrong' || newData.val() === 'pass')"
    }}}
    # For hints, the path is hintUsed/$uid/$index rather than replay/$index/$uid.
    max_hints="("+base+".child('size').val() === 20 ? 3 : "+base+".child('size').val() === 30 ? 4 : "+base+".child('size').val() === 40 ? 5 : 6)"
    room["hintUsed"]={"$uid":{"$index":{
        ".write":(match_conditions+" && "+index_rule+
            " && auth.uid === $uid && !data.exists() && "+
            "data.parent().numChildren() < "+max_hints),
        ".validate":"newData.val() === true"
    }}}
    room["ready"]={"$uid":{
        ".write":(
            "auth != null && auth.uid === $uid && "
            "(auth.uid === data.parent().parent().child('hostUid').val() || "
            "auth.uid === data.parent().parent().child('guestUid').val()) && "
            "data.parent().parent().child('phase').val() === 'lobby'"),
        ".validate":"newData.isBoolean()"
    }}
    # A legacy room can still finish with the old reveal behavior.
    # New rooms MUST have both replay outcomes before revealing a word.
    solved=room["solved"]["$index"]
    needle="newData.val() === 'reveal' &&"
    if solved[".write"].count(needle)!=1:
        raise RuntimeError("Existing solved/reveal policy changed")
    root="data.parent().parent()"
    both_retries=(
        "("+root+".child('replay').child($index).child("+
        root+".child('hostUid').val()).exists() && "+
        root+".child('replay').child($index).child("+
        root+".child('guestUid').val()).exists())")
    addition=(
        needle+" ("+root+".child('rulesVersion').val() !== 2 || "+
        both_retries+") &&")
    solved[".write"]=solved[".write"].replace(needle,addition,1)
    RULES.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    assert room["ready"]["$uid"][".validate"]=="newData.isBoolean()"
    assert "replay" in room and "hintUsed" in room
    print("Staged Arena v2 Firebase rules (not deployed).")

if __name__=="__main__":
    main()
