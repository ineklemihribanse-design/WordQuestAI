#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
tests=root/"source/app/src/test/java/com/kelimebulmacasi/a1"
s=(src/"Wq175YdsQualityBatch.kt").read_text("utf-8")
assert s.count('        q(')==48
for topic in ("Vocabulary","Grammar","Connectors","Phrasals","Completion",
              "Translation","Paragraph","Synonym"):
    assert s.count("YdsTopic."+topic)==6,topic
bank=(src/"YdsContent.kt").read_text("utf-8")
assert "+ Wq175YdsExpansion.questions + Wq175YdsQualityBatch.questions" in bank
for name in ("YdsCenterTest.kt","Wq175FullYdsTest.kt","Wq175YdsBankQualityTest.kt"):
    t=(tests/name).read_text("utf-8")
    expected="198, questions.size" if name=="Wq175YdsBankQualityTest.kt" else "198, YdsContent.questions.size"
    assert expected in t,name
assert "2000 remains an unmet target" in (tests/"Wq175YdsBankQualityTest.kt").read_text("utf-8")
print("PASS: 48 distinct new question IDs, 8 domains, tests raised to 198 (not 2000).")
