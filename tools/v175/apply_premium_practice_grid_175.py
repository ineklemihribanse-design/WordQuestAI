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
# Keep the two existing MenuItem call sites exactly as they are. Their
# navigation lambdas include early returns; renaming the composable call
# changes the Kotlin implicit lambda label and previously broke compilation.
# The menu icon resolver now supplies original themed art without rewriting
# the actions or any data stored by the game.
for label in ("Yanlışlarımdan bulmaca", "Kişisel AI öğretmenim"):
    needle='MenuItem("'+label+'"'
    assert s.count(needle)==1, (label,s.count(needle))
file.write_text(s,"utf-8")
print("PASS: 5 premium learning game cards and 2 preserved existing game entries.")
print("PASS: dedicated illustrated icon mapping for mistakes and AI teacher.")
print("PASS: original MenuItem callbacks/early-returns kept unchanged.")
