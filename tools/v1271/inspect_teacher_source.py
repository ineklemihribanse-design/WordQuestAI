#!/usr/bin/env python3
"""Print only matching teacher voice-code excerpts, not credentials or data."""
from pathlib import Path
from io import BytesIO
import base64,zipfile,re
archive=Path("v121-live-3d-teacher-overlay.b64")
blob=base64.b64decode(archive.read_bytes())
with zipfile.ZipFile(BytesIO(blob)) as z:
    targets=[n for n in z.namelist() if n.endswith(("LiveTeacherScreen.kt","TeacherAvatar.kt","TeacherStage3D.kt","TeacherConversation.kt","LiveTeacherViewModel.kt","ConversationAudio.kt","ConversationTts.kt"))]
    print("TEACHER_FILES",targets)
    for path in targets:
        data=z.read(path).decode("utf-8")
        lines=data.splitlines()
        print("TEACHER_FILE",path,"LINES",len(lines))
        selected=set()
        terms=("TextToSpeech","UtteranceProgressListener","startListening","stopListening","SpeechRecognizer","RecognitionListener","speaking","listening","thinking","TeacherStage3D","speak(","onDone","onStart","onError","speakText","tts","rememberLauncherForActivityResult","microphone","onResults","onEndOfSpeech","onReadyForSpeech","onBeginningOfSpeech","onPartialResults")
        for i,line in enumerate(lines):
            if any(w.lower() in line.lower() for w in terms):
                selected.update(range(max(0,i-7),min(len(lines),i+10)))
        if path.endswith("LiveTeacherScreen.kt"):
            selected.update(range(85, min(len(lines), 170)))
        if len(selected)>760:
            selected=set(sorted(selected)[:600])
        for i in sorted(selected):
            line=lines[i]
            if re.search(r'(AIza[0-9A-Za-z_-]{16,}|sk-[0-9A-Za-z_-]{15,})',line):
                line="[REDACTED CREDENTIAL]"
            print(f"{i+1:04d} {line}")

print("VOICE_AUDIO_SOURCE_SEARCH_BEGIN")
for overlay in ("v120-multi-ai-arena-overlay.b64","v121-live-3d-teacher-overlay.b64"):
    blob=base64.b64decode(Path(overlay).read_bytes())
    with zipfile.ZipFile(BytesIO(blob)) as z:
        for p in z.namelist():
            if not p.endswith(".kt"):continue
            data=z.read(p).decode("utf-8")
            if "class ConversationAudio" not in data and "UtteranceProgressListener" not in data:continue
            print("VOICE_SOURCE",overlay,p,"lines",len(data.splitlines()))
            lines=data.splitlines()
            if "class ConversationAudio" in data:
                selected=range(max(0,next(i for i,l in enumerate(lines) if "class ConversationAudio" in l)-18),min(len(lines),next(i for i,l in enumerate(lines) if "class ConversationAudio" in l)+250))
            else:
                selected=set()
                for i,line in enumerate(lines):
                    if "UtteranceProgressListener" in line or "TextToSpeech" in line:
                        selected.update(range(max(0,i-7),min(len(lines),i+12)))
            for i in selected:
                line=lines[i]
                if re.search(r'(AIza[0-9A-Za-z_-]{16,}|sk-[0-9A-Za-z_-]{15,})',line):line="[REDACTED]"
                print(f"V {i+1:04d} {line}")
print("VOICE_AUDIO_SOURCE_SEARCH_END")

print("ARCHIVE_AUDIO_SCAN_BEGIN")
for p in sorted(Path(".").glob("*")):
    if not (p.name.endswith((".b64",".zip"))):continue
    if p.stat().st_size>24_000_000:continue
    try:
        decoded=(base64.b64decode(p.read_bytes()) if p.name.endswith(".b64") else p.read_bytes())
        with zipfile.ZipFile(BytesIO(decoded)) as z:
            for name in z.namelist():
                if not name.endswith(".kt"):continue
                data=z.read(name).decode("utf-8",errors="replace")
                if any(k in data for k in ("class ConversationAudio","UtteranceProgressListener","TextToSpeech(")):
                    print("FOUND_AUDIO",p.name,name)
                    lines=data.splitlines()
                    ids=[i for i,line in enumerate(lines) if any(t in line for t in ("class ConversationAudio","UtteranceProgressListener","TextToSpeech(","fun play(","fun stop(","onRangeStart","onStart(","onDone("))]
                    selected=set()
                    for i in ids:
                        selected.update(range(max(0,i-6),min(len(lines),i+24)))
                    for i in sorted(selected):
                        if i>900:break
                        line=lines[i]
                        if re.search(r'(AIza[0-9A-Za-z_-]{16,}|sk-[0-9A-Za-z_-]{15,})',line):line="[REDACTED]"
                        print(f"A {i+1:04d} {line}")
    except (ValueError, OSError, zipfile.BadZipFile):
        pass
print("ARCHIVE_AUDIO_SCAN_END")

print("VOICE_EXACT_LINES_BEGIN")
blob=base64.b64decode(Path("v090-delta.b64").read_bytes())
with zipfile.ZipFile(BytesIO(blob)) as z:
    data=z.read("app/src/main/java/com/kelimebulmacasi/a1/ConversationAudio.kt").decode("utf-8").splitlines()
    for i in range(34,min(92,len(data))):
        print(f"V090_{i+1:03d} {data[i]}")
print("VOICE_EXACT_LINES_END")
