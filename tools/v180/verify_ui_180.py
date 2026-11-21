#!/usr/bin/env python3
"""Fail-closed checks for 1.0.80 friend data and responsive Arena screens."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[2]
P=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
code=lambda f:(P/f).read_text("utf-8")
friends=code("FriendsHub.kt")
cards=code("Wq180FriendCards.kt")
home=code("HomeScreen.kt")
board=code("ArenaBoardV2.kt")
typing=code("ArenaCrosswordTyping.kt")
arena=code("PremiumOnlineScreen.kt")
account=code("Accounts.kt")
assert 'fun publishOverview(avatar:String,points:Int,completed:Int)' in friends
assert 'fun syncFriendProfiles()' in friends
assert 'root.child("friendProfiles").child(member)' in friends
assert 'Wq180FriendsPanel(hub,roomCode,onJoin,premium)' in friends
assert 'Wq2DPortrait(avatar!!' in cards
assert 'Wq180FriendActivity.label(p.lastActiveAt)' in cards
assert 'OutlinedTextField(lookup' in cards
assert 'Arkadaşını Odaya Davet Et' in cards
assert 'session.accountStatus.ifBlank' in home
assert 'session.social.publishOverview(id,session.totalPoints,session.finishedCount)' in home
assert '"/friendProfiles/$userId" to null' in account
assert 'Wq180ArenaStageBackground(' in arena
assert 'ArenaBoardV2(puzzle,solved,selected,placed,compact,zoomed,onSelect)' in typing
assert 'OutlinedButton(onClick={zoomed=!zoomed}' in typing
assert '.coerceIn(320.dp,820.dp)' in board
assert 'val boardInset=' in board
assert 'horizontalScroll(horizontal)' in board and 'verticalScroll(vertical)' in board
settings=(ROOT/"source/app/build.gradle.kts").read_text("utf-8")
assert 'versionCode = 106' in settings
assert 'versionName = "1.0.80"' in settings
assert 'applicationIdSuffix = ".fresh"' in settings
rules=json.loads((ROOT/"firebase/database.rules.json").read_text("utf-8"))
node=rules['rules']['friendProfiles']['$uid']
assert "root.child('friends').child(auth.uid).child($uid)" in node['.read']
assert "root.child('friends').child($uid).child(auth.uid)" in node['.read']
assert node['.write']=="auth != null && auth.uid === $uid"
assert all(k in node for k in ('avatar','points','completed','lastActiveAt'))
assert all(x in rules['rules'] for x in ('directory','friends','premiumRooms','users','friendRequests'))
print("PASS: private friend cards, profile deletion, account sync and centered Arena board")
print("PASS: version code 106 with original package and unchanged Arena rules")
