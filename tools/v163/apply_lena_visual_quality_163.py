#!/usr/bin/env python3
"""Stage Lena 1.0.63 visual-quality update on the signed v1.0.62 codebase.

Reuses the 1.0.62 working voice assistant, API bridge and base rig.
The *staged* GLB is upgraded with real irises/pupils and targeted mouth/eye
geometry. Original v6 GLB remains untouched; no extra textures or databases.
"""
from pathlib import Path
import hashlib,json,subprocess,sys,tempfile

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
PKG=APP/"src/main/java/com/kelimebulmacasi/a1"
GRADLE=APP/"build.gradle.kts"
SOURCE=ROOT/"assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
GLB=APP/"src/main/assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
META=GLB.parent/"animation-map.json"

def once(s,a,b,label):
    if s.count(a)!=1: raise AssertionError(f"{label}: expected one, got {s.count(a)}")
    return s.replace(a,b,1)

def main():
    g=GRADLE.read_text("utf-8")
    assert g.count("versionCode = 87")==1
    assert g.count('versionName = "1.0.62-lena-precise-face"')==1
    assert 'applicationIdSuffix = ".fresh"' in g
    assert GLB.exists() and META.exists()
    assert SOURCE.exists()
    for name in ("Teacher2DPortrait.kt","ArenaApprovedHome.kt","VocabularyNeonHome.kt",
                 "LenaLive3DStage.kt","LenaIsolatedVoiceTeacherActivity.kt",
                 "TeacherCredentialBridgeProvider.kt","LenaFaceAnimationClips.kt"):
        assert (PKG/name).is_file(),name
    original_hash=hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    assert original_hash=="548d70117df4599300c77dc3fde3c72a2111f78ff582e741862c25fcdf364617"
    m=json.loads(META.read_text("utf-8"))
    assert m["baseAssetSha256"]==original_hash
    assert m["sha256"]==hashlib.sha256(GLB.read_bytes()).hexdigest()

    # New primitive materials + eyelids/pupils are attached to the same
    # skinned mesh. The original v1.0.62 brow/cheek morphs are removed.
    with tempfile.TemporaryDirectory(prefix="wq-v163-") as td:
        temp=Path(td)/"colored_eyes.glb"
        subprocess.run([sys.executable,
                        str(ROOT/"tools/v163/upgrade_lena_163.py"),
                        str(GLB),str(temp),"--manifest",str(META)],check=True)
        GLB.write_bytes(temp.read_bytes())

    subprocess.run([sys.executable,str(ROOT/"tools/verify_lena_glb.py"),str(GLB)],check=True)
    m=json.loads(META.read_text("utf-8"))
    assert m["sha256"]==hashlib.sha256(GLB.read_bytes()).hexdigest()
    assert m["baseAssetSha256"]==original_hash
    assert set(m["facialAnimations"])=={
        "Lena_V162_Face_IdleBlink","Lena_V162_Face_TalkBlink"}
    assert (PKG/"LenaFaceAnimationClips.kt").read_text("utf-8").count(
        "Lena_V162_Face_TalkBlink")==1

    g=once(g,"versionCode = 87","versionCode = 88","monotonic v163 code")
    g=once(g,'versionName = "1.0.62-lena-precise-face"',
           'versionName = "1.0.63-lena-eyes-brown-hair"',"v163 name")
    GRADLE.write_text(g,"utf-8")
    print("v1.0.63 Lena visual upgrade staged: real eyes, blue irises, dark pupils, precise blink and mouth, brown hair.")
    print("Preserved: Alex, signed package, conversation threads, saved AI providers, Arena, Word Memory.")
    print("APK Lena GLB size:",GLB.stat().st_size)

if __name__=="__main__":main()
