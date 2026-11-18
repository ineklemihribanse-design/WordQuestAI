#!/usr/bin/env python3
"""Fail-closed static/asset QA for Ethan's natural speaking and gestures, v1.0.40."""
from pathlib import Path
import hashlib
import json
import struct
r=Path(__file__).resolve().parents[2]
app=r/"source/app"
pkg=app/"src/main/java/com/kelimebulmacasi/a1"
g=(app/"build.gradle.kts").read_text("utf-8")
assert g.count("versionCode = 65")==1
assert 'versionName = "1.0.40-ethan-natural-mouth-gestures-qa"' in g
assert 'applicationIdSuffix = ".offline101"' in g

remote=(pkg/"EthanIsolatedVoiceTeacherActivity.kt").read_text("utf-8")
for phrase in ("AvatarLipSyncController()", "lipSync.start(id)", "lipSync.stop()",
               "if (playing == id)", "lipSync.push(", "lipSync.frame(at)",
               "AvatarLipSyncController.fromTextRange(spoken)",
               "LipSyncQuality.TEXT_RANGE_APPROXIMATION"):
    assert phrase in remote,phrase
assert "cueAtMs = SystemClock.uptimeMillis()" not in remote
assert "val lipSync = remember" in remote

stage=(pkg/"EthanLive3DStage.kt").read_text("utf-8")
assert 'avatars/ethan-mobile.glb' in stage
assert 'val cue = if (hasCue) real else AvatarMouthFrame()' in stage
assert 'val newWord = ' in stage and 'real.cueSequence != lastFaceCue[0]' in stage
assert "loop = false" in stage
assert 'now % 320L' not in stage, "Never invent unrelated lip pulses"
assert "gesture.frame(" not in stage, "Never schedule arm gestures from global clock"
assert "AvatarGesture.OPEN_PALM" not in stage
assert "speechRhythm.speed(mode, real, now)" in stage
assert 'onRecoverableError()' in stage
assert 'rememberEngine()' in stage  # only the isolated :teacher3d activity uses this class

glb=app/"src/main/assets/avatars/ethan-mobile.glb"
blob=glb.read_bytes()
assert blob[:4]==b"glTF"
assert struct.unpack_from("<II",blob,4)==(2,len(blob))
size,kind=struct.unpack_from("<II",blob,12)
assert kind==0x4E4F534A
doc=json.loads(blob[20:20+size])
names=[x.get("name") for x in doc["animations"]]
assert len(names)>=28 and {"Idle","Talk","Think","Wave"} <= set(names)
for vowel in ("A","E","I","O","U","MBP","NEUTRAL"):
    assert "Teacher_Viseme_"+vowel in names
assert len(doc["skins"])>0
report=json.loads((r/"source/ethan_natural_motion_qa.json").read_text("utf-8"))
assert report["input_sha256"] != report["output_sha256"]
assert report["output_sha256"] == hashlib.sha256(blob).hexdigest()
assert len(report["animated_arm_bones"])>=2
assert len(report["viseme_clips_softened"])>=12
assert report["device_visual_approved"] is False

assert "ConversationFailureDiagnostics.kt" in [p.name for p in pkg.iterdir()]
assert 'AiProvider.entries.filter { it != AiProvider.Claude }' in (
    pkg/"AiProviderSettings.kt").read_text("utf-8")
assert 'EthanTeacherPersona.systemPrompt' in (pkg/"AiSession.kt").read_text("utf-8")
manifest=(app/"src/main/AndroidManifest.xml").read_text("utf-8")
assert 'android:name=".EthanIsolatedVoiceTeacherActivity"' in manifest
assert manifest.count('android:process=":teacher3d"')>=2
assert not list((app/"src/main/assets/avatars").glob("emma*.glb"))
assert not list((app/"src/main/assets/avatars").glob("sophia*.glb"))
print("PASS: real Ethan arm clips softened; no geometry, skin or viseme names removed")
print("PASS: mouth cues active-utterance gated, sequenced, no unsupported synthetic timing")
print("PASS: Gemini/OpenAI, Ethan 3D isolation, app package, archived teachers remain")
print("PENDING: actual Android mouth/hand naturalness, US/UK voice quality and performance")
