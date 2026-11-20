#!/usr/bin/env python3
from pathlib import Path
p=Path(__file__).resolve().parents[2]/"source/app/src/main/java/com/kelimebulmacasi/a1/YdsCenterScreen.kt"
s=p.read_text("utf-8")
assert s.count('if (!exam) {')>=1
assert 'Yanıt kaydedildi. Doğru cevaplar sınav sırasında gösterilmez.' in s
assert 'BodyText(current.explanation)' in s
assert 'start(true, full=true)' in s
assert 'start(true)' in s
assert 'store.record(current, correct)' in s
assert 'session.recordYdsAttempt(current, correct, text)' in s
print("PASS: no answer leak during 15-min or 180-min exam; practice feedback and personal statistics preserved")
