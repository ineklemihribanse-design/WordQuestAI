#!/usr/bin/env python3
"""Replace unreliable WebView avatar with always-visible native Compose teacher."""
from pathlib import Path

repo = Path(__file__).resolve().parents[1]
source = repo / "source"
assert source.is_dir(), "Restored Android source folder missing"
original = source / "app/src/main/java/com/kelimebulmacasi/a1/TeacherStage3D.kt"
assert original.is_file()
text = original.read_text(encoding="utf-8")
assert "WebView(context)" in text, "Previous renderer mismatch, refusing blind patch"

native = (repo / "tools/v123/TeacherStage3D.kt").read_text(encoding="utf-8")
assert "import androidx.compose.foundation.Canvas" in native
assert "internal fun TeacherStage3D(" in native
assert "android.webkit.WebView" not in native and "AndroidView(" not in native, "Native fallback cannot depend on WebView"
original.write_text(native, encoding="utf-8")

gradle = source / "app/build.gradle.kts"
g = gradle.read_text(encoding="utf-8")
assert g.count("versionCode = 45") == 1
assert g.count('versionName = "1.0.22-3d-render-fix"') == 1
assert 'applicationIdSuffix = ".offline101"' in g
g = g.replace("versionCode = 45", "versionCode = 46")
g = g.replace('versionName = "1.0.22-3d-render-fix"', 'versionName = "1.0.23-native-avatar-safety"')
gradle.write_text(g, encoding="utf-8")

screen = source / "app/src/main/java/com/kelimebulmacasi/a1/LiveTeacherScreen.kt"
s = screen.read_text(encoding="utf-8")
old = "3D · Parmağınla döndür"
assert s.count(old) == 1, "Unexpected toolbar text; do not mislabel the 2D stage"
s = s.replace(old, "Canlı öğretmen · 2D güvenli görünüm")
screen.write_text(s, encoding="utf-8")
print("Installed native Android avatar renderer, versionCode=46")
