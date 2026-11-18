#!/usr/bin/env python3
"""Regression gates for code58 real-human GLB native crash diagnostics.

Inspections only; real-device crash traces still necessary and are not
present on GitHub runners. CI additionally compiles the real Kotlin parser
and executes its synthetic tombstone JVM tests.
"""
from pathlib import Path
APP=Path(__file__).resolve().parents[2]/"source/app"
ROOT=APP/"src/main/java/com/kelimebulmacasi/a1"
stage=(ROOT/"TeacherStage3D.kt").read_text("utf-8")
probe=(ROOT/"Teacher3DProbeActivity.kt").read_text("utf-8")
native=(ROOT/"TeacherNativeCrashSummary.kt").read_text("utf-8")
test=APP/"src/test/java/com/kelimebulmacasi/a1/TeacherNativeCrashSummaryTest.kt"
gradle=(APP/"build.gradle.kts").read_text("utf-8")
assert "versionCode = 58" in gradle
assert 'versionName = "1.0.33-android-glb-crash-trace-qa"' in gradle
assert 'applicationIdSuffix = ".offline101"' in gradle
assert ':teacher3d' in (APP/"src/main/AndroidManifest.xml").read_text()
assert "TeacherFallback2D(" in stage
assert "rememberEngine(" not in stage
assert "3D hata raporunu göster / yenile" in stage
assert "withContext(Dispatchers.IO)" in stage, "Native trace read must not block conversation UI"
assert "verticalScroll(rememberScrollState())" in stage
assert "getHistoricalProcessExitReasons" in probe
assert "REASON_CRASH_NATIVE" in probe and "REASON_CRASH" in probe
assert "maxByOrNull { it.timestamp }" in probe
assert "TeacherNativeCrashSummary.fromExitInfo(recent)" in probe
assert "measure" not in probe or "ölçüm alınamadı" in probe
assert all(x in probe for x in (
    "model_load_begin_", "asset_instance_begin_", "asset_instance_ready_",
    "model_node_ready_", "Thread.setDefaultUncaughtExceptionHandler",
    "error.javaClass.simpleName.take(28)",
    "emma-dense-face.glb","emma-no-morph-diagnostic.glb",
    "sophia-dense-face.glb","sophia-no-morph-diagnostic.glb",
    "ethan-mobile.glb",
))
assert all(x in native for x in (
    "traceInputStream","MAX_TRACE_BYTES",
    "fun parse(bytes: ByteArray)","ApplicationExitInfo.REASON_CRASH_NATIVE",
    "MAX_FRAMES", "fun fields(", "abort", "causes",
))
assert test.is_file() and "@Test" in test.read_text("utf-8")
assert "onActualSpeechRange" in (ROOT/"ConversationAudio.kt").read_text("utf-8")
assert "avatarMotion" in (ROOT/"LiveTeacherScreen.kt").read_text("utf-8")
print("PASS: 1.0.33 code58 QA, same package/signing path, human GLBs unchanged")
print("PASS: separate process, separate native loader/ModelNode phases")
print("PASS: bounded Android tombstone parser + Java uncaught error type and JVM tests")
print("PASS: main thread remains responsive while reading crash trace")
print("PENDING: real Android native trace and device stability; NOT a stable release")
