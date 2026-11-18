#!/usr/bin/env python3
"""Install v1.0.27 premium-avatar candidate without changing existing data or signing."""
from pathlib import Path
repo=Path(__file__).resolve().parents[1]
source=repo/"source"
assert source.is_dir(), "Previous 1.0.26 source overlay must be restored first"
pkg=source/"app/src/main/java/com/kelimebulmacasi/a1"
stage=pkg/"TeacherStage3D.kt"
before=stage.read_text(encoding="utf-8")
assert "ModelNode(" in before and "TeacherFallback2D(" in before
assert "Scene(" in before
after=(repo/"tools/v127/TeacherStage3D.kt").read_text(encoding="utf-8")
assert "rememberMainLightNode" in after and "TeacherFallback2D(" in after
assert "Tam Boy" in after and "Bel" in after and "Yüz" in after
assert "autoAnimate = true" in after and "isOpaque = false" in after
assert "GLSurfaceView" not in after and "WebView" not in after
stage.write_text(after,encoding="utf-8")
gfile=source/"app/build.gradle.kts"
g=gfile.read_text(encoding="utf-8")
assert g.count("versionCode = 49")==1 and g.count('versionName = "1.0.26-rigged-humanoid-glb"')==1
assert 'applicationIdSuffix = ".offline101"' in g
assert 'implementation("io.github.sceneview:sceneview:2.3.0")' in g
g=g.replace("versionCode = 49","versionCode = 50")
g=g.replace('versionName = "1.0.26-rigged-humanoid-glb"','versionName = "1.0.27-premium-human-avatar"')
gfile.write_text(g,encoding="utf-8")
print("Premium avatar candidate stage installed. Build/visual QA required; not for automatic production release.")
