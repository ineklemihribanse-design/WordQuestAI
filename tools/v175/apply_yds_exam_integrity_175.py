#!/usr/bin/env python3
"""Make timed YDS exams genuinely blind: answers and explanations appear only in study mode."""
from pathlib import Path
p=Path(__file__).resolve().parents[2]/"source/app/src/main/java/com/kelimebulmacasi/a1/YdsCenterScreen.kt"
s=p.read_text("utf-8")
old='''            if (answerChoice != -1) {
                BodyText(if (answerChoice == current.answer) "✓ Doğru cevap" else "Doğrusu: ${current.options[current.answer]}",
                    if (answerChoice == current.answer) Forest else ErrorInk)
                BodyText(current.explanation)
                PrimaryAction(if (activePosition == activeIds.lastIndex) "Sonucu gör" else "Sonraki soru", onClick = {'''
new='''            if (answerChoice != -1) {
                if (!exam) {
                    BodyText(if (answerChoice == current.answer) "✓ Doğru cevap" else "Doğrusu: ${current.options[current.answer]}",
                        if (answerChoice == current.answer) Forest else ErrorInk)
                    BodyText(current.explanation)
                } else {
                    BodyText("Yanıt kaydedildi. Doğru cevaplar sınav sırasında gösterilmez.")
                }
                PrimaryAction(if (activePosition == activeIds.lastIndex) "Sonucu gör" else "Sonraki soru", onClick = {'''
assert s.count(old)==1,("YDS answer-feedback anchor",s.count(old))
s=s.replace(old,new,1)
p.write_text(s,"utf-8")
print("PASS: study explanations retained; timed mini/full YDS answers no longer leak between questions")
