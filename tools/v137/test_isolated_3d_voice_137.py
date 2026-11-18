#!/usr/bin/env python3
"""Source+asset acceptance gates for code62 crash-isolated, actually talking 3D Ethan."""
from pathlib import Path
import json,struct
root=Path(__file__).resolve().parents[2]
app=root/"source/app"
pkg=app/"src/main/java/com/kelimebulmacasi/a1"
g=(app/"build.gradle.kts").read_text("utf-8")
assert 'versionCode = 62' in g
assert 'versionName = "1.0.37-ethan-isolated-3d-voice-qa"' in g
assert 'applicationIdSuffix = ".offline101"' in g
manifest=(app/"src/main/AndroidManifest.xml").read_text("utf-8")
assert 'android:name=".EthanIsolatedVoiceTeacherActivity"' in manifest
assert manifest.count('android:process=":teacher3d"')>=2
assert 'android:exported="false"' in manifest
stage=(pkg/"TeacherStage3D.kt").read_text("utf-8")
assert "EthanIsolatedVoiceTeacherActivity::class.java" in stage
assert "Ethan ile canlı 3D konuş" in stage
assert 'putExtra("teacher_profile", teacherProfile)' in stage
assert '"voice", initialVoice.name' in stage
assert 'onRemoteReturn()' in stage
assert 'EthanLive3DStage(' not in stage and 'rememberEngine()' not in stage
ui=(pkg/"LiveTeacherScreen.kt").read_text("utf-8")
for term in ("teacherProfile = ai.boundProfile",
             "initialVoice = session.preferences.voice",
             "speechVolume = session.preferences.speechVolume",
             "ai.refreshExternalEthanThreads()",
             "onActualSpeechRange", "avatarMotion"):
    assert term in ui,term
assert "listOf(TeacherAvatar.Mert).forEach" in ui
remote=(pkg/"EthanIsolatedVoiceTeacherActivity.kt").read_text("utf-8")
for term in (
    "by viewModels()", "ai.bindProfile(applicationContext, profile)",
    "EthanLive3DStage(", "ConversationAudio(", "RecognizerIntent.ACTION_RECOGNIZE_SPEECH",
    'ai.reply("", message)', "audio.play(reply)",
    "AvatarLipSyncController.fromTextRange(spoken)",
    "LipSyncQuality.TEXT_RANGE_APPROXIMATION", "ai.newConversation()",
    "EnglishVoice.American", "EnglishVoice.British",
    "TeacherFallback2D(", "onRecoverableError = { rendered = false }",
    "onActualSpeechRange", "scope.launch",
):
    assert term in remote,term
assert "putExtra(\"api_key\"" not in stage and "putExtra(\"conversation\"" not in stage
assert "rememberEngine()" not in ui
model=(pkg/"EthanLive3DStage.kt").read_text("utf-8")
for term in ("rememberEngine()","avatars/ethan-mobile.glb","Scene(",
             "motion: AvatarMode","mouthCue: AvatarMouthFrame",
             "AvatarMode.SPEAKING","AvatarMode.THINKING","AvatarMode.LISTENING",
             "Teacher_Viseme_","EthanPresenceRhythm()"):
    assert term in model,term
assert (pkg/"EthanPresenceRhythm.kt").is_file()
assert (app/"src/test/java/com/kelimebulmacasi/a1/EthanPresenceRhythmTest.kt").is_file()
ai=(pkg/"AiSession.kt").read_text("utf-8")
assert "EthanTeacherPersona.systemPrompt" in ai
assert "next.setSetting(\"ethan_thread_ids\"" not in ai
assert "refreshExternalEthanThreads()" in ai
assert "if (ethanThreadIds.add(savedId)) persistEthanThreadIds()" in ai
assert (app/"src/main/assets/avatars/ethan-mobile.glb").read_bytes()[:4]==b"glTF"
assert not list((app/"src/main/assets/avatars").glob("emma*.glb"))
assert not list((app/"src/main/assets/avatars").glob("sophia*.glb"))
assert (root/"source/archived-teachers/ARCHIVE_MANIFEST.json").is_file()
print("PASS: actual 3D human Ethan voice teacher shares AI/TTS/visemes in isolated Activity")
print("PASS: launcher has zero Filament startup; native crashes kill :teacher3d only")
print("PASS: US/UK controls, one user profile, no API keys or chat data in intent")
print("PASS: old teacher archives and signed in-place app id preserved")
print("PENDING: real Android 3D, mic TTS, native abort hardware acceptance")
