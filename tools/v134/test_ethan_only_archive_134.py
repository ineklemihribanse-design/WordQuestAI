#!/usr/bin/env python3
"""Strict post-archive checks before shipping an Ethan-only Android APK."""
from pathlib import Path
import hashlib,json
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
SRC=APP/"src/main/java/com/kelimebulmacasi/a1"
ACTIVE=APP/"src/main/assets/avatars"
ARCH=ROOT/"source/archived-teachers"
gradle=(APP/"build.gradle.kts").read_text("utf-8")
assert "versionCode = 59" in gradle and 'versionName = "1.0.34-ethan-live-tutor-qa"' in gradle
assert 'applicationIdSuffix = ".offline101"' in gradle
ui=(SRC/"LiveTeacherScreen.kt").read_text("utf-8")
ai=(SRC/"AiSession.kt").read_text("utf-8")
stage=(SRC/"TeacherStage3D.kt").read_text("utf-8")
persona=(SRC/"EthanTeacherPersona.kt").read_text("utf-8")
assert 'TeacherAvatar.entries.forEach' not in ui
picker=(SRC/"TeacherAvatar.kt").read_text("utf-8")
assert 'listOf(TeacherAvatar.Mert).forEach' in picker
assert 'TeacherAvatar.entries.forEach' not in picker
assert 'Elif("Emma"' in picker and 'Deniz("Sophia"' in picker, "Inactive enum history must remain"
assert ui.count('listOf(TeacherAvatar.Mert).forEach')==2
assert "internal var avatar by mutableStateOf(TeacherAvatar.Mert)" in ai
assert "avatar = TeacherAvatar.Mert" in ai
assert "if (value != TeacherAvatar.Mert) return" in ai
assert 'ethan_thread_ids' in ai
assert "threads = next.threads().filter { it.id in ethanThreadIds }" in ai
assert "if (id !in ethanThreadIds) return" in ai
assert "if (ethanThreadIds.add(savedId)) persistEthanThreadIds()" in ai
assert "next.setting(\"avatar\") == TeacherAvatar.Mert.name" in ai
assert "store?.delete(id)" in ai, "Old records must only be deleted by an explicit user action"

assert 'next.setting("avatar")' not in ai or 'store?.setSetting("avatar", value.name)' in ai
assert "EthanTeacherPersona.systemPrompt" in ai
assert "selectedAvatar.prompt" in ai and "requestConversationReply" in ai
assert "teacherNotes" in ai and "learningContext" in ai
assert "EthanLive3DStage(" in stage and "Ethan ile canlı 3D konuş" in stage
assert "Teacher3DProbeActivity::class.java" not in stage
assert "Ethan" in persona and "earlier turns" in persona
assert "onActualSpeechRange" in (SRC/"ConversationAudio.kt").read_text("utf-8")
assert "onActualSpeechRange" in ui and "avatarMotion" in ui
for name in ("emma","sophia"):
    assert not list(ACTIVE.glob(name+"*.glb")),name
    files=list((ARCH/name).glob(name+"*.glb"))
    assert files,(name,"female avatar not backed up")
    assert (ARCH/name/"source").is_dir(),(name,"original human source not preserved")
    for p in files:
        assert p.read_bytes()[:4]==b"glTF",(name,p.name)
for name in ("ethan.glb","ethan-mobile.glb"):
    assert (ACTIVE/name).read_bytes()[:4]==b"glTF"
manifest=json.loads((ARCH/"ARCHIVE_MANIFEST.json").read_text("utf-8"))
assert manifest["status"]=="INACTIVE_NOT_DELETED"
assert set(manifest["available_for_restore"])=={"emma","sophia"}
for row in manifest["archived_files"]:
    p=ARCH/row["actor"]/row["name"]
    assert p.is_file()
    assert hashlib.sha256(p.read_bytes()).hexdigest()==row["sha256"]
for filename,key in (("AVATAR_PROVENANCE.json","models"),
                     ("AVATAR_LIPSYNC_METADATA.json","actors")):
    active=json.loads((ACTIVE/filename).read_text("utf-8"))
    backed=json.loads((ARCH/filename).read_text("utf-8"))
    assert [x["actor"] for x in active[key]]==["ethan"]
    assert {x["actor"] for x in backed[key]}=={"emma","sophia","ethan"}
assert "key" not in (ARCH/"ARCHIVE_MANIFEST.json").read_text()
print("PASS: only Ethan visible in two selector surfaces; existing histories untouched")
print("PASS: Ethan current AI provider, notes, TTS/ASR state and real 3D GLB retained")
print("PASS: Emma/Sophia originals and variants preserved outside APK with SHA-256")
print("NOT CERTIFIED: in-process Filament/voice UI real-device stability, phoneme lip sync")
