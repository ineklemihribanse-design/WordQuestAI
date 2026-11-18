#!/usr/bin/env python3
"""QA-only v1.0.31 extension applied AFTER completed v1.0.30 reconstruction.

No alteration to user data, signing secrets, AI providers or US/UK voices.
Keep the Filament engine out of the main chat activity. This does NOT
publish an APK: code56 is only for Android/CI QA until device acceptance.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[2]
source = root / "source"
pkg = source / "app/src/main/java/com/kelimebulmacasi/a1"
stage = pkg / "TeacherStage3D.kt"
probe = pkg / "Teacher3DProbeActivity.kt"
gradle = source / "app/build.gradle.kts"

s = stage.read_text(encoding="utf-8")
old = probe.read_text(encoding="utf-8")
g = gradle.read_text(encoding="utf-8")
assert "TeacherFallback2D(" in s and "rememberLauncherForActivityResult" in s
assert "rememberEngine()" not in s, "Native renderer may not start in the main chat process"
assert "rememberEngine()" in old and "createModelInstance(asset)" in old
assert "Teacher3DHealth" in old
assert g.count("versionCode = 55") == 1
assert g.count('versionName = "1.0.30-isolated-real-glb-qa"') == 1
assert 'applicationIdSuffix = ".offline101"' in g
assert (pkg / "AvatarMotionCore.kt").exists()

template = (root / "tools/v131/Teacher3DProbeActivity.kt").read_text(encoding="utf-8")
assert "emma-mobile-lite.glb" in template and "sophia-mobile-lite.glb" in template
assert "ethan-mobile.glb" in template
assert "model_load_begin" in template and "Teacher3DHealth" in template
assert "cameraPreset" in template and "Scene(" in template
assert "playingIdle by remember { mutableStateOf(true) }" in template
probe.write_text(template, encoding="utf-8")

g = g.replace("versionCode = 55", "versionCode = 56", 1)
g = g.replace('versionName = "1.0.30-isolated-real-glb-qa"',
              'versionName = "1.0.31-human-avatar-stability-qa"', 1)
gradle.write_text(g, encoding="utf-8")
print("1.0.31 QA code56: lightweight licensed females, Idle on first frame, per-avatar camera")
print("Live 3D/phoneme timing and physical-device crash root causes are NOT yet verified")
