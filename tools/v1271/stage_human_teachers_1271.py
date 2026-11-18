#!/usr/bin/env python3
"""Stage validated Rocketbox human 3D teacher assets in restored Android source.

Must run AFTER the visual-source audit, WebP-to-PNG/JPEG normalization,
bake_human_visemes_1271.py, and v1.0.27 SceneView Kotlin stage install.
No APK, release, or automatic user update is performed by this script.

A human reviewer still must approve the faces, outfits and natural movements.
"""
from pathlib import Path
from shutil import copy2
import hashlib,json,struct

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"candidate-human-teachers"
DEST=ROOT/"source/app/src/main/assets/avatars"
STAGE=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1/TeacherStage3D.kt"
NOTICES=ROOT/"tools/v127/ROCKETBOX_MODEL_NOTICES.md"

def read_doc(path):
    b=Path(path).read_bytes()
    assert b[:4]==b"glTF" and struct.unpack_from("<I",b,8)[0]==len(b)
    size,tag=struct.unpack_from("<II",b,12)
    assert tag==0x4E4F534A
    return json.loads(b[20:20+size].decode("utf-8"))

def main():
    assert STAGE.is_file() and "ModelNode" in STAGE.read_text()
    assert NOTICES.is_file()
    DEST.mkdir(parents=True,exist_ok=True)
    meta=[]
    names={"emma":"Emma","sophia":"Sophia","ethan":"Ethan"}
    for actor,name in names.items():
        candidate=SRC/actor/(actor+"-animated.glb")
        data=SRC/actor/"AUDIT.json"
        lipinfo=SRC/actor/(actor+"-animated.visemes.json")
        assert candidate.is_file() and data.is_file() and lipinfo.is_file(),actor
        orig=json.loads(data.read_text())
        mouth=json.loads(lipinfo.read_text())
        doc=read_doc(candidate)
        assert orig["actor"]==actor and orig["visual_accepted"] is False
        assert orig["source_license"].startswith("MIT")
        assert "memoriesrohit/avatar-studio" in orig["source"]
        assert "VRM" not in doc.get("extensions",{}),"Anime/VRoid inputs prohibited"
        assert "EXT_texture_webp" not in doc.get("extensionsRequired",[])
        assert len(doc.get("skins",[]))>=1
        assert len(doc.get("animations",[]))==28
        assert len(mouth["viseme_clips"])==7 and len(mouth["original_animations"])==13
        assert mouth["blink_clip"]=="Teacher_Blink"
        assert len(mouth["composite_blink_clips"])==7
        assert all(x.startswith("Teacher_Viseme_") and x.endswith("_Blink")
                   for x in mouth["composite_blink_clips"])
        assert len(mouth["active_face_nodes"])>=1
        assert all(x["targets"]==52 for x in mouth["active_face_nodes"])
        copy2(candidate,DEST/(actor+".glb"))
        meta.append({"actor":actor,"label":name,"file":actor+".glb",
                     "source_url":orig["source"],"license":"MIT",
                     "original_animation_names":mouth["original_animations"],
                     "facial_morph_targets":52,
                     "embedded_mouth_clips":["Teacher_Viseme_"+x for x in mouth["viseme_clips"]],
                     "skin_count":len(doc["skins"]),
                     "blink_clip":mouth["blink_clip"],
                     "composite_blink_clips":mouth["composite_blink_clips"],
                     "sha256":hashlib.sha256(candidate.read_bytes()).hexdigest(),
                     "visual_approved":False})
    (DEST/"AVATAR_PROVENANCE.json").write_text(json.dumps({
        "version":"1.0.27.1-human-model-prototype",
        "status":"UNAPPROVED_VISUAL_QA","family":"Microsoft Rocketbox / Avatar Studio",
        "models":meta,"no_anime_or_vroid_assets":True,
        "user_review_required":True},ensure_ascii=False,indent=2)+"\n")
    (DEST/"AVATAR_LIPSYNC_METADATA.json").write_text(json.dumps({
        "quality":"TEXT_RANGE_APPROXIMATION",
        "original_audio_not_modified":True,
        "actors":[{"actor":x["actor"],"clips":x["embedded_mouth_clips"],
                   "rigs":x["skin_count"],"targetCount":x["facial_morph_targets"],
                   "blink_clip":x["blink_clip"],
                   "composite_blink_clips":x["composite_blink_clips"]}
                  for x in meta]},indent=2,ensure_ascii=False)+"\n")
    copy2(NOTICES,DEST/"HUMAN_3D_LICENSE_NOTICES.md")
    # Old 1.0.27 renderer retained; only model source description corrected.
    stage=STAGE.read_text()
    assert "Requires offline licensed VRoid assets validated by tools/v127." in stage
    stage=stage.replace("Requires offline licensed VRoid assets validated by tools/v127.",
        "Requires licensed Microsoft Rocketbox human GLBs with ARKit facial animations.")
    stage=stage.replace("Distinct facial morphs and neutral arms are baked into the GLB clips.",
        "Original rigged movements and independent ARKit mouth clips remain available.")
    STAGE.write_text(stage)
    print("Staged three fully rigged human candidates and notices; no anime assets")
    print("VISUAL APPROVAL PENDING — NO APK / NO RELEASE")
if __name__=="__main__":main()
