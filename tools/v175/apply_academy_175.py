#!/usr/bin/env python3
"""Group existing YDS navigation under one academy; keep real destinations."""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v175"
def one(s,a,b,label):
    if s.count(a)!=1: raise AssertionError(f"{label}: {s.count(a)}")
    return s.replace(a,b,1)

shutil.copyfile(PKG/"WordQuest175Academy.kt",SRC/"WordQuest175Academy.kt")
p=SRC/"HomeScreen.kt"
s=p.read_text("utf-8")
s=one(s,'''    val unfinished = session.continueGame
    ScreenPage("", "", null) {''',
    '''    val unfinished = session.continueGame
    var wq175AcademyOpen by rememberSaveable { mutableStateOf(false) }
    if (wq175AcademyOpen) {
        Wq175YdsAcademy(
            back = { wq175AcademyOpen = false },
            prep = yds,
            exams = ydsCenter
        )
        return
    }
    ScreenPage("", "", null) {''',"single YDS academy navigation")
s=one(s,'''                    MenuItem("YDS Hazırlık", "Kolay, orta ve zor bulmacalar", "YDS", yds)
                    MenuItem("YDS Sınav Merkezi", "Paragraf, çeviri, yakın anlam ve süreli mini denemeler", "✎", ydsCenter)''',
    '''                    MenuItem("YDS Akademi",
                        "Kelime · Gramer · Paragraf · Deneme · Gelişim",
                        "YDS") { wq175AcademyOpen = true }''',
    "unify YDS under one entry")
s=one(s,
    '''    ScreenPage(mode.title, if (mode == GameMode.Yds) "Zorluk seviyesini seç" else "İngilizce seviyesini seç", back) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {''',
    '''    ScreenPage(mode.title, if (mode == GameMode.Yds) "Zorluk seviyesini seç" else "İngilizce seviyesini seç", back) {
        Wq175PackageIntro(mode, level)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {''',
    "category landing intro")
p.write_text(s,"utf-8")
print("PASS: single YDS Academy with both existing working routes and airy package header.")
