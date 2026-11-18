#!/usr/bin/env python3
"""1.0.42: replace native 3D Ethan at build-time with a real-human 2D AI tutor.

Fail closed on an unexpected source layout. Preserve appId, signing keystore,
profile database, encrypted Gemini/OpenAI credentials, conversations, saved
content, US/UK pronunciation and in-app updater. The entire 1.0.41 code
snapshot is retained on archive/ethan-3d-v1.0.41 and under tools/v12x-v141.
"""
from pathlib import Path
import hashlib
import json
import re
import shutil
root=Path(__file__).resolve().parents[2]
app=root/"source/app"
pkg=app/"src/main/java/com/kelimebulmacasi/a1"
assets=app/"src/main/assets"
tests=app/"src/test/java/com/kelimebulmacasi/a1"
gradle=app/"build.gradle.kts"
manifest=app/"src/main/AndroidManifest.xml"

def once(text,old,new,what):
    assert text.count(old)==1,(what,text.count(old))
    return text.replace(old,new,1)

g=gradle.read_text("utf-8")
assert g.count("versionCode = 66")==1
assert g.count('versionName = "1.0.41-ethan-video-pose-qa"')==1
assert 'applicationIdSuffix = ".offline101"' in g
assert (pkg/"EthanIsolatedVoiceTeacherActivity.kt").exists()
assert (pkg/"EthanLive3DStage.kt").exists()
assert (assets/"avatars/ethan-mobile.glb").exists()

# Preserve the actual optimized rig and GLB in an off-APK archive for QA
# inspection; the original source recipes and code are in a dedicated Git
# archive branch and are not deleted from repository history.
archive=root/"source/archived-teachers/ethan-3d-v1.0.41"
archive.mkdir(parents=True,exist_ok=True)
records=[]
for f in sorted((assets/"avatars").glob("*.glb")):
    data=f.read_bytes()
    assert data[:4]==b"glTF",(f.name,"damaged old 3D asset")
    dest=archive/f.name
    shutil.copyfile(f,dest)
    records.append({"name":f.name,"bytes":len(data),
                    "sha256":hashlib.sha256(data).hexdigest()})
    f.unlink()
assert any(row["name"]=="ethan-mobile.glb" for row in records)
(archive/"MANIFEST.json").write_text(json.dumps({
    "reason":"3D Ethan replaced by native 2D teacher; archive not in APK",
    "recover_from_git_branch":"archive/ethan-3d-v1.0.41",
    "files":records,
},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

portrait=root/"tools/v142/teacher2d_atlas.webp"
assert portrait.is_file()
raw=portrait.read_bytes()
assert len(raw)>100_000 and raw[:4]==b"RIFF" and raw[8:12]==b"WEBP"
dest=assets/"teacher2d/teacher2d_atlas.webp"
dest.parent.mkdir(parents=True,exist_ok=True)
dest.write_bytes(raw)

# Compose-only stage with a fully local facial/hand sprite atlas.
(pkg/"TeacherStage2D.kt").write_text(
    (root/"tools/v142/TeacherStage2D.kt").read_text("utf-8"),encoding="utf-8")
(pkg/"Teacher2DPortrait.kt").write_text(
    (root/"tools/v142/Teacher2DPortrait.kt").read_text("utf-8"),encoding="utf-8")
(pkg/"Teacher2DPersona.kt").write_text(
    (root/"tools/v142/Teacher2DPersona.kt").read_text("utf-8"),encoding="utf-8")
(pkg/"TeacherStage3D.kt").unlink()

# No user-accessible 3D screens or native renderer bundled in the final APK.
# The archived branch retains all real Ethan and native test code.
remove_sources=(
    "EthanLive3DStage.kt","EthanIsolatedVoiceTeacherActivity.kt",
    "Teacher3DProbeActivity.kt","Teacher3DRenderer.kt",
    "EthanPresenceRhythm.kt","EthanTeacherPersona.kt",
)
for name in remove_sources:
    (pkg/name).unlink(missing_ok=True)
for test in tests.glob("*.kt"):
    if test.name.startswith(("Teacher3D","EthanPresence","EthanTeacherPersona")):
        test.unlink()

# Remove disabled :teacher3d activities without touching other manifest entries.
xml=manifest.read_text("utf-8")
for activity in ("Teacher3DProbeActivity","EthanIsolatedVoiceTeacherActivity"):
    regex=r'\s*<activity\b[^>]*android:name="\.'+activity+r'"[^>]*/>'
    xml,n=re.subn(regex,"",xml,flags=re.DOTALL)
    assert n==1,(activity,"manifest unexpectedly changed",n)
assert 'android:process=":teacher3d"' not in xml
manifest.write_text(xml,"utf-8")

# Drop 3D libraries so Filament is not merely hidden behind a button.
old_lines=g.splitlines()
g="\n".join(line for line in old_lines if
    not any(token in line.lower() for token in
        ("io.github.sceneview", "com.google.android.filament",
         "filament-android", "sceneview-android"))) + "\n"
assert len(g.splitlines())<=len(old_lines)
assert "sceneview" not in g.lower() and "filament-android" not in g.lower()
g=once(g,"versionCode = 66","versionCode = 67","versionCode")
g=once(g,'versionName = "1.0.41-ethan-video-pose-qa"',
       'versionName = "1.0.42-realistic-2d-live-teacher-qa"', "versionName")
gradle.write_text(g,"utf-8")

# Keep existing chat, mic, word-range callbacks, audio rate and US/UK voices.
screen=pkg/"LiveTeacherScreen.kt"
s=screen.read_text("utf-8")
s=once(s,"TeacherStage3D(","TeacherStage2D(","Main native 2D stage")
s=s.replace("Ethan ·","Alex ·").replace("Ethan'a","Alex'e")
screen.write_text(s,"utf-8")
picker=pkg/"TeacherAvatar.kt"
p=picker.read_text("utf-8")
assert p.count('Mert("Ethan"')==1, "Legacy selected teacher source changed"
p=p.replace('Mert("Ethan"','Mert("Alex"',1)
picker.write_text(p,"utf-8")
ai=pkg/"AiSession.kt"
a=ai.read_text("utf-8")
a=once(a,"EthanTeacherPersona.systemPrompt","Teacher2DPersona.systemPrompt",
       "Switch live AI teacher identity")
# Historical \u201cethan_thread_ids\u201d storage remains as a compatibility namespace;
# read-only profile bind never migrates or destroys user data.
assert "ethan_thread_ids" in a and "refreshExternalEthanThreads" in a
ai.write_text(a,"utf-8")
print("1.0.42 QA: real-human 2D teacher portrait with animated word-cued mouth and arms")
print("Ethan: 3D screen, model binaries and Filament/SceneView removed from shipping APK")
print("Archive: complete original project snapshot branch + generated GLB outside APK")
print("Unchanged: user profiles, conversations, credentials, voices, app ID and signer")
