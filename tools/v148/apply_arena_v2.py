#!/usr/bin/env python3
"""Integrate Arena V2 UI after all WordQuest overlays, without touching teacher/learning data."""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
TOOLS = ROOT / "tools/v148"


def once(source: str, old: str, new: str, label: str) -> str:
    count = source.count(old)
    if count != 1:
        raise AssertionError(f"{label}: expected one anchor, got {count}")
    return source.replace(old, new, 1)


def patch_online() -> None:
    file = SRC / "PremiumOnlineScreen.kt"
    source = file.read_text(encoding="utf-8")
    for old, new in (
        ("private val OnlineNight = Color(0xFF112720)", "private val OnlineNight = Color(0xFFF8F6FE)"),
        ("private val OnlinePanel = Color(0xFF1C3A30)", "private val OnlinePanel = Color(0xFFFFFFFF)"),
        ("private val OnlineCream = Color(0xFFF4ECD9)", "private val OnlineCream = Color(0xFF293253)"),
        ("private val OnlineGold = Color(0xFFE7C582)", "private val OnlineGold = Color(0xFF725DD3)"),
        ("private val OnlineSoft = Color(0xFFC5D8CD)", "private val OnlineSoft = Color(0xFF68748F)"),
        ("private val OnlineGood = Color(0xFF98D8A4)", "private val OnlineGood = Color(0xFF248E80)"),
    ):
        source = once(source, old, new, "arena palette " + old)

    opening = '        if (match == null) {\n            ArenaBanner("CANLI YARIŞMALAR"'
    end_marker = '        } else {\n            PremiumOnlineMatchScreen('
    start = source.index(opening)
    end = source.index(end_marker, start)
    landing = """        if (match == null) {
            ArenaHomeV2(
                playerName = session.profileName,
                loggedIn = client.ready,
                canCreate = client.canCreate,
                quotaLabel = if(client.quotaLoaded)
                    "Bugün kalan oda hakkı: " + (10 - client.usedToday).coerceAtLeast(0) + " / 10"
                    else "Oda hakların hazırlanıyor",
                mode = selectedMode, onMode = {selectedMode=it},
                level = level, onLevel = {
                    level=it; topic=PremiumArenaRules.allTopics
                    size=packs.availableSizes(it).firstOrNull() ?: 20
                },
                topic = topic, topics = packs.topics(level, selectedMode != "quiz"),
                onTopic = { topic=it;size=packs.availableSizes(level,it).firstOrNull() ?: 20 },
                sizes = packs.availableSizes(level, topic),
                size = size, onSize = {size=it},
                seconds = seconds, allowedTimes = PremiumArenaRules.allowedTimes,
                onSeconds = {seconds=it},
                roomCode = codeInput, onRoomCode = {codeInput=it.take(9).uppercase()},
                onCreate = {
                    val boardId = if(selectedMode=="quiz") "" else
                        packs.nextPuzzle(level,size,topic,session.profileKey)?.id.orEmpty()
                    if(selectedMode=="quiz" || boardId.isNotBlank())
                        client.create(selectedMode,level,topic,
                            if(selectedMode=="quiz")12 else size,
                            if(selectedMode=="team")0 else seconds,boardId)
                    else message="Bu seviyede yeterli bulmaca bulunamadı. Başka konu seç."
                },
                onJoin = {client.join(codeInput)},
                onAccount = account,
                onOffline = offline,
                onFriends = {
                    if(session.social.enabled)
                        MaterialTheme(colorScheme=lightColorScheme(
                            primary=OnlineGold,onSurface=OnlineCream,
                            onSurfaceVariant=OnlineSoft,surface=OnlinePanel)) {
                            FriendsPanel(session.social,onJoin={sender,code ->
                                client.join(code) {session.social.dismissInvite(sender)}
                            },premium=true)
                        }
                    else Text("Arkadaş davetleri için hesabına giriş yap.",
                        color=OnlineSoft,fontSize=12.sp)
                }
            )
"""
    source = source[:start] + landing + source[end:]
    # Original online panels were styled for a dark background. Use the light
    # ArenaV2Panel only here; offline learning/game panels remain untouched.
    source = source.replace("ArenaPanelBlock {", "ArenaV2Panel {")
    source = once(source,
        '    if (match.phase == "lobby") ArenaV2Panel {',
        '    if (match.phase == "lobby") ArenaRoomPlayersV2(hostName,guestName,\n'
        '        match.guestUid.isNotBlank(), match.hostAvatar, match.guestAvatar,\n'
        '        match.hostUid in match.readyBy, match.guestUid in match.readyBy)\n'
        '    if (match.phase == "lobby") ArenaV2Panel {',
        "room player cards")
    source = once(source,
        '    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {\n'
        '        Text("$hostName  ★ ${match.score(match.hostUid, questions)}", color = Color.White,',
        '    if (match.mode == "quiz") Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {\n'
        '        Text("$hostName  ★ ${match.score(match.hostUid, questions)}", color = OnlineCream,',
        "quiz-only compact scores")
    # Add a true live scoreboard in crossword mode instead of duplicating strips.
    source = once(source,
        '                if (keyboardIsOpen) Text("Çözülen ${solved.size}/${match.size} · Puan $myScore" + '
        '(countdown?.let { " · ${it} sn" } ?: ""), color = OnlineGold, fontSize = 12.sp, '
        'fontWeight = FontWeight.SemiBold) '
        'else ArenaScoreStrip(solved.size, match.size, myScore, countdown)',
        '                ArenaLiveScoreV2(hostName, guestName,\n'
        '                    match.score(match.hostUid,questions), match.score(match.guestUid,questions),\n'
        '                    match.turnUid == match.hostUid, countdown,solved.size,match.size,match.cooperative,\n'
        '                    match.hostAvatar,match.guestAvatar)',
        "live match scoreboard")
    source = once(source,
        '        "finished" -> {\n            // Both players see the winner animation',
        '        "finished" -> {\n'
        '            if (!match.cooperative) ArenaVictoryV2(\n'
        '                if (myScore >= otherScore) myName else rivalName,\n'
        '                if (myScore >= otherScore) isHost else !isHost,\n'
        '                myScore == otherScore,\n'
        '                if (myScore >= otherScore) { if (isHost) match.hostAvatar else match.guestAvatar }\n'
        '                else { if (isHost) match.guestAvatar else match.hostAvatar })\n'
        '            // Both players see the winner animation',
        "winner mascot")
    source = once(source,
        '        val canAnswer = myTurn && (countdown == null || countdown > 0) && !alreadyUnknown',
        '        val canAnswer = myTurn && (countdown == null || countdown > 0) && ' +
        '(if(match.rulesVersion >= 2 && !match.cooperative) ' +
        'PremiumOnlineLimits.canAttempt(match,selected,myUid) else !alreadyUnknown)',
        "one attempt per round")
    source = once(source,
        '                        fun submitAnswer() {',
        '''                        if(match.rulesVersion >= 2 && !match.cooperative) {
                            val left=(ArenaDuelEngine.hintsFor(match.size) -
                                match.hintBy[myUid].orEmpty().size).coerceAtLeast(0)
                            val used=selected in match.hintBy[myUid].orEmpty()
                            if(used) {
                                val pos=fixedPattern.indexOf('.')
                                if(pos>=0) Text("İpucu: " + (pos+1) + ". harf " +
                                    word.entry.letters[pos],color=OnlineGold,
                                    fontWeight=FontWeight.Bold,fontSize=12.sp)
                            } else ArenaOutlineButton("İPUCU (" + left + ")",{
                                client.useHint(selected)
                            },Modifier.fillMaxWidth())
                            if(PremiumOnlineLimits.replayRound(match))
                                Text("SON ŞANS TURU · Kelimeler yeniden deneniyor",
                                    color=OnlineGold,fontSize=12.sp)
                            else if(PremiumOnlineLimits.gaveUpOrFailed(match,selected,myUid))
                                Text("Bu kelime bekleyenlerde. Son şans turunda açılacak.",
                                    color=OnlineSoft,fontSize=12.sp)
                        }
                        fun submitAnswer() {''',
        "show private clue and replay status")
    source = once(source,
        '                        if (match.cooperative || !alreadyUnknown) {',
        '                        if (match.cooperative || (match.rulesVersion >= 2 && canAnswer) || ' +
        '(match.rulesVersion < 2 && !alreadyUnknown)) {',
        "prevent repeated passes on same word")
    source = once(source,
        '                            ArenaOutlineButton("BİLMİYORUM · CEZASIZ", {',
        '                            ArenaOutlineButton(if(match.rulesVersion >= 2) "PAS GEÇ" ' +
        'else "BİLMİYORUM · CEZASIZ", {',
        "pass label")
    source = once(source,
        '                if (isHost) ArenaGoldButton("YARIŞMAYI BAŞLAT", client::start,',
        '''                if (match.rulesVersion >= 2) {
                    ArenaOutlineButton(if(myUid in match.readyBy) "HAZIR ✓" else "HAZIRIM",
                        client::toggleReady,Modifier.fillMaxWidth())
                    if(match.guestUid.isNotBlank() &&
                        !match.readyBy.containsAll(listOf(match.hostUid,match.guestUid)))
                        Text("İki oyuncunun da hazır olması gerekiyor.",
                            color=OnlineSoft,fontSize=12.sp)
                }
                if (isHost) ArenaGoldButton("YARIŞMAYI BAŞLAT", client::start,''',
        "live ready toggle")
    source = once(source,
        '                    enabled = match.guestUid.isNotBlank())',
        '                    enabled = match.guestUid.isNotBlank() && ' +
        '(match.rulesVersion < 2 || ' +
        'match.readyBy.containsAll(listOf(match.hostUid,match.guestUid))))',
        "ready-gated match start")
    file.write_text(source,encoding="utf-8")
    assert 'ArenaHomeV2(' in source and 'ArenaLiveScoreV2(' in source
    assert 'ArenaVictoryV2(' in source and 'ArenaRoomPlayersV2(' in source


