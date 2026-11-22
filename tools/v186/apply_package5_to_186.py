#!/usr/bin/env python3
"""Apply staged Package 5 only after the complete Package 4 / 1.0.85 assembly.

Nothing here publishes, signs, migrates data or deploys Firebase rules.
Refuse unexpected source drift and compute every edit before writing.
"""
from pathlib import Path
import subprocess
import tempfile
import shutil
import base64
import hashlib

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
            # Package 3 and Package 5 already agree on merging the selected
            # CEFR levels. This is only an extra space after a Kotlin comma,
            # not a competing feature or a reason to reject the full release.
            # Limit the exception to this one exact statement.
            if (ours.strip() == 'levels.flatMap { Wq175MatchingBank.load(context, it) }'
                and new.strip() == 'levels.flatMap { Wq175MatchingBank.load(context,it) }'):
                decisions.append("keep Package 3 multilevel matching (format-only conflict)")
                return ours
            # Both packages change the same selector region. Retain P3's
            # multi-level AND multi-word-type controls, while bringing in P5's
            # separate English-definition matching and the new game modes.
            if ("Wq186LevelChips(levels" in ours and
                "Wq186TopicChips(" in ours and
                'if((mode=="classic" || mode=="timed")' in ours and
                'mode=="definitions"' in new and
                'Wq186OptionTiles(listOf("A1"' in new and
                'if(mode!="definitions")' in new):
                expected_if='if((mode=="classic" || mode=="timed") && wordSource=="Hazır Paketten Seç") {'
                if ours.count(expected_if)!=1:
                    raise ValueError("P3/P5 selector guard differs")
                combined=ours.replace(expected_if,
                    'if((mode=="classic" || mode=="timed" || mode=="definitions")\n'
                    '                        && wordSource=="Hazır Paketten Seç") {',1)
                combined=combined.replace('Text("Seviye",color=MatchInk,',
                    'Text("Seviye · çoklu seçim",color=MatchInk,',1)
                kind_label='                        Text("Kelime türü",color=MatchInk,fontWeight=FontWeight.Bold)'
                if combined.count(kind_label)!=1:
                    raise ValueError("P3/P5 word-type selection anchor differs")
                combined=combined.replace(kind_label,
                    '                        if(mode!="definitions") {\n'
                    '    '+kind_label,1)
                combined=combined.rstrip('\n')+'\n                        }\n'
                decisions.append("preserve P3 multilevel/type chips with P5 English definitions mode")
                return combined
            # Matching bank filtering must honor P3 multi-kind selections
            # without applying word types to P5 English-definition questions.
            if ("kinds.isEmpty()" in ours and "it.kind in kinds" in ours
                and 'mode=="definitions"' in new and
                'kind=="Tümü"' in new and 'it.kind==kind' in new):
                a='kind=="Tümü" || it.kind==kind'
                if new.count(a)!=1:
                    raise ValueError("P3/P5 filter drift")
                decisions.append("combine P3 multi-kind filter with P5 definitions exemption")
                return new.replace(a,'kinds.isEmpty() || it.kind in kinds',1)
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
        all_conflicts=list(matcher.finditer(merged.stdout))
        print("P3_P5_MATCHING_CONFLICT_COUNT",len(all_conflicts),flush=True)
        for ix,clash in enumerate(all_conflicts,1):
            ours_text,base_text,theirs_text=clash.groups()
            print("P3_P5_CONFLICT",ix,"OURS",repr(ours_text[:700]),
                  "NEW",repr(theirs_text[:700]),flush=True)
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
    # The assembled v1.0.85 already routes AiSession through the home
    # header and premium menu. Never inject a second AI parameter here.
    if "Page.Home -> HomeScreen(" not in h or "ai: AiSession" not in h:
        raise ValueError("SAFE STOP: prior home AI routing is absent")
    if "internal fun Wq175PremiumHomeGrid(" not in p or "ai:AiSession" not in p:
        raise ValueError("SAFE STOP: prior premium home AI routing is absent")
    if "if (matchingOpen) Wq175MatchingDialog(session,ai," not in p:
        p=change_once(p,
            "if (matchingOpen) Wq175MatchingDialog(session,",
            "if (matchingOpen) Wq175MatchingDialog(session,ai,",
            "matching uses existing authenticated AI")
    print("KEEP existing v185 HomeScreen and premium AI routes")

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
    # The 1.0.75 approved compact portrait intentionally modifies the same
    # header as Package 5. Preserve its dynamic phone/tablet BoxWithConstraints
    # and replace only known dimensions; mode bar replacement is exact against
    # its untouched pre-P5 baseline.
    lena_live=(APP/"LenaMessengerBrand.kt").read_text("utf-8")
    lena_base=(ROOT/"tools/v169/LenaMessengerBrand.kt").read_text("utf-8")
    lena_new=(P5/"LenaMessengerBrand.kt").read_text("utf-8")
    def mode_block(contents:str)->str:
        beginning=contents.index("internal fun LenaMessengerModeBar(")
        end=contents.index("\n}\n\n/** Decorative speaking indicator",beginning)
        return contents[beginning:end+2]
    old_mode=mode_block(lena_base)
    live_mode=mode_block(lena_live)
    revised_mode=mode_block(lena_new)
    if live_mode!=old_mode:
        raise ValueError("SAFE STOP: secondary Lena modes differ from audited baseline")
    lena_live=change_once(lena_live,live_mode,revised_mode,
        "working overflow menu preserving existing mode callbacks")
    lena_live=change_once(lena_live,
        "import androidx.compose.material3.OutlinedButton",
        "import androidx.compose.material3.OutlinedButton\n"
        "import androidx.compose.material3.DropdownMenu\n"
        "import androidx.compose.material3.DropdownMenuItem",
        "Lena overflow controls imports")
    brand_adjustments=[
        ("LenaActualAvatar(if(compact)54.dp else 92.dp)",
         "LenaActualAvatar(if(compact)54.dp else 68.dp)"),
        ("Spacer(Modifier.width(if(compact)8.dp else 16.dp))",
         "Spacer(Modifier.width(if(compact)8.dp else 10.dp))"),
        ("fontSize = if(compact)22.sp else 29.sp",
         "fontSize = if(compact)22.sp else 25.sp"),
        ("if(!compact) Box(Modifier.width(1.dp).height(66.dp)",
         "if(!compact) Box(Modifier.width(1.dp).height(50.dp)"),
        ("Spacer(Modifier.width(if(compact)5.dp else 12.dp))",
         "Spacer(Modifier.width(if(compact)5.dp else 7.dp))"),
        ("avatar = userAvatar.value, size = if(compact)43.dp else 64.dp",
         "avatar = userAvatar.value, size = if(compact)43.dp else 46.dp"),
        (".padding(vertical = 6.dp)\n            .shadow(8.dp",
         ".padding(vertical = 3.dp)\n            .shadow(4.dp"),
        (".padding(horizontal = 18.dp, vertical = 17.dp)",
         ".padding(horizontal = 12.dp, vertical = 9.dp)"),
    ]
    for i,(old_dim,new_dim) in enumerate(brand_adjustments):
        lena_live=change_once(lena_live,old_dim,new_dim,
            "Lena responsive header "+str(i))
    if "val compact = maxWidth < 450.dp" not in lena_live:
        raise ValueError("SAFE STOP: previously approved small-screen portrait missing")
    updated_lena=lena_live
    updated_composer=safe_three_way(
        ROOT/"tools/v171/LenaRichComposer.kt",
        P5/"LenaRichComposer.kt",APP/"LenaRichComposer.kt")

    # The v168 modal offered speed and volume but omitted the installed
    # voices and American/British switch. Replace just that existing modal.
    chat=APP/"ConversationScreen.kt"
    chat_original=chat.read_text("utf-8")
    if "voiceSettingsOpen" not in chat_original:
        raise ValueError("SAFE STOP: Lena voice settings control not found")
    # 1.0.69+ can have either a modal already or only a voice button;
    # both routes now use the same real profile-scoped device voice picker.
    start=chat_original.find("        if (voiceSettingsOpen) {")
    end=chat_original.find("        deleteCandidate?.let { thread ->",
        start if start>=0 else 0)
    if end<0:
        raise ValueError("SAFE STOP: Lena dialog boundary missing")
    if start>=0 and end<start:
        raise ValueError("SAFE STOP: Lena voice modal location changed")
    chat_original=change_once(chat_original,
        "    var speaking by remember { mutableStateOf<Long?>(null) }",
        "    var speaking by remember { mutableStateOf<Long?>(null) }\n"
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

    # Recompute offsets after adding translation state. Some approved later
    # layouts retain the old settings dialog, others only its live button.
    start=chat_original.find("        if (voiceSettingsOpen) {")
    end=chat_original.find("        deleteCandidate?.let { thread ->",
        start if start>=0 else 0)
    if end<0 or (start>=0 and end<start):
        raise ValueError("SAFE STOP: altered Lena voice modal position")
    new_voice='        if (voiceSettingsOpen) Wq186LenaVoiceSettings(session) { voiceSettingsOpen = false }\n'
    if start>=0:
        updated_chat=chat_original[:start]+new_voice+chat_original[end:]
    else:
        updated_chat=chat_original[:end]+new_voice+chat_original[end:]

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
        if "val phoneLayout = maxWidth < 600.dp" not in board:
            raise ValueError("SAFE STOP: missing approved phone layout")
        # The approved v175 phone viewport can be replaced by Package 4's
        # merged puzzle geometry. Restore only the missing, audited cell
        # readability behavior, never override another active implementation.
        if "else if(phoneGrid) maxOf(regularTile, 28.dp) else regularTile" not in board:
            phone_var = "    val phoneGrid = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 600\n"
            # 1.0.85 can already declare phoneGrid above the viewport.
            # Only insert a new declaration when none is present.
            if "val phoneGrid" in board:
                phone_var = ""
            board=change_once(board,
                "    val tile = if (enlarged) maxOf(regularTile, 30.dp) else regularTile",
                phone_var + """    val tile = if (enlarged) maxOf(regularTile, 30.dp)
        else if(phoneGrid) maxOf(regularTile, 28.dp) else regularTile""",
                "retain the single existing phoneGrid and minimum cell size")
            board=change_once(board,
                "    val windowHeight = minOf(height, actualHeight)",
                "    val windowHeight = if(phoneGrid && !enlarged) height else minOf(height, actualHeight)",
                "phone board viewport height")
            import re
            pattern=r"LaunchedEffect\(selected,\s*enlarged,\s*[^)]*\)\s*\{\s*if\s*\(enlarged\)\s*\{"
            located=list(re.finditer(pattern,board))
            if len(located)==1:
                matched=located[0].group(0)
                updated=matched.replace("selected, enlarged,",
                    "selected, enlarged, phoneGrid,",1)
                updated=updated.replace("if (enlarged) {",
                    "if (enlarged || phoneGrid) {",1)
                board=change_once(board,matched,updated,
                    "auto-scroll enlarged phone crossword grid")
            elif "if (enlarged || phoneGrid)" not in board:
                raise ValueError(
                    "SAFE STOP: existing selection scroll logic changed; " +
                    repr(board[board.find("LaunchedEffect(selected"):][:300]))

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
    if "fontSize = 33.sp, lineHeight = 36.sp" in board:
        board=change_once(board,
            "fontSize = 33.sp, lineHeight = 36.sp",
            "fontSize = if(phoneLayout)26.sp else 33.sp, lineHeight = if(phoneLayout)30.sp else 36.sp",
            "crossword compact heading")
    elif "game.title" in board and "phoneLayout" in board:
        print("KEEP pre-existing approved responsive crossword heading")
    else:
        raise ValueError("SAFE STOP: missing crossword title or mobile viewport")

    # Compile-time declarations must be unique even after prior overlays.
    header_start=h.index("private fun HomeScreen(")
    header_end=h.index(") {",header_start)
    premium_start=p.index("internal fun Wq175PremiumHomeGrid(")
    premium_end=p.index(") {",premium_start)
    if h[header_start:header_end].count("ai: AiSession")!=1:
        raise ValueError("SAFE STOP: HomeScreen AiSession parameter must appear once")
    if p[premium_start:premium_end].count("ai:AiSession")!=1:
        raise ValueError("SAFE STOP: premium home AiSession parameter must appear once")
    if board.count("val phoneGrid")!=1:
        raise ValueError("SAFE STOP: mobile crossword phoneGrid must appear once")
    if merged_text.count("fun Wq186OptionTiles(")!=0:
        raise ValueError("SAFE STOP: matching screen must reuse Package 4 option tiles")

    # Use the exact approved icon (resized to 96px and WebP-compressed).
    # The SHA guard also detects truncation or accidental alterations in the
    # transport-safe base64 file; no remote image URL or runtime download.
    icon_source=P5/"approved_english_meaning_icon.webp.b64"
    icon_raw=base64.b64decode(icon_source.read_text("ascii").strip(),validate=True)
    if (hashlib.sha256(icon_raw).hexdigest()!=
        "7a747dfc18cc791bd69e8f55099f44a5828248028344628dec682f9faccdcdec" or
        len(icon_raw)!=3236 or icon_raw[:4]!=b"RIFF" or
        icon_raw[8:12]!=b"WEBP"):
        raise ValueError("SAFE STOP: original approved matching icon checksum differs")
    icon_dest=ROOT/"source/app/src/main/res/drawable-nodpi/wq186_english_meaning.webp"
    if icon_dest.exists():
        raise ValueError("SAFE STOP: new matching icon conflicts with existing drawable")

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
    icon_dest.parent.mkdir(parents=True,exist_ok=True)
    icon_dest.write_bytes(icon_raw)
    print("PASS: Package 5 source overlay installed; 20/30 pairs and folders ready")
    print("NO publication / Firebase write / data reset")

if __name__=="__main__":
    main()
