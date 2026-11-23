#!/usr/bin/env python3
"""Print narrow assembly diagnostics needed for Arena solo and two-page setup.
Runs only on ephemeral CI sources; never changes source files or users' data."""
from pathlib import Path
root=Path(__file__).resolve().parents[2]/"source/app/src/main/java/com/kelimebulmacasi/a1"
samples = {
    "ArenaApprovedHome.kt": ("matchSettingsPage","Wq175ArenaHeroAndModes(","Wq174UnifiedMatchSettings(","Text(quotaLabel","onOffline","onCreate","onJoin","onSolo","val solo","PremiumArenaScreen"),
    "PremiumArenaScreen.kt": ("fun PremiumArenaScreen(","val context","selectedMode","nextPuzzle(","nextQuestions(","fun resetBoard(","when(selectedMode)","if(selectedMode","onMode(","onLevel(","beginGame","currentLevel","premiumArena"),
    "PremiumArenaContent.kt": ("fun nextPuzzle(","fun nextQuestions(","fun questionsForSeed(","PremiumArenaRules.matchesLevel","val eligible","fun next"),
    "WordQuest174Arena.kt": ("fun Wq174UnifiedMatchSettings(","if(solo)","Wq186ArenaPresetControls(","if(isSolo && mode","Text(\"Maç Ayarları\""),
    "PremiumOnlineRoom.kt": ("var package6Rules","Wq186ArenaPresetStore.read","package6\" to","mode==\"team\""),
}
for name,words in samples.items():
    p=root/name
    if not p.is_file():
        print("ARENA AUDIT MISSING:",name)
        continue
    lines=p.read_text("utf-8").splitlines()
    printed=set()
    print("ARENA AUDIT FILE:",name,"lines",len(lines))
    for i,line in enumerate(lines):
        if any(w in line for w in words):
            for j in range(max(0,i-3),min(len(lines),i+6)):
                if j not in printed:
                    print(f"ARENA AUDIT {name}:{j+1}: {lines[j][:210]}")
                    printed.add(j)
    print("ARENA AUDIT END",name)
