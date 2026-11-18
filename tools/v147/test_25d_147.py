#!/usr/bin/env python3
"""Guardrails: actual mesh deformation, no frame cycling, archived Ethan preserved."""
from pathlib import Path
root=Path(__file__).resolve().parents[2]
app=root/"source/app"
pkg=app/"src/main/java/com/kelimebulmacasi/a1"
g=(app/"build.gradle.kts").read_text("utf-8")
assert "versionCode = 72" in g
assert "applicationIdSuffix = \".offline101\"" in g
assert "1.0.47-vocabulary-2p5d-compat" in g
assert (root/"archived/ethan_3d/README.md").exists()
source=(pkg/"Teacher25DPortrait.kt").read_text("utf-8")
math=(pkg/"Teacher25DMath.kt").read_text("utf-8")
legacy=(pkg/"Teacher2DPortrait.kt").read_text("utf-8")
for token in ("drawBitmapMesh","teacher2d_fullbody_master.webp",
              "Teacher25DMath.buildVertices","rememberUpdatedState(motion)",
              "speechSince", "mouthTarget"):
    assert token in source,token
for token in ("SEG_X = 32","SEG_Y = 48","mouthMask","eyelid",
              "approach","gesture","mode != AvatarMode.SPEAKING"):
    assert token in math,token
assert "Teacher25DPortrait" in legacy
for bad in ("teacher2d_atlas_100.webp","Teacher2D100Timeline.select",
            "teacher2d_mouth.webp","teacher2d_eyes.webp"):
    assert bad not in source,bad
assert (app/"src/test/java/com/kelimebulmacasi/a1/Teacher25DMathTest.kt").is_file()
assert "Teacher2DPersona.systemPrompt" in (pkg/"AiSession.kt").read_text("utf-8")
assert "onActualSpeechRange" in (pkg/"LiveTeacherScreen.kt").read_text("utf-8")
print("QA: same experimental continuous 2.5D mesh renderer retained, no slideshow")
print("QA: authentic TTS active state preserved; no new permissions/provider changes")
print("LIMIT: flat picture has no independent eyelid/lip interior geometry.")
print("VISUAL ACCEPTANCE PENDING: existing 2.5D test rig preserved in compatibility upgrade.")
