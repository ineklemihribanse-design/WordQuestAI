#!/usr/bin/env python3
"""v125: use a fresh server-side quota namespace, 20 successful atomic rooms per day."""
from pathlib import Path
import re, hashlib
p=Path('source/app/src/main/java/com/kelimebulmacasi/a1')
board=p/'PuzzleScreen.kt'
digest=hashlib.sha256(board.read_bytes()).digest()
def one(s,a,b):
    assert s.count(a)==1,(a,s.count(a))
    return s.replace(a,b,1)
f=p/'FirebaseLiveArena.kt';s=f.read_text()
s=one(s,'const val roomsPerAccountPerUtcDay = 10','const val roomsPerAccountPerUtcDay = 20')
s=s.replace('child("roomQuota")','child("roomQuotaV2")').replace('günlük 10 ', 'günlük 20 ')
s,n=re.subn(r'else reserveQuotaSlot\(quota,\s*free,\s*0,\s*code\)\s*\{\s*slot\s*->','else run { val slot = free.firstOrNull()',s)
assert n==1,n
s=one(s,'db.reference.child("rooms").child(code).setValue(initial)', 'db.reference.updateChildren(mapOf<String, Any>("roomQuotaV2/$uid/$dayIndex/$slot" to code, "rooms/$code" to initial))')
f.write_text(s)
f=p/'PremiumOnlineRoom.kt';s=f.read_text()
s=s.replace('child("roomQuota")','child("roomQuotaV2")')
s=one(s,'usedToday < 10','usedToday < SparkArenaLimits.roomsPerAccountPerUtcDay')
s=one(s,'coerceAtMost(10)','coerceAtMost(SparkArenaLimits.roomsPerAccountPerUtcDay)')
s=one(s,'(0..9).map(Int::toString)','(0 until SparkArenaLimits.roomsPerAccountPerUtcDay).map(Int::toString)')
s,n=re.subn(r'reserve\(quota,\s*free,\s*0,\s*code\)\s*\{\s*slot\s*->','run quotaAttempt@ { val slot = free.firstOrNull()',s)
assert n==1,n
s=s.replace('return@reserve','return@quotaAttempt')
s=one(s,'database.reference.child("premiumRooms").child(code).setValue(initial)', 'database.reference.updateChildren(mapOf<String, Any>("roomQuotaV2/$myUid/$day/$slot" to code, "premiumRooms/$code" to initial))')
f.write_text(s)
# Keep the UI and existing tests in sync; no unrelated literal 10 changes.
changed=0
for f in p.glob('*.kt'):
    if f==board:continue
    s=f.read_text()
    t=re.sub(r'\b10\s*-\s*client\.usedToday\b','SparkArenaLimits.roomsPerAccountPerUtcDay - client.usedToday',s)
    t=re.sub(r'(\$[{]SparkArenaLimits\.roomsPerAccountPerUtcDay - client\.usedToday[}]\s*/\s*)10',r'\g<1>20',t)
    t=re.sub(r'(client\.usedToday\s*<\s*)10\b',r'\g<1>SparkArenaLimits.roomsPerAccountPerUtcDay',t)
    t=t.replace('günlük 10 oda hakkı','günlük 20 oda hakkı')
    if t!=s:f.write_text(t);changed+=1
assert changed, 'No quota label matched; inspect current source'
for f in Path('source/app/src/test').rglob('*.kt'):
    s=f.read_text(); t=s.replace('assertEquals(10, SparkArenaLimits.roomsPerAccountPerUtcDay)','assertEquals(20, SparkArenaLimits.roomsPerAccountPerUtcDay)')
    if t!=s:f.write_text(t)
assert hashlib.sha256(board.read_bytes()).digest()==digest
print('PASS: 20 slots, one-time namespace transition, atomic room creation, protected phone board')
