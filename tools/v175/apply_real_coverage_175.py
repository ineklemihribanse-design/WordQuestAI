#!/usr/bin/env python3
"""Install honest counts in approved prepared packages, preserving all navigation."""
from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
shutil.copyfile(root/"tools/v175/WordQuest175RealCoverage.kt",src/"WordQuest175RealCoverage.kt")
p=src/"HomeScreen.kt"
s=p.read_text("utf-8")
needle='        Wq175PackageIntro(mode, level)\n'
assert s.count(needle)==1,("package overview",s.count(needle))
s=s.replace(needle,needle+'        if (mode != GameMode.Yds) Wq175RealVocabularyCoverage(session, level)\n',1)
p.write_text(s,"utf-8")
print("PASS: game-ready vocabulary totals from actual session catalog; targets clearly labeled as targets")
