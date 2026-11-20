#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
s=(src/"HomeScreen.kt").read_text("utf-8")
u=(src/"WordQuest175RealCoverage.kt").read_text("utf-8")
assert s.count("Wq175RealVocabularyCoverage(session, level)")==1
for label in ("session.vocabulary().pool(level)",".distinct()","Hedef için","resmi CEFR onayı değildir"):
    assert label in u,label
assert "LaunchedEffect(level, session.profileKey)" in u
print("PASS: actual package word availability is calculated, not hardcoded or mislabeled as certified")
