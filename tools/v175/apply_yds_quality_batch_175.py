#!/usr/bin/env python3
"""Add 48 distinct authored YDS questions; never substitute fake 2000-question counts."""
from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
tests=root/"source/app/src/test/java/com/kelimebulmacasi/a1"
shutil.copyfile(root/"tools/v175/Wq175YdsQualityBatch.kt",src/"Wq175YdsQualityBatch.kt")
p=src/"YdsContent.kt"
s=p.read_text("utf-8")
needle=" + Wq175YdsExpansion.questions"
assert s.count(needle)==1,("YDS additive anchor",s.count(needle))
p.write_text(s.replace(needle,needle+" + Wq175YdsQualityBatch.questions",1),"utf-8")
for file in ("YdsCenterTest.kt","Wq175FullYdsTest.kt","Wq175YdsBankQualityTest.kt"):
    p=tests/file
    s=p.read_text("utf-8")
    assert "150, YdsContent.questions.size" in s,file
    s=s.replace("150, YdsContent.questions.size","198, YdsContent.questions.size")
    s=s.replace("150, YdsContent.questions.map { it.id }.toSet().size",
                "198, YdsContent.questions.map { it.id }.toSet().size")
    s=s.replace("Only the existing 150 authored questions are installed; 2000 remains a target",
                "198 authored questions installed; 2000 remains an unmet target")
    p.write_text(s,"utf-8")
print("PASS: additive authored bank now 198 questions (previous 150 plus 48).")
print("QUALITY NOTE: This step checks authored content structure, NOT external teacher certification.")
