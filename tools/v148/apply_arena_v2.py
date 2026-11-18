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
        '    if (match.phase == "lobby") ArenaRoomPlayersV2(hostName,guestName,match.guestUid.isNotBlank())\n'
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
        '                    match.turnUid == match.hostUid, countdown,solved.size,match.size,match.cooperative)',
        "live match scoreboard")
    source = once(source,
        '        "finished" -> {\n            // Both players see the winner animation',
        '        "finished" -> {\n'
        '            if (!match.cooperative) ArenaVictoryV2(\n'
        '                if (myScore >= otherScore) myName else rivalName,\n'
        '                if (myScore >= otherScore) isHost else !isHost,\n'
        '                myScore == otherScore)\n'
        '            // Both players see the winner animation',
        "winner mascot")
    file.write_text(source,encoding="utf-8")
    assert 'ArenaHomeV2(' in source and 'ArenaLiveScoreV2(' in source
    assert 'ArenaVictoryV2(' in source and 'ArenaRoomPlayersV2(' in source


def main() -> None:
    if not SRC.is_dir():
        raise RuntimeError("Assembled Android sources required; run after v147 and v122.")
    for name in ("ArenaV2Visuals.kt", "ArenaV2Lobby.kt", "ArenaV2Panel.kt"):
        shutil.copyfile(TOOLS / name, SRC / name)
    patch_online()
    print("Arena V2 lobby, player room, live scoring and animated victory integrated.")
    print("Preserved app id, version, other modules and all legacy database rules.")


if __name__ == "__main__":
    main()
