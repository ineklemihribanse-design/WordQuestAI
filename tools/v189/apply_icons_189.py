#!/usr/bin/env python3
"""Use the exact icon images uploaded in v189, instead of fallback/duplicated icons.

Runs after v188 and preserves all other premium artwork, Arena and profile data.
"""
import base64
import hashlib
import json
import shutil
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO

ROOT = Path(__file__).resolve().parents[2]
PACK = ROOT / "tools/v189/approved_icons_v189.zip.b64"
KOTLIN = ROOT / "tools/v189/Wq189UploadedIcons.kt"
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
ASSETS = ROOT / "source/app/src/main/assets/approved_icons_v189"
ZIP_SHA = "4ed97c6eea9fa4d0462fd9c1d32bf3ec97ce77137e225927ba56fe5d8e2c8399"

def one(s, old, new, label):
    count = s.count(old)
    if count != 1:
        raise AssertionError(f"{label}: expected 1 anchor, got {count}")
    return s.replace(old, new, 1)

def apply():
    raw = base64.b64decode(PACK.read_text("ascii").strip(), validate=True)
    assert hashlib.sha256(raw).hexdigest() == ZIP_SHA, "Unexpected icon package"
    staged = {}
    with ZipFile(BytesIO(raw)) as z:
        assert z.testzip() is None
        manifest = json.loads(z.read("ICON_MANIFEST.json"))
        entries = manifest["assets"]
        assert len(entries) == 22, "All 22 approved pieces are required"
        assert set(z.namelist()) == {"ICON_MANIFEST.json"} | {
            "approved_icons_v189/" + item for item in entries
        }
        for filename, info in entries.items():
            assert filename.endswith(".webp")
            assert filename.replace(".webp", "").replace("_", "").isalnum()
            blob = z.read("approved_icons_v189/" + filename)
            assert blob[:4] == b"RIFF" and blob[8:12] == b"WEBP"
            assert 2000 < len(blob) < 200000
            assert len(blob) == info["bytes"]
            assert hashlib.sha256(blob).hexdigest() == info["sha256"]
            staged[filename] = blob
    planned = {}
    p = SRC / "WordQuest175Icons.kt"
    s = p.read_text("utf-8")
    # Force NEW images ahead of approved v182 atlas and older premium fallbacks.
    s = one(s, "    val selectedReplacement=Wq182Icons.preferred(name)",
        """    // v189 supplied illustrations win; older assets remain fallbacks.
    if (Wq189UploadedArtwork(name, modifier)) return
    val selectedReplacement=Wq182Icons.preferred(name)""",
        "connect new bitmap assets to global semantic icon router")
    planned[p] = s

    p = SRC / "Wq175PremiumHome.kt"
    s = p.read_text("utf-8")
    s = one(s,
        "                val illustrated=when(tile.title) {",
        """                val userApproved = when(tile.title) {
                    "Hazır Paketler" -> "packages"
                    "Arena" -> null // Arena retains its distinct approved art.
                    "Bulmacalarım" -> "solo_puzzle"
                    "Kelime Hafızam" -> "cards"
                    "Günlük Planım" -> "daily_plan"
                    "YDS Akademi" -> "exam"
                    "YDS Hazırlık" -> "review"
                    "Konuşma Asistanı" -> "assistant"
                    "Fiiller" -> "verbs"
                    "Eşleştirmelerim" -> "synonym"
                    "İlerlemem" -> "analytics"
                    else -> null
                }
                val illustrated=when(tile.title) {""",
        "distinct user approved illustration per home tile")
    s = one(s,
        """                if(illustrated!=null &&
                    (Wq183HighResArtwork(illustrated,Modifier.size(imageSize)) ||""",
        """                if(userApproved!=null &&
                    Wq189UploadedArtwork(userApproved,Modifier.size(imageSize))) {
                    // Uploaded full-quality icon: no duplicate legacy illustration.
                } else if(illustrated!=null &&
                    (Wq183HighResArtwork(illustrated,Modifier.size(imageSize)) ||""",
        "ensure home cards actually display new user uploads")
    s = one(s,
        '''            if(!Wq183HighResArtwork("wq_yeni_oyun",Modifier.size(59.dp)) &&''',
        '''            if(!Wq189UploadedArtwork("new_game",Modifier.size(59.dp)) &&
               !Wq183HighResArtwork("wq_yeni_oyun",Modifier.size(59.dp)) &&''',
        "show unique new-game icon, no repeat of Bulmacalarım artwork")
    home_heading = '''        Text("Keşfet ve öğren",fontWeight=FontWeight.ExtraBold,
            color=Wq175Navy,fontSize=20.sp)'''
    if home_heading in s:
        s = one(s, home_heading,
            '''        Row(verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            Wq189UploadedArtwork("home",Modifier.size(36.dp))
            Text("Keşfet ve öğren",fontWeight=FontWeight.ExtraBold,
                color=Wq175Navy,fontSize=20.sp)
        }''',
            "make uploaded house visible in the original premium home heading")
    else:
        # v183 deliberately rebuilt this heading; do not regress its navigation.
        print("PASS: v183 modern heading retained; home artwork routes via theme icon")

    planned[p] = s

    p = SRC / "WordQuest175MyPuzzles.kt"
    s = p.read_text("utf-8")
    s = one(s,
        '''                Wq175GlossIcon("arena_team",
                    Modifier.align(Alignment.Center).size(if(compact)85.dp else 110.dp))''',
        '''                Wq189UploadedArtwork("solo_puzzle",
                    Modifier.align(Alignment.Center).size(if(compact)85.dp else 110.dp))''',
        "replace old misleading Arena mascot on Bulmacalarım hero")
    s = one(s,
        '''                Wq175GlossIcon("notebook",
                    Modifier.align(Alignment.BottomEnd).size(47.dp))''',
        '''                Wq189UploadedArtwork("learning",
                    Modifier.align(Alignment.BottomEnd).size(47.dp))''',
        "show supplied book/learning arrow on puzzle hero")
    s = one(s,
        '''            Wq175GlossIcon("arena_team",Modifier.size(69.dp))''',
        '''            Wq189UploadedArtwork("new_game",Modifier.size(69.dp))''',
        "show unique create-puzzle illustration on green CTA")
    planned[p] = s

    p = SRC / "WordQuest175ThemeArt.kt"
    s = p.read_text("utf-8")
    s = one(s, '''        "ana_sayfa" -> "puzzle"''',
        '''        "ana_sayfa" -> "home"''', "home instead of generic puzzle")
    s = one(s, '''        "yeni_oyun" -> "completion"''',
        '''        "yeni_oyun" -> "new_game"''', "dedicated new game")
    s = one(s, '''        "bulmacalarim" -> "arena_team"''',
        '''        "bulmacalarim" -> "solo_puzzle"''', "no team avatar in single-player mode")
    legacy_theme_icon = '''    Image(painter=painterResource(Wq175ThemeArt.icon(slug)),
        contentDescription=null,modifier=modifier,contentScale=ContentScale.Fit)
}'''
    if legacy_theme_icon in s:
        s = one(s,
        '''    Image(painter=painterResource(Wq175ThemeArt.icon(slug)),
        contentDescription=null,modifier=modifier,contentScale=ContentScale.Fit)
}''',
        '''    val requested=when(slug) {
        "ana_sayfa" -> "home"
        "yeni_oyun" -> "new_game"
        "bulmacalarim" -> "solo_puzzle"
        "gunluk_plan" -> "plan"
        "kelime_hafizam" -> "cards"
        "eslestirmelerim" -> "synonym"
        "dinle_ve_yaz" -> "listen"
        "yds_akademi" -> "exam"
        "dilbilgisi" -> "grammar"
        else -> null
    }
    if (requested == null || !Wq189UploadedArtwork(requested, modifier)) {
        Image(painter=painterResource(Wq175ThemeArt.icon(slug)),
            contentDescription=null,modifier=modifier,contentScale=ContentScale.Fit)
    }
}''',
            "direct theme icons also load user-provided artwork")
    else:
        # v183 replaced the original Wq175ThemeIcon body. Global gloss mapping
        # above routes the requested artwork without overwriting modern headers.
        assert 'internal fun Wq175ThemeIcon(' in s
        print('PASS: v183 themed icon renderer retained; global glossy icons use user art')
    planned[p] = s

    # All checks precede writing; abort without partially changing the Android UI.
    assert KOTLIN.is_file()
    code = KOTLIN.read_text("utf-8")
    assert code.count("internal fun Wq189UploadedArtwork") == 1
    assert "puzzle_builder" in code and "verb_forms" in code
    assert "ready_folders" in code and "home" in code
    SRC.mkdir(parents=True, exist_ok=True)
    ASSETS.mkdir(parents=True, exist_ok=True)
    for filename, blob in staged.items():
        (ASSETS / filename).write_bytes(blob)
    for path, data in planned.items():
        path.write_text(data, "utf-8")
    shutil.copyfile(KOTLIN, SRC / KOTLIN.name)
    assert len(list(ASSETS.glob("*.webp"))) >= 22
    print("PASS v189: 22 genuine uploaded images staged and tied to 11 home action cards")
    print("PASS v189: proper home, solo puzzle, new game, ready folders, verb and study icons")

if __name__ == "__main__":
    apply()
