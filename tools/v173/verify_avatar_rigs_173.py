#!/usr/bin/env python3
"""Release gate for user-uploaded WordQuest 1.0.73 avatars.

Candidate mode inspects any staged GLBs and reports missing models or licenses.
Release mode refuses to pass unless every original user model is available,
skinned, animated, manually approved, licensed, and Android-renderer tested.
Do not add NC licensed models to the build for commercial distribution.
"""
import argparse
import hashlib
import json
import struct
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
MANIFEST=Path(__file__).with_name("autorig_10_qa_manifest.json")
ASSETS=ROOT/"assets/avatars/wordquest_v173_qa"
REQUIRED={"idle","select","wave","celebrate","trophy_raise","loss"}

def glb_metadata(path):
    with path.open("rb") as f:
        header=f.read(12)
        assert len(header)==12,"GLB header truncated"
        magic,version,total=struct.unpack("<4sII",header)
        assert magic==b"glTF" and version==2,"Not glTF 2.0"
        assert path.stat().st_size==total,"GLB size mismatch"
        chunk_len,chunk_type=struct.unpack("<I4s",f.read(8))
        assert chunk_type==b"JSON","Missing JSON chunk"
        doc=json.loads(f.read(chunk_len))
    return doc

def verify_file(record,path):
    if path.stat().st_size!=record["bytes"]:
        raise AssertionError("file size mismatched: "+str(path))
    with path.open("rb") as f:
        digest=hashlib.file_digest(f,"sha256").hexdigest()
    if digest!=record["sha256"]:
        raise AssertionError("SHA256 mismatched: "+str(path))
    obj=glb_metadata(path)
    assert len(obj.get("skins",[]))==record["skinCount"],path
    assert {a.get("name") for a in obj.get("animations",[])}==REQUIRED,path
    assert all(len(s.get("joints",[]))>=record["joints"] for s in obj["skins"]),path
    mesh_nodes=[n for n in obj.get("nodes",[]) if "mesh" in n]
    assert all("skin" in node for node in mesh_nodes if
        not obj["meshes"][node["mesh"]].get("name","").startswith("WQ173_Trophy")),path
    assert any("Trophy" in n.get("name","") for n in obj["nodes"]),path
    for skin in obj["skins"]:
        assert "inverseBindMatrices" in skin
    return True

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--release",action="store_true",
        help="Fail closed until actual production shipping permissions and QA.")
    parser.add_argument("--candidate",action="store_true",
        help="Report available GLB QA sources and pending blockers.")
    opt=parser.parse_args()
    d=json.loads(MANIFEST.read_text("utf-8"))
    names={x["id"] for x in d["characters"]}
    assert len(d["characters"])==10 and len(names)==10
    assert REQUIRED==set(d["requiredClips"])
    assert d["requiredJoints"]>=18
    pending=[]
    for item in d["characters"]:
        path=ASSETS/item["file"]
        if not path.exists():
            pending.append(item["id"]+": asset not in repository")
        else:
            try:
                verify_file(item,path)
            except Exception as exc:
                pending.append(item["id"]+": "+repr(exc))
        if item["publicationBlocked"] or item["license"]=="unverified":
            pending.append(item["id"]+": publication rights not cleared")
        if not item["manualRigQAApproved"]:
            pending.append(item["id"]+": manual skin/motion QA incomplete")
        if not item["androidRendererQAApproved"]:
            pending.append(item["id"]+": real Android 3D renderer QA incomplete")
    print("WordQuest 1.0.73: "+str(len(d["characters"]))+" user-source avatars")
    for item in pending:print("PENDING",item)
    if opt.release:
        if pending:raise SystemExit("RELEASE BLOCKED: "+str(len(pending))+" open gates")
        print("RELEASE GATE PASSED")
    else:
        # A candidate build still runs and preserves the last published APK.
        print("CANDIDATE STATUS:",len(pending),"pending checks — no 1.0.73 release")
    return 0

if __name__=="__main__":
    raise SystemExit(main())
