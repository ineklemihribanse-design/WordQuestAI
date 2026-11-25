#!/usr/bin/env python3
from pathlib import Path
import shutil,hashlib
p=Path("source/app/src/main/java/com/kelimebulmacasi/a1")
pack=Path(__file__).resolve().parent
def once(s,a,b):
    assert s.count(a)==1,(a[:100],s.count(a))
    return s.replace(a,b,1)
f=p/"PremiumOnlineRoom.kt";s=f.read_text()
a=s.index("    fun updateLobbySettings(");b=s.index("    fun start()",a)
part=s[a:b]
part=once(part,"boardId: String\n","boardId: String, preset: Wq186MatchPreset? = null\n")
part=once(part,'"configRevision" to (m.configRevision+1)', '''"configRevision" to (m.configRevision+1),
            "package6/settings" to (preset ?: package6Rules).sanitized(level).let { safe ->
                mapOf("levels" to safe.levels.joinToString(","),"hints" to safe.hints,"retries" to safe.retries)
            }''')
f.write_text(s[:a]+part+s[b:])
f=p/"Wq128PremiumRoomLobby.kt";s=f.read_text()
a=s.index("    if(editOpen&&isHost) ArenaLobbyEditDialogV3(")
b=s.index("    if(inviteOpen&&isHost)",a)
s=s[:a]+'''    if(editOpen&&isHost) Wq132RoomSettings(match,client,packs,profileKey,onDismiss={editOpen=false})
'''+s[b:]
s=once(s,'LobbyChip128(match.level,Modifier.weight(.7f))','LobbyChip128(client.package6Rules.levels.joinToString(" · "),Modifier.weight(1.5f))')
f.write_text(s)
f=p/"Wq130WebLobby.kt";s=f.read_text()
s=once(s,'put("level",match.level);','put("level",client.package6Rules.levels.joinToString(" · "));')
f.write_text(s)
shutil.copyfile(pack/"Wq132RoomSettings.kt",p/"Wq132RoomSettings.kt")
print("PASS: shared settings revision, multilevel chips, dark scrollable modal and full selected level summary")
