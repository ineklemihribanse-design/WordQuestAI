#!/usr/bin/env python3
"""Fast deterministic repository-level tests before the expensive Android build."""
import json
import subprocess
import tempfile
import unittest
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
V=ROOT/"tools/v183"

class SourceContracts(unittest.TestCase):
    def test_all_python_scripts_parse(self):
        import ast
        for p in V.glob("*.py"):
            with self.subTest(path=p.name):
                ast.parse(p.read_text("utf-8"),filename=str(p))

    def test_no_surprise_online_mascot_share(self):
        text=(V/"Wq183MascotStore.kt").read_text("utf-8")
        self.assertIn("context.noBackupFilesDir",text)
        self.assertIn("ZIP_MAX",text)
        self.assertIn("UNPACKED_MAX",text)
        self.assertIn("MAX",text)
        self.assertIn("isCustomId",text)
        self.assertNotIn("FirebaseDatabase",text)
        self.assertNotIn("HttpURLConnection",text)
        self.assertIn("readFully(bytes)",text)
        self.assertIn("Wq183MascotStore.rename(", (V/"Wq183MascotManager.kt").read_text("utf-8"))

    def test_opponent_custom_mascot_transport_is_opt_in_and_wired(self):
        share=(V/"Wq183ArenaShare.kt").read_text("utf-8")
        manager=(V/"Wq183MascotManager.kt").read_text("utf-8")
        overlay=(V/"apply_quality_183.py").read_text("utf-8")
        self.assertIn("hostMascotPreview",share)
        self.assertIn("guestMascotPreview",share)
        self.assertIn("if (!allowed ||",share)
        self.assertIn("MAX_RAW = 13_000",share)
        self.assertIn("FirebaseAuth.AuthStateListener",share)
        self.assertIn("auth: FirebaseAuth?, database: FirebaseDatabase?",share)
        self.assertIn('database.reference.child("premiumRooms").child(code)',share)
        self.assertNotIn("FirebaseAuth.getInstance().currentUser",share)
        self.assertNotIn("FirebaseDatabase.getInstance().reference",share)
        self.assertIn("auth=session.accounts.socialAuth()", overlay)
        self.assertIn("database=session.accounts.socialDatabase()", overlay)
        self.assertIn('update("Accounts.kt"', overlay)
        self.assertIn('getReference(".info/connected")',share)
        self.assertIn("currentCoroutineContext().ensureActive()",share)
        self.assertIn("Wq183ArenaShare.setConsent",manager)
        self.assertIn("Wq183ArenaShareBridge(",overlay)
        self.assertIn("Wq183ArenaShare.remoteImage(id)",overlay)
        self.assertTrue((ROOT/"firebase/database.rules.v183.json").is_file())

    def test_both_players_are_drawn_in_live_and_finished_arena(self):
        hud=(V/"Wq183OnlinePortraitStrip.kt").read_text("utf-8")
        overlay=(V/"apply_quality_183.py").read_text("utf-8")
        self.assertIn("Wq2DPortrait(safeId", hud)
        self.assertIn("hostAvatar: String?", hud)
        self.assertIn("guestAvatar: String?", hud)
        self.assertIn("hostScore: Int", hud)
        self.assertIn("guestScore: Int", hud)
        self.assertIn("maxLines = 1", hud)
        self.assertIn('mode == "team"', hud)
        self.assertIn('shutil.copyfile(TOOLS / "Wq183OnlinePortraitStrip.kt"', overlay)
        self.assertIn('update("PremiumOnlineScreen.kt", online_two_player_portraits)', overlay)
        self.assertIn('Wq183OnlinePortraitStrip(', overlay)
        self.assertIn('hostAvatar=match.hostAvatar, guestAvatar=match.guestAvatar', overlay)
        self.assertIn("render both participant maskots in online match + finished HUD", overlay)

    def test_5_answers_and_contrast(self):
        text=(V/"Wq183QuizQuality.kt").read_text("utf-8")
        self.assertIn("answers.size == 5",text)
        self.assertIn("correct in 0..4",text)
        self.assertIn("Color(0xFF241C35)",text)
        patch=(V/"apply_quality_183.py").read_text("utf-8")
        self.assertIn('"PremiumOnlineScreen.kt"',patch)
        self.assertIn('"PremiumArenaScreen.kt"',patch)

    def test_arena_uses_original_fallback_if_art_is_missing(self):
        text=(V/"Wq183ArenaBackdrop.kt").read_text("utf-8")
        self.assertIn("Wq181ArenaHeroStage",text)
        self.assertIn("ContentScale.Crop",text)
        self.assertIn("arena_tablet_13_16x10_koyu.webp",text)
        self.assertIn("arena_telefon_9x19_5_koyu.webp",text)

    def test_template_is_zip_v1_with_png_and_tr_help(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/"template.zip"
            subprocess.run(["python3",str(V/"build_mascot_template_183.py"),str(path)],
                check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
            with zipfile.ZipFile(path) as z:
                self.assertIsNone(z.testzip())
                manifest=json.loads(z.read("manifest.json"))
                self.assertEqual(manifest["formatVersion"],1)
                self.assertEqual(manifest["images"]["idle"],"images/idle.png")
                self.assertTrue(z.read("images/idle.png").startswith(b"\x89PNG\r\n\x1a\n"))
                self.assertIn("TÜRKÇE",z.read("README_TR.txt").decode("utf-8"))

    def test_release_gate_refuses_missing_evidence(self):
        r=subprocess.run(["python3",str(V/"verify_release_gate_183.py")],
             cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        self.assertEqual(r.returncode,2)
        self.assertIn(b"BLOCKED",r.stdout)

    def test_preserve_old_icon_routes(self):
        p=ROOT/"tools/v182/Wq182Icons.kt"
        self.assertTrue(p.is_file())
        s=p.read_text("utf-8")
        self.assertIn('"arena_quiz" to "wq_bes_sikli_duello"',s)
        self.assertIn('"arena_team" to "wq_ortak_bulmaca"',s)

    def test_5_navigation_hubs_and_real_destinations(self):
        text=(V/"apply_home_183.py").read_text("utf-8")
        for section in ["ÖĞREN","OYUNLAR","ARENA","YDS AKADEMİ","GÜNLÜK PLANIM"]:
            self.assertIn(section,text)
        for dest in ["newGame","learning","packages","arena","ydsCenter","practice","assistant"]:
            self.assertIn(dest,text)

if __name__ == "__main__":
    unittest.main(verbosity=2)
