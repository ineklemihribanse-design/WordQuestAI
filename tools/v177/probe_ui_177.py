#!/usr/bin/env python3
"""Print stable function signatures in assembled Android files for 1.0.77 patches."""
from pathlib import Path
root=Path(__file__).resolve().parents[2]/"source/app/src/main/java/com/kelimebulmacasi/a1"
tasks={
    "HomeScreen.kt":["Page.Custom","Page.Edit","ScreenPage(\"Hesap\"","Wq175PuzzleSourceShortcuts","Wq175PuzzleCreateCta","fun edit(","Page.Editor","Page.EditCustom","CustomEditor("],
    "CustomEditor.kt":["@Composable","Kamerayla çek","Galeriden seç","PDF seç","Kelime listesi yapıştır","Yapay zekâyla kelime üret","fun Custom","fun Editor","WordPair","pairs","scanPairs","session","onSaved","onSave","addWord"],
    "VocabularyNeonPractice.kt":["val turn","val showingBack","Kartı Çevir","graphicsLayer","fun next()","val card","VocabularyConceptVisual","if (studyMode == \"card\")"],
    "PracticeHubScreen.kt":["PracticeKind.Story","PracticeKind.Sentence","fun","questions","selection","LaunchedEffect","story"],
    "WordQuest175Arena.kt":["Wq175ArenaModeSymbol","Wq175ModeCard","Wq175GlossIcon","arena_duel"],
}
for name,needles in tasks.items():
    path=root/name
    print("\n### PROBE "+name+" exists="+str(path.is_file()))
    if not path.is_file():continue
    lines=path.read_text("utf-8").splitlines()
    hits=[i for i,s in enumerate(lines) if any(x in s for x in needles)]
    print("hits="+str([i+1 for i in hits])[:1200]+" total_lines="+str(len(lines)))
    chunks=[]
    for index in hits:
        a=max(0,index-(12 if name=="CustomEditor.kt" else 5))
        b=min(len(lines),index+(20 if name=="CustomEditor.kt" else 12))
        if chunks and a<=chunks[-1][1]:
            chunks[-1]=(chunks[-1][0],max(b,chunks[-1][1]))
        else: chunks.append((a,b))
    cap={"CustomEditor.kt":290,"HomeScreen.kt":260,"PracticeHubScreen.kt":160,
         "VocabularyNeonPractice.kt":185,"WordQuest175Arena.kt":65}[name]
    count=0
    for a,b in chunks:
        for i in range(a,b):
            if count>=cap: break
            print(f"UI177:{name}:{i+1}: {lines[i]}")
            count+=1
        if count>=cap:break
