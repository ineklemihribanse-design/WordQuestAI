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


def matching_shortcuts(s):
    # Each import mode receives its own callback; no more Learning-only routing.
    s=once(s,
        '    selected:String?=null\n) {',
        '    selected:String?=null,\n'
        '    onPdf:()->Unit=onImport,\n'
        '    onCamera:()->Unit=onImport,\n'
        '    onAi:()->Unit=onImport\n) {',
        "optional per-import-mode callbacks")
    s=once(s,
        'Wq175PuzzleSource("PDF’den İçe Aktar","pdf",Color(0xFFE7F3FF),onImport)',
        'Wq175PuzzleSource("PDF’den İçe Aktar","pdf",Color(0xFFE7F3FF),onPdf)',
        "PDF local callback")
    s=once(s,
        'Wq175PuzzleSource("Görsel / Kamera","camera",Color(0xFFFFF1E4),onImport)',
        'Wq175PuzzleSource("Görsel / Kamera","camera",Color(0xFFFFF1E4),onCamera)',
        "camera local callback")
    s=once(s,
        'Wq175PuzzleSource("Yapay Zekâ","ai_teacher",Color(0xFFF4E8FF),onImport)',
        'Wq175PuzzleSource("Yapay Zekâ","ai_teacher",Color(0xFFF4E8FF),onAi)',
        "AI local callback")
    return s


def import_dialog(s):
    # Old Word Memory behavior remains when onAccepted is null. A matching
    # import returns only selected reviewed pairs to the matching session.
    s=once(s,
        '    session:AppSession,ai:AiSession,onDismiss:()->Unit\n) {',
        '    session:AppSession,ai:AiSession,onDismiss:()->Unit,\n'
        '    onAccepted:((List<WordPair>)->Unit)?=null,\n'
        '    initialSource:String="all"\n) {',
        "separate local import consumer")
    s=once(s,
        '    AlertDialog(onDismissRequest={if(!busy)onDismiss()},',
        '''    LaunchedEffect(initialSource) {
        when(initialSource) {
            "pdf" -> openDocument.launch(arrayOf("application/pdf"))
            "camera" -> camera()
            "gallery" -> openDocument.launch(arrayOf("image/*"))
        }
    }
    AlertDialog(onDismissRequest={if(!busy)onDismiss()},''',
        "launch requested file picker within current import window")
    old='''                        var saved=0
                        var skipped=0
                        for((index,pair) in pairs.withIndex()){
                            if(excluded[index]==true)continue
                            if(!pair.valid){skipped++;continue}
                            if(session.addManualLearning(pair.answer,pair.clue,""))saved++
                            else skipped++
                        }
                        pairs=emptyList()
                        previewPage=0
                        excluded.clear()
                        status="Deftere "+saved+" kelime eklendi. "+skipped+
                            " geçersiz veya tekrarlanan kayıt atlandı."'''
    new='''                        val chosen=pairs.withIndex()
                            .filter{(index,pair)->excluded[index]!=true && pair.valid}
                            .map{it.value}.distinctBy{englishLetters(it.answer)}
                        if(onAccepted!=null) {
                            if(chosen.size>=2) onAccepted(chosen)
                            else status="Eşleştirme için en az iki farklı kelime seç."
                        } else {
                            var saved=0
                            var skipped=0
                            for(pair in chosen) {
                                if(session.addManualLearning(pair.answer,pair.clue,""))saved++
                                else skipped++
                            }
                            pairs=emptyList()
                            previewPage=0
                            excluded.clear()
                            status="Deftere "+saved+" kelime eklendi. "+skipped+
                                " geçersiz veya tekrarlanan kayıt atlandı."
                        }'''
    s=once(s,old,new,"matching import does not modify LearningStore")
    s=once(s,
        'Text("Seçilen Kelimeleri Kaydet")',
        'Text(if(onAccepted!=null)"Eşleştirme Kartlarını Kullan" else "Seçilen Kelimeleri Kaydet")',
        "source-specific confirmation label")
    s=once(s,
        'title={Text("Kelimeleri İçe Aktar",fontWeight=FontWeight.ExtraBold,',
        'title={Text(if(onAccepted!=null)"Eşleştirmeye Kelime Aktar" else "Kelimeleri İçe Aktar",fontWeight=FontWeight.ExtraBold,',
        "import context title")
    s=once(s,
        'Text("PDF, fotoğraf veya AI önerileri önce burada düzenlenir. "+\n                    "Onay olmadan deftere kaydedilmez.",',
        'Text(if(onAccepted!=null)"PDF, fotoğraf veya AI önerilerini gözden geçir; seçtiklerin yalnızca bu eşleştirme turunda kullanılacak."\n                    else "PDF, fotoğraf veya AI önerileri önce burada düzenlenir. Onay olmadan deftere kaydedilmez.",',
        "import explanation matches destination")
    return s


