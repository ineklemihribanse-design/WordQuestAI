#!/usr/bin/env python3
"""Apply privacy-limited friend profile cards and move account sync into header.

Runs after the v1.0.79 overlays. No user progress or Firebase account is reset.
"""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
TOOLS=ROOT/"tools/v180"
def once(s,old,new,where):
    n=s.count(old)
    if n!=1:raise AssertionError(f"{where} anchor count {n}: {old[:95]}")
    return s.replace(old,new,1)

friends=SRC/"FriendsHub.kt"
s=friends.read_text("utf-8")
s=once(s,
    'internal data class FriendEntry(val uid: String, val name: String)',
    '''internal data class FriendEntry(val uid:String,val name:String,
    val avatarId:String?=null,val points:Int?=null,
    val completedGames:Int?=null,val lastActiveAt:Long?=null)
internal data class Wq180FriendPublic(
    val avatarId:String,val points:Int,val completedGames:Int,val lastActiveAt:Long?
)''',
    "friend model with optional verified friend-only public stats")
s=once(s,
    '    private val nameObservers = mutableMapOf<String, Pair<DatabaseReference, ValueEventListener>>()',
    '''    private val nameObservers = mutableMapOf<String, Pair<DatabaseReference, ValueEventListener>>()
    private val publicProfiles=mutableMapOf<String,Wq180FriendPublic>()
    private val profileObservers=mutableMapOf<String,Pair<DatabaseReference,ValueEventListener>>()''',
    "friend-only listeners")
s=once(s,
    '        nameObservers.clear()\n        db = null',
    '''        nameObservers.clear()
        profileObservers.values.forEach { (ref,listener)->ref.removeEventListener(listener) }
        profileObservers.clear()
        publicProfiles.clear()
        db = null''',
    "stop friend profile observers on sign-out")
s=once(s,
    '        refreshNames()\n    }\n\n    private fun refreshNames() {',
    '''        syncFriendProfiles()
        refreshNames()
    }

    /** Only mutual friends can read this summary (see RTDB friendProfiles rules). */
    private fun syncFriendProfiles() {
        val root=db?.reference?:return
        val wanted=friendIds.toSet()
        profileObservers.keys.toList().filter{it !in wanted}.forEach{stale->
            profileObservers.remove(stale)?.let{(ref,listener)->ref.removeEventListener(listener)}
            publicProfiles.remove(stale)
        }
        wanted.filter{it !in profileObservers}.forEach{member->
            val ref=root.child("friendProfiles").child(member)
            val listener=object:ValueEventListener {
                override fun onDataChange(snapshot:DataSnapshot) {
                    if(!enabled || member !in friendIds)return
                    val avatar=snapshot.child("avatar").getValue(String::class.java)
                    val points=snapshot.child("points").getValue(Long::class.java)
                    val completed=snapshot.child("completed").getValue(Long::class.java)
                    val active=snapshot.child("lastActiveAt").getValue(Long::class.java)
                    if(Wq2DMascots.valid(avatar) && points!=null && completed!=null)
                        publicProfiles[member]=Wq180FriendPublic(
                            avatar!!,points.coerceIn(0L,1000000000L).toInt(),
                            completed.coerceIn(0L,1000000L).toInt(),active)
                    else publicProfiles.remove(member)
                    refreshNames()
                }
                override fun onCancelled(error:DatabaseError) {
                    publicProfiles.remove(member)
                    refreshNames()
                    if(error.code==DatabaseError.PERMISSION_DENIED)
                        status="Arkadaş profilleri için yeni Firebase kurallarının yayımlanması gerekiyor."
                }
            }
            profileObservers[member]=ref to listener
            ref.addValueEventListener(listener)
        }
    }

    private fun refreshNames() {''',
    "mutual-friend-only Firebase readers")
s=once(s,
    '        friends = friendIds.map { FriendEntry(it, currentNames[it] ?: "Oyuncu") }',
    '''        friends=friendIds.map{member->
            val profile=publicProfiles[member]
            FriendEntry(member,currentNames[member] ?: "Oyuncu",
                profile?.avatarId,profile?.points,profile?.completedGames,
                profile?.lastActiveAt)
        }''',
    "display only verified remote summaries for mutual friends")
