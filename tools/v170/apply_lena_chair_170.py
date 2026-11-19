#!/usr/bin/env python3
"""1.0.70: replace the procedural-placeholder chair with the user's actual GLB.

This patch deliberately FAILS if the exact original user file isn't in GitHub.
It never claims the app contains furniture from a local ChatGPT attachment that
wasn't actually published to GitHub. Previous 1.0.69 features and Lena files remain.
"""
from pathlib import Path
import hashlib, shutil, subprocess, sys

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
PKG=APP/"src/main/java/com/kelimebulmacasi/a1"
GRADLE=APP/"build.gradle.kts"

def replace_once(data, old, new, label):
    n=data.count(old)
    if n!=1: raise AssertionError(f"{label}: expected 1 source anchor, got {n}")
    return data.replace(old,new,1)

def main():
    g=GRADLE.read_text("utf-8")
    assert g.count("versionCode = 94")==1
    assert g.count('versionName = "1.0.69-lena-premium-3d-studio"')==1
    assert 'applicationIdSuffix = ".fresh"' in g
    assert (PKG/"LenaLive3DStage.kt").exists()
    assert (PKG/"ArenaApprovedHome.kt").exists()
    assert (PKG/"VocabularyNeonHome.kt").exists()

    user_model=ROOT/"assets/studio/chairs_and_window_set_-_game_ready.glb"
    if not user_model.is_file():
        raise RuntimeError("Missing EXACT uploaded chair/window GLB. "
             "Upload the user file to assets/studio/chairs_and_window_set_-_game_ready.glb "
             "on feature/v170-lena-user-chair-asset-and-rig-qa before building.")
    # The stage script validates the original's SHA-256, author, license,
    # 2 source meshes, 4 original textures, and exact binary byte count.
    subprocess.run([sys.executable,str(ROOT/"tools/v170/stage_user_chair_170.py")],
                   check=True)
    if hashlib.sha256(user_model.read_bytes()).hexdigest()!=hashlib.sha256(
        (APP/"src/main/assets/studio"/user_model.name).read_bytes()).hexdigest():
        raise RuntimeError("User chair GLB was changed while packaging")

    original_lena=APP/"src/main/assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
    assert original_lena.stat().st_size > 1_000_000
    before=hashlib.sha256(original_lena.read_bytes()).hexdigest()
    portrait=APP/"src/main/res/drawable-nodpi/wq167_lena_portrait.png"
    portrait_before=hashlib.sha256(portrait.read_bytes()).hexdigest()

    shutil.copyfile(ROOT/"tools/v170/LenaLive3DStage.kt", PKG/"LenaLive3DStage.kt")
    staged=(PKG/"LenaLive3DStage.kt").read_text("utf-8")
    assert '"studio/chairs_and_window_set_-_game_ready.glb"' in staged
    assert 'if (userChairSet != null) add(userChairSet)' in staged
    assert 'AvatarMode.GREETING' in staged and '01_Idle_Natural_Breathing' in staged
    assert 'cameraPreset by remember { mutableIntStateOf(1) }' in staged
    assert hashlib.sha256(original_lena.read_bytes()).hexdigest()==before
    assert hashlib.sha256(portrait.read_bytes()).hexdigest()==portrait_before

    g=replace_once(g,"versionCode = 94","versionCode = 95","version")
    g=replace_once(g,'versionName = "1.0.69-lena-premium-3d-studio"',
        'versionName = "1.0.70-user-chair-safe-greeting"',"version name")
    GRADLE.write_text(g,"utf-8")
    print("1.0.70: exact user's CC-BY GLB chairs/window staged in 3D scene.")
    print("Lena bone rig unchanged. Device-reported broken wave withheld for safety.")
    print("Signed app, saved learning and conversation data untouched.")

if __name__=="__main__": main()