def local_matching(s):
    s=once(s,
        'internal fun Wq175MatchingDialog(session:AppSession,onDismiss:()->Unit,onImport:()->Unit) {',
        'internal fun Wq175MatchingDialog(session:AppSession,ai:AiSession,onDismiss:()->Unit,onImport:()->Unit) {',
        "provide existing AI session for matching import")
    s=once(s,
        '    var wordSource by rememberSaveable { mutableStateOf("Hazır Paketten Seç") }',
        '''    var wordSource by rememberSaveable { mutableStateOf("Hazır Paketten Seç") }
    var importOpen by remember { mutableStateOf(false) }
    var importSource by remember { mutableStateOf("all") }
    var importedPairs by remember { mutableStateOf<List<Wq175MatchPair>>(emptyList()) }''',
        "matching session owns temporary reviewed imported words")
    s=once(s,
        '''                    mode=="notebook" || wordSource=="Kendi Kelimelerinle" ->''',
        '''                    wordSource=="İçe Aktarılan Kelimeler" && importedPairs.isNotEmpty() ->
                        importedPairs
                    mode=="notebook" || wordSource=="Kendi Kelimelerinle" ->''',
        "imported pairs feed matching game directly")
    s=once(s,
        '            val personalDeck = mode=="notebook" || (mode!="mistakes" && wordSource=="Kendi Kelimelerinle")',
        '            val personalDeck = mode=="notebook" || wordSource=="İçe Aktarılan Kelimeler" || (mode!="mistakes" && wordSource=="Kendi Kelimelerinle")',
        "imported pairs minimum count")
    s=once(s,
        '    Dialog(onDismissRequest=onDismiss,',
        '''    if(importOpen) Wq175ImportWordsDialog(
        session=session, ai=ai,
        onDismiss={importOpen=false},
        onAccepted={ pairs ->
            importedPairs=pairs.map{
                Wq175MatchPair("İçe Aktarılan","Kişisel",it.answer,it.clue)
            }
            wordSource="İçe Aktarılan Kelimeler"
            mode="classic"
            issue=""
            importOpen=false
        },
        initialSource=importSource
    )
    Dialog(onDismissRequest=onDismiss,''',
        "local import dialog above matching game")
    s=once(s,'    Dialog(onDismissRequest=onDismiss,',
        '    if(!importOpen) Dialog(onDismissRequest=onDismiss,',
        "hide matching window while its own import preview is open")
    s=once(s,
        '''                        onImport={onDismiss();onImport()},
                        heading="Eşleştirme için kelime kaynağın",
                        guidance="PDF, görsel ve yapay zekâ önce Kelime Hafızam’daki içe aktarma bölümüne gider. "+
                            "Kelimeleri onayladıktan sonra geri dönüp Kendi Kelimelerinle seç.",
                        selected=wordSource)''',
        '''                        onImport={importSource="all";importOpen=true},
                        onPdf={importSource="pdf";importOpen=true},
                        onCamera={importSource="camera";importOpen=true},
                        onAi={importSource="ai";importOpen=true},
                        heading="Eşleştirme için kelime kaynağın",
                        guidance="PDF, kamera ve yapay zekâdan gelen kelimeleri burada inceleyip doğrudan eşleştirmeye aktar. Kelime Hafızam'a yönlendirme yapılmaz.",
                        selected=wordSource)
                    if(wordSource=="İçe Aktarılan Kelimeler")
                        Text("Hazır: "+importedPairs.size+" eşleştirme kartı",
                            color=MatchTeal,fontWeight=FontWeight.ExtraBold)''',
        "matching import action source stays local")
    return s


