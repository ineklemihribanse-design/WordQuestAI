#!/usr/bin/env python3
"""Install a real, rigged-human Android GLB probe in a separate process.

This runs AFTER v1.0.29 safemode and the full 1.0.27.1 human model pipeline.
The user-facing conversation (Gemini/US/UK/mic/history) stays in the main
process. The isolated Activity is intentional: a native Filament SIGABRT
cannot be caught in Kotlin, but can no longer terminate the chat process.
No automatic publication of untested renderer builds is permitted.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[2]
src = root / "source"
pkg = src / "app/src/main/java/com/kelimebulmacasi/a1"
stage = pkg / "TeacherStage3D.kt"
gradle = src / "app/build.gradle.kts"
manifest = src / "app/src/main/AndroidManifest.xml"

old_stage = stage.read_text("utf-8")
assert "if (!experimental3D) {" in old_stage
assert "rememberEngine()" in old_stage
assert "mouthCue:" in old_stage and "motion:" in old_stage
assert "TeacherFallback2D(" in old_stage

for name in ("TeacherStage3D.kt", "Teacher3DProbeActivity.kt"):
    dest = pkg / name
    dest.write_text((root / "tools/v130" / name).read_text("utf-8"), "utf-8")

g = gradle.read_text("utf-8")
assert g.count("versionCode = 54") == 1
assert g.count('versionName = "1.0.29-teacher-crash-safemode"') == 1
assert 'applicationIdSuffix = ".offline101"' in g
g = g.replace("versionCode = 54", "versionCode = 55", 1)
g = g.replace(
    'versionName = "1.0.29-teacher-crash-safemode"',
    'versionName = "1.0.30-isolated-real-glb-qa"', 1
)
gradle.write_text(g, "utf-8")

xml = manifest.read_text("utf-8")
assert xml.count("</application>") == 1
assert "Teacher3DProbeActivity" not in xml
activity = """
        <!-- Real GLB model rendering: isolated from the conversational app. -->
        <activity
            android:name=".Teacher3DProbeActivity"
            android:exported="false"
            android:process=":teacher3d" />
"""
xml = xml.replace("</application>", activity + "    </application>", 1)
manifest.write_text(xml, "utf-8")
print("QA 1.0.30: real GLB SceneView probe isolated in :teacher3d; code55")
