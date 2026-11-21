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

def puzzle_editor_cards(s):
    old='''                    Column(
                        Modifier.fillMaxWidth().background(LightLime, RoundedCornerShape(16.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        PrimaryAction("Kamerayla çek", ::takePhoto, busy.isEmpty() && cameraFileName == null)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f), enabled = busy.isEmpty(),
                            ) { Text("Galeriden seç", fontFamily = Sans, fontWeight = FontWeight.SemiBold) }
                            OutlinedButton(
                                onClick = { picker.launch(arrayOf("application/pdf")) }, modifier = Modifier.weight(1f), enabled = busy.isEmpty(),
                            ) { Text("PDF seç", fontFamily = Sans, fontWeight = FontWeight.SemiBold) }
                        }
                        OutlinedButton(onClick = { pasteOpen = true }, modifier = Modifier.fillMaxWidth(), enabled = busy.isEmpty()) {
                            Text("Kelime listesi yapıştır", fontFamily = Sans, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(onClick = { aiOpen = true }, modifier = Modifier.fillMaxWidth(), enabled = busy.isEmpty()) {
                            Text("Yapay zekâyla kelime üret", fontFamily = Sans, fontWeight = FontWeight.Bold)
                        }
                        BodyText("İngilizce kelime ve Türkçe anlamı aynı satırda olan tabloları seç.")
                    }'''
    # The actual unchanged callbacks run in this CustomEditor, whose addImported
    # function stores rows in the current puzzle draft (not Word Memory).
    new='''                    Wq177PuzzleImportCards(
                        onCamera={if(busy.isEmpty() && cameraFileName==null)takePhoto()},
                        onGallery={if(busy.isEmpty())picker.launch(arrayOf("image/*"))},
                        onPdf={if(busy.isEmpty())picker.launch(arrayOf("application/pdf"))},
                        onPaste={if(busy.isEmpty())pasteOpen=true},
                        onAi={if(busy.isEmpty())aiOpen=true}
                    )'''
    return once(s,old,new,"5 real local import actions become premium editor cards")


def account_header(s):
    old='''    ScreenPage("Hesap", "", back) {
        Text("Oyun karakterim",color=Ink,fontFamily=Serif,
            fontSize=20.sp,fontWeight=FontWeight.Bold)
        Wq2DCompactSelector(chosenMaskot.value,
            onClick={choosingMaskot=true})
        BodyText("Seçtiğin karakter Arena, Kelime Hafızam ve profilinde kullanılır.")
        val account = session.accountName'''
    new='''    ScreenPage("Hesap", "", back) {
        Row(
            Modifier.fillMaxWidth()
                .background(Color(0xFFF3ECFF),RoundedCornerShape(23.dp))
                .padding(15.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(14.dp)
        ) {
            Wq2DPortrait(chosenMaskot.value,Modifier.size(82.dp))
            Column(Modifier.weight(1f),
                verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(session.accountName?.takeIf { it.isNotBlank() } ?: "Oyuncu",
                    fontFamily=Serif,color=Ink,fontSize=25.sp,
                    fontWeight=FontWeight.ExtraBold)
                Text("Oyun maskotum · "+Wq2DMascots.label(chosenMaskot.value),
                    color=Color(0xFF655183),fontSize=12.sp)
                TextButton(onClick={choosingMaskot=true}) {
                    Text("Karakteri değiştir",fontSize=13.sp)
                }
            }
        }
        BodyText("Seçtiğin karakter Arena, Kelime Hafızam ve profilinde kullanılır.")
        val account = session.accountName'''
    return once(s,old,new,"account name beside currently chosen mascot")


def local_source_editor(s):
    # The existing MyPuzzles shortcuts no longer jump to Word Memory.
    return once(s,'onImport={go(Page.Learning)})',
        'onImport={edit(null)})',
        "My Puzzles import stays inside its own editor")


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
    import shutil
    shutil.copyfile(ROOT/"tools/v177/Wq177PuzzleImportCards.kt",
                    SRC/"Wq177PuzzleImportCards.kt")
    patch("CustomEditor.kt",puzzle_editor_cards)
    patch("HomeScreen.kt",account_header)
    patch("HomeScreen.kt",local_source_editor)
    version()
    print("PASS: v1.0.77 code103, approved premium Arena sword icon and responsive reversible study card")

if __name__=="__main__":main()