s=once(s,
    '    fun searchName(input: String) {',
    '''    /** Opt-in profile summary: no email or detailed learning history is shared. */
    fun publishOverview(avatar:String,points:Int,completed:Int){
        val root=db?.reference?:return
        if(!enabled || nickname.isBlank() || !Wq2DMascots.valid(avatar))return
        root.child("friendProfiles").child(uid).setValue(mapOf(
            "avatar" to avatar,
            "points" to points.coerceIn(0,1000000000),
            "completed" to completed.coerceIn(0,1000000),
            "lastActiveAt" to ServerValue.TIMESTAMP
        )).addOnFailureListener{
            if(enabled) status="Arkadaş profili paylaşımı için Firebase Rules güncellenmeli."
        }
    }

    fun searchName(input: String) {''',
    "owner-only public profile publisher")
start=s.index('@Composable\ninternal fun FriendsPanel(')
assert start>=0 and 'Text("Arkadaşlar ·' in s[start:],"old friends panel changed"
s=s[:start]+'''@Composable
internal fun FriendsPanel(hub:FriendsHub,roomCode:String?=null,
    onJoin:((String,String)->Unit)?=null,premium:Boolean=false) {
    Wq180FriendsPanel(hub,roomCode,onJoin,premium)
}
'''
friends.write_text(s,"utf-8")
print("PASS: friends display card profiles, progress, last activity and image; request/invite actions unchanged")

home=SRC/"HomeScreen.kt"
s=home.read_text("utf-8")
s=once(s,
    '    LaunchedEffect(session.profileKey) { ai.bindProfile(appContext, session.profileKey) }',
    '''    LaunchedEffect(session.profileKey) { ai.bindProfile(appContext, session.profileKey) }
    // Only authenticated accounts publish minimal shared statistics.
    // Timestamp is server time; never infer that an absent heartbeat means "online".
    LaunchedEffect(session.accountName,session.profileKey) {
        while(session.accountName!=null && session.social.enabled) {
            session.social.publishOverview(
                Wq2DMascots.selected(appContext,session.profileKey),
                session.totalPoints,session.finishedCount)
            kotlinx.coroutines.delay(120000L)
        }
    }''',
    "publish minimal friend-visible statistics from active account")
s=once(s,
    '''                Text("Oyun maskotum · "+Wq2DMascots.label(chosenMaskot.value),
                    color=Color(0xFF655183),fontSize=12.sp)
                TextButton(onClick={choosingMaskot=true})''',
    '''                Text("Oyun maskotum · "+Wq2DMascots.label(chosenMaskot.value),
                    color=Color(0xFF655183),fontSize=12.sp)
                if(session.accountName!=null)
                    Text(session.accountStatus.ifBlank { "Bulut eşitlemesi bekleniyor" },
                        color=Color(0xFF4B785F),fontSize=11.sp)
                TextButton(onClick={choosingMaskot=true})''',
    "account sync text directly beside own mascot")
s=once(s,
    '            MenuItem(session.profileName, session.accountStatus.ifEmpty { "Kayıtlı hesap" }, "✓")\n',
    '',
    "remove redundant sync box below mascot")
s=once(s,
    '''            Wq2DMascots.choose(appContext,session.profileKey,id)
            chosenMaskot.value=id''',
    '''            Wq2DMascots.choose(appContext,session.profileKey,id)
            chosenMaskot.value=id
            if(session.accountName!=null)
                session.social.publishOverview(id,session.totalPoints,session.finishedCount)''',
    "publish freshly selected mascot without waiting for heartbeat")
home.write_text(s,"utf-8")
import shutil
shutil.copyfile(TOOLS/"Wq180FriendCards.kt",SRC/"Wq180FriendCards.kt")
print("PASS: account cloud status beside mascot; searching and friend invitations become cards")
