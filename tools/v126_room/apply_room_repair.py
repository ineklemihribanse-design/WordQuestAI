#!/usr/bin/env python3
"""Preserve v125 networking, fix stale quota denominator and surface room creation errors."""
from pathlib import Path
import re, hashlib
p=Path('source/app/src/main/java/com/kelimebulmacasi/a1')
board=p/'PuzzleScreen.kt'
before=hashlib.sha256(board.read_bytes()).digest()
changed=0
for f in p.glob('*.kt'):
    if f==board:continue
    s=f.read_text()
    out=[]
    for line in s.splitlines(keepends=True):
        if ('quotaLabel' in line or 'oda hakk' in line.lower() or 'client.usedToday' in line) and re.search(r'/\s*10\b',line):
            line,n=re.subn(r'/(\s*)10\b',r'/\1${SparkArenaLimits.roomsPerAccountPerUtcDay}',line)
            changed+=n
        out.append(line)
    t=''.join(out)
    if t!=s:f.write_text(t)
assert changed>0,'Expected stale quota denominator not found'
room=p/'PremiumOnlineRoom.kt'
s=room.read_text()
a='if (!canCreate) { status = "Oda oluşturmak için hesabınla giriş yap ve kullanıcı adını kaydet."; return }'
assert s.count(a)==1
s=s.replace(a,'''if (!canCreate) {
            status = when {
                busy -> "Oda işlemi devam ediyor."
                !ready || session.accounts.isGuest -> "Oda oluşturmak için hesabınla giriş yap ve kullanıcı adını kaydet."
                !quotaLoaded -> "Oda hakları henüz yüklenmedi. İnternet ve Firebase kurallarını kontrol et."
                usedToday >= SparkArenaLimits.roomsPerAccountPerUtcDay -> "Günlük 20 oda hakkın doldu."
                else -> "Günlük oda hakkı yenileniyor. Tekrar dene."
            }
            return
        }''')
room.write_text(s)
f=p/'PremiumOnlineScreen.kt'
s=f.read_text()
a='    val match = client.match'
assert s.count(a)==1
s=s.replace(a,a+'''
    // Keep network status visible even if the settings layout hides the old label.
    val wq126StatusContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(client.status) {
        if (client.status.isNotBlank()) {
            android.widget.Toast.makeText(wq126StatusContext, client.status, android.widget.Toast.LENGTH_LONG).show()
        }
    }
''')
f.write_text(s)
assert hashlib.sha256(board.read_bytes()).digest()==before
print("PASS: quota denominator uses central limit; room error visible; board untouched")
