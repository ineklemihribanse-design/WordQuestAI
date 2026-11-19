#!/usr/bin/env python3
"""Rebuild Lena corrected lips and saturated linear colors into v1.0.62.
Add only replacement model asset & targeted Kotlin clip/lighting patches.
Use signed v1.0.61 codebase and preserve every other application module.
"""
from pathlib import Path
import hashlib,json,subprocess,sys
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
PKG=APP/"src/main/java/com/kelimebulmacasi/a1"
TEST=APP/"src/test/java/com/kelimebulmacasi/a1"
GLB=APP/"src/main/assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
MAPPING=GLB.parent/"animation-map.json"
SOURCE=ROOT/"assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
GRADLE=APP/"build.gradle.kts"
V162=ROOT/"tools/v162"

def once(s,old,new,label):
    if s.count(old)!=1:
        raise AssertionError(f"{label}: one exact anchor expected; found {s.count(old)}")
    return s.replace(old,new,1)

def main():
    g=GRADLE.read_text("utf-8")
    assert g.count("versionCode = 86")==1
    assert g.count('versionName = "1.0.61-lena-expression-colors"')==1
    assert 'applicationIdSuffix = ".fresh"' in g
    assert SOURCE.is_file() and GLB.is_file() and MAPPING.is_file()
    assert SOURCE.read_bytes()[:4]==b"glTF"
    assert (PKG/"TeacherCredentialBridgeProvider.kt").is_file()
    for f in ("Teacher2DPortrait.kt","ArenaApprovedHome.kt","VocabularyNeonHome.kt"):
        assert (PKG/f).is_file(),f
    assert hashlib.sha256(SOURCE.read_bytes()).hexdigest()=="548d70117df4599300c77dc3fde3c72a2111f78ff582e741862c25fcdf364617"

    # v161 is overbroad. Overwrite staged GLB from ORIGINAL v6, never from
    # existing v161 so masks/materials cannot accumulate or bloat.
    subprocess.run([
        sys.executable,str(V162/"rebuild_lena_162.py"),
        str(SOURCE),str(GLB),str(MAPPING),
    ],check=True)
    subprocess.run([sys.executable,str(ROOT/"tools/verify_lena_glb.py"),str(GLB)],check=True)
    m=json.loads(MAPPING.read_text("utf-8"))
    assert "Lena_V162_Face_TalkBlink" in m["facialAnimations"]
    assert "Lena_V162_Face_IdleBlink" in m["facialAnimations"]
    assert m["sha256"]==hashlib.sha256(GLB.read_bytes()).hexdigest()

    for target in (PKG/"LenaFaceAnimationClips.kt",
                   TEST/"LenaFaceAnimationClipsTest.kt"):
        s=target.read_text("utf-8")
        assert s.count("Lena_V161_Face_TalkBlink")>=1
        assert s.count("Lena_V161_Face_IdleBlink")>=1
        s=s.replace("Lena_V161_Face_TalkBlink","Lena_V162_Face_TalkBlink")
        s=s.replace("Lena_V161_Face_IdleBlink","Lena_V162_Face_IdleBlink")
        target.write_text(s,"utf-8")

    # Original 100000 intensity flattened skin and costume colors into
    # pale white. Keep HDR light but reduce intensity for color contrast.
    stage=PKG/"LenaLive3DStage.kt"
    s=stage.read_text("utf-8")
    s=once(s,"intensity = 100_000f","intensity = 50_000f","scene light")
    stage.write_text(s,"utf-8")

    g=once(g,"versionCode = 86","versionCode = 87","code")
    g=once(g,'versionName = "1.0.61-lena-expression-colors"',
           'versionName = "1.0.62-lena-precise-face"', "name")
    GRADLE.write_text(g,"utf-8")
    print("v1.0.62 staged corrected true lip-only face, eyelid-only blink, linear colors and softer HDR light.")
    print("Model bytes",GLB.stat().st_size,"original source bytes",SOURCE.stat().st_size)
    print("No changes to Alex, chat, saved AI key, Word Memory, Arena or app identity.")

if __name__=="__main__":main()
