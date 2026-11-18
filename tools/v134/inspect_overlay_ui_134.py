#!/usr/bin/env python3
"""Fast inspection of the archived teacher UI before expensive Gradle assembly."""
from pathlib import Path
from io import BytesIO
import base64,zipfile,re
raw=base64.b64decode(Path("v121-live-3d-teacher-overlay.b64").read_bytes())
with zipfile.ZipFile(BytesIO(raw)) as z:
    target=["TeacherAvatar.kt","LiveTeacherScreen.kt","AiSession.kt"]
    for key in target:
        for name in z.namelist():
            if not name.endswith("/"+key):continue
            lines=z.read(name).decode("utf-8").splitlines()
            print("### "+key+" ("+str(len(lines))+" lines)")
            pat=r"TeacherAvatar|selectedAvatar|selectAvatar|avatar|speak|onPlaying|threadId|newThread|setSetting|bind\(|reply\(|history|notes|loadThread|selectThread"
            hits=[i for i,line in enumerate(lines) if re.search(pat,line,re.I)]
            if key=="AiSession.kt":
                print("### AI THREAD DETAILS (70..95 and 130..173)")
                for start,end in ((69,95),(130,173)):
                    for q in range(start,min(end,len(lines))):
                        t=lines[q].strip()
                        if re.search(r"AIza[\\w_-]{16,}|sk-[\\w_-]{15,}",t):t="[SECRET REDACTED]"
                        print(f"{q+1:04}: {t[:220]}")
            visited=set()
            for i in hits[:95]:
                for q in range(max(0,i-2),min(len(lines),i+4)):
                    if q in visited: continue
                    visited.add(q)
                    t=lines[q].strip()
                    if re.search(r"AIza[\w_-]{16,}|sk-[\w_-]{15,}",t):t="[SECRET REDACTED]"
                    print(f"{q+1:04}: {t[:220]}")
