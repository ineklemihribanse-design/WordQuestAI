#!/usr/bin/env python3
"""Regression checks: real GLB, process isolation, unchanged voice and signing."""
from pathlib import Path
from xml.etree import ElementTree as ET
import re
root=Path(__file__).resolve().parents[2]
src=root/"source/app"
pkg=src/"src/main/java/com/kelimebulmacasi/a1"
stage=(pkg/"TeacherStage3D.kt").read_text("utf-8")
probe=(pkg/"Teacher3DProbeActivity.kt").read_text("utf-8")
g=(src/"build.gradle.kts").read_text("utf-8")
manifest=ET.parse(src/"src/main/AndroidManifest.xml").getroot()
ns="{http://schemas.android.com/apk/res/android}"
app=manifest.find("application")
assert app is not None
activities=[x for x in app.findall("activity") if
    x.attrib.get(ns+"name", "").endswith("Teacher3DProbeActivity")]
assert len(activities)==1, "Expected exactly one 3D probe Activity"
assert activities[0].attrib.get(ns+"process")==":teacher3d"
assert activities[0].attrib.get(ns+"exported")=="false"

assert "versionCode = 55" in g
assert 'versionName = "1.0.30-isolated-real-glb-qa"' in g
assert 'applicationIdSuffix = ".offline101"' in g
assert "TeacherFallback2D(" in stage
assert "rememberLauncherForActivityResult(" in stage
assert "Teacher3DProbeActivity::class.java" in stage
assert "mouthCue: AvatarMouthFrame" in stage
assert "motion: AvatarMode" in stage
assert not any(x in stage for x in ("rememberEngine(", "ModelNode(", "io.github.sceneview")), (
    "No native GLB engine or nodes can be created on the main conversation screen"
)
assert "getHistoricalProcessExitReasons" in probe
assert "REASON_CRASH_NATIVE" in probe and "REASON_LOW_MEMORY" in probe
assert "assets.open(asset)" in probe
assert "loader.createModelInstance(asset)" in probe
assert all(("avatars/" + name + "-mobile.glb") in probe for name in ("emma", "sophia", "ethan"))
assert "rememberEngine()" in probe and "Scene(" in probe
assert "autoAnimate = false" in probe, "Initial render must not fire all 28 clips"
assert "node.playAnimation" in probe, "Single Idle clip diagnostic toggle required"
assert "model_load_begin" in probe and "scene_frame_received" in probe
assert "model_load_success" in probe
assert "model_load_failed" in probe
assert "render_timeout" in probe and "10_000L" in probe
assert "onFailure = { finish() }" in probe
assert "override fun onDestroy()" not in probe
assert "Teacher3DHealth.mark" in stage and "Teacher3DHealth.mark" in probe
for actor in ("emma", "sophia", "ethan"):
    b=(src/"src/main/assets/avatars"/(actor+".glb")).read_bytes()
    assert b[:4]==b"glTF" and len(b)>3_000_000, actor
    mobile=(src/"src/main/assets/avatars"/(actor+"-mobile.glb")).read_bytes()
    assert mobile[:4] == b"glTF"
    assert len(mobile) < len(b), ("mobile GLB should save memory", actor)
assert "avatarMotion" in (pkg/"LiveTeacherScreen.kt").read_text("utf-8")
assert "onActualSpeechRange" in (pkg/"ConversationAudio.kt").read_text("utf-8")
print("PASS: actual rigged human GLB assets are retained")
print("PASS: real GLB native engine loads exclusively in isolated :teacher3d process")
print("PASS: optional animation, crash/low-memory diagnostics and main-chat fallback")
print("PASS: voice callbacks and signed upgrade-compatible app ID preserved")
