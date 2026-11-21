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
    # A flashcard can always rotate in either direction, without implicitly
    # grading the learner. Primary navigation must be in the first viewport:
    # light-on-light "Önceki" at the bottom was effectively invisible.
    s=once(s,
        '        fun next() {\n'
        '            if (!reviews.containsKey(position)) record(false)\n'
        '            focus.clearFocus()',
        '        fun next() {\n'
        '            // Navigating is not an incorrect answer: skip does not grade.\n'
        '            focus.clearFocus()',
        "Next immediately advances without marking an unseen word incorrect")
    s=once(s,
        '        LinearProgressIndicator(progress = { (position + 1f) / deck.size },\n'
        '            modifier = Modifier.fillMaxWidth().height(6.dp)\n'
        '                .clip(RoundedCornerShape(5.dp)), color = MemoryCyan,\n'
        '            trackColor = Color(0xFFDCE8F5))',
        '''        LinearProgressIndicator(progress = { (position + 1f) / deck.size },
            modifier = Modifier.fillMaxWidth().height(6.dp)
                .clip(RoundedCornerShape(5.dp)), color = MemoryCyan,
            trackColor = Color(0xFFDCE8F5))
        // Always above the card, visible without scrolling on narrow phones.
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick={focus.clearFocus(); position--},
                enabled=position>0,
                modifier=Modifier.weight(1f).heightIn(min=50.dp),
                shape=RoundedCornerShape(16.dp),
                colors=ButtonDefaults.outlinedButtonColors(
                    contentColor=DarkText,
                    disabledContentColor=DarkText.copy(alpha=.35f)
                ),
                border=androidx.compose.foundation.BorderStroke(
                    1.dp, if(position>0)Color(0xFF7B92AC) else Color(0xFFCDD7E2))
            ) {
                Text("‹ Önceki",fontSize=15.sp,fontWeight=FontWeight.Bold)
            }
            Button(
                onClick={next()},
                modifier=Modifier.weight(1f).heightIn(min=50.dp),
                shape=RoundedCornerShape(16.dp),
                colors=ButtonDefaults.buttonColors(
                    containerColor=MemoryNavy,contentColor=Color.White)
            ) {
                Text(if(position==deck.lastIndex)"Bitir ›" else "Sonraki ›",
                    fontSize=15.sp,fontWeight=FontWeight.Bold)
            }
        }''',
        "high-contrast Previous/Next visible immediately under progress")
    s=once(s,
        'targetValue = if (studyMode == "card" && (flipped || reviewed)) 180f else 0f,',
        'targetValue = if (studyMode == "card" && flipped) 180f else 0f,',
        "no forced flip after user grades")
    s=once(s,
        'animationSpec = tween(durationMillis = 390, easing = FastOutSlowInEasing), label = "card-turn"',
        'animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing), label = "card-turn"',
        "smooth half-second easing for both directions")
    s=once(s,
        '        val showingBack = turn > 90f\n'
        '        Surface(modifier = Modifier.fillMaxWidth().heightIn(min = 245.dp)',
        '        val showingBack = turn > 90f\n'
        '        BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {\n'
        '        val wideCard = maxWidth >= 600.dp\n'
        '        Surface(modifier = Modifier.widthIn(max=if(wideCard)620.dp else 460.dp)\n'
        '            .fillMaxWidth()\n'
        '            .heightIn(min=if(wideCard)340.dp else 295.dp)\n'
        '            .clickable(enabled=studyMode=="card"){flipped=!flipped}',
        "phone/tablet responsive tap-to-flip card")
    start=s.index('        if (studyMode == "card") {\n            if (!flipped && !reviewed) {')
    end=s.index('        if (reviewed) Text("Tamamlanan:',start)
    replacement='''        } // end responsive card container
        if (studyMode == "card") {
            Button(
                onClick={flipped=!flipped},
                modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),
                colors=ButtonDefaults.buttonColors(
                    containerColor=MemoryLilac,contentColor=MemoryNavy),
                shape=RoundedCornerShape(16.dp)
            ) {
                Text(if(flipped)"↶  Soruyu Göster" else "↻  Cevabı Göster",
                    fontSize=16.sp,fontWeight=FontWeight.Bold)
            }
            if (flipped && !reviewed) {
                Text("Bu kelimeyi ne kadar iyi biliyorsun?",
                    color=DarkText,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick={record(false)},
                        modifier=Modifier.weight(1f).heightIn(min=50.dp),
                        colors=ButtonDefaults.outlinedButtonColors(contentColor=DarkText),
                        shape=RoundedCornerShape(16.dp)
                    ) {
                        Text("Tekrar çalış",fontSize=14.sp,fontWeight=FontWeight.Bold)
                    }
                    Button(
                        onClick={record(true)},
                        modifier=Modifier.weight(1f).heightIn(min=50.dp),
                        shape=RoundedCornerShape(16.dp),
                        colors=ButtonDefaults.buttonColors(
                            containerColor=Color(0xFF1E705E),
                            contentColor=Color.White)
                    ) {
                        Text("Öğrendim",fontSize=14.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
            if (reviewed) {
                Text("Değerlendirme kaydedildi. Sonraki kartla devam edebilir ya da bu kartı tekrar çevirebilirsin.",
                    color=DarkText.copy(alpha=.8f),fontSize=12.sp)
            }
        }
'''
    s=s[:start]+replacement+s[end:]
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
        'internal fun Wq175MatchingDialog(session:AppSession,ai:AiSession,onDismiss:()->Unit) {',
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
    # The Home dashboard is a separate composable from the parent navigator.
    # The parent already owns "ai", but HomeScreen didn't receive it.
    # Pass the same existing provider through the signature and call site;
    # never construct a second session (which would lose API/provider state).
    import re
    signature = re.compile(
        r'(\\bfun\\s+HomeScreen\\s*\\(\\s*session\\s*:\\s*AppSession\\s*,)',
        re.MULTILINE
    )
    # Raw regex tokens above are Python escapes (not JS string escapes).
    s, matched = signature.subn(r'\\1 ai: AiSession,', s, count=1)
    if matched != 1:
        raise AssertionError("HomeScreen signature must receive existing AiSession")
    s = once(s,
        '        Page.Home -> HomeScreen(\n            session,',
        '        Page.Home -> HomeScreen(\n            session, ai,',
        "pass AI session into HomeScreen from parent router")
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
        'if (matchingOpen) Wq175MatchingDialog(session,ai,\n        onDismiss={matchingOpen=false})',
        "no route to Kelime Hafızam from matching import")
    return s


