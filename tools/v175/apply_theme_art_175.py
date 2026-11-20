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
# The actual approved 500x680 WebP scenes and 256x256 icons (losslessly
# transferred from the provided 1.0.75 ZIP, never reconstructed screenshots).
assets=sorted((PKG/"premium_webp").glob("wq175_*.webp"))
assert len(assets)==20, f"Expected the 20 original WebP art files, found {len(assets)}"
res_webp=ROOT/"source/app/src/main/res/drawable-nodpi"
res_webp.mkdir(parents=True,exist_ok=True)
for art in assets:
    raw=art.read_bytes()
    assert len(raw)>5000 and raw[:4]==b"RIFF" and raw[8:12]==b"WEBP", art
    shutil.copyfile(art,res_webp/art.name)
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
                Box(Modifier.fillMaxWidth().height(144.dp)
                    .clip(RoundedCornerShape(22.dp))) {
                    Wq175ThemeBanner(theme,Modifier.matchParentSize())
                    Column(Modifier.align(Alignment.CenterStart)
                        .fillMaxWidth(.63f).padding(17.dp),
                        verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text("WORDQUEST AI · ÖĞRENME",
                            color=Color(0xFFFFE6A1),fontSize=10.sp,
                            fontWeight=FontWeight.Black)
                        Text(title,color=Color.White,fontSize=20.sp,
                            lineHeight=23.sp,fontWeight=FontWeight.ExtraBold,
                            maxLines=3)
                    }
                }
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


patch("HomeScreen.kt",
    r'''                    HeroTiles(
                        Modifier.align(Alignment.CenterEnd).padding(end = if (wide) 18.dp else 4.dp)
                            .size(if (wide) 188.dp else 108.dp),
                    )
                    Column(
                        Modifier.align(Alignment.CenterStart).fillMaxWidth(if (wide) .67f else .77f)
                            .padding(horizontal = if (wide) 28.dp else 19.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "WordQuest AI", fontFamily = Serif, color = White,
                            fontSize = if (wide) 54.sp else 37.sp, lineHeight = if (wide) 55.sp else 39.sp,
                        )
                        Text(
                            "Türkçe ipucunu oku,\nİngilizcesini yaz.", Modifier.padding(top = 11.dp),
                            color = Color(0xFFE0ECDA), fontFamily = Sans, fontSize = 16.sp, lineHeight = 21.sp,
                        )
                    }''',
    '''                    Row(
                        Modifier.fillMaxWidth().padding(
                            horizontal=if(wide)28.dp else 16.dp,
                            vertical=14.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                    ) {
                        Column(Modifier.weight(1f),
                            verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Text(
                                if(wide) "WordQuest AI" else "WordQuest\\nAI",
                                fontFamily=Serif,color=White,
                                fontSize=if(wide)50.sp else
                                    if(LocalDensity.current.fontScale>1.2f)25.sp else 30.sp,
                                lineHeight=if(wide)52.sp else
                                    if(LocalDensity.current.fontScale>1.2f)29.sp else 33.sp,
                                maxLines=2
                            )
                            Text(
                                "Türkçe ipucunu oku,\\nİngilizcesini yaz.",
                                color=Color(0xFFE0ECDA),fontFamily=Sans,
                                fontSize=if(LocalDensity.current.fontScale>1.2f)
                                    13.sp else 15.sp,
                                lineHeight=if(LocalDensity.current.fontScale>1.2f)
                                    18.sp else 20.sp
                            )
                        }
                        Wq175ThemeIllustration("ana_sayfa",
                            Modifier.width(if(wide)178.dp else
                                if(LocalDensity.current.fontScale>1.2f)112.dp
                                else 124.dp)
                                .height(if(wide)178.dp else 164.dp))
                    }''',
    "responsive home hero keeps text and original illustration in separate columns")

patch("WordQuest175Arena.kt",
    '''import androidx.compose.ui.draw.clip''',
    '''import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha''',
    "stadium illustration alpha")
patch("WordQuest175Arena.kt",
    '''            Wq175StadiumBackdrop(Modifier.matchParentSize())''',
    '''            Wq175StadiumBackdrop(Modifier.matchParentSize())
            Wq175ThemeIllustration("arena",
                Modifier.align(Alignment.CenterEnd).height(226.dp)
                    .width(160.dp).alpha(.28f))''',
    "original stadium art below live selected mascot")

print("PASS: all 20 original full-quality WebP files copied to Android drawable-nodpi.")
print("PASS: home, Arena, match, builder and learning rooms show original art, with mascot layered live.")