def home_matching_ai(s):
    return once(s,
        '                Wq175PremiumHomeGrid(\n                    session=session,',
        '                Wq175PremiumHomeGrid(\n                    session=session,ai=ai,',
        "pass existing AI provider to local matching import")


def premium_grid_ai(s):
    s=once(s,
        'internal fun Wq175PremiumHomeGrid(\n    session:AppSession,',
        'internal fun Wq175PremiumHomeGrid(\n    session:AppSession,ai:AiSession,',
        "matching premium menu accepts same AI provider")
    s=once(s,
        'if (matchingOpen) Wq175MatchingDialog(session,\n        onDismiss={matchingOpen=false},onImport=learning)',
        'if (matchingOpen) Wq175MatchingDialog(session,ai,\n        onDismiss={matchingOpen=false},onImport=learning)',
        "no route to Kelime Hafızam from matching import")
    return s


def daily_content_and_rotation(s):
    s=once(s,
        '''            Wq175PremiumPracticeGrid { kind ->
            selected = kind
            questionIndex = 0
        }''',
        '''            Wq175PremiumPracticeGrid { kind ->
            selected = kind
            questionIndex = Wq177DailyRotation.next(
                context,profileKey,kind,Wq177DailyOriginals.total(kind))
        }''',
        "daily game re-entry shuffles instead of fixed first question")
    s=once(s,
        'PracticeKind.Sentence -> PracticeContent.sentences[number % PracticeContent.sentences.size]',
        'PracticeKind.Sentence -> (PracticeContent.sentences + Wq177DailyOriginals.sentences).let { it[number % it.size] }',
        "More to Read sentence practice additive source")
    s=once(s,
        'PracticeKind.Grammar -> PracticeContent.grammar[number % PracticeContent.grammar.size]',
        'PracticeKind.Grammar -> (PracticeContent.grammar + Wq177DailyOriginals.grammar).let { it[number % it.size] }',
        "Test Attack grammar error practice additive source")
    s=once(s,
        'PracticeKind.Story -> PracticeContent.story[number % PracticeContent.story.size]',
        'PracticeKind.Story -> (PracticeContent.story + Wq177DailyOriginals.story).let { it[number % it.size] }',
        "More to Read story completion additive source")
    if s.count('questionIndex++')==1:
        s=once(s,'questionIndex++',
            'questionIndex=Wq177DailyRotation.next(context,profileKey,selected ?: PracticeKind.Story,Wq177DailyOriginals.total(selected ?: PracticeKind.Story))',
            "non-sequential next practice question")
    return s


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
    assert 'Wq175GlossIcon("arena_"+kind,Modifier.size(69.dp))' in (SRC/"WordQuest175Arena.kt").read_text("utf-8"), "Approved sword icon missing"
    patch("VocabularyNeonPractice.kt",vocabulary)
    import shutil
    shutil.copyfile(ROOT/"tools/v177/Wq177PuzzleImportCards.kt",
                    SRC/"Wq177PuzzleImportCards.kt")
    patch("CustomEditor.kt",puzzle_editor_cards)
    patch("HomeScreen.kt",account_header)
    patch("HomeScreen.kt",local_source_editor)
    patch("WordQuest175MyPuzzles.kt",matching_shortcuts)
    patch("WordQuest175ImportWords.kt",import_dialog)
    patch("WordQuest175Matching.kt",local_matching)
    patch("Wq175PremiumHome.kt",premium_grid_ai)
    patch("HomeScreen.kt",home_matching_ai)
    shutil.copyfile(ROOT/"tools/v177/Wq177DailyOriginals.kt",SRC/"Wq177DailyOriginals.kt")
    shutil.copyfile(ROOT/"tools/v177/Wq177DailyRotation.kt",SRC/"Wq177DailyRotation.kt")
    patch("PracticeHubScreen.kt",daily_content_and_rotation)
    version()
    print("PASS: v1.0.77 code103, approved premium Arena sword icon and responsive reversible study card")

if __name__=="__main__":main()
