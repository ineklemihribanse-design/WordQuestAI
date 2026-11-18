#!/usr/bin/env python3
"""Audit relevant provider UI source (repository code only, never device secrets).

Runs on the CI-assembled Kotlin sources after the Ethan 1.0.37 overlays.
An inspection helper only: does not mutate the app.
"""
from pathlib import Path
import re
root=Path("source/app/src")
files=[
 root/"main/java/com/kelimebulmacasi/a1/AiProviderSettings.kt",
 root/"main/java/com/kelimebulmacasi/a1/AiProviders.kt",
 root/"main/java/com/kelimebulmacasi/a1/AiSession.kt",
 root/"test/java/com/kelimebulmacasi/a1/AiProviderAndPhraseTest.kt",
]
for p in files:
    assert p.exists(),p
    lines=p.read_text("utf8").splitlines()
    print("###",p.name,"lines",len(lines))
    if p.name in ("AiProviderSettings.kt","AiProviders.kt","AiProviderAndPhraseTest.kt"):
        selected=range(min(len(lines),310))
    else:
        targets=[i for i,line in enumerate(lines) if re.search(
            r"AiProvider|Claude|Gemini|OpenAI|provider|model|credentials|bindProfile|hasKey",line,re.I)]
        indices=set()
        for i in targets[:95]:
            indices.update(range(max(0,i-2),min(len(lines),i+3)))
        selected=sorted(indices)
    for i in selected:
        line=lines[i]
        line=re.sub(r'AIza[0-9A-Za-z_-]{16,}|sk-[A-Za-z0-9_-]{15,}',"[SECRET REDACTED]",line)
        print(f"{i+1:04d}: {line[:270]}")
