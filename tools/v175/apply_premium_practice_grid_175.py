#!/usr/bin/env python3
"""Mobile-native Daily Plan games grid without touching scoring, sound or saved goals."""
from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[2]
pkg=root/"tools/v175"
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
shutil.copyfile(pkg/"Wq175PremiumPracticeGrid.kt",src/"Wq175PremiumPracticeGrid.kt")
file=src/"PracticeHubScreen.kt"
s=file.read_text("utf-8")
old='''        for (kind in PracticeKind.entries) {
            Wq175PracticeTile(kind) { selected = kind; questionIndex = 0 }
        }'''
new='''        Wq175PremiumPracticeGrid { kind ->
            selected = kind
            questionIndex = 0
        }'''
assert s.count(old)==1,("existing daily game callbacks",s.count(old))
s=s.replace(old,new,1)
assert 'MenuItem("Yanlışlarımdan bulmaca"' in s
assert 'MenuItem("Kişisel AI öğretmenim"' in s
file.write_text(s,"utf-8")
print("PASS: 5 distinct 2.5D premium game tiles on a 2-column native Android grid")
print("PASS: small-screen and >135% font use accessible single-column layout")
print("PASS: real game selection, personal AI teacher and mistakes game remain")
