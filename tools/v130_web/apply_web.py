#!/usr/bin/env python3
from pathlib import Path
import hashlib,shutil
main=Path("source/app/src/main")
p=main/"java/com/kelimebulmacasi/a1"
pack=Path(__file__).resolve().parent
protected={f:hashlib.sha256(f.read_bytes()).digest() for f in main.rglob("*") if f.is_file()}
shutil.copyfile(pack/"Wq130WebLobby.kt",p/"Wq130WebLobby.kt")
out=main/"assets/web";out.mkdir(parents=True,exist_ok=True)
shutil.copyfile(pack/"arena_lobby.html",out/"arena_lobby.html")
assert all(hashlib.sha256(f.read_bytes()).digest()==v for f,v in protected.items())
assert "Wq130WebLobby(" in (p/"Wq128PremiumRoomLobby.kt").read_text()
assert "Wq130WebBundle.PATH" in (p/"WqContentUpdates.kt").read_text()
print("PASS: isolated WebView pilot and native fallback installed; all existing files preserved")
