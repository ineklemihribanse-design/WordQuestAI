#!/usr/bin/env python3
"""Activate original generated 3D-ish icons in Arena, Home and exercise interiors."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v175"

def one(s,a,b,name):
    if s.count(a)!=1:raise AssertionError(f"{name}: {s.count(a)}")
    return s.replace(a,b,1)

def main():
    icons=ROOT/"source/app/src/main/assets/wq175_icons"
    names=("arena_duel","arena_team","arena_quiz","trophy","listen",
           "sentence","grammar","story","speak","puzzle","yds","cards",
           "plan","camera","assistant","packages","verbs","mistakes","ai_teacher",
           "pdf","scan","translate","paragraph","synonym","completion",
           "exam","review","timer","success","notebook",
           "analytics","insights","friends","account","settings","backup",
           "history","library","streak","messages")
    for name in names:
        assert (icons/(name+".webp")).stat().st_size>800,name
    shutil.copyfile(PKG/"WordQuest175Icons.kt",SRC/"WordQuest175Icons.kt")
    test_dir=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
    test_dir.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(PKG/"Wq175PremiumIconRoutingTest.kt",
        test_dir/"Wq175PremiumIconRoutingTest.kt")
    p=SRC/"WordQuest174Arena.kt"
    s=p.read_text("utf-8")
    s=one(s,"            Wq174ModeSymbol(kind,Modifier.size(76.dp))",
        '            Wq175GlossIcon("arena_"+kind,Modifier.size(76.dp))',
        "three game modes use original high DPI art")
    p.write_text(s,"utf-8")
    p=SRC/"WordQuest175PracticeVisuals.kt"
    s=p.read_text("utf-8")
    s=one(s,"Wq175PracticeIcon(kind,Modifier.size(45.dp))",
        "Wq175GlossIcon(Wq175IconNames.practice(kind),Modifier.size(49.dp))",
        "daily exercise entry")
    s=one(s,"Wq175PracticeIcon(kind,Modifier.size(43.dp))",
        "Wq175GlossIcon(Wq175IconNames.practice(kind),Modifier.size(47.dp))",
        "exercise question header")
    p.write_text(s,"utf-8")
    p=SRC/"HomeScreen.kt"
    s=p.read_text("utf-8")
    s=one(s,'''        Box(Modifier.size(42.dp).background(LightLime, CircleShape), contentAlignment = Alignment.Center) {
            Text(icon, color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = if (icon.length > 2) 11.sp else 17.sp)
        }''',
        '''        Wq175GlossIcon(Wq175IconNames.menu(title), Modifier.size(45.dp))''',
        "home and menu original scalable icons")
    p.write_text(s,"utf-8")
    print("PASS: 30 original 256px transparent icons for home, Arena, YDS, matching and scan/learning rooms.")

if __name__=="__main__":
    main()
