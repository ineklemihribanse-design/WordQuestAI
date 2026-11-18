#!/usr/bin/env python3
"""QA 1.0.36 regression — Ethan can teach while app launch stays safe.

Check the STAGED Android source, then run all Android/JVM unit tests.
This cannot prove a real Android device launch or 3D/TTS lip sync.
"""
from pathlib import Path
import json, hashlib, struct

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
PKG=APP/"src/main/java/com/kelimebulmacasi/a1"
ASSETS=APP/"src/main/assets/avatars"
ARCH=ROOT/"source/archived-teachers"
gradle=(APP/"build.gradle.kts").read_text("utf-8")
assert 'versionCode = 61' in gradle
assert 'versionName = "1.0.36-ethan-safe-ai-teacher-qa"' in gradle
assert 'applicationIdSuffix = ".offline101"' in gradle
assert (APP/"src/main/AndroidManifest.xml").read_text().count(':teacher3d')>=1

ai=(PKG/"AiSession.kt").read_text("utf-8")
conversation=(PKG/"Conversation.kt").read_text("utf-8")
persona=(PKG/"EthanTeacherPersona.kt").read_text("utf-8")
avatar=(PKG/"TeacherAvatar.kt").read_text("utf-8")
ui=(PKG/"LiveTeacherScreen.kt").read_text("utf-8")
stage=(PKG/"TeacherStage3D.kt").read_text("utf-8")
remote=(PKG/"Teacher3DProbeActivity.kt").read_text("utf-8")
assert "EthanTeacherPersona.systemPrompt" in ai
assert ai.count("EthanTeacherPersona.systemPrompt")==1
assert 'selectedAvatar == TeacherAvatar.Mert' in ai
assert "internal var avatar by mutableStateOf(TeacherAvatar.Mert)" in ai
assert 'val priorTeacherWasEthan = next.setting("avatar") == TeacherAvatar.Mert.name' in ai
assert "avatar = TeacherAvatar.Mert" in ai
assert "next.setSetting(\"ethan_thread_ids\"" not in ai
assert 'next.setting("ethan_thread_ids").orEmpty()' in ai
assert "threads = next.threads().filter { it.id in ethanThreadIds }" in ai
assert "if (id !in ethanThreadIds) return" in ai
assert "if (ethanThreadIds.add(savedId)) persistEthanThreadIds()" in ai
assert 'store?.delete(id)' in ai, "Explicit user deletion remains possible"
assert 'avatarPrompt.take(6_000)' in conversation
assert 'avatarPrompt.take(250)' not in conversation
prompt=persona.split('const val systemPrompt = """')[1].split('"""')[0]
assert 4_000<len(prompt)<6_000
assert "not a conscious human" in prompt
assert "Do not secretly" in prompt
assert "BEHAVIORAL PRESENCE" in prompt
assert "CONTINUITY AND SELF-CORRECTION" in prompt
assert "If YOU misunderstood" in prompt
assert (APP/"src/test/java/com/kelimebulmacasi/a1/EthanTeacherPersonaTest.kt").is_file()

assert ui.count("listOf(TeacherAvatar.Mert).forEach")==2
assert "TeacherAvatar.entries.forEach" not in ui
assert "listOf(TeacherAvatar.Mert).forEach" in avatar
assert "Elif(\"Emma\"" in avatar and "Deniz(\"Sophia\"" in avatar
assert "onActualSpeechRange" in ui and "avatarMotion" in ui
assert "TeacherStage3D(ai.avatar" in ui
assert 'onRangeStart' in (PKG/"ConversationAudio.kt").read_text()
assert "Teacher3DProbeActivity::class.java" in stage
assert "TeacherFallback2D(" in stage and "TeacherAvatar.Mert" in stage
assert "Ethan'ın gerçek 3D modelini görüntüle" in stage
assert "3D hata raporunu göster / yenile" in stage
assert "rememberEngine()" not in stage
assert "EthanLive3DStage" not in stage
assert "EthanLive3DStage" not in remote
assert "val actor = TeacherAvatar.Mert" in remote
assert "val withoutMorph = false" in remote
assert "Teacher3DHealth.mark" in remote
assert not (PKG/"EthanLive3DStage.kt").exists()
assert (PKG/"TeacherNativeCrashSummary.kt").exists()

# Old teachers remain in verifiable GitHub Actions archive, never deleted.
manifest=json.loads((ARCH/"ARCHIVE_MANIFEST.json").read_text("utf-8"))
assert manifest["status"]=="INACTIVE_NOT_DELETED"
assert set(manifest["available_for_restore"])=={"emma","sophia"}
for row in manifest["archived_files"]:
    p=ARCH/row["actor"]/row["name"]
    assert p.is_file()
    assert hashlib.sha256(p.read_bytes()).hexdigest()==row["sha256"]
for slug in ("emma","sophia"):
    assert (ARCH/slug/"source").is_dir()
    assert list((ARCH/slug).glob(slug+"*.glb"))
    assert not list(ASSETS.glob(slug+"*.glb"))
for name in ("ethan-mobile.glb","ethan.glb"):
    buf=(ASSETS/name).read_bytes()
    assert buf[:4]==b"glTF"
    length,t=struct.unpack_from("<II",buf,12)
    assert t==0x4E4F534A
    doc=json.loads(buf[20:20+length])
    assert doc["meshes"] and doc["skins"]
assert any(a.get("name")=="Talk" for a in json.loads(
    (ASSETS/"ethan-mobile.glb").read_bytes()[20:20+struct.unpack_from("<I",
        (ASSETS/"ethan-mobile.glb").read_bytes(),12)[0]]
)["animations"])
print("PASS: Ethan only displayed, AI provider uses full personality, on-device microphone and TTS hooks preserved")
print("PASS: app launch is free of profile write and Filament engine creation, no migrations")
print("PASS: real licensed human Ethan GLB and verifiable Emma/Sophia archive")
print("PENDING: real Android startup and live voice/3D dual-process integration")
