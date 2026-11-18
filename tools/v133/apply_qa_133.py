#!/usr/bin/env python3
"""Install v1.0.33 crash FORensics QA after full v1.0.32 staged sources.

This is a diagnostic rebuild based on physical Android evidence that BOTH
Elif_dense_52morph and Elif_base_geometry SIGABRT in the model-loading step.
Does not change any of the real adult human GLB textures, face meshes,
animation clips, Gemini settings or data, or app signing identity.
No production release; test on Android before claiming a crash fix.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
PKG=APP/"src/main/java/com/kelimebulmacasi/a1"
TESTS=APP/"src/test/java/com/kelimebulmacasi/a1"
GRADLE=APP/"build.gradle.kts"

g=GRADLE.read_text("utf-8")
assert g.count("versionCode = 57")==1
assert g.count('versionName = "1.0.32-female-native-morph-diagnostics-qa"')==1
assert 'applicationIdSuffix = ".offline101"' in g
current=(PKG/"Teacher3DProbeActivity.kt").read_text("utf-8")
assert "model_load_begin_" in current and "Teacher3DHealth" in current
assert "getHistoricalProcessExitReasons" in current
assert (PKG/"TeacherStage3D.kt").is_file()

for filename in ("TeacherStage3D.kt", "Teacher3DProbeActivity.kt",
                 "TeacherNativeCrashSummary.kt"):
    (PKG/filename).write_text((ROOT/"tools/v133"/filename).read_text("utf-8"), "utf-8")

TESTS.mkdir(parents=True,exist_ok=True)
(TESTS/"TeacherNativeCrashSummaryTest.kt").write_text(
    (ROOT/"tools/v133/TeacherNativeCrashSummaryTest.kt").read_text("utf-8"), "utf-8")

g=g.replace("versionCode = 57","versionCode = 58",1)
g=g.replace('versionName = "1.0.32-female-native-morph-diagnostics-qa"',
            'versionName = "1.0.33-android-glb-crash-trace-qa"',1)
GRADLE.write_text(g,"utf-8")
print("v1.0.33 QA, code58, same signed app ID; no release")
print("3D renderer phases split: glb instance begin/ready, node ready")
print("Android API31+ protobuf tombstone error summary + Java exception class captured")
print("Phone GPU render stability is NOT verified by this script")
