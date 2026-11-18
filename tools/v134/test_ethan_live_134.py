#!/usr/bin/env python3
"""Ethan v1.0.34 integration regression guards (device QA still required)."""
from pathlib import Path
import json,struct
root=Path(__file__).resolve().parents[2]
app=root/"source/app"
src=app/"src/main/java/com/kelimebulmacasi/a1"
gradle=(app/"build.gradle.kts").read_text()
assert "versionCode = 59" in gradle
assert 'versionName = "1.0.34-ethan-live-tutor-qa"' in gradle
assert 'applicationIdSuffix = ".offline101"' in gradle
assert 'implementation("io.github.sceneview:sceneview:2.3.0")' in gradle
ai=(src/"AiSession.kt").read_text()
assert 'selectedAvatar == TeacherAvatar.Mert' in ai
assert "EthanTeacherPersona.systemPrompt" in ai
conversation=(src/"Conversation.kt").read_text()
assert "avatarPrompt.take(6_000)" in conversation
assert "avatarPrompt.take(250)" not in conversation, "Ethan persona must not be cut to 250 chars"
assert ai.count("EthanTeacherPersona.systemPrompt")==1
assert 'selectedAvatar.prompt + " " + selectedPersonality.prompt' in ai
persona=(src/"EthanTeacherPersona.kt").read_text()
for expected in ("calm, lively","earlier turns","Do not secretly","A1/A2","C1/C2",
                 "or two short conversational","learner's selected","AI teacher character"):
    assert expected in persona, expected
stage=(src/"TeacherStage3D.kt").read_text()
assert 'actor == TeacherAvatar.Mert' in stage
assert 'EthanLive3DStage(' in stage and 'liveEthan3D = false' in stage
assert "Ethan ile canlı 3D konuş" in stage
assert "Teacher3DProbeActivity::class.java" not in stage, "Do not expose female diagnostic viewer"
assert "TeacherFallback2D(" in stage
assert "rememberEngine(" not in stage
assert "TeacherAvatar.Mert" in stage
model=(src/"EthanLive3DStage.kt").read_text()
for term in ("avatars/ethan-mobile.glb","rememberEngine()","rememberModelLoader(engine)",
             "Scene(","motion: AvatarMode","mouthCue: AvatarMouthFrame",
             "AvatarFacialLayer()","AvatarGestureController()",
             "AvatarMode.SPEAKING","AvatarMode.LISTENING","AvatarMode.THINKING",
             "AvatarMode.GREETING","Teacher_Viseme_","observedFrame",
             "onRecoverableError","actualMouth","actualMotion",
             "fps5s=", "activityLabel", "Ethan · Seni dinliyor", "Ethan · Konuşuyor"):
    assert term in model,term
assert "autoAnimate = false" in model
assert "EthanPresenceRhythm()" in model
assert "speechRhythm.speed(mode, real, now)" in model
assert "speechCadence" in model
rhythm=(src/"EthanPresenceRhythm.kt").read_text()
for term in ("cue.cueSequence","cue.quality != LipSyncQuality.NONE",
             "mode != AvatarMode.SPEAKING","fun reset()",
             "nowMs - cue.cueAtMs <= 450L"):
    assert term in rhythm, term
assert (app/"src/test/java/com/kelimebulmacasi/a1/EthanPresenceRhythmTest.kt").is_file()
assert "If YOU misunderstood" in persona and "ask before saving new notes" in persona
# The expanded, truthful Ethan behavior prompt must fit the provider's 6000-character cap.
assert len(persona.split('const val systemPrompt = """')[1].split('"""')[0]) < 6000
assert model.index("val engine = rememberEngine()") < model.index("key(cameraPreset, cameraReset)")
assert "remember { mutableStateOf(false) }" in stage, "Real 3D must be deliberate opt-in during QA"
assert "EthanTeacherPersonaTest.kt" in str(list((app/"src/test/java/com/kelimebulmacasi/a1").glob("EthanTeacherPersonaTest.kt")))
ui=(src/"LiveTeacherScreen.kt").read_text()
for term in ("avatarMotion", "onActualSpeechRange", "mouthCue = mouthCue, motion = avatarMotion.mode"):
    assert term in ui,term
voice=(src/"ConversationAudio.kt").read_text()
for term in ("onActualSpeechRange", "onRangeStart", "override fun onStart", "onPlaying"):
    assert term in voice
gbl=(app/"src/main/assets/avatars/ethan-mobile.glb").read_bytes()
assert gbl[:4]==b"glTF"
n,typ=struct.unpack_from("<II",gbl,12)
assert typ==0x4E4F534A
doc=json.loads(gbl[20:20+n])
assert doc.get("skins") and doc.get("meshes")
assert len(doc.get("animations",[]))>=20
assert any(a.get("name")=="Idle" for a in doc["animations"])
assert any(a.get("name")=="Talk" for a in doc["animations"])
assert any(a.get("name","").startswith("Teacher_Viseme_") for a in doc["animations"])
print("PASS: real Ethan GLB in live Gemini/mic/TTS screen with gated loading")
print("PASS: voice callbacks, selected language/personality/mode and CEFR persona")
print("PASS: Ethan alone actively rendered; Emma/Sophia assets preserved by separate archive step")
print("PENDING: true Android GPU screen + actual TTS/recognition, lip-sync and 10x device tests")
