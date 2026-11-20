#!/usr/bin/env python3
"""Connect existing WordQuest PDF/image/camera/MLKit scan and personal AI to FSRS."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v175"
home=SRC/"VocabularyNeonHome.kt"
s=home.read_text("utf-8")

def once(a,b,label):
    global s
    if s.count(a)!=1:raise AssertionError(f"{label}: {s.count(a)}")
    s=s.replace(a,b,1)

for file in ("DocumentScan.kt","ScanPhotoProvider.kt","WordPairs.kt",
             "AiSession.kt","CustomEditor.kt"):
    assert (SRC/file).exists(),file
shutil.copyfile(PKG/"WordQuest175ImportWords.kt",
    SRC/"WordQuest175ImportWords.kt")
TEST=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
TEST.mkdir(parents=True,exist_ok=True)
shutil.copyfile(PKG/"Wq175ImportPagingTest.kt",
    TEST/"Wq175ImportPagingTest.kt")

once('    var addWordOpen by rememberSaveable(session.profileKey) { mutableStateOf(false) }',
     '    var addWordOpen by rememberSaveable(session.profileKey) { mutableStateOf(false) }\n'
     '    var importWordsOpen by rememberSaveable(session.profileKey) { mutableStateOf(false) }',
    "import dialog state")
once('    if(addWordOpen) Wq175AddWordDialog(',
     '    if(importWordsOpen) Wq175ImportWordsDialog(\n'
     '        session=session,ai=ai,onDismiss={importWordsOpen=false}\n'
     '    )\n    if(addWordOpen) Wq175AddWordDialog(',
    "attach import dialog")
once('                Wq175AddWordEntry(onClick={addWordOpen=true})',
     '                Wq175WordAddBar(\n'
     '                    onManual={addWordOpen=true},\n'
     '                    onImport={importWordsOpen=true}\n'
     '                )',
    "unified compact add toolbar")
home.write_text(s,"utf-8")
print("PASS: PDF/photo/camera imports share the existing on-device scanner;")
print("AI suggestions use the user's saved provider key and require a preview approval.")
