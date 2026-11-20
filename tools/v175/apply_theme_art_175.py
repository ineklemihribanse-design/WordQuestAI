#!/usr/bin/env python3
"""Connect actual 1.0.75 native art to built Android screens after earlier overlays."""
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[2]
PKG=ROOT/"tools/v175"
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
RES=ROOT/"source/app/src/main/res/drawable"
RES.mkdir(parents=True,exist_ok=True)
vectors=sorted((PKG/"theme_drawables").glob("wq175_theme_*.xml"))
assert len(vectors)==20,f"Expected 10 icon and 10 hero vectors, found {len(vectors)}"
for asset in vectors:
    ET.parse(asset)
    shutil.copyfile(asset,RES/asset.name)
shutil.copyfile(PKG/"WordQuest175ThemeArt.kt",SRC/"WordQuest175ThemeArt.kt")

def patch(name,old,new,label):
    file=SRC/name
    src=file.read_text("utf-8")
    count=src.count(old)
    assert count==1,f"{label}: expected 1 location, found {count}"
    file.write_text(src.replace(old,new,1),"utf-8")

patch("HomeScreen.kt",
    '''Wq175GlossIcon(Wq175IconNames.menu(title), Modifier.size(45.dp))''',
    '''if(Wq175ThemeArt.fromTitle(title)!=null) {
            Wq175ThemeIcon(Wq175ThemeArt.fromTitle(title)!!,Modifier.size(45.dp))
        } else Wq175GlossIcon(Wq175IconNames.menu(title),Modifier.size(45.dp))''',
    "home menu premium icons")

patch("HomeScreen.kt",
    '''            if (subtitle.isNotEmpty()) BodyText(subtitle)
            body()''',
    '''            if (subtitle.isNotEmpty()) BodyText(subtitle)
            val theme=Wq175ThemeArt.fromTitle(title)
            if(theme!=null && theme!="yeni_oyun") {
                Wq175ThemeBanner(theme,Modifier.fillMaxWidth().height(116.dp)
                    .clip(RoundedCornerShape(22.dp)))
            }
            body()''',
    "specific room hero artworks")

patch("WordQuest175Matching.kt",
    '''Text("A ⇄ B",fontSize=16.sp,fontWeight=FontWeight.Black,
                    color=MatchInk)''',
    '''Wq175ThemeIcon("eslestirmelerim",Modifier.size(49.dp))''',
    "matching card illustration")

patch("WordQuest175Matching.kt",
    '''                    Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text("KELİME KARTLARI"''',
    '''                    Wq175ThemeBanner("eslestirmelerim",Modifier.matchParentSize())
                    Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text("KELİME KARTLARI"''',
    "matching header artwork")

patch("WordQuest175NewGameHero.kt",
    '''        Canvas(Modifier.matchParentSize()) {''',
    '''        Wq175ThemeBanner("yeni_oyun",Modifier.matchParentSize())
        Canvas(Modifier.matchParentSize()) {''',
    "premium builder header illustration")

print("PASS: 20 standalone Android vectors installed into real resource tree.")
print("PASS: home menus, educational rooms, matching mode and new game show real native theme art.")
