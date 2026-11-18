#!/usr/bin/env python3
"""Emergency v1.0.35 in-place recovery after installed 1.0.34 crashes at APP ICON.

Release strategy: restore the already device-launch-tested 1.0.33 code
path from the staged 1.0.33 source, rather than guessing at a root cause.
VersionCode 60 > broken 59: Android will accept an in-place update with
the original signing certificate without deleting app/user/profile data.

The 1.0.34 Ethan live in-process SceneView and persistent Ethan-thread
migration ARE NOT executed for this recovery APK. All 1.0.34 source code,
Gemini teacher persona, and Emma/Sophia restoration/archive recipes remain
in the repository. No removal of the existing app is required.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
PKG=APP/"src/main/java/com/kelimebulmacasi/a1"
G=APP/"build.gradle.kts"
g=G.read_text(encoding="utf-8")
assert g.count("versionCode = 58")==1
assert g.count('versionName = "1.0.33-android-glb-crash-trace-qa"')==1
assert 'applicationIdSuffix = ".offline101"' in g

ai=(PKG/"AiSession.kt").read_text(encoding="utf-8")
assert "ethan_thread_ids" not in ai, "Do not ship the suspected startup profile migration"
assert "internal var avatar by mutableStateOf(TeacherAvatar.Elif)" in ai
assert 'threads = next.threads()' in ai
assert "EthanTeacherPersona.systemPrompt" not in ai
stage=(PKG/"TeacherStage3D.kt").read_text(encoding="utf-8")
assert "EthanLive3DStage(" not in stage
assert "Teacher3DProbeActivity::class.java" in stage
assert "TeacherFallback2D(" in stage
assert "rememberEngine()" not in stage
assert "rememberLauncherForActivityResult" in stage
assets=APP/"src/main/assets/avatars"
assert (assets/"ethan-mobile.glb").read_bytes()[:4]==b"glTF"
assert (assets/"emma-dense-face.glb").read_bytes()[:4]==b"glTF"
assert (assets/"sophia-dense-face.glb").read_bytes()[:4]==b"glTF"
assert "avatarPrompt.take(250)" in (PKG/"Conversation.kt").read_text()
G.write_text(g.replace("versionCode = 58","versionCode = 60",1)
    .replace('versionName = "1.0.33-android-glb-crash-trace-qa"',
             'versionName = "1.0.35-launch-recovery-qa"',1),encoding="utf-8")

print("STARTUP RECOVERY QA: 1.0.35 code60 package offline101, same existing Android signature")
print("Restored exact 1.0.33 runtime code paths without 1.0.34 AI-thread migration")
print("Restored safe isolated 3D renderer test, main conversation stays in safe 2D")
print("Conversations, Gemini API keys, app data, US/UK pronunciations, archives NOT removed")
print("NOT CLAIMED: root cause of 1.0.34 launch crash, or phone acceptance of code60")
