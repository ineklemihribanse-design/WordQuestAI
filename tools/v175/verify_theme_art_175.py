#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET
import hashlib
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
d=root/"source/app/src/main/res/drawable"
names=("ana_sayfa","yeni_oyun","arena","yds_akademi","dinle_ve_yaz",
       "dilbilgisi","kelime_hafizam","eslestirmelerim","gunluk_plan","bulmacalarim")
for name in names:
    for kind in ("icon","banner"):
        file=d/("wq175_theme_"+name+"_"+kind+".xml")
        assert file.is_file(),file
        ET.parse(file)
# Check both source art and copied Android compiled resources byte-for-byte.
webp_src=root/"tools/v175/premium_webp"
webp_dst=root/"source/app/src/main/res/drawable-nodpi"
assert len(list(webp_src.glob("wq175_*.webp")))==20
for name in names:
    for kind in ("hero","icon"):
        filename="wq175_"+name+"_"+kind+".webp"
        raw=(webp_src/filename).read_bytes()
        copied=(webp_dst/filename).read_bytes()
        assert len(raw)>5000 and raw[:4]==b"RIFF" and raw[8:12]==b"WEBP", filename
        assert hashlib.sha256(raw).digest()==hashlib.sha256(copied).digest(), filename
home=(src/"HomeScreen.kt").read_text("utf-8")
match=(src/"WordQuest175Matching.kt").read_text("utf-8")
builder=(src/"WordQuest175NewGameHero.kt").read_text("utf-8")
assert "Wq175ThemeBanner(theme," in home
assert "Wq175ThemeIcon(Wq175ThemeArt.fromTitle(title)!!" in home
assert "Wq175ThemeBanner(\"eslestirmelerim\"" in match
assert "Wq175ThemeIcon(\"eslestirmelerim\"" in match
assert "Wq175ThemeBanner(\"yeni_oyun\"" in builder
assert "Wq2DPortrait(" in builder
assert "Wq2DPortrait(selectedAvatar" in (src/"WordQuest175Arena.kt").read_text("utf-8")
theme=(src/"WordQuest175ThemeArt.kt").read_text("utf-8")
assert "ContentScale.Fit" in theme and "fun originalHero(slug:String)" in theme
for name in names:
    assert "R.drawable.wq175_"+name+"_hero" in theme
    assert "R.drawable.wq175_"+name+"_icon" in theme
assert 'Wq175ThemeIllustration("ana_sayfa"' in home
assert 'Wq175ThemeIllustration("arena"' in (src/"WordQuest175Arena.kt").read_text("utf-8")
print("PASS: 20 exact original WebP heroes/icons copied without alteration and used by Compose.")
practice=(src/"WordQuest175PracticeVisuals.kt").read_text("utf-8")
assert 'Wq175ThemeIllustration(suppliedHero,' in practice
assert 'Wq175GlossIcon(Wq175IconNames.practice(kind)' in practice
print("PASS: ten premium rooms and live chosen Arena mascot remain connected.")
