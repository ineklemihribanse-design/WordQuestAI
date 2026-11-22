#!/usr/bin/env python3
"""Apply staged Package 5 only after the complete Package 4 / 1.0.85 assembly.

Nothing here publishes, signs, migrates data or deploys Firebase rules.
Refuse unexpected source drift and compute every edit before writing.
"""
from pathlib import Path
import subprocess
import tempfile
import shutil

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
P5=ROOT/"tools/v186/package5"
V175=ROOT/"tools/v175"
FILES=[
    "Wq186MeaningBank.kt",
    "Wq186EnglishMeaningIcon.kt",
    "Wq186MatchingFolders.kt",
    "Wq186MatchingImporter.kt",
]

def change_once(contents: str, old: str, new: str, label: str) -> str:
    count=contents.count(old)
    if count != 1:
        raise ValueError(f"SAFE STOP {label}: expected one anchor, found {count}")
    return contents.replace(old,new,1)

def main() -> None:
    if not (APP/"Wq186GameCards.kt").is_file():
        raise SystemExit("SAFE STOP: Package 4 is not assembled")
    if not (APP/"Wq185FriendMascot.kt").is_file():
        raise SystemExit("SAFE STOP: Package 3 / profile 1.0.85 is missing")
    for file in FILES:
        if not (P5/file).is_file() or (APP/file).exists():
            raise SystemExit("SAFE STOP: missing new file or clashing source "+file)

    original=V175/"WordQuest175Matching.kt"
    revised=P5/"WordQuest175Matching.kt"
    current=APP/"WordQuest175Matching.kt"
    if not all(f.is_file() for f in (original,revised,current)):
        raise SystemExit("SAFE STOP: incomplete matching merge")
    with tempfile.TemporaryDirectory() as directory:
        d=Path(directory)
        f_ours,f_base,f_new=(d/"ours.kt",d/"base.kt",d/"new.kt")
        f_ours.write_bytes(current.read_bytes())
        f_base.write_bytes(original.read_bytes())
        f_new.write_bytes(revised.read_bytes())
        merged=subprocess.run(
            ["git","merge-file","-p","--diff3",str(f_ours),str(f_base),str(f_new)],
            text=True,capture_output=True,
        )
    if merged.returncode != 0 or any(tag in merged.stdout for tag in
        ("<<<<<<<","=======",">>>>>>>")):
        raise SystemExit("SAFE STOP: approved matching UI changed; manual merge required")

    # Pass the existing bound AI session down to the matching screen; do not
    # create an anonymous session or store provider credentials in the game.
    home=APP/"HomeScreen.kt"
    premium=APP/"Wq175PremiumHome.kt"
    h=home.read_text("utf-8")
    p=premium.read_text("utf-8")
    h=change_once(h,
        "Page.Home -> HomeScreen(\n            session,",
        "Page.Home -> HomeScreen(\n            session, ai,",
        "existing Home navigation")
    h=change_once(h,
        "private fun HomeScreen(\n    session: AppSession,",
        "private fun HomeScreen(\n    session: AppSession,\n    ai: AiSession,",
        "HomeScreen signature")
    h=change_once(h,
        "Wq175PremiumHomeGrid(\n                    session=session,",
        "Wq175PremiumHomeGrid(\n                    session=session, ai=ai,",
        "premium home argument")
    p=change_once(p,
        "internal fun Wq175PremiumHomeGrid(\n    session:AppSession,",
        "internal fun Wq175PremiumHomeGrid(\n    session:AppSession,\n    ai:AiSession,",
        "premium home signature")
    p=change_once(p,
        "if (matchingOpen) Wq175MatchingDialog(session,",
        "if (matchingOpen) Wq175MatchingDialog(session,ai,",
        "matching opens with existing AI provider")

    # All merge operations validated; now write only the prepared sources.
    current.write_text(merged.stdout,"utf-8")
    home.write_text(h,"utf-8")
    premium.write_text(p,"utf-8")
    for file in FILES:
        shutil.copyfile(P5/file,APP/file)
    print("PASS: Package 5 source overlay installed; 20/30 pairs and folders ready")
    print("NO publication / Firebase write / data reset")

if __name__=="__main__":
    main()