def main() -> None:
    if not SRC.is_dir():
        raise RuntimeError("Assembled Android sources required; run after v147 and v122.")
    for name in ("ArenaV2Visuals.kt", "ArenaV2Lobby.kt", "ArenaV2Panel.kt", "ArenaBoardV2.kt", "ArenaDuelEngine.kt"):
        shutil.copyfile(TOOLS / name, SRC / name)
    test_dir = ROOT / "source/app/src/test/java/com/kelimebulmacasi/a1"
    test_dir.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(TOOLS / "ArenaDuelEngineTest.kt", test_dir / "ArenaDuelEngineTest.kt")
    shutil.copyfile(TOOLS / "ArenaFirebaseReplayTest.kt", test_dir / "ArenaFirebaseReplayTest.kt")
    # The V2 server model is versioned and preserves legacy match behavior.
    # Production Firebase rules MUST be deployed before shipping this beta.
    shutil.copyfile(TOOLS / "PremiumOnlineRoomV2.kt", SRC / "PremiumOnlineRoom.kt")
    patch_online()
    print("Arena V2 lobby, player room, live scoring and animated victory integrated.")
    print("Cloud rule deployment still required before distributing V2 online matches.")
    print("Preserved app id, version, other modules and all legacy database rules.")


if __name__ == "__main__":
    main()
