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
    mascot_valid="newData.val() === 'robot' || newData.val() === 'fox' || newData.val() === 'owl' || newData.val() === 'rabbit' || newData.val() === 'bear'"
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
    # RTDB security rules do not provide numChildren(); count existing allowed
    # indices explicitly. This is bounded to 50 supported crossword words.
    count_hints="("+" + ".join(
        "(data.parent().child('"+str(i)+"').exists() ? 1 : 0)"
        for i in range(50)
    )+")"
    room["hintUsed"]={"$uid":{"$index":{
        ".write":(match_conditions+" && "+index_rule+
            " && auth.uid === $uid && !data.exists() && "+
            count_hints+" < "+max_hints),
        ".validate":"newData.val() === true"
    }}}
    # Host can adjust the room configuration while both players are in lobby.
    # The monotonic revision invalidates old ready acknowledgements automatically.
    host_lobby=(
        "auth != null && auth.uid === data.parent().child('hostUid').val() "
        "&& data.parent().child('phase').val() === 'lobby' "
        "&& data.parent().child('rulesVersion').val() === 2"
    )
    for key in ("mode","level","topic","size","seconds","boardId","quizSeed"):
        room[key][".write"]=host_lobby
    room["configRevision"]={
        ".write":host_lobby,
        ".validate":(
            "newData.isNumber() && newData.val() >= 1 "
            "&& newData.val() <= 99999 && "
            "newData.val() === (data.exists() ? data.val() + 1 : 1)"
        )
    }
    # Security gate: the room may start only if BOTH players acknowledged
    # the current revision. Client-side disabled buttons are not sufficient.
    phase=room["phase"]
    old_phase_rule=phase[".write"]
    marker="data.val() === 'lobby' && newData.val() === 'playing' && data.parent().child('guestUid').exists()"
    if old_phase_rule.count(marker)!=1:
        raise RuntimeError("Lobby phase validation anchor changed")
    both_ready=(
        "data.parent().child('rulesVersion').val() !== 2 || "
        "(data.parent().child('ready').child(data.parent().child('hostUid').val()).val() === "
        "data.parent().child('configRevision').val() && "
        "data.parent().child('ready').child(data.parent().child('guestUid').val()).val() === "
        "data.parent().child('configRevision').val())"
    )
    phase[".write"]=old_phase_rule.replace(marker,marker+" && ("+both_ready+")",1)
    room["ready"]={"$uid":{
        ".write":(
            "auth != null && auth.uid === $uid && "
            "(auth.uid === data.parent().parent().child('hostUid').val() || "
            "auth.uid === data.parent().parent().child('guestUid').val()) && "
            "data.parent().parent().child('phase').val() === 'lobby'"),
        ".validate":(
            "newData.isNumber() && newData.val() === "
            "newData.parent().parent().child('configRevision').val()"
        )
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
    assert "configRevision" in room["ready"]["$uid"][".validate"]
    assert "replay" in room and "hintUsed" in room
    print("Staged Arena v2 Firebase rules (not deployed).")

if __name__=="__main__":
    main()
