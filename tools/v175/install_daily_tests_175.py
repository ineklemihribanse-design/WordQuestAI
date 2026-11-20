#!/usr/bin/env python3
from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[2]
test=root/"source/app/src/test/java/com/kelimebulmacasi/a1"
test.mkdir(parents=True,exist_ok=True)
shutil.copyfile(root/"tools/v175/Wq175DailyGoalsTest.kt",test/"Wq175DailyGoalsTest.kt")
print("PASS: daily goal Kotlin JVM tests installed")
