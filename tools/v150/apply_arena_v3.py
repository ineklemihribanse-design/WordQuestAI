#!/usr/bin/env python3
"""Apply reviewed Arena V3 layouts to assembled Android source after Arena V2."""
from pathlib import Path
import shutil

BASE=Path(__file__).resolve().parents[2]
SRC=BASE/"source/app/src/main/java/com/kelimebulmacasi/a1"
TOOLS=BASE/"tools/v150"

def once(text,old,new,name):
    count=text.count(old)
    if count!=1:raise RuntimeError(f"{name}: expected 1 anchor, got {count}")
    return text.replace(old,new,1)

def main():
    if not (SRC/"PremiumOnlineScreen.kt").is_file():
        raise RuntimeError("Assemble Arena V2 sources before patching V3")
    for name in ("ArenaV3Home.kt","ArenaV3Lobby.kt","ArenaV3Rules.kt"):
        shutil.copyfile(TOOLS/name,SRC/name)
    screen=SRC/"PremiumOnlineScreen.kt"
    s=screen.read_text(encoding="utf-8")
    s=once(s,"            ArenaHomeV2(","            ArenaHomeV3(","replace old home")
    # Remove duplicated player cards and old ready panel; new room lobby
    # owns the full layout and keeps the controls visible at 320-500dp widths.
    start=s.index('    if (match.phase == "lobby") ArenaRoomPlayersV2(')
    end=s.index('    if (match.phase == "lobby") ArenaV2Panel {',start)
    s=s[:start]+s[end:]
    first=s.index('    if (match.phase == "lobby") ArenaV2Panel {')
    next_when=s.index('    when (match.phase) {',first)
    s=s[:first]+s[next_when:]
    lobby_start=s.index('        "lobby" -> {',s.index('    when (match.phase) {'))
    playing_start=s.index('        "playing" -> {',lobby_start)
    new_lobby='''        "lobby" -> {
            ArenaRoomLobbyV3(
                match = match, client = client, isHost = isHost,
                myUid = myUid, hostName = hostName, guestName = guestName,
                packs = packs, profileKey = session.profileKey,
                onInvites = {
                    if (session.social.enabled)
                        MaterialTheme(colorScheme=lightColorScheme(
                            primary=OnlineGold, surface=Color.White,
                            onSurface=OnlineCream, onSurfaceVariant=OnlineSoft)) {
                            FriendsPanel(session.social, roomCode=match.code,
                                onJoin={sender,code ->
                                    client.join(code){session.social.dismissInvite(sender)}
                                },premium=true)
                        }
                    else Text("Arkadaş davetleri için hesabına giriş yap.",
                        color=OnlineSoft,fontSize=12.sp)
                }
            )
        }
'''
    s=s[:lobby_start]+new_lobby+s[playing_start:]
    assert 'ArenaRoomLobbyV3(' in s and 'ArenaHomeV3(' in s
    assert 'ArenaRoomPlayersV2(' not in s
    screen.write_text(s,encoding="utf-8")
    # Offline play shares the approved Arena entrance and keeps local match logic.
    solo=SRC/"PremiumArenaScreen.kt"
    offline=solo.read_text(encoding="utf-8")
    opening=offline.index('            PremiumStage.Lobby -> {')
    closing=offline.index('            PremiumStage.Crossword -> {',opening)
    block='''            PremiumStage.Lobby -> {
                ArenaHomeV3(
                    playerName=session.profileName,loggedIn=true,canCreate=true,
                    quotaLabel="Çevrimdışı oyun · İnternet gerekmez",
                    mode=if(selectedMode=="quiz")"quiz" else "duel",
                    onMode={selectedMode=if(it=="quiz")"quiz" else "crossword"},
                    level=level,onLevel={
                        level=it;topic=PremiumArenaRules.allTopics
                        size=packs.availableSizes(it).firstOrNull()?:20
                    },
                    topic=topic,topics=packs.topics(level,selectedMode=="crossword"),
                    onTopic={topic=it;size=packs.availableSizes(level,it).firstOrNull()?:20},
                    sizes=packs.availableSizes(level,topic),size=size,onSize={size=it},
                    seconds=timeSeconds,allowedTimes=PremiumArenaRules.allowedTimes,
                    onSeconds={timeSeconds=it},
                    roomCode="",onRoomCode={},onCreate=online,onJoin=online,
                    onAccount=back,onOffline=::begin,onFriends={},
                    soloInitially=true
                )
                if(feedback.isNotEmpty()) Text(feedback,color=ArenaGold)
            }
'''
    offline=offline[:opening]+block+offline[closing:]
    for old,new in (
        ("private val ArenaNight = Color(0xFF112720)","private val ArenaNight = Color(0xFFF8F6FE)"),
        ("private val ArenaPanel = Color(0xFF1C3A30)","private val ArenaPanel = Color(0xFFFFFFFF)"),
        ("private val ArenaPanelLight = Color(0xFF2A4B3E)","private val ArenaPanelLight = Color(0xFFF0E8FF)"),
        ("private val ArenaGold = Color(0xFFE7C582)","private val ArenaGold = Color(0xFF8051CC)"),
        ("private val ArenaCream = Color(0xFFF4ECD9)","private val ArenaCream = Color(0xFF293253)"),
        ("private val ArenaTextSoft = Color(0xFFC5D8CD)","private val ArenaTextSoft = Color(0xFF787391)"),
        ("private val ArenaSuccess = Color(0xFF91DDB3)","private val ArenaSuccess = Color(0xFF258A53)"),
        ("Color(0xFF385A4A), RoundedCornerShape(19.dp)","Color(0xFFE5D7FB), RoundedCornerShape(19.dp)"),
    ):
        offline=once(offline,old,new,"solo palette")
    solo.write_text(offline,encoding="utf-8")
    assert 'ArenaHomeV3(' in offline
    room=SRC/"PremiumOnlineRoom.kt"
    current=room.read_text(encoding="utf-8")
    assert 'fun updateLobbySettings(' in current
    assert 'configRevision' in current and 'readyBy' in current
    print("Arena V3 approved appearance and revision-aware lobby integrated.")

if __name__=="__main__":
    main()
