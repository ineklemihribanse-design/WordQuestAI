#!/usr/bin/env python3
"""1.0.41 Android QA: improve actual user-video head tilt, arm expressiveness and mouth clip state.

After the v140 device video, adjust the glTF FLOAT32 Talk/Idle blending in
v140/tune_ethan_glb_140.py and avoid re-launching a completed one-shot face
clip every render frame. Keep Ethan and SceneView confined to :teacher3d.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[2]
app = root/"source/app"
pkg = app/"src/main/java/com/kelimebulmacasi/a1"
gradle=app/"build.gradle.kts"

def replace_once(value, before, after, name):
    assert value.count(before)==1, (name,value.count(before))
    return value.replace(before,after,1)

g=gradle.read_text("utf-8")
assert "versionCode = 65" in g
assert 'versionName = "1.0.40-ethan-natural-mouth-gestures-qa"' in g
assert 'applicationIdSuffix = ".offline101"' in g
tuner=(root/"tools/v140/tune_ethan_glb_140.py").read_text("utf-8")
assert 'head_torso_written' in tuner and 'talk_strength = 0.25' in tuner
assert 'factor = {"A":0.78' in tuner

stage=pkg/"EthanLive3DStage.kt"
s=stage.read_text("utf-8")
s=replace_once(s,
    '    val lastFaceCue = remember(node) { longArrayOf(-1L) }',
    '''    val lastFaceCue = remember(node) { longArrayOf(-1L) }
    // One-shot clips may disappear from playingAnimations when finished.
    // Track the selected face independently to prevent 60 FPS restarts.
    val lastFaceName = remember(node) { arrayOf("") }''',
    "Independent selected face state")
s=replace_once(s,
    '''                            if (previous.size != 1 || previous[0] != faceIndex || newWord) {
                                previous.forEach(node::stopAnimation)
                                // Never loop the short blink clip many times per blink.
                                node.playAnimation(faceIndex, speed = 1f, loop = false)
                            }''',
    '''                            // Re-apply ONLY if the desired expression changed
                            // or a real new TTS text-range cue arrived. Filament
                            // removes finished one-shot clips from its playback map;
                            // querying that map alone caused constant restarts.
                            if (lastFaceName[0] != faceName || newWord) {
                                previous.forEach(node::stopAnimation)
                                node.playAnimation(faceIndex, speed = 1f, loop = false)
                                lastFaceName[0] = faceName
                            }''',
    "Avoid restarting one-shot face clip every video frame")
stage.write_text(s,"utf-8")

g=g.replace("versionCode = 65", "versionCode = 66",1).replace(
    'versionName = "1.0.40-ethan-natural-mouth-gestures-qa"',
    'versionName = "1.0.41-ethan-video-pose-qa"',1)
gradle.write_text(g,"utf-8")
print("1.0.41 QA: soften recorded head/torso roll, restore arm amplitude, make mouth more legible")
print("1.0.41 QA: one-shot facial animations no longer restarted every 60-Hz frame")
print("Retained: real Ethan rig, app ID, US/UK voices, AI credentials and isolated :teacher3d")
print("PENDING: actual Android device video comparison; no signed release authorized")
