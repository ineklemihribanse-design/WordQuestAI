#!/usr/bin/env python3
"""Source-level evidence after all 1.0.77 Compose patches are applied."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
FILES={
    "CustomEditor.kt":(
        "Wq177PuzzleImportCards(",
        "onPdf={if(busy.isEmpty())picker.launch(arrayOf(\"application/pdf\"))}",
        "onCamera={if(busy.isEmpty() && cameraFileName==null)takePhoto()}",
        "onAi={if(busy.isEmpty())aiOpen=true}",
        "addImported(found, label)",
    ),
    "HomeScreen.kt":(
        'onImport={edit(null)})',
        'Wq2DPortrait(chosenMaskot.value,Modifier.size(82.dp))',
        'session.accountName?.takeIf { it.isNotBlank() }',
        "Wq175PremiumHomeGrid(",
        "session=session,ai=ai,",
    ),
    "WordQuest175Matching.kt":(
        'if(importOpen) Wq175ImportWordsDialog(',
        'if(!importOpen) Dialog(onDismissRequest=onDismiss,',
        'onPdf={importSource="pdf";importOpen=true}',
        'onCamera={importSource="camera";importOpen=true}',
        'onAi={importSource="ai";importOpen=true}',
        'wordSource=="İçe Aktarılan Kelimeler"',
        'importedPairs=pairs.map{',
    ),
    "WordQuest175ImportWords.kt":(
        'onAccepted:((List<WordPair>)->Unit)?=null,',
        'initialSource:String="all"',
        'if(onAccepted!=null)',
        'if(chosen.size>=2) onAccepted(chosen)',
        'initialSource',
    ),
    "WordQuest175Arena.kt":(
        'Wq175GlossIcon("arena_"+kind,Modifier.size(64.dp))',
    ),
    "VocabularyNeonPractice.kt":(
        'val wideCard = maxWidth >= 600.dp',
        '.widthIn(max=if(wideCard)620.dp else 460.dp)',
        '.clickable(enabled=studyMode=="card"){ flipped=!flipped }',
        'targetValue = if (studyMode == "card" && flipped)',
        'Text("↶  Ön yüzü tekrar gör"',
    ),
    "PracticeHubScreen.kt":(
        "Wq177DailyRotation.next(",
        "PracticeContent.grammar + Wq177DailyOriginals.grammar",
        "PracticeContent.story + Wq177DailyOriginals.story",
        "PracticeContent.sentences + Wq177DailyOriginals.sentences",
    ),
    "Wq177DailyOriginals.kt":(
        "PracticeQuestion(",
        "val sentences:",
        "val grammar:",
        "val story:",
    ),
}
for path,needs in FILES.items():
    text=(SRC/path).read_text("utf-8")
    for needle in needs:
        assert needle in text,(path,needle)
    print("PASS v1.0.77:",path,len(needs),"source assertions")
assert 'onImport={onDismiss();onImport()}' not in (SRC/"WordQuest175Matching.kt").read_text("utf-8")
assert 'onImport={go(Page.Learning)})' not in (SRC/"HomeScreen.kt").read_text("utf-8")
for name in ("Wq177PuzzleImportCards.kt","Wq177DailyOriginals.kt","Wq177DailyRotation.kt"):
    assert (SRC/name).exists(),name
assert (SRC/"WordQuest175Arena.kt").read_text("utf-8").count('Wq175GlossIcon("arena_"+kind')==1
gradle=(ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert 'versionCode = 103' in gradle
assert 'versionName = "1.0.77"' in gradle
assert 'applicationIdSuffix = ".fresh"' in gradle
print("PASS v1.0.77 source routing, responsive native cards, artwork, and upgrade identity")
