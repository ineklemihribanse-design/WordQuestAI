#!/usr/bin/env python3
"""Print only real post-integration crossword editor/layout fragments for v190 QA."""
from pathlib import Path
p=Path(__file__).resolve().parents[2]/"source/app/src/main/java/com/kelimebulmacasi/a1/PuzzleScreen.kt"
s=p.read_text("utf-8").splitlines()
words=("wordDraft","inlineTyping","submitInlineWord","inlineEditor","revealAndLearn",
       "previewLetters","val wide =","fittedBoardHeight","GameHeading(game",
       "ActiveClue(","Actions(view","BoardFrame(","WordFields(","LaunchedEffect(keyboardVisible")
found=[i for i,x in enumerate(s) if any(w in x for w in words)]
groups=[]
for i in found:
    low=max(0,i-7);high=min(len(s),i+18)
    if groups and low <= groups[-1][1]: groups[-1]=(groups[-1][0],max(high,groups[-1][1]))
    else:groups.append((low,high))
for low,high in groups:
    print(f"\nPUZZLE AUDIT FRAGMENT {low+1}-{high}:")
    for j in range(low,high):
        print(f"PUZZLE AUDIT {j+1}: {s[j][:180]}")
