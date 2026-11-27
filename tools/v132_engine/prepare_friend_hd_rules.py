#!/usr/bin/env python3
"""Offline-only merge for optional friend HD. Preserve all existing live rules."""
import argparse,copy,json
from pathlib import Path
READ="auth != null && (auth.uid === $uid || (root.child('friends').child(auth.uid).child($uid).val() === true && root.child('friends').child($uid).child(auth.uid).val() === true && root.child('friendProfiles').child($uid).child('avatarPreview').isString() && data.child('preview').val() === root.child('friendProfiles').child($uid).child('avatarPreview').val()))"
FRIEND_READ="auth != null && (auth.uid === $uid || (root.child('friends').child(auth.uid).child($uid).val() === true && root.child('friends').child($uid).child(auth.uid).val() === true))"
OWNER="auth != null && auth.uid === $uid"
PATTERN=r"/^custom_[a-f0-9]{32}:[A-Za-z0-9+\/=]+$/"
def validator(maximum):
    return {".validate":f"newData.isString() && newData.val().length >= 140 && newData.val().length <= {maximum} && newData.val().matches({PATTERN})"}
NODE={"$uid":{".read":READ,".write":OWNER,".validate":"newData.hasChildren(['preview','hd'])","preview":validator(18050),"hd":validator(160100),"$other":{".validate":False}}}
def merge(live):
    root=live.get("rules",{})
    profile=root.get("friendProfiles",{}).get("$uid",{})
    if profile.get(".read")!=FRIEND_READ or profile.get(".write")!=OWNER or "avatarPreview" not in profile:
        raise ValueError("Existing mutual-friend privacy contract not verified")
    for k in (".read",".write"):
        if root.get(k) not in (None,False):
            raise ValueError("Unexpected root access grant")
    if "friendMascotHd" in root and root["friendMascotHd"]!=NODE:
        raise ValueError("Existing HD rules differ; manual review required")
    out=copy.deepcopy(live)
    out["rules"]["friendMascotHd"]=copy.deepcopy(NODE)
    return out
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--live",required=True,type=Path);p.add_argument("--output",required=True,type=Path)
    args=p.parse_args()
    with args.output.open("x",encoding="utf-8") as f:
        json.dump(merge(json.loads(args.live.read_text("utf-8"))),f,ensure_ascii=False,indent=2)
        f.write("\n")
    print("Prepared optional HD rules; no Firebase deployment performed.")
if __name__=="__main__":main()
