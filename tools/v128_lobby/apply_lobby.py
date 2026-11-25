#!/usr/bin/env python3
from pathlib import Path
import hashlib,shutil
p=Path("source/app/src/main/java/com/kelimebulmacasi/a1")
main=Path("source/app/src/main")
pack=Path(__file__).resolve().parent
protected={f:hashlib.sha256(f.read_bytes()).digest() for f in main.rglob("*") if f.is_file() and f.name not in ("PremiumOnlineScreen.kt","Wq183ArenaShare.kt")}
def once(s,a,b):
    assert s.count(a)==1,(a[:100],s.count(a))
    return s.replace(a,b,1)
f=p/"PremiumOnlineScreen.kt";s=f.read_text()
s=once(s,"            ArenaApprovedRoomLobby(","            Wq128PremiumRoomLobby(")
a=s.index("    Wq186ArenaSocialHud(")
b=s.index("    when (match.phase) {",a)
assert "val gameTitle" in s[:a] and "if (match.cooperative)" in s[a:b]
# Keep every existing gameplay control and score, only stop rendering them above the room.
s=s[:a]+"    if (match.phase != \"lobby\") {\n"+s[a:b]+"    }\n"+s[b:]
f.write_text(s)
f=p/"Wq183ArenaShare.kt";s=f.read_text()
s=once(s,"if (Wq183MascotStore.isCustomId(id)) remote[id] else null","if (Wq183MascotStore.isCustomId(id)) Wq128RoomHd.remoteImage(id) ?: remote[id] else null")
a="    // A player's maskot can change while a match is active."
s=once(s,a,"    Wq128RoomHdBridge(room,side,myId,rivalId,selected,allowed,connected.value)\n\n"+a)
f.write_text(s)
for name in ("Wq128PremiumRoomLobby.kt","Wq128RoomHd.kt"):
    shutil.copyfile(pack/name,p/name)
assert all(hashlib.sha256(f.read_bytes()).digest()==v for f,v in protected.items())
print("PASS: responsive native lobby, independent HD consented preview, exact artwork and puzzle source preserved")
