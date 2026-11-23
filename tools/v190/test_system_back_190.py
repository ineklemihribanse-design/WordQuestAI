#!/usr/bin/env python3
"""Fail-closed regression checks on final assembled Compose navigation source."""
from pathlib import Path
import importlib.util
import unittest

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"

class AndroidBackNavigationChecks(unittest.TestCase):
    def test_home_category_consumes_hardware_back(self):
        s = (SRC / "Wq175PremiumHome.kt").read_text("utf-8")
        self.assertIn("import androidx.activity.compose.BackHandler", s)
        self.assertEqual(s.count("BackHandler(enabled = activeSection != null)"), 1)
        self.assertIn("BackHandler(enabled = activeSection != null) {\n        activeSection = null\n    }", s)
        self.assertLess(s.index("BackHandler(enabled = activeSection != null)"),
                        s.index("val categories = listOf("))
        for title in ("ÖĞREN", "OYUNLAR", "YDS AKADEMİ", "GÜNLÜK PLANIM"):
            self.assertIn(title, s)

    def test_top_level_back_keeps_navigation_history(self):
        s = (SRC / "HomeScreen.kt").read_text("utf-8")
        self.assertEqual(s.count("    BackHandler {\n        if (page == Page.Home.name) confirmExitApp = true"), 1)
        self.assertIn("else requestBack()", s)
        self.assertIn("navigationTrail = navigationTrail.dropLast(1)", s)
        self.assertIn("Page.Game -> origin", s)
        self.assertIn("Page.Packages -> packageOrigin", s)
        self.assertIn("Page.NewGame -> newGameOrigin", s)
        self.assertIn("session.closeGame()", s)
        self.assertIn("confirmLeaveWork = true", s)

    def test_root_exit_requires_confirmation(self):
        s = (SRC / "HomeScreen.kt").read_text("utf-8")
        self.assertIn("var confirmExitApp by rememberSaveable { mutableStateOf(false) }", s)
        self.assertIn('title = { Text("Uygulamadan çıkılsın mı?") }', s)
        self.assertIn("if (confirmExitApp && page == Page.Home.name) AlertDialog(", s)
        self.assertIn("confirmExitApp = false", s)
        self.assertIn("(appContext as? android.app.Activity)?.finish()", s)
        self.assertNotIn("BackHandler(page != Page.Home.name)", s)

    def test_replace_one_rejects_missing_or_duplicate_anchor(self):
        path = ROOT / "tools/v190/apply_system_back_190.py"
        spec = importlib.util.spec_from_file_location("wq_system_back", path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        self.assertEqual(mod.replace_one("a b c", "b", "z", "fixture"), "a z c")
        with self.assertRaises(AssertionError):
            mod.replace_one("a a", "a", "z", "duplicate")
        with self.assertRaises(AssertionError):
            mod.replace_one("abc", "missing", "z", "absent")


if __name__ == "__main__":
    unittest.main(verbosity=2)
