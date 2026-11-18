#!/usr/bin/env python3
"""Install real CC0 humanoid GLB renderer into restored WordQuest source."""
from pathlib import Path

repo=Path(__file__).resolve().parents[1]
source=repo/"source"
assert source.is_dir(),"Restore prior source overlays before 1.0.26"
pkg=source/"app/src/main/java/com/kelimebulmacasi/a1"
original=pkg/"TeacherStage3D.kt"
old=original.read_text(encoding="utf-8")
assert "Teacher3DSettings" in (pkg/"Teacher3DRenderer.kt").read_text()
assert "NativeTeacher3DView" in old, "Version mismatch: expected 1.0.25 native stage"
stage=(repo/"tools/v126/TeacherStage3D.kt").read_text(encoding="utf-8")
assert "ModelNode(" in stage and "TeacherFallback2D" in stage
assert "GLSurfaceView" not in stage and "WebView" not in stage
original.write_text(stage,encoding="utf-8")

gradle=source/"app/build.gradle.kts"
g=gradle.read_text(encoding="utf-8")
assert g.count("versionCode = 48")==1
assert g.count('versionName = "1.0.25-teacher-anatomy-polish"')==1
assert 'applicationIdSuffix = ".offline101"' in g
assert g.count("dependencies {")==1
g=g.replace("dependencies {",
    'dependencies {\n    implementation("io.github.sceneview:sceneview:2.3.0")',1)
g=g.replace("versionCode = 48","versionCode = 49")
g=g.replace('versionName = "1.0.25-teacher-anatomy-polish"',
            'versionName = "1.0.26-rigged-humanoid-glb"')
gradle.write_text(g,encoding="utf-8")
print("1.0.26: native Filament rigged human avatar loader; versionCode=49")
