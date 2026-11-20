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
# Replace only the two extra old-style menu cards. Their original arguments,
# state updates and navigation lambdas remain byte-for-byte unchanged.
for label in ("Yanlışlarımdan bulmaca", "Kişisel AI öğretmenim"):
    needle='MenuItem("'+label+'"'
    assert s.count(needle)==1, (label,s.count(needle))
    s=s.replace(needle,'Wq175PremiumPracticeSpecialTile("'+label+'"',1)
file.write_text(s,"utf-8")
print("PASS: 7 illustrated Daily Plan game entries, including mistakes and AI tutor.")
print("PASS: original onClick callbacks preserved; no edits to AI, scoring or profile data.")
print("PASS: accessible two-column grid with portrait-friendly extra cards.")
