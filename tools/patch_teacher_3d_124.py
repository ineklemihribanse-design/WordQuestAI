#!/usr/bin/env python3
"""Install native 3D teachers, keeping old saved enum IDs and signed-update identity."""
from pathlib import Path

repo = Path(__file__).resolve().parents[1]
src = repo / "source"
assert src.is_dir(), "Source must be restored before applying this patch"
pkg = src / "app/src/main/java/com/kelimebulmacasi/a1"

def replace_once(text, old, new, where):
    n=text.count(old)
    assert n == 1, f"Expected 1 instance of {old!r} in {where}; got {n}"
    return text.replace(old,new,1)

# Preserve 1.0.23 as an offline fallback for Vulkan/GLES driver failures.
portrait = pkg / "TeacherStage3D.kt"
old = portrait.read_text(encoding="utf-8")
assert "internal fun TeacherStage3D(" in old and "Canvas(" in old
portrait_backup = old.replace("internal fun TeacherStage3D(", "internal fun TeacherFallback2D(", 1)
(pkg / "TeacherFallback2D.kt").write_text(portrait_backup, encoding="utf-8")

# Replace all visible teacher-portrait rendering with actual GLES2 3D geometry.
stage = (repo / "tools/v124/TeacherStage3D.kt").read_text(encoding="utf-8")
renderer = (repo / "tools/v124/Teacher3DRenderer.kt").read_text(encoding="utf-8")
assert "NativeTeacher3DView" in stage and "GLSurfaceView.Renderer" in renderer
assert "MotionEvent.ACTION_MOVE" in renderer and "glDrawElements" in renderer
assert "WebView" not in stage and "webView" not in renderer
portrait.write_text(stage, encoding="utf-8")
(pkg / "Teacher3DRenderer.kt").write_text(renderer, encoding="utf-8")

unit_tests = src / "app/src/test/java/com/kelimebulmacasi/a1/Teacher3DCameraTest.kt"
unit_tests.write_text((repo / "tools/v124/Teacher3DCameraTest.kt").read_text(encoding="utf-8"),encoding="utf-8")

# Change only display labels. Keep enum names Elif/Deniz/Mert intact so the
# existing SQLite conversation history and saved preferences still deserialize.
avatars = pkg / "TeacherAvatar.kt"
s = avatars.read_text(encoding="utf-8")
for name, international in (("Elif","Emma"),("Deniz","Sophia"),("Mert","Ethan")):
    s = replace_once(s, f'{name}("{name}",', f'{name}("{international}",', "TeacherAvatar.kt")
avatars.write_text(s, encoding="utf-8")

# Replace obsolete 2D message with supported 3D gestures, no new privileges.
ui = pkg / "LiveTeacherScreen.kt"
s = ui.read_text(encoding="utf-8")
s = replace_once(s, "Canlı öğretmen · 2D güvenli görünüm",
                 "3D · Döndür · İki parmakla yakınlaş", "LiveTeacherScreen.kt")
ui.write_text(s, encoding="utf-8")

gradle = src / "app/build.gradle.kts"
g = gradle.read_text(encoding="utf-8")
g = replace_once(g, "versionCode = 46", "versionCode = 47", "app/build.gradle.kts")
g = replace_once(g, 'versionName = "1.0.23-native-avatar-safety"',
                    'versionName = "1.0.24-native-fullbody-3d"', "app/build.gradle.kts")
assert 'applicationIdSuffix = ".offline101"' in g
gradle.write_text(g, encoding="utf-8")
print("v1.0.24: real-time native GLES2 full body, zoom, rotate; labels Emma/Sophia/Ethan; history preserved")
