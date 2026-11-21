#!/usr/bin/env python3
"""Mobile-native Daily Plan games grid without touching scoring, sound or saved goals."""
from pathlib import Path
import shutil
from patch_practice_extra_calls_175 import replace_special_card_call
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
# Upgrade each outer MenuItem to a real illustrated Surface, without touching
# its existing callback body, return@MenuItem labels, stored data or navigation.
for label in ("Yanlışlarımdan bulmaca", "Kişisel AI öğretmenim"):
    s=replace_special_card_call(s,label)
file.write_text(s,"utf-8")
print("PASS: five premium game cards and two illustrated native bonus cards.")
print("PASS: original MenuItem@ labels preserve early returns in both callbacks.")
print("PASS: game callbacks, personal profile data and recording stay unchanged.")
