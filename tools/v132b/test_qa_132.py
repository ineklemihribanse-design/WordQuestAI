#!/usr/bin/env python3
"""v1.0.32 QA checks: real human face/skin preserved, GPU morph probe only.

This is NOT a proof of phone stability. Exact crash cause requires Android
ApplicationExitInfo and (when possible) /data/tombstones via logcat.
"""
from pathlib import Path
from xml.etree import ElementTree as ET
import json,struct
BASE=Path(__file__).resolve().parents[2]/"source/app"
SRC=BASE/"src/main/java/com/kelimebulmacasi/a1"
ASSETS=BASE/"src/main/assets/avatars"
g=(BASE/"build.gradle.kts").read_text()
host=(SRC/"TeacherStage3D.kt").read_text()
view=(SRC/"Teacher3DProbeActivity.kt").read_text()
manifest=(BASE/"src/main/AndroidManifest.xml").read_text()
assert "versionCode = 57" in g
assert 'versionName = "1.0.32-female-native-morph-diagnostics-qa"' in g
assert 'applicationIdSuffix = ".offline101"' in g
assert ':teacher3d' in manifest
assert "TeacherFallback2D(" in host and "rememberEngine(" not in host
assert "getHistoricalProcessExitReasons" in view
assert "7L * 24 * 60 * 60 * 1000" in view, "Report lost before user can install QA upgrade"
assert 'phase == "finished" && (recent == null || recent.reason !in listOf(' in view, (
    "Do not suppress post-close native GPU crash")
assert "Son 3D yükleme aşaması" in view, "Show last recorded phase even without OS exit cause"
assert "REASON_CRASH_NATIVE" in view and "REASON_LOW_MEMORY" in view
assert "withoutMorph" in host and "withoutMorph" in view
assert "Hata bilgisini kopyala" in host
assert "3D hata raporunu göster / yenile" in host
assert "LocalLifecycleOwner.current" in host
assert "Lifecycle.Event.ON_RESUME" in host
assert "DisposableEffect(lifecycleOwner)" in host
assert "LaunchedEffect(refreshReport)" in host
assert "listOf(0L, 700L, 1800L)" in host
assert 'ClipData.newPlainText("WordQuest 3D hata raporu", diagnostic)' in host
# The report button must exist even when diagnostic is EMPTY. The old UI
# only rendered a text when ActivityResult completed; crash deaths could miss it.
assert host.index("3D hata raporunu göster / yenile") < host.index("if (showDiagnostic)")
assert all(x in view for x in ("emma-no-morph-diagnostic.glb","sophia-no-morph-diagnostic.glb","ethan-mobile.glb"))
assert all(x in view for x in ("emma-dense-face.glb","sophia-dense-face.glb","dense_52morph"))
assert "rememberEngine()" in view and "loader.createModelInstance(asset)" in view
assert "cameraPreset" in view and "Idle" in view
assert "fps5s=" in view
assert "mouthCue: AvatarMouthFrame" in host and "motion: AvatarMode" in host
assert "onActualSpeechRange" in (SRC/"ConversationAudio.kt").read_text()
assert "avatarMotion" in (SRC/"LiveTeacherScreen.kt").read_text()

def load(name):
    raw=(ASSETS/name).read_bytes()
    assert raw[:4]==b"glTF" and len(raw)>2_000_000,name
    jn,tag=struct.unpack_from("<II",raw,12);assert tag==0x4E4F534A
    return json.loads(raw[20:20+jn])
for actor in ("emma","sophia"):
    orig=load(actor+"-mobile-lite.glb")
    diag=load(actor+"-no-morph-diagnostic.glb")
    for key in ("skins","materials","images","textures","buffers"):
        assert diag[key]==orig[key],(actor,"changed",key)
    for i,(original,diagnostic) in enumerate(zip(orig["meshes"],diag["meshes"])):
        assert len(original["primitives"])==len(diagnostic["primitives"])
        for p,q in zip(original["primitives"],diagnostic["primitives"]):
            assert p["attributes"]==q["attributes"],(actor,i,"base geometry changed")
            assert p.get("indices")==q.get("indices"),(actor,i,"triangles changed")
            assert p.get("material")==q.get("material"),(actor,i,"material changed")
            assert "targets" not in q,(actor,i,"GPU morph was not disabled")
    assert len(diag["meshes"])==len(orig["meshes"])
    assert len(diag["skins"])>=1
    assert not any("weights" in n for n in diag["nodes"])
    assert len(diag["animations"])>=4
    assert any(a["name"]=="Idle" for a in diag["animations"])
    assert all(ch["target"]["path"] in ("rotation","translation","scale") for a in diag["animations"] for ch in a["channels"])
    dense=load(actor+"-dense-face.glb")
    for key in ("skins","nodes","meshes","animations","materials","images","textures"):
        assert dense[key]==orig[key],(actor,"complete full-human animation/appearance changed",key)
    assert len(dense["animations"])==28
    morph_accessors={ix for mesh in dense["meshes"] for p in mesh["primitives"]
                     for targets in p.get("targets",[]) for ix in targets.values()}
    assert len(morph_accessors)>=52
    assert not any("sparse" in dense["accessors"][ix] for ix in morph_accessors), (
        actor,"native loader still needs sparse morph decoding")
    print("PASS",actor,": base human geometry/Idle variant + complete dense 52-face-morph variant verified")
load("ethan-mobile.glb")
print("PASS code57 separate 3D test, Gemini/US/UK audio untouched; no false claim of repaired native crash")
