#!/usr/bin/env python3
"""Pure source regressions for the staged v1.0.86 British English default."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
MODULE = ROOT / "tools/v186/apply_british_paragraph_default_186.py"
spec = importlib.util.spec_from_file_location("paragraph_patch", MODULE)
patch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(patch)

class BritishDefaultTests(unittest.TestCase):
    def test_practice_defaults_and_voice_paths(self):
        text = (ROOT / "android-current/app/src/main/java/com/kelimebulmacasi/a1/PracticeHubScreen.kt").read_text()
        self.assertIn("mutableStateOf(EnglishVoice.British.name)", text)
        self.assertIn("?: EnglishVoice.British", text)
        self.assertIn("pronunciation.pronounce(listOf(word), practiceAccent", text)
        self.assertIn("putExtra(RecognizerIntent.EXTRA_LANGUAGE, practiceAccent.locale.toLanguageTag())", text)
        self.assertIn('onClick = { practiceAccentName = EnglishVoice.American.name', text)
        self.assertIn('onClick = { practiceAccentName = EnglishVoice.British.name', text)

    def test_paragraph_default_and_fallback(self):
        sample = (
            "private fun Wq175ParagraphPracticeDialog() {\n"
            'var accent by rememberSaveable(profileKey) { mutableStateOf("US") }\n'
            'val locale = if (accent == "UK") Locale.UK else Locale.US\n'
            "}\n"
        )
        updated = patch.patched(sample)
        self.assertIn(patch.NEW, updated)
        self.assertNotIn(patch.OLD, updated)
        self.assertEqual(updated, patch.patched(updated))
        self.assertIn('val locale = if (accent == "UK") Locale.UK else Locale.US', updated)

    def test_paragraph_patch_fails_closed(self):
        with self.assertRaises(ValueError):
            patch.patched('var accent = "US"')
        with self.assertRaises(ValueError):
            patch.patched('Wq175ParagraphPracticeDialog val locale = if (accent == "UK") Locale.UK else Locale.US')
        
    def test_file_patch_preserves_other_bytes_and_is_idempotent(self):
        sample = (
            "fun Wq175ParagraphPracticeDialog() {}\n"
            '    var accent by rememberSaveable(profileKey) { mutableStateOf("US") }\n'
            '    val locale = if (accent == "UK") Locale.UK else Locale.US\n'
        )
        with tempfile.TemporaryDirectory() as d:
            file = Path(d) / patch.PARAGRAPH_PATHS[0]
            file.parent.mkdir(parents=True)
            file.write_text(sample)
            self.assertEqual(patch.apply(Path(d)), 1)
            output = file.read_text()
            self.assertEqual(output, sample.replace(patch.OLD, patch.NEW, 1))
            self.assertEqual(patch.apply(Path(d)), 0)

if __name__ == "__main__":
    unittest.main()
