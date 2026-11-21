#!/usr/bin/env python3
"""v1.0.81: the existing original Arena hero as wallpaper, bigger true avatars,
contrast-safe text, higher-visibility real Firebase hints, responsive full grid.
Runs strictly after v180; no account, friend or Firebase schema changes.
"""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v181"
def once(s,a,z,why):
    n=s.count(a)
    if n!=1:raise AssertionError(f"{why}: expected 1 anchor got {n}: {a[:90]!r}")
    return s.replace(a,z,1)
def edit(name,transform):
    path=SRC/name
    before=path.read_text("utf-8")
    after=transform(before)
    if before==after:raise AssertionError("No changes "+name)
    path.write_text(after,"utf-8")
    print("PASS 181:",name)

shutil.copyfile(PKG/"Wq181ArenaHeroStage.kt",SRC/"Wq181ArenaHeroStage.kt")
shutil.copyfile(PKG/"Wq181ArenaHintBar.kt",SRC/"Wq181ArenaHintBar.kt")

def online(s):
    s=once(s,
        '''Wq180ArenaStageBackground(
            Modifier.matchParentSize(), dark = match?.phase == "playing")''',
        '''Wq181ArenaHeroStage(
            Modifier.matchParentSize(), active = match?.phase == "playing")''',
        "replace lavender gradient with exact 1.0.75 original illustrated Arena")
    s=once(s,
        '                val fixedPattern = crosswordFixedPattern(puzzle, solved, selected)',
        '''                val fixedPattern = crosswordFixedPattern(puzzle, solved, selected)
                val hintIsUsed=selected in match.hintBy[myUid].orEmpty()
                val revealedIndex=if(hintIsUsed)
                    Wq179ArenaOrder.hintPosition(match.code,selected,fixedPattern)
                    else -1
                val revealedChar=word.entry.letters.getOrNull(revealedIndex)''',
        "derive revealed letter once from the same selected crossword")
    s=once(s,
        '                    Wq179ArenaClueHeader(puzzle,selected)\n',
        '''                    Wq179ArenaClueHeader(puzzle,selected)
                    if(match.rulesVersion>=2 && match.mode=="duel" && !match.cooperative) {
                        val hintsLeft=(ArenaDuelEngine.hintsFor(match.size) -
                            match.hintBy[myUid].orEmpty().size).coerceAtLeast(0)
                        Wq181ArenaHintBar(
                            left=hintsLeft,
                            alreadyUsed=hintIsUsed,
                            canUse=(myTurn && (countdown==null || countdown>0)
                                && selected !in solved && !hintIsUsed),
                            revealed=if(revealedIndex>=0 && revealedChar!=null)
                                "${revealedIndex+1}. harf: $revealedChar"
                                else null,
                            onHint={client.useHint(selected)}
                        )
                    }
''',
        "insert visible hint after existing clue header without depending on replay panel")
    s=once(s,
        '''ArenaCompactCrosswordBoard(puzzle, solved, selected, keyboardIsOpen, answerText) {''',
        '''ArenaCompactCrosswordBoard(puzzle, solved, selected, keyboardIsOpen,
                    answerText,hintIndex=revealedIndex,hintChar=revealedChar) {''',
        "show the actual revealed letter in its highlighted grid cell")
    block_start='                            val left=(ArenaDuelEngine.hintsFor(match.size) -'
    block_end='                            if(PremiumOnlineLimits.replayRound(match))'
    a=s.find(block_start);d=s.find(block_end,a)
    if a<0 or d<0 or s.count(block_start)!=1 or s.count(block_end)!=1:
        raise AssertionError("old below-grid hint block differs; refuse to duplicate")
    s=s[:a]+s[d:]
    s=once(s,
        'Text(gameTitle, color = OnlineCream, fontSize = 19.sp, fontWeight = FontWeight.Black)',
        'Text(gameTitle, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)',
        "readable white match title on art")
    s=once(s,
        'color = if (myTurn) OnlineGood else OnlineGold,',
        'color = if (myTurn) Color(0xFFA9FFE0) else Color(0xFFFFE0A4),',
        "high contrast status on illustrated stage")
    s=once(s,
        'Text("Sıra otomatik değişiyor…", color = OnlineSoft, fontSize = 11.sp)',
        'Text("Sıra otomatik değişiyor…", color = Color(0xFFFFE3B4), fontSize = 11.sp)',
        "readable turn transition indicator")
    return s

def scoreboard(s):
    s=once(s,
        '''    Row(modifier.heightIn(min=95.dp)''',
        '''    val tablet=androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp>=600
    Row(modifier.heightIn(min=if(tablet)116.dp else 99.dp)''',
        "tablet/phone adaptive scoreboard dimensions")
    s=once(s,
        'ArenaMascotV2(avatar,Modifier.size(if(active)54.dp else 47.dp))',
        '''ArenaMascotV2(avatar,Modifier.size(
            if(tablet) { if(active)85.dp else 73.dp }
            else { if(active)66.dp else 57.dp }))''',
        "more prominent real participant mascot portraits")
    s=once(s,
        'Text(name,color=Color.White,fontSize=11.sp,maxLines=1,',
        'Text(name,color=Color.White,fontSize=if(tablet)14.sp else 12.sp,maxLines=1,',
        "larger participant label without overflow")
    return s

