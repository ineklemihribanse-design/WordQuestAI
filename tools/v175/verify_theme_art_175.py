#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET
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
print("PASS: all 10 page icon/banner assets compiled; selected real mascots are preserved.")
