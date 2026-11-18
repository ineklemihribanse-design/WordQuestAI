#!/usr/bin/env python3
"""QA assertions: no 2D/animated mannequin substitution, no changed app identity."""
from pathlib import Path
from xml.etree import ElementTree as ET
import json, struct
root = Path(__file__).resolve().parents[2]
app = root / "source/app"
pkg = app / "src/main/java/com/kelimebulmacasi/a1"
g = (app / "build.gradle.kts").read_text("utf-8")
probe = (pkg / "Teacher3DProbeActivity.kt").read_text("utf-8")
host = (pkg / "TeacherStage3D.kt").read_text("utf-8")
assert 'versionName = "1.0.31-human-avatar-stability-qa"' in g
assert "versionCode = 56" in g
assert 'applicationIdSuffix = ".offline101"' in g
assert 'implementation("io.github.sceneview:sceneview:2.3.0")' in g
assert "TeacherFallback2D(" in host and "Teacher3DProbeActivity::class.java" in host
assert "rememberEngine(" not in host and "ModelNode(" not in host
assert 'android:process=":teacher3d"' in (app/"src/main/AndroidManifest.xml").read_text()
assert "getHistoricalProcessExitReasons" in probe
assert "REASON_CRASH_NATIVE" in probe and "REASON_LOW_MEMORY" in probe
assert "model_load_begin" in probe and "render_timeout" in probe
assert "10_000L" in probe and "onFailure = { finish() }" in probe
assert "var playingIdle by remember { mutableStateOf(true) }" in probe
assert "if (!observedFrame) return@LaunchedEffect" in probe
assert "node.playAnimation(idle" in probe
assert "cameraPreset" in probe and "cameraReset" in probe
assert all(x in probe for x in ('"Tam Boy"', '"Bel"', '"Yüz"', 'rememberCameraManipulator'))
assert "key(cameraPreset, cameraReset)" in probe
assert probe.index("val engine = rememberEngine()") < probe.index("key(cameraPreset, cameraReset)")
assert all(s in probe for s in ("graphicsKb", "pssKb", "firstFrameMs"))
assert "fps5s" in probe and "windowMs >= 5000L" in probe
assert all(s in probe for s in ("emma-mobile-lite.glb", "sophia-mobile-lite.glb", "ethan-mobile.glb"))
assert "autoAnimate = false" in probe
assert "AvatarMotionCore.kt" not in probe  # no duplicate controller re-initialized on isolated Activity
assert (pkg / "ConversationAudio.kt").is_file()
assert "onActualSpeechRange" in (pkg / "ConversationAudio.kt").read_text()
assert "avatarMotion" in (pkg / "LiveTeacherScreen.kt").read_text()

assets = app / "src/main/assets/avatars"
def header_and_doc(name):
    b=(assets/name).read_bytes()
    assert b[:4] == b"glTF" and len(b)>2_000_000, name
    version,length=struct.unpack_from("<II", b, 4)
    assert version==2 and length==len(b),name
    n,tag=struct.unpack_from("<II", b, 12)
    assert tag==0x4E4F534A
    return len(b),json.loads(b[20:20+n])
for actor in ("emma","sophia"):
    full,doc=header_and_doc(actor+"-mobile.glb")
    lite,lite_doc=header_and_doc(actor+"-mobile-lite.glb")
    assert lite<full,(actor,"female lite asset must save compressed memory")
    for key in ("nodes", "meshes", "skins", "animations", "materials", "accessors", "textures", "scenes"):
        assert lite_doc[key]==doc[key],(actor,key,"rig or animation altered")
    assert len(lite_doc["animations"])==28
    assert len(lite_doc["skins"])>=1
    assert 52 in [len(p.get("targets",[])) for m in lite_doc["meshes"] for p in m["primitives"]]
    print(actor,"mobile MiB",round(full/1048576,2),"lite MiB",round(lite/1048576,2))
header_and_doc("ethan-mobile.glb")
print("PASS: 1.0.31 code56 QA, upgrade identity, separate native process, three real GLB models")
print("PASS: female GLBs reduced; same rig, animation set, face morphs")
print("PASS: camera presets, first-frame gated Idle and Android GPU/PSS logging")
print("NOT TESTED: real-device 10x cycles, crash causes, visual/art approval and live speech lip sync")
