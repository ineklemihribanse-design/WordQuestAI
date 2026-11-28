import unittest
from classify_release_impact import classify

class ImpactTests(unittest.TestCase):
    def test_fast_explicit_art_only(self):
        self.assertEqual(classify([("M","assets/user_uploads/arena_v184/arena_tablet_13_16x10.webp")])["level"],"fast")
    def test_standard_arena(self):
        self.assertEqual(classify([("M","tools/v132_engine/Wq150ArenaEntry.kt")])["level"],"standard")
    def test_full_overrides(self):
        for path in ["tools/v132_engine/Wq150Gateway.kt",".github/workflows/publish.yml","firebase.rules.json","tools/delta_android/WqDeltaApplier.java","unknown"]:
            self.assertEqual(classify([("M","tools/v132_engine/Wq150ArenaEntry.kt"),("M",path)])["level"],"full")
    def test_unknown_and_deletions(self):
        self.assertEqual(classify([])["level"],"full")
        self.assertEqual(classify([("D","source/app/src/main/res/drawable/ic_gold.xml")])["level"],"full")
        self.assertEqual(classify([("A","assets/user_uploads/new.png")])["level"],"full")
        self.assertEqual(classify([("M","assets/user_uploads/unknown.webp")])["level"],"full")
        self.assertEqual(classify([("M","source/app/src/main/res/drawable/ic_gold.xml")])["level"],"full")
if __name__=="__main__":
    unittest.main()
