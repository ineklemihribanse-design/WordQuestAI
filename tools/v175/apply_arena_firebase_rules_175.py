#!/usr/bin/env python3
"""Stage (never deploy) v1.0.75 multi-size quiz Realtime Database rules.

Firebase's currently deployed rules may accept only 12 questions. This file
prepares updated validated rules for manual controlled deployment, with no
token, private key or live Firebase mutations.
"""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[2]
source=root/"source/database.rules.json"
raw=json.loads(source.read_text("utf-8"))
room=raw["rules"]["premiumRooms"]["$code"]
size=room["size"]
answer=room["answers"]["$uid"]["$index"]
old_size=size[".validate"]
assert "newData.val() === 12" in old_size and "quiz" in old_size
size[".validate"]=(
    "(newData.parent().child('mode').val() !== 'quiz' && "
    "(newData.val() === 20 || newData.val() === 30 || "
    "newData.val() === 40 || newData.val() === 50)) || "
    "(newData.parent().child('mode').val() === 'quiz' && "
    "(newData.val() === 5 || newData.val() === 10 || "
    "newData.val() === 12 || newData.val() === 15 || newData.val() === 20))"
)
old_write=answer[".write"]
old_index="$index.matches(/^([0-9]|1[0-1])$/)"
assert old_index in old_write and "'quiz'" in old_write
parent="data.parent().parent().parent().child('size').val()"
checks=[
    (5,"/^([0-4])$/"),
    (10,"/^([0-9])$/"),
    (12,"/^([0-9]|1[0-1])$/"),
    (15,"/^([0-9]|1[0-4])$/"),
    (20,"/^([0-9]|1[0-9])$/"),
]
scoped="("+" || ".join(
    "("+parent+" === "+str(n)+" && $index.matches("+pattern+"))"
    for n,pattern in checks
)+")"
answer[".write"]=old_write.replace(old_index,scoped,1)
assert answer[".write"].count(scoped)==1
source.write_text(json.dumps(raw,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print("PASS: staged quiz rules for 5,10,12,15,20 only; other users, quota and room rules preserved.")
print("NOT DEPLOYED: Firebase owner must publish this rules file and test with two accounts.")
