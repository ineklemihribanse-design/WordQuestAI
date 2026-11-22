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
    "Wq186LenaVoiceSettings.kt",
    "Wq186LenaTranslation.kt",
    "Wq186EnglishDefinitionsOnline.kt",
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
    # Only resolve known v1.0.85 local-import and responsive-art overlaps.
    if merged.returncode < 0:
        raise SystemExit("SAFE STOP: git merge-file failed: "+merged.stderr[:500])
    if merged.returncode:
        import re
        matcher=re.compile(
            r"^<<<<<<<[^\n]*\n(.*?)^\|\|\|\|\|\|\|[^\n]*\n(.*?)^=======\n(.*?)^>>>>>>>[^\n]*\n",
            re.MULTILINE|re.DOTALL)
        decisions=[]
        def choose(match):
            ours,old,new=match.groups()
            if ("internal fun Wq175MatchingDialog" in ours and
                "onDismiss:()->Unit)" in ours and
                "onImport:()->Unit" in new):
                decisions.append("retain approved AI-enabled dialog signature")
                return ours
            if ("var importSource" in ours and "var importedPairs" in ours
                and "var levels" in new and "quickSeconds" in new):
                decisions.append("replace transient imports with persistent folders")
                return new
            if ("İçe Aktarılan Kelimeler" in ours and
                "selectedFolder" in new and "mode==\"definitions\"" in new):
                decisions.append("add stored folder and English definition bank source")
                return new
            if ("val personalDeck" in ours and
                "val personalDeck" in new and "Klasörlerim" in new):
                decisions.append("retain notebook and add local folder source")
                return new
            if ("Wq175ImportWordsDialog" in ours and
                "Wq186MatchingImporter" in new):
                decisions.append("upgrade transient imported-list modal to folders")
                return new
            if ("BoxWithConstraints" in ours and
                'Wq175ThemeBanner("eslestirmelerim"' in ours and
                "if(!played) Box" in new):
                decisions.append("retain approved responsive illustrated heading")
                art=ours.replace("                BoxWithConstraints(",
                    "                if(!played) BoxWithConstraints(",1)
                art=art.replace("Oynanan: ${store.played}",
                    "Oynanan: ${progressStore.played}")
                art=art.replace("Kazanılan: ${store.wins}",
                    "Kazanılan: ${progressStore.wins}")
                art=art.replace("En iyi: ${store.best}",
                    "En iyi: ${progressStore.best}")
                return art
            if (("İçe Aktarılan Kelimeler" in ours or
                "Wq175PuzzleSourceShortcuts" in ours) and
                ("Wq186OptionTiles" in new or
                 "Wq186MatchingImporter" in new or
                 "Klasörlerim" in new)):
                decisions.append("choose reviewed persistent folder source controls")
                return new
            print("UNRECOGNIZED MATCHING CONFLICT",flush=True)
            print("OURS:",ours[:1600],flush=True)
            print("NEW:",new[:1600],flush=True)
            raise ValueError("unapproved matching conflict")
        try:
            merged_text=matcher.sub(choose,merged.stdout)
        except ValueError as error:
            raise SystemExit("SAFE STOP: "+str(error))
        if len(decisions)!=merged.returncode:
            raise SystemExit(f"SAFE STOP: expected {merged.returncode} conflicts, audited {len(decisions)}")
        print("Audited matching resolutions:",decisions,flush=True)
    else:
        merged_text=merged.stdout
    if any(marker in merged_text for marker in
        ("<<<<<<<","|||||||","=======",">>>>>>>")):
        raise SystemExit("SAFE STOP: unresolved matching conflict")
    if any(token in merged_text for token in
        ("importSource","importedPairs","onImport:()->Unit")):
        raise SystemExit("SAFE STOP: matching legacy import symbols not migrated")
    if (merged_text.count('Wq186MatchingImporter(')!=1 or
        merged_text.count('Wq175ThemeBanner("eslestirmelerim"')!=1):
        raise SystemExit("SAFE STOP: reviewed importer or responsive art missing")

    # Pass the existing bound AI session down to the matching screen; do not
    # create an anonymous session or store provider credentials in the game.
    home=APP/"HomeScreen.kt"
    premium=APP/"Wq175PremiumHome.kt"
    h=home.read_text("utf-8")
    p=premium.read_text("utf-8")
    def ensure_once(contents:str,old:str,new:str,label:str)->str:
        if contents.count(new)==1:
            print("KEEP existing approved route:",label)
            return contents
        return change_once(contents,old,new,label)
    h=ensure_once(h,
        "Page.Home -> HomeScreen(\n            session,",
        "Page.Home -> HomeScreen(\n            session, ai,",
        "existing Home navigation")
    h=ensure_once(h,
        "private fun HomeScreen(\n    session: AppSession,",
        "private fun HomeScreen(\n    session: AppSession,\n    ai: AiSession,",
        "HomeScreen signature")
    h=ensure_once(h,
        "Wq175PremiumHomeGrid(\n                    session=session,",
        "Wq175PremiumHomeGrid(\n                    session=session, ai=ai,",
        "premium home argument")
    p=ensure_once(p,
        "internal fun Wq175PremiumHomeGrid(\n    session:AppSession,",
        "internal fun Wq175PremiumHomeGrid(\n    session:AppSession,\n    ai:AiSession,",
        "premium home signature")
    p=ensure_once(p,
        "if (matchingOpen) Wq175MatchingDialog(session,",
        "if (matchingOpen) Wq175MatchingDialog(session,ai,",
        "matching opens with bound AI provider")

    # v185 mascot privacy: keep the switch in its existing place but avoid
    # tall wrapping prose in a narrow phone column.
    def safe_three_way(baseline:Path,revised:Path,live:Path)->str:
        if not all(x.is_file() for x in (baseline,revised,live)):
            raise ValueError("SAFE STOP: missing "+str(live))
        with tempfile.TemporaryDirectory() as folder:
            d=Path(folder)
            a,b,c=(d/"current",d/"base",d/"new")
            a.write_bytes(live.read_bytes())
            b.write_bytes(baseline.read_bytes())
            c.write_bytes(revised.read_bytes())
            result=subprocess.run(["git","merge-file","-p","--diff3",
                str(a),str(b),str(c)],text=True,capture_output=True)
            if result.returncode!=0 or any(x in result.stdout for x in
                ("<<<<<<<","=======",">>>>>>>")):
                raise ValueError("SAFE STOP: unexpected merge conflict "+live.name)
            return result.stdout
    updated_mascot=safe_three_way(
        ROOT/"tools/v185/Wq185FriendMascot.kt",
        P5/"Wq185FriendMascot.kt",APP/"Wq185FriendMascot.kt")
    updated_lena=safe_three_way(
        ROOT/"tools/v169/LenaMessengerBrand.kt",
        P5/"LenaMessengerBrand.kt",APP/"LenaMessengerBrand.kt")
    updated_composer=safe_three_way(
        ROOT/"tools/v171/LenaRichComposer.kt",
        P5/"LenaRichComposer.kt",APP/"LenaRichComposer.kt")

    # The v168 modal offered speed and volume but omitted the installed
    # voices and American/British switch. Replace just that existing modal.
    chat=APP/"ConversationScreen.kt"
    chat_original=chat.read_text("utf-8")
    start=chat_original.find("        if (voiceSettingsOpen) {")
    end=chat_original.find("        deleteCandidate?.let { thread ->",start)
    if start<0 or end<start or chat_original.count("if (voiceSettingsOpen)")!=1:
        raise ValueError("SAFE STOP: Lena voice-settings modal differs")
    chat_original=change_once(chat_original,
        "    var speaking by remember { mutableStateOf<Long?>(null) }",
        "    var speaking by remember { mutableStateOf<Long?>(null) }\\n"
        "    var translatingDraft by remember { mutableStateOf(false) }",
        "Lena draft translation state")
    chat_original=change_once(chat_original,
        "    val canEdit = !sending && !recording",
        "    val canEdit = !sending && !recording && !translatingDraft",
        "translate while preserving draft and chat")
    extra="""                onTranslate = { targetEnglish ->
                    if(ai.draft.isNotBlank() && !translatingDraft) {
                        translatingDraft=true
                        val original=ai.draft.trim()
                        scope.launch {
                            try {
                                val translated=ai.translateDraftForLena(
                                    apiKey,original,targetEnglish)
                                if(ai.draft.trim()==original) {
                                    ai.draft=translated.take(maxMessageLength)
                                    error=""
                                }else error="Taslak değişti, çeviri uygulanmadı."
                            }catch(cancelled:CancellationException){throw cancelled}
                            catch(e:Exception){
                                error=e.message ?: "Çeviri alınamadı."
                            }finally {translatingDraft=false}
                        }
                    }
                },
"""
    chat_original=change_once(chat_original,
        "                onLanguageToggle = {",extra+
        "                onLanguageToggle = {",
        "Lena chat translate callback")
    ai_file=APP/"AiSession.kt"
    ai_source=ai_file.read_text("utf-8")
    marker="""    internal suspend fun suggestWords(key: String, topic: String, level: String, count: Int, used: Set<String>): List<WordPair> =
        withProviderKey(key) { requestOnlineWords(it, topic, level, count, used, provider, providerModel) }
"""
    addition=marker+"""
    /** Translate a draft without modifying saved conversations or account data. */
    internal suspend fun translateDraftForLena(
        key:String,text:String,targetEnglish:Boolean
    ):String = withProviderKey(key) {
        requestWq186LenaTranslation(provider,providerModel,it,text,targetEnglish)
    }
"""
    ai_source=change_once(ai_source,marker,addition,"Lena authenticated translation")
    ai_source=change_once(ai_source,
        "    internal suspend fun reply(key: String, message: String,",
        """    /** English-only matching definitions use the same active private provider key. */
    internal suspend fun defineMatchingWords(
        key:String, words:List<String>, level:String
    ):Map<String,String> = withProviderKey(key) {
        requestWq186EnglishDefinitions(provider,providerModel,it,level,words)
    }

    internal suspend fun reply(key: String, message: String,""",
        "authenticated English matching meanings")

    # Offsets must be recomputed after inserting translation state and handler.
    start=chat_original.find("        if (voiceSettingsOpen) {")
    end=chat_original.find("        deleteCandidate?.let { thread ->",start)
    if start<0 or end<start:
        raise ValueError("SAFE STOP: altered Lena voice modal after adding translation")
    updated_chat=(chat_original[:start]+
        '        if (voiceSettingsOpen) Wq186LenaVoiceSettings(session) { voiceSettingsOpen = false }\n'+
        chat_original[end:])

    # Crop oversize mascot slots while retaining each user's exact own ZIP.
    h=change_once(h,"205.dp else 150.dp","170.dp else 118.dp",
        "custom mascot width phone/tablet")
    h=change_once(h,"314.dp else 236.dp","250.dp else 190.dp",
        "custom mascot height phone/tablet")

    # Give phones a larger share of the crossword screen. Tablet dimensions
    # and the stored puzzle/clock data are unchanged.
    puzzle=APP/"PuzzleScreen.kt"
    board=puzzle.read_text("utf-8")
    # Earlier phone polish uses 16dp margins, 28dp minimum cells, and a
    # 47%-height board. Those already exceed the new proposed 20dp fallback.
    if "val available = (maxWidth - if(phoneLayout)16.dp else 36.dp)" in board:
        board=change_once(board,
            ".padding(horizontal = if (wide) 23.dp else if(phoneLayout)8.dp else 16.dp, vertical = if (keyboardVisible) 7.dp else 13.dp)",
            ".padding(horizontal = if (wide) 23.dp else if(phoneLayout)8.dp else 16.dp, vertical = if (keyboardVisible) 6.dp else 9.dp)",
            "keep v175 physical phone board and tighten vertical gap")
        for sign in ("val phoneLayout = maxWidth < 600.dp",
            "else if(phoneGrid) maxOf(regularTile, 28.dp) else regularTile"):
            if sign not in board: raise ValueError("SAFE STOP: missing approved phone viewport "+sign)
    else:
        board=change_once(board,
            "val available = (maxWidth - 36.dp).coerceAtLeast(220.dp)",
            "val available = (maxWidth - if (wide) 36.dp else 20.dp).coerceAtLeast(220.dp)",
            "mobile board viewport width")
        board=change_once(board,
            ".padding(horizontal = if (wide) 23.dp else 16.dp, vertical = if (keyboardVisible) 7.dp else 13.dp)",
            ".padding(horizontal = if (wide) 23.dp else 8.dp, vertical = if (keyboardVisible) 6.dp else 9.dp)",
            "mobile board spacing")
    board=change_once(board,
        "Arrangement.spacedBy(if (keyboardVisible) 8.dp else 14.dp)",
        "Arrangement.spacedBy(if (keyboardVisible) 6.dp else 9.dp)",
        "mobile board vertical whitespace")
    board=change_once(board,
        "fontSize = 33.sp, lineHeight = 36.sp",
        "fontSize = 26.sp, lineHeight = 30.sp",
        "crossword compact heading")

    # All merge operations validated; now write only the prepared sources.
    current.write_text(merged_text,"utf-8")
    home.write_text(h,"utf-8")
    premium.write_text(p,"utf-8")
    (APP/"Wq185FriendMascot.kt").write_text(updated_mascot,"utf-8")
    (APP/"LenaMessengerBrand.kt").write_text(updated_lena,"utf-8")
    (APP/"LenaRichComposer.kt").write_text(updated_composer,"utf-8")
    ai_file.write_text(ai_source,"utf-8")
    chat.write_text(updated_chat,"utf-8")
    puzzle.write_text(board,"utf-8")
    for file in FILES:
        shutil.copyfile(P5/file,APP/file)
    print("PASS: Package 5 source overlay installed; 20/30 pairs and folders ready")
    print("NO publication / Firebase write / data reset")

if __name__=="__main__":
    main()
