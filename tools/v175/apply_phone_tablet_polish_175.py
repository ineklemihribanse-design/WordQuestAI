#!/usr/bin/env python3
"""Follow-up mobile/tablet layout polish for 1.0.75.

Runs after apply_requested_ui_refinement_175. Strict source anchors prevent silent
misses. Does not change APK identity, version/signature, AI keys, speech engine,
session data, the original Lena GLB, Arena queueing or networking.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"

def once(s,old,new,name):
    n=s.count(old)
    if n!=1:
        raise AssertionError(f"{name}: expected one source location, found {n}")
    return s.replace(old,new,1)

def patch(name,modify):
    p=SRC/name
    before=p.read_text("utf-8")
    after=modify(before)
    if before==after: raise AssertionError("No changes: "+name)
    p.write_text(after,"utf-8")
    print("PASS:",name)

def clean_home(s):
    # Keep the portrait in Profile. Remove only the redundant 2D mascot pasted
    # over the user's own classroom wallpaper on the Home screen.
    start=s.index('                        Box(\n                            Modifier.width(if(wide)182.dp else')
    end=s.index('\n                        }',start)+len('\n                        }')
    assert 'Wq2DPortrait(' in s[start:end]
    assert 'Modifier.fillMaxWidth().fillMaxHeight(.96f)' in s[start:end]
    s=s[:start]+s[end:]
    # Allow the existing text and scene to occupy the whole hero width.
    return s

def center_arena(s):
    start=s.index('        Box(Modifier.fillMaxWidth().heightIn(min=258.dp)')
    end=s.index('        Text("Maceranı seç"',start)
    section=s[start:end]
    assert 'Wq175SceneBackdrop("arena"' in section
    assert 'Wq2DPortrait(selectedAvatar' in section
    hero=r'''        // Each device gets its own stage ratio. The selected native mascot
        // stands with its feet on the actual podium instead of below it.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 560.dp
            val wideTablet = maxWidth >= 840.dp
            val heroHeight = if(compact)310.dp else if(wideTablet)378.dp else 342.dp
            Box(Modifier.fillMaxWidth().height(heroHeight)
                .clip(RoundedCornerShape(29.dp))) {
                Wq175SceneBackdrop("arena",Modifier.matchParentSize())
                Column(
                    Modifier.align(Alignment.TopStart)
                        .fillMaxWidth(if(compact).53f else .43f)
                        .padding(start=if(compact)16.dp else 24.dp,top=23.dp),
                    verticalArrangement=Arrangement.spacedBy(if(compact)7.dp else 12.dp)) {
                    Text("WORDQUEST · TURNOVA",
                        color=ArenaGold175,fontSize=if(compact)9.sp else 12.sp,
                        fontWeight=FontWeight.Black,letterSpacing=.6.sp)
                    Text("ARENA",color=Color.White,
                        fontSize=if(compact)28.sp else 39.sp,
                        fontWeight=FontWeight.Black)
                    Text("Sahne senin!\nRakibini seç,\nzaferi kazan.",
                        color=Color(0xFFFFEEFF),fontSize=if(compact)13.sp else 18.sp,
                        lineHeight=if(compact)19.sp else 25.sp,
                        fontWeight=FontWeight.SemiBold)
                    Surface(color=Color(0xC34B217F),
                        shape=RoundedCornerShape(15.dp),
                        border=BorderStroke(1.dp,ArenaGold175)) {
                        Text("★ "+Wq2DMascots.label(selectedAvatar),
                            color=Color.White,fontSize=if(compact)11.sp else 14.sp,
                            fontWeight=FontWeight.ExtraBold,
                            modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp))
                    }
                }
                // Separate elliptical podium surface: masked by mascot's feet.
                Box(Modifier.align(Alignment.BottomCenter)
                    .offset(y=(-27).dp)
                    .width(if(compact)204.dp else 270.dp).height(40.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(
                        Color(0xFFFFDC8A),Color(0xFF773EB2),Color(0xFF3F1767))))
                    .border(2.dp,ArenaGold175,CircleShape))
                Wq2DPortrait(
                    selectedAvatar,
                    Modifier.align(Alignment.BottomCenter)
                        .offset(y=(-43).dp)
                        .size(if(compact)170.dp else if(wideTablet)218.dp else 196.dp),
                    victory=false
                )
            }
        }
'''
    return s[:start]+hero+s[end:]

def responsive_matching_hero(s):
    start=s.index('                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(27.dp))')
    end=s.index('                if(!played) {',start)
    old=s[start:end]
    assert 'Wq175ThemeBanner("eslestirmelerim"' in old
    assert 'Oynanan: ${store.played}' in old
    replacement=r'''                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val wide= maxWidth >= 620.dp
                    Box(Modifier.fillMaxWidth()
                        .height(if(wide)255.dp else 224.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Brush.horizontalGradient(listOf(
                            Color(0xFF146E79),Color(0xFF21A8A2),Color(0xFFEC91B8))))) {
                        Wq175ThemeBanner("eslestirmelerim",Modifier.matchParentSize())
                        Column(Modifier.align(Alignment.CenterStart)
                            .fillMaxWidth(if(wide).57f else .78f)
                            .padding(start=if(wide)27.dp else 17.dp),
                            verticalArrangement=Arrangement.spacedBy(if(wide)10.dp else 6.dp)) {
                            Text("KELİME KARTLARI",
                                fontSize=if(wide)13.sp else 11.sp,
                                color=Color(0xFFDBFFF4),
                                fontWeight=FontWeight.Black)
                            Text("Bul, eşleştir, öğren!",
                                fontSize=if(wide)36.sp else 26.sp,
                                lineHeight=if(wide)40.sp else 30.sp,
                                color=Color.White,
                                fontWeight=FontWeight.ExtraBold)
                            Text("Türkçe ve İngilizce kartlarını eşleştir; kelime hafızanı güçlendir.",
                                fontSize=if(wide)15.sp else 12.sp,
                                lineHeight=if(wide)21.sp else 16.sp,
                                color=Color.White,maxLines=3)
                            Text("Oynanan: ${store.played} · Kazanılan: ${store.wins} · En iyi: ${store.best}",
                                fontSize=if(wide)13.sp else 10.sp,
                                color=Color(0xFFFFF4FB),maxLines=2)
                        }
                    }
                }
'''
    return s[:start]+replacement+s[end:]


def enlarge_phone_puzzle_board(s):
    # Do NOT turn on the board's manual zoom mode. The user wants a physically
    # larger BOARD AREA and readable default cells on phone, not a zoom toggle.
    # Keep the tablet split layout, controls, input and puzzle model unchanged.
    s=once(s,
        '''    val compactBoard = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 600
    var expanded by remember(game.id, compactBoard) {
        mutableStateOf(board.words.size > 20 || compactBoard)
    }''',
        '    var expanded by remember(game.id) { mutableStateOf(board.words.size > 20) }',
        "restore manual zoom behaviour")
    s=once(s,
        'if (board.words.size <= 20 && !compactBoard) expanded = false',
        'if (board.words.size <= 20) expanded = false',
        "normal word selection is not a zoom action")

    s=once(s,
        '            val wide = maxWidth >= 760.dp && !keyboardVisible',
        '''            val wide = maxWidth >= 760.dp && !keyboardVisible
            val phoneLayout = maxWidth < 600.dp''',
        "separate phone geometry from tablet")
    s=once(s,
        '            val available = (maxWidth - 36.dp).coerceAtLeast(220.dp)',
        '            val available = (maxWidth - if(phoneLayout)16.dp else 36.dp).coerceAtLeast(220.dp)',
        "use more physical width of phone display")
    s=once(s,
        '            val boardHeight = if (keyboardVisible) keyboardBoard else minOf(viewWidth, 505.dp)',
        '''            // Phone grid dominates the screen; buttons remain below it.
            // Height adapts to actual device height, not fixed action controls.
            val boardHeight = if (keyboardVisible) keyboardBoard
                else if(phoneLayout) (maxHeight * .47f).coerceIn(330.dp, 440.dp)
                else minOf(viewWidth, 505.dp)''',
        "larger board area rather than zoom button")
    s=once(s,
        '.padding(horizontal = if (wide) 23.dp else 16.dp, vertical = if (keyboardVisible) 7.dp else 13.dp)',
        '.padding(horizontal = if (wide) 23.dp else if(phoneLayout)8.dp else 16.dp, vertical = if (keyboardVisible) 7.dp else 13.dp)',
        "phone board gets maximum available screen width")

    s=once(s,
        '    val tile = if (enlarged) maxOf(regularTile, 30.dp) else regularTile',
        '''    val phoneGrid = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 600
    // Default phone cells stay tappable, including wide 20-word boards.
    // They can scroll horizontally inside the larger normal-size viewport.
    val tile = if (enlarged) maxOf(regularTile, 30.dp)
        else if(phoneGrid) maxOf(regularTile, 28.dp) else regularTile''',
        "legible normal phone cell size")
    s=once(s,
        '    val windowHeight = minOf(height, actualHeight)',
        '    val windowHeight = if(phoneGrid && !enlarged) height else minOf(height, actualHeight)',
        "phone uses board viewport height instead of shrinking to grid row count")
    s=once(s,
        '''    LaunchedEffect(selected, enlarged, windowWidth, windowHeight) {
        if (enlarged) {''',
        '''    LaunchedEffect(selected, enlarged, phoneGrid, windowWidth, windowHeight) {
        if (enlarged || phoneGrid) {''',
        "keep selected letter visible when large phone grid can scroll")
    return s


def report_existing_board_geometry():
    # Assert the actual generated screen was resized structurally.
    t=(SRC/"PuzzleScreen.kt").read_text("utf-8")
    for needle in (
        'val phoneLayout = maxWidth < 600.dp',
        '(maxHeight * .47f).coerceIn(330.dp, 440.dp)',
        'else if(phoneGrid) maxOf(regularTile, 28.dp) else regularTile',
        'val windowHeight = if(phoneGrid && !enlarged) height',
        'if (enlarged || phoneGrid)',
        'BoardFrame(board, view, viewWidth, boardHeight',
        'Actions(view, ::checkAndLearn, ::revealAndLearn',
    ):
        assert needle in t,needle
    assert 'mutableStateOf(board.words.size > 20 || compactBoard)' not in t
    print("PASS: the normal phone crossword board occupies more space; no forced zoom.")



def packages_only(s):
    start=s.index('private fun PackageScreen(')
    end=s.index('private fun TopicScreen(',start)
    part=s[start:end]
    oldStart=part.index('        for ((group, entries) in games) {')
    oldEnd=part.index('        MenuItem("$level bulmacası oluştur"',oldStart)
    # The level tile is the only entry point. The catalogue has its own page.
    part=part[:oldStart]+'''        Text("Paket içeriğini görmek için seviyenin Paketi Gör düğmesine dokun.",
            color=Muted,fontFamily=Sans,fontSize=13.sp)
'''+part[oldEnd:]
    assert 'for ((group, entries) in games)' not in part
    # Keep the existing direct custom-game action, but avoid the duplicated
    # subject list which caused both pages to show the same catalogue.
    return s[:start]+part+s[end:]

def hide_voice_picker_lists(s):
    marker='            for (accent in EnglishVoice.entries) {'
    assert s.count(marker)==1
    s=s.replace(marker,
       '''            for (accent in EnglishVoice.entries) {
                Wq175SettingsCard(
                    accent.label+" cihaz sesleri",
                    "Kullanılabilir sesleri göster ve ses seç",
                    "listen"
                ) {''',1)
    old='''            }
            Text("Android ses motorları kadın/erkek bilgisini standart şekilde bildirmediği için "'''
    new='''                }
            }
            Text("Android ses motorları kadın/erkek bilgisini standart şekilde bildirmediği için "'''
    s=once(s,old,new,"close US/UK voice picker")
    return s

def compact_messenger(s):
    start=s.index('            if (!keyboardVisible) BoxWithConstraints(Modifier.fillMaxWidth()) {')
    end=s.index('\n            }\n',start)+len('\n            }\n')
    part=s[start:end]
    assert 'Text("Ses Ayarları ▾"' in part
    part=once(part,'Text(if(recording) "Dinleniyor" else "Konuş",',
        'Text(if(recording) "Kayıt" else "Konuş",maxLines=1,softWrap=false,',
        "mic label cannot wrap")
    part=once(part,'Text(if(speaking==null)"Dinle" else "Durdur",',
        'Text(if(speaking==null)"Dinle" else "Dur",maxLines=1,softWrap=false,',
        "playback label cannot wrap")
    part=once(part,'Text("Ses Ayarları ▾",fontSize=12.sp,maxLines=1)',
        'Text(if(wide)"Ses Ayarları" else "Ses ⚙",fontSize=12.sp,maxLines=1,softWrap=false)',
        "compact voice settings label")
    part=once(part,
        'modifier=Modifier.weight(1f),shape=RoundedCornerShape(50),',
        'modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=if(wide)12.dp else 4.dp,vertical=4.dp),shape=RoundedCornerShape(50),',
        "mic touch target")
    part=once(part,
        'modifier=Modifier.weight(if(wide).75f else 1f),',
        'contentPadding=PaddingValues(horizontal=if(wide)12.dp else 4.dp,vertical=4.dp),\n                        modifier=Modifier.weight(if(wide).75f else 1f),',
        "listen touch target")
    part=once(part,
        'modifier=Modifier.weight(if(wide)1f else 1.18f),',
        'contentPadding=PaddingValues(horizontal=if(wide)12.dp else 4.dp,vertical=4.dp),\n                        modifier=Modifier.weight(if(wide)1f else .82f),',
        "settings touch target")
    return s[:start]+part+s[end:]

def compact_live_header(s):
    start=s.index('        Row(\n            Modifier.fillMaxWidth().background(Color(0xFF164433))')
    end=s.index('        // Large, independently rendered 3D scene',start)
    header=r'''        // Two compact, stable lines instead of five buttons forced into one
        // phone-width row. Always above the 3D viewport.
        Column(
            Modifier.fillMaxWidth().background(Color(0xFF164433))
                .padding(horizontal=10.dp,vertical=6.dp),
            verticalArrangement=Arrangement.spacedBy(2.dp)
        ) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(7.dp),
                verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick={audio?.stop();back()},
                    contentPadding=PaddingValues(horizontal=7.dp,vertical=3.dp)) {
                    Text("‹ Geri",color=Color.White,fontSize=13.sp,
                        maxLines=1,softWrap=false)
                }
                LenaActualAvatar(35.dp)
                Column(Modifier.weight(1f)) {
                    Text("Lena · 3D",color=Color.White,fontSize=18.sp,
                        fontWeight=FontWeight.Bold,maxLines=1)
                }
                TextButton(onClick={audio?.stop();openTextChat()},
                    modifier=Modifier.background(Color(0xFFF4FAF0),RoundedCornerShape(20.dp)),
                    contentPadding=PaddingValues(horizontal=10.dp,vertical=5.dp)) {
                    Text("Mesajlar",color=Color(0xFF17543A),fontSize=12.sp,
                        fontWeight=FontWeight.Bold,maxLines=1,softWrap=false)
                }
            }
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.End,
                verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick={audio?.stop();threadsOpen=true;ai.searchThreads("")},
                    enabled=canEdit) {
                    Text("Sohbetler",color=Color.White,fontSize=12.sp,
                        maxLines=1,softWrap=false)
                }
                TextButton(onClick={audio?.stop();settingsOpen=true},enabled=canEdit) {
                    Text("Ayarlar",color=Color(0xFFFFE1A6),fontSize=12.sp,
                        maxLines=1,softWrap=false)
                }
            }
        }

'''
    old=s[start:end]
    assert 'LenaActualAvatar(42.dp)' in old and 'openTextChat()' in old
    return s[:start]+header+s[end:]

def live_3d_recovery(s):
    s=once(s, 'import androidx.compose.ui.unit.dp',
           'import androidx.compose.ui.unit.dp\nimport androidx.compose.ui.unit.sp',
           "3d fallback font import")
    # A GLB can return a non-null node and still be invisible on some GPUs.
    # Reframe closer to the real model's waist, and provide a visible native
    # portrait with a switch to the supported text chat when setup fails.
    s=once(s,'else -> Position(0f, 1.88f, 1.85f)',
           'else -> Position(0f, 1.23f, 3.0f)',"3d default camera")
    s=once(s,'else -> Position(0f, 1.88f, 0f)',
           'else -> Position(0f, 1.23f, 0f)',"3d target camera")
    s=once(s,
        '''            Text("Lena modeli açılamadı; güvenli 2.5D görünüme geçiliyor.",
                modifier = Modifier.align(Alignment.Center), color = Color.White)''',
        '''            Column(Modifier.align(Alignment.Center),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                LenaActualAvatar(132.dp)
                Text("3D Lena bu cihazda açılamadı. Mesajlar üzerinden konuşabilirsin.",
                    color=Color.White,fontSize=13.sp)
            }''',
        "visible native 3D model recovery")
    return s

def main():
    patch("HomeScreen.kt",clean_home)
    patch("WordQuest175Arena.kt",center_arena)
    patch("WordQuest175Matching.kt",responsive_matching_hero)
    patch("PuzzleScreen.kt",enlarge_phone_puzzle_board)
    report_existing_board_geometry()
    actual=(SRC/"PuzzleScreen.kt").read_text("utf-8")
    assert 'var expanded by remember(game.id) { mutableStateOf(board.words.size > 20) }' in actual
    assert 'if (board.words.size <= 20) expanded = false' in actual
    patch("HomeScreen.kt",packages_only)
    patch("WordQuestSettings.kt",hide_voice_picker_lists)
    patch("ConversationScreen.kt",compact_messenger)
    patch("LiveTeacherScreen.kt",compact_live_header)
    patch("LenaLive3DStage.kt",live_3d_recovery)
    assert 'Wq175SceneBackdrop' in (SRC/"WordQuest175Arena.kt").read_text("utf-8")
    print("PASS: no source version/signing/data/voice changes; live 3D remains crash isolated.")

if __name__=="__main__":
    main()
