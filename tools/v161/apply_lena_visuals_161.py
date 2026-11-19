#!/usr/bin/env python3
"""Integrate verified v1.0.61 Lena colors + real mouth/blink morph animations.

Runs after v1.0.60 source assembly. The untouched v6 GLB remains in git as
a recoverable original. Only the APK's staged copy is colored/animated.
Never change the Android package, keystore, existing data, Alex or Arena.
"""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
PKG=APP/"src/main/java/com/kelimebulmacasi/a1"
TEST=APP/"src/test/java/com/kelimebulmacasi/a1"
ASSETS=APP/"src/main/assets/avatars/lena"
SOURCE=ROOT/"assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
GLB=ASSETS/SOURCE.name
MAP=ASSETS/"animation-map.json"
GRADLE=APP/"build.gradle.kts"
V161=ROOT/"tools/v161"

def once(s,before,after,label):
    if s.count(before)!=1:
        raise AssertionError(f"{label}: expected one anchor, got {s.count(before)}")
    return s.replace(before,after,1)

def main():
    g=GRADLE.read_text("utf-8")
    assert g.count("versionCode = 85")==1
    assert g.count('versionName = "1.0.60-lena-thread-persistence"')==1
    assert 'applicationIdSuffix = ".fresh"' in g
    assert GLB.exists() and MAP.is_file()
    assert (PKG/"LenaLive3DStage.kt").is_file()
    assert (PKG/"LenaAnimationClips.kt").is_file()
    assert (PKG/"LenaIsolatedVoiceTeacherActivity.kt").is_file()
    assert (PKG/"TeacherCredentialBridgeProvider.kt").is_file()
    for name in ("Teacher2DPortrait.kt","ArenaApprovedHome.kt","VocabularyNeonHome.kt"):
        assert (PKG/name).is_file(),name

    source_sha=hashlib.sha256(SOURCE.read_bytes()).hexdigest()
    staged_map=json.loads(MAP.read_text("utf-8"))
    assert staged_map["sha256"]==source_sha, "Existing original Lena asset changed unexpectedly"
    assert hashlib.sha256(GLB.read_bytes()).hexdigest()==source_sha

    with tempfile.TemporaryDirectory(prefix="lena-v161-") as tmp:
        painted=Path(tmp)/"Lena_colored.glb"
        subprocess.run([sys.executable,str(V161/"paint_lena_glb.py"),str(SOURCE),
                        str(painted),"--manifest",str(MAP)],check=True)
        subprocess.run([sys.executable,str(V161/"add_lena_face_morphs.py"),
                        str(painted),str(GLB),"--manifest",str(MAP)],check=True)
    subprocess.run([sys.executable,str(ROOT/"tools/verify_lena_glb.py"),
                    str(GLB)],check=True)
    # The structural tests above verify 18 original+added clips; no need to
    # load a GL scene in CI (actual animated pixels need device verification).
    manifest=json.loads(MAP.read_text("utf-8"))
    assert manifest["baseAssetSha256"]==source_sha
    assert manifest["sha256"]==hashlib.sha256(GLB.read_bytes()).hexdigest()
    assert all(x in manifest["facialAnimations"]
               for x in ("Lena_V161_Face_IdleBlink","Lena_V161_Face_TalkBlink"))

    shutil.copyfile(V161/"LenaFaceAnimationClips.kt",PKG/"LenaFaceAnimationClips.kt")
    TEST.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(V161/"LenaFaceAnimationClipsTest.kt",
                    TEST/"LenaFaceAnimationClipsTest.kt")
    shutil.copyfile(V161/"LenaBodyAnimationClipsTest.kt",
                    TEST/"LenaAnimationClipsTest.kt")

    clips_file=PKG/"LenaAnimationClips.kt"
    clips=clips_file.read_text("utf-8")
    clips=once(clips,'private const val IDLE = "01_Idle_Natural_Breathing"',
               'private const val IDLE = "09_Resting_With_Blinks"',"precomposed breathing+blink")
    clips=once(clips,'AvatarMode.GREETING -> "02_Greeting_Gentle_Wave"',
               'AvatarMode.GREETING -> "10_Wave_With_Smile"',"greeting with smile")
    clips=once(clips,'AvatarMode.LISTENING -> "04_Listening_Nod"',
               'AvatarMode.LISTENING -> "12_Listening_With_Blinks"',"listening with blink")
    clips=once(clips,'AvatarMode.SPEAKING -> "03_Talking_Hand_Gestures"',
               'AvatarMode.SPEAKING -> "11_Talking_With_Blinks"',"talking body+eyes")
    clips_file.write_text(clips,"utf-8")

    stage=PKG/"LenaLive3DStage.kt"
    s=stage.read_text("utf-8")
    s=once(s,
        '''        val index = clips[clip] ?: return@LaunchedEffect
        runCatching {
            model.playingAnimations.keys.toList().forEach(model::stopAnimation)
            model.playAnimation(index,
                speed = if (motion == AvatarMode.SPEAKING)
                    speechRate.coerceIn(.7f, 1.3f) else 1f,
                loop = motion != AvatarMode.GREETING,
            )
        }.onFailure { Log.e("WQLenaLive", "Animation playback failure", it) }''',
        '''        val index = clips[clip] ?: return@LaunchedEffect
        // The v6 facial skeleton has zero weighted face vertices. The new
        // face animation animates glTF morph WEIGHTS; body only rotates bones.
        // They run together safely without two competing facial morph clips.
        val faceIndex = LenaFaceAnimationClips.select(motion, clips.keys)?.let { clips[it] }
        runCatching {
            model.playingAnimations.keys.toList().forEach(model::stopAnimation)
            model.playAnimation(index,
                speed = if (motion == AvatarMode.SPEAKING)
                    speechRate.coerceIn(.7f, 1.3f) else 1f,
                loop = motion != AvatarMode.GREETING,
            )
            if (faceIndex != null) {
                // SPEAKING is set from TextToSpeech.onStart/onPlaying, NEVER
                // from receiving AI text. When audio ends it switches to
                // idle blinks and the mouth returns to its closed rest pose.
                model.playAnimation(faceIndex,
                    speed = if (motion == AvatarMode.SPEAKING)
                        speechRate.coerceIn(.7f, 1.3f) else 1f,
                    loop = true,
                )
            }
        }.onFailure { Log.e("WQLenaLive", "Animation playback failure", it) }''',
        "concurrent body and actual mouth/blink morph animations",
    )
    stage.write_text(s,"utf-8")
    g=once(g,"versionCode = 85","versionCode = 86","monotonic versionCode")
    g=once(g,'versionName = "1.0.60-lena-thread-persistence"',
           'versionName = "1.0.61-lena-expression-colors"',"versionName")
    GRADLE.write_text(g,"utf-8")
    print("v1.0.61 staged: original 201-bone rig, +2 true morph targets, 18 clips, color vertex material.")
    print("Mouth motion uses TTS playing state, approximate only; Alex/Arena/Word Memory untouched.")
    print("GLB size",GLB.stat().st_size,"(original",SOURCE.stat().st_size,")")

if __name__=="__main__":
    main()
