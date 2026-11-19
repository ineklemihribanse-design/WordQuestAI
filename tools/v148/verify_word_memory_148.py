#!/usr/bin/env python3
"""Static QA: no loss of persisted word records, app signer or existing 2.5D renderer."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
K=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
GRADLE=(ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert "versionCode = 73" in GRADLE
assert 'versionName = "1.0.48-neon-word-memory"' in GRADLE
assert 'applicationIdSuffix = ".offline101"' in GRADLE
studio=(K/"LearningStudioScreen.kt").read_text("utf-8")
assert "VocabularyNeonHome(session, pronunciation, back)" in studio
assert "LearningHistoryScreen" in studio or (K/"LearningScreen.kt").exists()
assert "Teacher25DPortrait" in (K/"Teacher2DPortrait.kt").read_text("utf-8")
home=(K/"VocabularyNeonHome.kt").read_text("utf-8")
assert "ProfileRobotSelection" in home
assert "wordquest_profile_robots" in home
assert "Geçmiş Oyunlarım" in home
assert "Akıllı Koleksiyonlar" in home
assert "session.searchLearning" in home
practice=(K/"VocabularyNeonPractice.kt").read_text("utf-8")
for required in ("VocabularyAnswerChecker.grade", "reviewAnyLearning",
    "VocabularyConceptVisual", "VocabularyVisualCache", "Tür Eşleştir", "Yazarak Öğren",
    "pinLearning", 'if (flipped || reviewed) answer else prompt'):
    assert required in practice,required
image=(K/"VocabularyCardImage.kt").read_text("utf-8")
assert '"response_format"' in image
assert "gemini-nano-banana-2.1" in image
assert "x-goog-api-key" in image
assert '"store", false' in image
assert '"output_image"' in image
assert '"steps"' in image
assert "vocabulary_visuals_v1" in image
assert "content:///" not in image
ai=(K/"AiSession.kt").read_text("utf-8")
assert "generateVocabularyVisual" in ai and 'withProviderKey("")' in ai
assert "credentials?.key(provider)" in ai
assert not (ROOT/"source/app/src/main/assets/avatars/ethan-mobile.glb").exists()
print("v148 static QA PASS: data history intact, guarded AI key, version 73 and 2.5D renderer")
print("Device UI, real Gemini API output and signing still require runtime tests")
