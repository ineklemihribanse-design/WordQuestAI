#!/usr/bin/env python3
"""WordQuest AI 1.0.86 candidate overlay AFTER v1.0.85.

Requires exact verified v185 source. Stops on unexpected anchors, never touches
Firebase rules, personal mascot archives, Android applicationId, or user files.
"""
from pathlib import Path
import shutil

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
SRC_TEST=ROOT/"source/app/src/test/java/com/kelimebulmacasi/a1"
PKG=ROOT/"tools/v186"

def exact_replace(text,old,new,label):
    count=text.count(old)
    assert count==1, f"{label}: expected exactly one source anchor, got {count}"
    return text.replace(old,new,1)

def apply():
    assert (SRC/"Wq185FriendMascot.kt").is_file(), "Run 1.0.85 overlay first"
    assert (SRC/"Wq184BitmapTrim.kt").is_file(), "Run 1.0.84 overlay first"
    gradle=ROOT/"source/app/build.gradle.kts"
    version=gradle.read_text("utf-8")
    assert version.count("versionCode = 111")==1
    assert version.count('versionName = "1.0.85"')==1
    assert 'applicationIdSuffix = ".fresh"' in version

    cards=SRC/"Wq180FriendCards.kt"
    s=cards.read_text("utf-8")
    assert "entry.avatarPreview" in s, "v185 friend preview missing"
    s=exact_replace(s,
        "    onMinor:(()->Unit)?=null\n){",
        "    onMinor:(()->Unit)?=null,\n    onProfile:(()->Unit)?=null\n){",
        "friend tile profile action")
    s=exact_replace(s,
        "if(mainButton!=null && onMain!=null || minorButton!=null && onMinor!=null) {",
        "if(mainButton!=null && onMain!=null || minorButton!=null && onMinor!=null || onProfile!=null) {",
        "profile action row visibility")
    s=exact_replace(s,
        "                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){",
        "                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){\n"
        "                        if(onProfile!=null)\n"
        "                            Wq180SmallAction(\"Profili incele\",onProfile,outlined=true)",
        "profile action label")
    s=exact_replace(s,
        "    var lookup by remember{mutableStateOf(\"\")}",
        "    var lookup by remember{mutableStateOf(\"\")}\n"
        "    var inspectedUid by remember{mutableStateOf<String?>(null)}",
        "profile sheet selected friend ID")
    s=exact_replace(s,
        "                    onMinor=if(canInvite)null else { {hub.removeFriend(p.uid)} })",
        "                    onMinor=if(canInvite)null else { {hub.removeFriend(p.uid)} },\n"
        "                    onProfile={inspectedUid=p.uid})",
        "show profile on mutual friend's card only")
    assert s.rstrip().endswith("}\n}"), "friend panel ending changed unexpectedly"
    s=s.rstrip()[:-1]+'''
    // Read live friend state; closing the friendship immediately hides details.
    val friendProfile = hub.friends.firstOrNull { it.uid==inspectedUid }
    if(friendProfile!=null) Wq186FriendProfileDetail(friendProfile){
        inspectedUid=null
    }
}
'''
    assert s.count("onProfile={inspectedUid=p.uid}")==1
    cards.write_text(s,"utf-8")


    # Avoid skipping into failed/passed clues waiting for the replay phase.
    # The server snapshot remains authoritative for whether a word can be tried.
    online=SRC/"PremiumOnlineScreen.kt"
    onlineText=online.read_text("utf-8")
    onlineText=exact_replace(onlineText,
        '''                            if(!freeSkipUsed && canAnswer &&
                                puzzle.words.indices.count{it !in solved}>1) {''',
        '''                            val nextPlayable=Wq186ArenaNextWord.next(
                                match.code,myUid,match.size,solved,selected) {
                                index->PremiumOnlineLimits.canAttempt(match,index,myUid)
                            }
                            if(!freeSkipUsed && canAnswer &&
                                nextPlayable!=null) {''',
        "free skip can only select truly playable clues")
    onlineText=exact_replace(onlineText,
        '''                                    setWordIndex(Wq179ArenaOrder.next(
                                        match.code,myUid,puzzle.words.size,solved,selected))''',
        '''                                    setWordIndex(nextPlayable)''',
        "free skip cannot route into replay-waiting words")

    # Reuse the same server-authorized hint guard for the normal panel and
    # a compact hint control when the IME is open. Otherwise the hint button
    # disappears exactly while the player is typing on a small phone.
    onlineText=exact_replace(onlineText,
        '''                            canUse=(myTurn && (countdown==null || countdown>0)
                                && selected !in solved && !hintIsUsed
                                && fixedPattern.contains('.')),''',
        '''                            canUse=Wq186ArenaHintPolicy.canUse(
                                myTurn,countdown,selected in solved,hintIsUsed,
                                fixedPattern,
                                PremiumOnlineLimits.canAttempt(match,selected,myUid),
                                hintsLeft),''',
        "normal hint requires a currently playable selected clue")
    onlineText=exact_replace(onlineText,
        '''                ArenaCompactCrosswordBoard(puzzle, solved, selected, keyboardIsOpen,
                    answerText,hintIndex=revealedIndex,hintChar=revealedChar) {''',
        '''                if(keyboardIsOpen && match.rulesVersion>=2 &&
                    match.mode=="duel" && !match.cooperative) {
                    val hintsLeft=(ArenaDuelEngine.hintsFor(match.size) -
                        match.hintBy[myUid].orEmpty().size).coerceAtLeast(0)
                    Wq181ArenaHintBar(
                        left=hintsLeft,
                        alreadyUsed=hintIsUsed,
                        canUse=Wq186ArenaHintPolicy.canUse(
                            myTurn,countdown,selected in solved,hintIsUsed,
                            fixedPattern,
                            PremiumOnlineLimits.canAttempt(match,selected,myUid),
                            hintsLeft),
                        revealed=if(revealedIndex>=0 && revealedChar!=null)
                            "${revealedIndex+1}. harf: $revealedChar"
                            else null,
                        onHint={
                            if(Wq181ArenaHintSlot.reserve(context,match.code,
                                myUid,selected,fixedPattern)>=0)
                                client.useHint(selected)
                        }
                    )
                }
                ArenaCompactCrosswordBoard(puzzle, solved, selected, keyboardIsOpen,
                    answerText,hintIndex=revealedIndex,hintChar=revealedChar) {''',
        "IME-visible authorized hint action for active crossword clue")


    # Match snapshots can change only their attempt/pass status without
    # adding a solved word. Keep the currently highlighted clue actionable.
    # This does not grant a turn or advance the server's replay phase.
    onlineText=exact_replace(onlineText,
        '''wordIndex = Wq179ArenaOrder.next(match.code,myUid,match.size,match.solvedBy.keys,wordIndex)''',
        '''wordIndex = Wq186ArenaNextWord.next(
                    match.code,myUid,match.size,match.solvedBy.keys,wordIndex) {
                    index->PremiumOnlineLimits.canAttempt(match,index,myUid)
                } ?: wordIndex''',
        "auto-selected word must be replay/attempt eligible")
    onlineText=exact_replace(onlineText,
        '''    LaunchedEffect(match?.code, match?.solvedBy?.size, match?.phase) {''',
        '''    // Attempt/pass snapshots may change even when solved count stays fixed.
    // Re-select only if this exact clue is no longer answerable.
    LaunchedEffect(match) {
        val latest=match
        if(latest!=null && latest.phase=="playing" && latest.isCrossword &&
            !PremiumOnlineLimits.canAttempt(latest,wordIndex,myUid)) {
            val next=Wq186ArenaNextWord.next(
                latest.code,myUid,latest.size,latest.solvedBy.keys,wordIndex) {
                index->PremiumOnlineLimits.canAttempt(latest,index,myUid)
            }
            if(next!=null) {
                wordIndex=next
                answerText=""
            }
        }
    }
    LaunchedEffect(match?.code, match?.solvedBy?.size, match?.phase) {''',
        "react to failed/passed word attempts without resetting a valid draft")

    online.write_text(onlineText,encoding="utf-8")

    for source,destination in (
      ("Wq186ArenaGridPolicy.kt", "Wq186ArenaGridPolicy.kt"),
      ("Wq186ArenaNextWord.kt", "Wq186ArenaNextWord.kt"),
      ("Wq186ArenaHintPolicy.kt", "Wq186ArenaHintPolicy.kt"),
      ("ArenaBoardV186.kt","ArenaBoardV2.kt"),
      ("Wq186FriendProfileDetail.kt","Wq186FriendProfileDetail.kt"),
    ):
        assert (PKG/source).is_file()
        shutil.copyfile(PKG/source, SRC/destination)
    SRC_TEST.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(PKG/"Wq186ArenaGridPolicyTest.kt",
                    SRC_TEST/"Wq186ArenaGridPolicyTest.kt")
    shutil.copyfile(PKG/"Wq186ArenaNextWordTest.kt",
                    SRC_TEST/"Wq186ArenaNextWordTest.kt")
    shutil.copyfile(PKG/"Wq186ArenaHintPolicyTest.kt",
                    SRC_TEST/"Wq186ArenaHintPolicyTest.kt")
    version=version.replace("versionCode = 111","versionCode = 112",1)
    version=version.replace('versionName = "1.0.85"','versionName = "1.0.86"',1)
    gradle.write_text(version,"utf-8")
    print("PASS: 1.0.86 board IME viewport, larger mutually-shared friend profile; v185 cumulative, no Firebase mutation")

if __name__=="__main__":
    apply()
