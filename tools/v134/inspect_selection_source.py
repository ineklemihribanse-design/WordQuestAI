#!/usr/bin/env python3
"""Redacted read-only audit of final assembled teacher selection and voice wiring.

Only repository source identifiers, not user Gemini keys or conversation data.
Run after all 1.0.34 patching and before Android Gradle build.
"""
from pathlib import Path
import re
pkg = Path("source/app/src/main/java/com/kelimebulmacasi/a1")
paths = [p for p in pkg.glob("*.kt")
         if p.name in ("TeacherAvatar.kt", "LiveTeacherScreen.kt",
                       "TeacherStore.kt", "AiSession.kt", "ConversationAudio.kt",
                       "TeacherStage3D.kt", "AppSession.kt")
         or "Teacher" in p.name or "Conversation" in p.name]
pattern=re.compile(r"TeacherAvatar\.(?:entries|values|Elif|Deniz|Mert)|TeacherAvatar\b|selectedAvatar|avatarOptions|visibleAvatars|listOf\(|remember.*avatar|speakText|onActualSpeechRange|onPlaying|\bGemini\b|\.prompt|\.reply",re.I)
for p in sorted(set(paths)):
    lines=p.read_text("utf-8").splitlines()
    hits=[i for i,x in enumerate(lines) if pattern.search(x)]
    if not hits:continue
    print("###",p.name,"matches",len(hits),"total lines",len(lines))
    seen=set()
    for i in hits[:45]:
        for j in range(max(0,i-2),min(len(lines),i+4)):
            if j in seen:continue
            seen.add(j)
            text=lines[j].strip()
            if re.search(r"AIza[0-9A-Za-z_-]{16,}|sk-[0-9A-Za-z_-]{15,}",text):text="[REDACTED SECRET]"
            if len(text)>240:text=text[:240]+"..."
            print(f" {j+1:04}: {text}")
