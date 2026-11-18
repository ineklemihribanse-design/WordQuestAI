#!/usr/bin/env python3
"""v1.0.34: connect a LIVE, opt-in real 3D Ethan with AI personality and TTS.

This runs only after the complete 1.0.33 QA Android source reconstruction.
No changes to female GLBs, Gemini keys, US/UK TTS, user account data or
Room database. Same package identity, increment versionCode for QA only.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[2]
app = root / "source/app"
pkg = app / "src/main/java/com/kelimebulmacasi/a1"
test_pkg = app / "src/test/java/com/kelimebulmacasi/a1"
gradle = app / "build.gradle.kts"

def change_once(text, old, new, name):
    n=text.count(old)
    assert n==1, f"{name}: expected single source anchor {old[:100]!r}, found {n}"
    return text.replace(old,new,1)

g=gradle.read_text("utf-8")
assert g.count("versionCode = 58")==1
assert g.count('versionName = "1.0.33-android-glb-crash-trace-qa"')==1
assert 'applicationIdSuffix = ".offline101"' in g
assert 'io.github.sceneview:sceneview:2.3.0' in g

stage=(pkg / "TeacherStage3D.kt").read_text("utf-8")
assert "TeacherFallback2D(" in stage
assert "Teacher3DHealth.lastResult" in stage
assert "3D hata raporunu göster / yenile" in stage
assert "rememberEngine(" not in stage, "Main screen must not eagerly initialize Filament"
assert "mouthCue: AvatarMouthFrame" in stage and "motion: AvatarMode" in stage
assert "Teacher3DProbeActivity::class.java" in stage

live=(pkg / "LiveTeacherScreen.kt").read_text("utf-8")
assert "avatarMotion" in live and "onActualSpeechRange" in live
assert "TeacherStage3D(ai.avatar" in live
assert "mouthCue = mouthCue, motion = avatarMotion.mode" in live

audio=(pkg / "ConversationAudio.kt").read_text("utf-8")
assert "onActualSpeechRange" in audio and "override fun onStart" in audio
assert "onRangeStart" in audio
assert (app / "src/main/assets/avatars/ethan-mobile.glb").is_file()

ai_path=pkg / "AiSession.kt"
ai=ai_path.read_text("utf-8")
old='selectedAvatar.prompt + " " + selectedPersonality.prompt'
new='selectedAvatar.prompt + " " + selectedPersonality.prompt + ' + (
    '(if (selectedAvatar == TeacherAvatar.Mert) " " + EthanTeacherPersona.systemPrompt else "")'
)
ai=change_once(ai,old,new,"AiSession reply")
ai_path.write_text(ai,"utf-8")

# A previously unnoticed 250-character conversationRequest cap truncated
# EthanTeacherPersona.systemPrompt before the AI received its teaching style.
# The prompt is repository-owned, not arbitrary user input. Keep a bounded cap
# while allowing complete Ethan identity and CEFR/voice/memory instructions.
conversation_path=pkg / "Conversation.kt"
conversation=conversation_path.read_text("utf-8")
conversation=change_once(conversation,"avatarPrompt.take(250)",
                         "avatarPrompt.take(6_000)","Conversation system persona")
conversation_path.write_text(conversation,"utf-8")

for name in ("EthanTeacherPersona.kt", "EthanLive3DStage.kt", "EthanPresenceRhythm.kt", "TeacherStage3D.kt"):
    (pkg / name).write_text((root / "tools/v134" / name).read_text("utf-8"),"utf-8")

test_pkg.mkdir(parents=True,exist_ok=True)
(test_pkg / "EthanTeacherPersonaTest.kt").write_text(
    (root / "tools/v134/EthanTeacherPersonaTest.kt").read_text("utf-8"),"utf-8")
(test_pkg / "EthanPresenceRhythmTest.kt").write_text(
    (root / "tools/v134/EthanPresenceRhythmTest.kt").read_text("utf-8"),"utf-8")

g = g.replace("versionCode = 58","versionCode = 59",1)
g = g.replace('versionName = "1.0.33-android-glb-crash-trace-qa"',
              'versionName = "1.0.34-ethan-live-tutor-qa"',1)
gradle.write_text(g,"utf-8")
print("QA v1.0.34 code59: real Ethan in live voice UI via opt-in, full AI persona")
print("Gemini, user profile memory/notes, US/UK voices and female diagnostics unchanged")
print("NOT A DEVICE-VERIFIED STABLE RELEASE; do not sign or publish automatically")
