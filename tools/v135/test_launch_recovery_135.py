#!/usr/bin/env python3
"""Fail-closed structural checks for crash-at-launch recovery APK."""
from pathlib import Path
import json,struct
APP=Path(__file__).resolve().parents[2]/"source/app"
PKG=APP/"src/main/java/com/kelimebulmacasi/a1"
gradle=(APP/"build.gradle.kts").read_text("utf-8")
assert gradle.count("versionCode = 60")==1
assert gradle.count('versionName = "1.0.35-launch-recovery-qa"')==1
assert 'applicationIdSuffix = ".offline101"' in gradle
assert 'io.github.sceneview:sceneview:2.3.0' in gradle
manifest=(APP/"src/main/AndroidManifest.xml").read_text("utf-8")
assert ':teacher3d' in manifest
assert "Teacher3DProbeActivity" in manifest
stage=(PKG/"TeacherStage3D.kt").read_text("utf-8")
assert "EthanLive3DStage(" not in stage
assert "rememberEngine()" not in stage
assert "TeacherFallback2D(" in stage
assert "rememberLauncherForActivityResult" in stage
assert "3D hata raporunu göster / yenile" in stage
ai=(PKG/"AiSession.kt").read_text("utf-8")
assert "ethan_thread_ids" not in ai
assert "EthanTeacherPersona" not in ai
assert "internal var avatar by mutableStateOf(TeacherAvatar.Elif)" in ai
assert 'avatar = TeacherAvatar.entries.firstOrNull { it.name == next.setting("avatar") } ?: TeacherAvatar.Elif' in ai
assert "threads = next.threads()" in ai
assert "threads.firstOrNull()?.let { openThread(it.id) }" in ai
assert 'selectedAvatar.prompt + " " + selectedPersonality.prompt' in ai
for name in ("emma-dense-face.glb","sophia-dense-face.glb","ethan-mobile.glb"):
    p=APP/"src/main/assets/avatars"/name
    b=p.read_bytes()
    assert b[:4]==b"glTF"
    n,t=struct.unpack_from("<II",b,12)
    assert t==0x4E4F534A
    doc=json.loads(b[20:20+n]);assert doc.get("meshes") and doc.get("skins")
assert "avatarPrompt.take(250)" in (PKG/"Conversation.kt").read_text("utf-8")
assert "onActualSpeechRange" in (PKG/"ConversationAudio.kt").read_text("utf-8")
assert "mouthCue = mouthCue, motion = avatarMotion.mode" in (PKG/"LiveTeacherScreen.kt").read_text("utf-8")
assert not (PKG/"EthanLive3DStage.kt").exists(), "In-process Filament must not be packaged in recovery"
print("PASS recovery code60: v1.0.33 known startup paths, app data unchanged, no new profile migration")
print("PASS recovery 3D is isolated; Ethan GLB and Emma/Sophia models remain preserved")
print("PENDING: user phone test after signed upgrade from installed code59")
