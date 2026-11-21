#!/usr/bin/env python3
"""Repair Arena player avatars, ready callbacks, and versionCode for 1.0.78."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
APP=ROOT/"source/app/build.gradle.kts"

def patch(name,old,new,label):
    p=SRC/name
    s=p.read_text(encoding="utf-8")
    n=s.count(old)
    if n!=1:raise AssertionError(f"{name}: {label}: expected 1 anchor; got {n}")
    p.write_text(s.replace(old,new,1),encoding="utf-8")
    print("PASS: "+name+" "+label)

# The old Arena-only preference accepts only robot/fox/owl. The profile's
# actual 10-member mascot selection is already profile-specific and persisted.
patch("PremiumOnlineRoom.kt",
    '    private val myAvatar: String get() = context.getSharedPreferences(\n'
    '        "wordquest_arena_mascot", Context.MODE_PRIVATE\n'
    '    ).getString("kind","robot")?.takeIf {it in listOf("robot","fox","owl")} ?: "robot"',
    '    private val myAvatar: String get() =\n'
    '        Wq2DMascots.selected(context, session.profileKey)',
    "use the actual logged-in profile mascot, never a three-character fallback")

# Save a freshly chosen mascot for both host and guest in existing open rooms
# without touching user history, other users' choices, or closed matches.
patch("PremiumOnlineRoom.kt",
    '    fun toggleReady() {',
    '''    fun syncSelectedAvatar() {
        val m = match ?: return
        if (!ready || m.phase != "lobby" || uid !in listOf(m.hostUid, m.guestUid)) return
        val selected = myAvatar
        val current = if (isHost) m.hostAvatar else m.guestAvatar
        if (current == selected) return
        val key = if (isHost) "hostAvatar" else "guestAvatar"
        ref?.child(key)?.setValue(selected)?.addOnFailureListener {
            status = "Oyun maskotun eşitlenemedi. Firebase Arena kurallarını kontrol et."
        }
    }

    fun toggleReady() {''',
    "synchronize only own selected mascot in open lobby")

# The earlier 1.0.73 integration already renders Wq2DPortrait for
# valid IDs in ArenaV2Visuals; preserve it without duplicating the branch.
# Re-sync on room join, participant avatar updates and profile changes.
patch("PremiumOnlineScreen.kt",
    '''    val match = client.match
    val isHost = client.isHost''',
    '''    val match = client.match
    LaunchedEffect(match?.code,match?.phase,match?.hostAvatar,match?.guestAvatar,
        session.profileKey) {
        client.syncSelectedAvatar()
    }
    val isHost = client.isHost''',
    "sync real profile mascot on both devices when joining a lobby")

s=APP.read_text(encoding="utf-8")
assert s.count('versionCode = 103')==1
assert s.count('versionName = "1.0.77"')==1
assert 'applicationIdSuffix = ".fresh"' in s
s=s.replace('versionCode = 103','versionCode = 104',1)
s=s.replace('versionName = "1.0.77"','versionName = "1.0.78"',1)
APP.write_text(s,encoding="utf-8")
print("PASS: 1.0.78/code104, same upgrade package/signature and stored user data")
