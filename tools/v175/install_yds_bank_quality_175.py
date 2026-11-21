#!/usr/bin/env python3
"""Install YDS bank structural regression checks after all question additions."""
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"tools/v175/Wq175YdsBankQualityTest.kt"
dst=root/"source/app/src/test/java/com/kelimebulmacasi/a1/Wq175YdsBankQualityTest.kt"
assert src.exists()
dst.write_text(src.read_text("utf-8"),"utf-8")
s=dst.read_text("utf-8")
assert "150, questions.size" in s and "normalizedPrompts" in s
print("PASS: YDS bank structural quality regression installed; human language review still required")
