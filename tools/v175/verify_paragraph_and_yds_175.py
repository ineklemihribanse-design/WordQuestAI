#!/usr/bin/env python3
"""Assert generated source is actually installed and legacy features remain."""
from pathlib import Path
root = Path(__file__).resolve().parents[2]
src = root / "source/app/src/main/java/com/kelimebulmacasi/a1"
test = root / "source/app/src/test/java/com/kelimebulmacasi/a1"
hub = (src / "PracticeHubScreen.kt").read_text("utf-8")
para = (src / "WordQuest175ParagraphDictation.kt").read_text("utf-8")
yds = (src / "YdsContent.kt").read_text("utf-8")
bank = (src / "Wq175YdsExpansion.kt").read_text("utf-8")
tests = (test / "YdsCenterTest.kt").read_text("utf-8")
assert hub.count("Wq175ParagraphPracticeEntry(session.profileKey)") == 2
assert "PracticeKind.Dictation" in hub
assert para.count('Wq175Paragraph("') == 12
assert "TextToSpeech.QUEUE_FLUSH" in para and "Locale.US" in para and "Locale.UK" in para
assert "wq175GradeParagraph(item.english, answer)" in para
assert 'getSharedPreferences("wq175_paragraph_learning"' in para
assert "+ Wq175YdsGrammar.questions + Wq175YdsExpansion.questions" in yds
assert bank.count('q("') == 30
for category in ("Grammar", "Completion", "Synonym"):
    assert "YdsTopic." + category in bank
assert 'contains("Paragraf"' in bank and 'contains("Çeviri"' in bank
assert "assertEquals(150, YdsContent.questions.size)" in tests
assert "assertEquals(150, YdsContent.questions.map { it.id }.toSet().size)" in tests
assert (test / "Wq175ParagraphTest.kt").exists()
# Both existing dictation and generic speech providers must remain wired up.
assert "pronunciation.pronounce(" in hub
assert "RecognizerIntent.ACTION_RECOGNIZE_SPEECH" in hub
assert 'Wq175GlossIcon("paragraph"' in para
assert 'if(grade.accuracy==100)"success" else "review"' in para
print("PASS: additive paragraph, 150 YDS questions and old pronunciation/recording preserved")
