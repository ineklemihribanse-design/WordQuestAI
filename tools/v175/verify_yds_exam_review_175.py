#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parents[2]
src=root/"source/app/src/main/java/com/kelimebulmacasi/a1"
s=(src/"YdsCenterScreen.kt").read_text("utf-8")
assert 'wq175Answers by rememberSaveable(profile)' in s
assert 'wq175Answers = emptyMap()' in s
assert 'wq175Answers = wq175Answers + (current.id to originalIndex)' in s
assert 'Wq175YdsExamReview(activeIds,wq175Answers)' in s
assert s.index('Wq175YdsExamReview(activeIds,wq175Answers)') > s.index('} else if (finished && activeIds.isNotEmpty())')
assert 'if (Wq175YdsVisibility.reveal(exam))' in s
art=(src/"Wq175YdsExamReview.kt").read_text("utf-8")
for key in ('submitted[q.id] != q.answer','Senin yanıtın:','Doğru: ','q.explanation'):
    assert key in art,key
assert (root/"source/app/src/test/java/com/kelimebulmacasi/a1/Wq175YdsExamReviewTest.kt").exists()
print("PASS: answers and explanations only available on completed review; YDS score and options intact.")