def board(s):
    s=once(s,
        'positionedDraft:String, compact:Boolean, zoomed:Boolean=false, onSelect:(Int)->Unit',
        '''positionedDraft:String, compact:Boolean, zoomed:Boolean=false,
    hintIndex:Int=-1,hintChar:Char?=null,onSelect:(Int)->Unit''',
        "add one hint letter visual to real crossword grid")
    s=once(s,
        '        val gap=2.dp',
        '        val gap=if(maxWidth<500.dp)1.dp else 1.5.dp',
        "phone cells expand with tighter gaps")
    s=once(s,
        '''        val visibleHeight=(LocalConfiguration.current.screenHeightDp.dp*.72f)
            .coerceIn(320.dp,820.dp)''',
        '''        val visibleHeight=(LocalConfiguration.current.screenHeightDp.dp*.82f)
            .coerceIn(360.dp,1000.dp)''',
        "use more of phone/tablet height")
    s=once(s,
        '        val maxHeight=if(compact)180.dp else visibleHeight',
        '        val maxHeight=if(compact)210.dp else visibleHeight',
        "more visible board with keyboard open")
    s=once(s,
        '        val fitWidth=(maxWidth-12.dp-gap*cols)/cols',
        '        val fitWidth=(maxWidth-3.dp-gap*cols)/cols',
        "stretch grid to nearly full available width")
    s=once(s,
        '        val fitted=minOf(fitWidth,fitHeight).coerceIn(7.dp,54.dp)',
        '        val fitted=minOf(fitWidth,fitHeight).coerceIn(7.dp,60.dp)',
        "increase tablet cell maximum while still fitting")
    s=once(s,
        '        val boardWidth=(cell+gap)*cols+8.dp',
        '        val boardWidth=(cell+gap)*cols+3.dp',
        "remove excess horizontal margin")
    s=once(s,
        '        val selectedRows=chosen?.cells.orEmpty().map{it.row}',
        '''        val hintCell=chosen?.cells?.getOrNull(hintIndex)
        val selectedRows=chosen?.cells.orEmpty().map{it.row}''',
        "hint grid cell")
    s=once(s,
        '                                val locked=place in fixed',
        '''                                val locked=place in fixed
                                val hinted=active && place==hintCell && hintChar!=null''',
        "hint cell highlight")
    s=once(s,
        '''                                    locked->Color(0xFFC0EEE0)
                                    active->Color(0xFFDCD0FF)''',
        '''                                    locked->Color(0xFFC0EEE0)
                                    hinted->Color(0xFFFFE6A9)
                                    active->Color(0xFFDCD0FF)''',
        "gold hint cells")
    s=once(s,
        'Text((fixed[place]?:if(active)draft[place] else null)',
        'Text((fixed[place]?:if(hinted)hintChar else if(active)draft[place] else null)',
        "letter actually visible on selected cell")
    return s

def typing(s):
    s=once(s,
        'compact: Boolean, typed: String, onSelect: (Int)->Unit',
        '''compact: Boolean, typed: String, hintIndex:Int=-1,
    hintChar:Char?=null, onSelect: (Int)->Unit''',
        "typed input keeps backward compatibility and optional hint")
    s=once(s,
        'ArenaBoardV2(puzzle,solved,selected,placed,compact,zoomed,onSelect)',
        'ArenaBoardV2(puzzle,solved,selected,placed,compact,zoomed,hintIndex,hintChar,onSelect)',
        "forward revealed letter from game to board")
    s=once(s,
        '''        OutlinedButton(onClick={zoomed=!zoomed},
            contentPadding=PaddingValues(horizontal=14.dp,vertical=4.dp)){''',
        '''        OutlinedButton(onClick={zoomed=!zoomed},
            colors=androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                containerColor=androidx.compose.ui.graphics.Color(0xFFFFF7EF),
                contentColor=androidx.compose.ui.graphics.Color(0xFF34214F)),
            contentPadding=PaddingValues(horizontal=14.dp,vertical=4.dp)){''',
        "legible external zoom control")
    s=once(s,
        'Text(if(zoomed)"Küçült" else "Büyüt ⤢",fontSize=12.sp)',
        '''Text(if(zoomed)"Küçült" else "Büyüt ⤢",
                color=androidx.compose.ui.graphics.Color(0xFF34214F),fontSize=12.sp)''',
        "force dark zoom label")
    return s

edit("PremiumOnlineScreen.kt",online)
edit("ArenaReferenceComponents.kt",scoreboard)
edit("ArenaBoardV2.kt",board)
edit("ArenaCrosswordTyping.kt",typing)

gradle=ROOT/"source/app/build.gradle.kts"
s=gradle.read_text("utf-8")
assert s.count('versionCode = 106')==1 and s.count('versionName = "1.0.80"')==1
assert 'applicationIdSuffix = ".fresh"' in s
gradle.write_text(s.replace('versionCode = 106','versionCode = 107',1)
    .replace('versionName = "1.0.80"','versionName = "1.0.81"',1),"utf-8")
print("PASS: 1.0.81 code107, existing installed application ID unchanged.")