def daily_content_and_rotation(s):
    # Exact indentation differs between earlier overlays; find the native
    # Daily Plan mode-launch callback and replace only its index reset.
    import re
    entry=re.compile(
        r'(Wq175PremiumPracticeGrid\s*\{\s*kind\s*->)(.*?)(\n\s*\})',
        re.DOTALL)
    match=entry.search(s)
    if not match: raise AssertionError("Native Daily Plan grid callback missing")
    body=match.group(2)
    if 'selected = kind' not in body:
        raise AssertionError("Daily Plan lost its original selected kind callback")
    nextIndex='questionIndex = Wq177DailyRotation.next(context,profileKey,kind,Wq177DailyOriginals.total(kind))'
    updated,count=re.subn(r'questionIndex\s*=\s*0',nextIndex,body,count=1)
    if count!=1:
        # Some previous overlays changed the reset location. Keep the
        # selected-kind callback and set the session's shuffled question.
        updated=body+'\n            '+nextIndex
    s=s[:match.start(2)]+updated+s[match.end(2):]
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


def crossword_fit(s):
    # The 1.0.75 mobile enlargement enforced a 28dp minimum EVEN IN NORMAL
    # MODE, so a 22-row by 18-column board was much larger than its viewport.
    # Keep the 30dp touch-friendly cells only for the user's explicit zoom mode.
    s=once(s,
        '    var expanded by remember(game.id) { mutableStateOf(board.words.size > 20) }',
        '''    val phoneBoardOverview =
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 600
    var expanded by remember(game.id,phoneBoardOverview) {
        mutableStateOf(board.words.size > 20 && !phoneBoardOverview)
    }''',
        "all phone crosswords open with a fitted overview instead of forced zoom")
    s=once(s,
        '''    val tile = if (enlarged) maxOf(regularTile, 30.dp)
        else if(phoneGrid) maxOf(regularTile, 28.dp) else regularTile''',
        '''    // Overview uses BOTH puzzle axes (width / columns, height / rows).
    // Only the user's Büyüt action switches to 30dp scrollable cells.
    val tile = if (enlarged) maxOf(regularTile, 30.dp) else regularTile''',
        "remove forced 28dp phone cells that cut off entire boards")
    s=once(s,
        '    val windowHeight = if(phoneGrid && !enlarged) height else minOf(height, actualHeight)',
        '    val windowHeight = minOf(height, actualHeight)',
        "overview viewport follows actual fitted row count")
    # Auto-scroll only when zooming. In overview the entire board is visible;
    # no initial camera jump to a small subsection of the puzzle.
    s=once(s,
        '        if (enlarged || phoneGrid) {',
        '        if (enlarged) {',
        "selected-cell centering only when explicit zoom is on")
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
    patch("PuzzleScreen.kt",crossword_fit)
    version()
    print("PASS: v1.0.77 code103, approved premium Arena sword icon and responsive reversible study card")

if __name__=="__main__":main()
