#!/usr/bin/env python3
"""Apply WordQuest AI 1.0.77 over the fully assembled, verified 1.0.76 source.

Keep user data, registered package, Android upgrade signer, premium graphics,
Arena matchmaking and the existing More-to-Read/Test-Attack style YDS bank.
Do not overwrite older modules or reinitialise LearningStore.
"""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
APP=ROOT/"source/app"

def once(s,a,b,label):
    n=s.count(a)
    if n!=1:raise AssertionError(f"{label}: expected one anchor, got {n}")
    return s.replace(a,b,1)

def patch(file,fn):
    p=SRC/file
    old=p.read_text("utf-8")
    new=fn(old)
    assert old!=new,file
    p.write_text(new,"utf-8")
    print("1.0.77 PATCH:",file)

def arena(s):
    # Premium swords from the user's independent high-res icon pack were not
    # connected to the latest Wq175ModeCard: a simple Canvas symbol appeared.
    return once(s,'            Wq175ArenaModeSymbol(kind)',
        '            Wq175GlossIcon("arena_"+kind,Modifier.size(64.dp))',
        "actual approved Arena sword icon inside native mode card")

def vocabulary(s):
    # Better card flip: tap the card itself and center at a responsive width.
    # Don't force the reverse side solely because the response was graded.
    s=once(s,
        'targetValue = if (studyMode == "card" && (flipped || reviewed)) 180f else 0f,',
        'targetValue = if (studyMode == "card" && flipped) 180f else 0f,',
        "reversible card orientation")
    s=once(s,
        '        val showingBack = turn > 90f\n'
        '        Surface(modifier = Modifier.fillMaxWidth().heightIn(min = 245.dp)',
        '        val showingBack = turn > 90f\n'
        '        BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {\n'
        '        val wideCard = maxWidth >= 600.dp\n'
        '        Surface(modifier = Modifier.fillMaxWidth()\n'
        '            .widthIn(max=if(wideCard)620.dp else 460.dp)\n'
        '            .heightIn(min=if(wideCard)360.dp else 285.dp)\n'
        '            .clickable(enabled=studyMode=="card"){ flipped=!flipped }',
        "phone/tablet card dimensions and touch-to-flip")
    s=once(s,
        '        if (studyMode == "card") {\n'
        '            if (!flipped && !reviewed) {',
        '        } // end responsive card container\n'
        '        if (studyMode == "card") {\n'
        '            if(flipped) {\n'
        '                TextButton(onClick={flipped=false},modifier=Modifier.fillMaxWidth()) {\n'
        '                    Text("↶  Ön yüzü tekrar gör",color=MemoryNavy)\n'
        '                }\n'
        '            }\n'
        '            if (!flipped && !reviewed) {',
        "allow flipping back without losing progress")
    return s

def home_import_routes(s):
    # Source buttons in My Puzzles used to send PDFs/camera/AI to Word
    # Memory instead of the actual personal crossword editor.
    return once(s,'onImport={go(Page.Learning)})',
        'onImport={edit(null)})',
        "My Puzzles imported words stay in its own editor")


def version():
    file=APP/"build.gradle.kts"
    s=file.read_text("utf-8")
    s=once(s,'versionCode = 102','versionCode = 103',"monotonic upgrade version")
    s=once(s,'versionName = "1.0.76"','versionName = "1.0.77"',"user-visible release")
    assert 'applicationIdSuffix = ".fresh"' in s and 'isDebuggable = false' in s
    file.write_text(s,"utf-8")

def main():
    for name in ("HomeScreen.kt","CustomEditor.kt","PracticeHubScreen.kt",
                 "VocabularyNeonPractice.kt","WordQuest175Arena.kt"):
        assert (SRC/name).is_file(),name
    patch("WordQuest175Arena.kt",arena)
    patch("VocabularyNeonPractice.kt",vocabulary)
    patch("HomeScreen.kt",home_import_routes)
    version()
    print("PASS: v1.0.77 code103, approved premium Arena sword icon and responsive reversible study card")

if __name__=="__main__":main()
