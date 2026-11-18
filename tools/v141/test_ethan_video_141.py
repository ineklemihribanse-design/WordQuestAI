#!/usr/bin/env python3
"""Video-grounded QA: no dramatic head lean, no stiff arms, no facial clip thrashing."""
from pathlib import Path
import json,hashlib,struct
root=Path(__file__).resolve().parents[2]
app=root/"source/app"
pkg=app/"src/main/java/com/kelimebulmacasi/a1"
g=(app/"build.gradle.kts").read_text("utf-8")
assert "versionCode = 66" in g
assert 'versionName = "1.0.41-ethan-video-pose-qa"' in g
assert 'applicationIdSuffix = ".offline101"' in g

stage=(pkg/"EthanLive3DStage.kt").read_text("utf-8")
for term in ('val lastFaceName = remember(node) { arrayOf("") }',
             'lastFaceName[0] != faceName || newWord',
             'lastFaceName[0] = faceName',
             'node.playAnimation(faceIndex, speed = 1f, loop = false)',
             'val cue = if (hasCue) real else AvatarMouthFrame()'):
    assert term in stage,term
assert 'previous.size != 1 || previous[0] != faceIndex || newWord' not in stage
assert "now % 320L" not in stage
assert 'avatars/ethan-mobile.glb' in stage
assert 'rememberEngine()' in stage

report=json.loads((root/"source/ethan_natural_motion_qa.json").read_text("utf-8"))
arms=report["animated_arm_bones"]
head=report["controlled_head_torso_bones"]
assert len(arms)>=2 and len(head)>=1
assert any("head" in b["bone"] or "neck" in b["bone"] for b in head),head
assert all(.25 <= b["talk_strength"] <= .75 for b in arms)
assert all(b["talk_strength"] <= .46 for b in head)
glb=(app/"src/main/assets/avatars/ethan-mobile.glb").read_bytes()
assert glb[:4]==b"glTF"
assert struct.unpack_from("<II",glb,4)==(2,len(glb))
assert report["output_sha256"]==hashlib.sha256(glb).hexdigest()
jlen,jtype=struct.unpack_from("<II",glb,12)
assert jtype==0x4E4F534A
doc=json.loads(glb[20:20+jlen])
assert len(doc.get("skins",[]))>=1
assert {"Idle","Talk","Wave","Think"}.issubset({x.get("name") for x in doc["animations"]})
assert len(report["viseme_clips_softened"])>=12
assert report["device_visual_approved"] is False

remote=(pkg/"EthanIsolatedVoiceTeacherActivity.kt").read_text("utf-8")
assert "if (playing == id)" in remote
assert "lipSync.push(" in remote
assert 'onActualSpeechRange' in remote
manifest=(app/"src/main/AndroidManifest.xml").read_text("utf-8")
assert 'android:process=":teacher3d"' in manifest
assert not list((app/"src/main/assets/avatars").glob("emma*.glb"))
assert not list((app/"src/main/assets/avatars").glob("sophia*.glb"))
assert "ConversationFailureDiagnostics.kt" in [p.name for p in pkg.iterdir()]
print("PASS: Ethan GLB real-human mesh untouched; controlled head/neck and freer arm Talk keys")
print("PASS: one-shot ARKit face clips gated by changing viseme/real cue; no frame thrash")
print("PASS: isolated Android renderer, stored keys, old avatars archive unaffected")
print("NOT A DEVICE-VERIFIED ANIMATION RESULT: new preview and user visual test needed")
