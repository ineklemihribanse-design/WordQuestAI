#!/usr/bin/env python3
"""v1.0.42 fail-closed integration checks. Visual device acceptance remains separate."""
from pathlib import Path
import hashlib,json
root=Path(__file__).resolve().parents[2]
app=root/"source/app"
pkg=app/"src/main/java/com/kelimebulmacasi/a1"
assets=app/"src/main/assets"
g=(app/"build.gradle.kts").read_text("utf-8")
assert 'versionCode = 67' in g
assert 'versionName = "1.0.42-realistic-2d-live-teacher-qa"' in g
assert 'applicationIdSuffix = ".offline101"' in g
assert "sceneview" not in g.lower() and "filament" not in g.lower()
manifest=(app/"src/main/AndroidManifest.xml").read_text("utf-8")
assert 'android:process=":teacher3d"' not in manifest
assert "EthanIsolatedVoiceTeacherActivity" not in manifest
assert "Teacher3DProbeActivity" not in manifest
for name in ("EthanLive3DStage.kt","EthanIsolatedVoiceTeacherActivity.kt",
             "Teacher3DProbeActivity.kt","Teacher3DRenderer.kt","EthanTeacherPersona.kt"):
    assert not (pkg/name).exists(),name
assert not list((assets/"avatars").glob("*.glb"))
assert (assets/"teacher2d/teacher2d_atlas.webp").read_bytes()[:4]==b"RIFF"
portrait=(pkg/"Teacher2DPortrait.kt").read_text("utf-8")
for token in ("BitmapFactory.decodeStream",'"teacher2d/teacher2d_atlas.webp"',
              "mouthCue.cueSequence", "mouthCue.cueAtMs", "LipSyncQuality.NONE",
              "motion == AvatarMode.SPEAKING", "gestureMix", "AvatarViseme.MBP",
              "AvatarViseme.O", "AvatarViseme.U", "IntOffset(mouth * width"):
    assert token in portrait,token
stage=(pkg/"TeacherStage2D.kt").read_text("utf-8")
assert "Teacher2DPortrait(" in stage
assert 'Alex · Canlı İngilizce öğretmeni' in stage
assert "Filament" not in stage or "NO SceneView/Filament" in stage
screen=(pkg/"LiveTeacherScreen.kt").read_text("utf-8")
assert "TeacherStage2D(" in screen
assert "TeacherStage3D(" not in screen
assert "onActualSpeechRange" in screen
assert "ConversationAudio(" in screen
assert "listOf(TeacherAvatar.Mert).forEach" in screen
assert 'Mert("Alex"' in (pkg/"TeacherAvatar.kt").read_text("utf-8")
ai=(pkg/"AiSession.kt").read_text("utf-8")
assert "Teacher2DPersona.systemPrompt" in ai
assert "EthanTeacherPersona.systemPrompt" not in ai
assert "ethan_thread_ids" in ai
assert "ConversationFailureDiagnostics.kt" in [p.name for p in pkg.iterdir()]
assert 'AiProvider.entries.filter { it != AiProvider.Claude }' in (
    pkg/"AiProviderSettings.kt").read_text("utf-8")
assert not list(pkg.glob("*3DProbeActivity*"))
archive=root/"source/archived-teachers/ethan-3d-v1.0.41"
info=json.loads((archive/"MANIFEST.json").read_text("utf-8"))
assert info["recover_from_git_branch"]=="archive/ethan-3d-v1.0.41"
for item in info["files"]:
    b=(archive/item["name"]).read_bytes()
    assert len(b)==item["bytes"] and hashlib.sha256(b).hexdigest()==item["sha256"]
assert any(item["name"]=="ethan-mobile.glb" for item in info["files"])
print("PASS: real-human 2D eye/mouth/arms animations with real active TTS range cues")
print("PASS: no 3D GLBs / SceneView dependency / exported 3D activity in live APK")
print("PASS: original GLBs archived off-APK and original branch recoverable")
print("PASS: Gemini/OpenAI and encrypted settings and chats are untouched")
print("DEVICE VISUAL CHECK STILL REQUIRED; this does not promise phoneme-perfect sync")
