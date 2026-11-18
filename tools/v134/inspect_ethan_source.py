#!/usr/bin/env python3
"""Read archived v1.0.21 live teacher source without exposing user credentials.

The final current source is later reconstructed in the main build pipeline.
This fast 1.0.21 inspection guides precise fail-closed v1.0.34 patching.
"""
import zipfile,base64,re
from io import BytesIO
from pathlib import Path
blob=base64.b64decode(Path("v121-live-3d-teacher-overlay.b64").read_bytes())
patterns=("LiveTeacherScreen.kt","ConversationAudio.kt","TeacherAvatar","TeacherConversation","AiTeacher","Gemini","TeacherState","TeacherRepository")
with zipfile.ZipFile(BytesIO(blob)) as z:
    candidates=[x for x in z.namelist() if x.endswith(".kt")]
    print("FILES",candidates)
    for p in candidates:
        data=z.read(p).decode("utf8")
        if not (any(term in p for term in patterns) or "Gemini" in data and "teacher" in data.lower()):continue
        lines=data.splitlines()
        print("=== SOURCE",p,"TOTAL",len(lines),"===")
        if p.endswith("LiveTeacherScreen.kt"):
            ranges=[(0,min(len(lines),260)),(260,min(len(lines),500))]
        else:
            ranges=[(0,min(len(lines),300))]
        for start,end in ranges:
            for i in range(start,end):
                line=lines[i]
                if re.search(r'AIza[0-9A-Za-z_-]{16,}|sk-[0-9A-Za-z_-]{15,}',line):
                    line="[SECRET REDACTED]"
                print(f"{i+1:04d} {line}")
