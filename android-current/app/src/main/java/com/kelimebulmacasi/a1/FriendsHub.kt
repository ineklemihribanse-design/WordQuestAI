package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.util.Locale

internal data class FriendEntry(val uid: String, val name: String)
internal data class FriendInvite(val uid: String, val name: String, val code: String, val arenaType: String = "legacy")

/** A one-time foreground alert for each Firebase request/invitation revision. */
internal enum class SocialAlertType { FRIEND_REQUEST, ROOM_INVITE }
internal data class SocialAlert(
    val type: SocialAlertType,
    val senderUid: String,
    val senderName: String,
    val timestamp: Long,
    val roomCode: String = "",
    val arenaType: String = "legacy",
) {
    val key: String get() = "${type.name}:$senderUid:$timestamp"
}
internal data class SocialJoinTarget(val senderUid: String, val roomCode: String, val arenaType: String = "legacy")

/** Account-scoped social features. No password or Google account e-mail is published. */
internal class FriendsHub(private val context: Context, private val accounts: Accounts) {
    var nickname by mutableStateOf("")
        private set
    var nameLoaded by mutableStateOf(false)
        private set
    var nameSaving by mutableStateOf(false)
        private set
    var friends by mutableStateOf<List<FriendEntry>>(emptyList())
        private set
    var requests by mutableStateOf<List<FriendEntry>>(emptyList())
        private set
    var invites by mutableStateOf<List<FriendInvite>>(emptyList())
        private set
    var found by mutableStateOf<List<FriendEntry>>(emptyList())
        private set
    var status by mutableStateOf("")
        private set

    private var db: FirebaseDatabase? = null
    private var uid = ""
    private val listening = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()
    private val currentNames = mutableMapOf<String, String>()
    private val nameObservers = mutableMapOf<String, Pair<DatabaseReference, ValueEventListener>>()
    private var friendIds = emptyList<String>()
    private var incomingRequests = emptyList<FriendEntry>()
    private var incomingInvites = emptyList<FriendInvite>()
    private var requestTimes = emptyMap<String, Long>()
    private var inviteTimes = emptyMap<String, Long>()
    private val acknowledgedAlerts = mutableSetOf<String>()
    var activeAlert by mutableStateOf<SocialAlert?>(null)
        private set
    var joinTarget by mutableStateOf<SocialJoinTarget?>(null)
        private set

    val enabled: Boolean get() = uid.isNotBlank() && db != null
    val currentUid: String get() = uid

    private fun watch(ref: DatabaseReference, onValue: (DataSnapshot) -> Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { if (enabled) onValue(snapshot) }
            override fun onCancelled(error: DatabaseError) {
                status = "Arkadaşlık verileri okunamadı: ${error.message}. Firebase kurallarını denetle."
            }
        }
        ref.addValueEventListener(listener)
        listening += ref to listener
    }

    fun connect(newUid: String) {
        if (newUid == uid && db != null) return
        disconnect()
        db = accounts.socialDatabase()
        uid = newUid.takeIf { it.isNotBlank() && db != null }.orEmpty()
        if (!enabled) return
        val root = db!!.reference
        val localName = context.getSharedPreferences("firebase_arena_nicknames", Context.MODE_PRIVATE)
            .getString(uid, "").orEmpty()
        if (ArenaNickname.isValid(localName)) nickname = ArenaNickname.normalize(localName)
        watch(root.child("users").child(uid).child("nickname")) { snap ->
            val name = snap.getValue(String::class.java).orEmpty()
            nickname = name.takeIf(ArenaNickname::isValid).orEmpty()
            nameLoaded = true
            if (nickname.isNotBlank()) {
                context.getSharedPreferences("firebase_arena_nicknames", Context.MODE_PRIVATE)
                    .edit().putString(uid, nickname).apply()
                publishDirectory(nickname)
            }
        }
        watch(root.child("friendRequests").child(uid)) { snap ->
            requestTimes = snap.children.mapNotNull { child ->
                child.key?.let { it to (child.child("createdAt").getValue(Long::class.java) ?: 0L) }
            }.toMap()
            incomingRequests = snap.children.mapNotNull { child ->
                val sender = child.key ?: return@mapNotNull null
                val name = child.child("senderName").getValue(String::class.java).orEmpty()
                FriendEntry(sender, ArenaNickname.safeDisplay(name, "Oyuncu"))
            }
            syncNameObservers()
        }
        watch(root.child("friends").child(uid)) { snap ->
            friendIds = snap.children.mapNotNull { it.key }.take(300)
            syncNameObservers()
        }
        watch(root.child("roomInvites").child(uid)) { snap ->
            inviteTimes = snap.children.mapNotNull { child ->
                child.key?.let { it to (child.child("sentAt").getValue(Long::class.java) ?: 0L) }
            }.toMap()
            incomingInvites = snap.children.mapNotNull { child ->
                val sender = child.key ?: return@mapNotNull null
                val code = child.child("code").getValue(String::class.java).orEmpty()
                val name = child.child("senderName").getValue(String::class.java).orEmpty()
                if (!RoomInvite.isValid(code)) null else FriendInvite(
                    sender, ArenaNickname.safeDisplay(name, "Arkadaş"), code,
                    if (child.child("arenaType").getValue(String::class.java) == "premium") "premium" else "legacy"
                )
            }
            syncNameObservers()
        }
    }

    fun disconnect() {
        listening.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        listening.clear()
        nameObservers.values.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        nameObservers.clear()
        db = null
        uid = ""
        nickname = ""
        nameLoaded = false
        nameSaving = false
        friendIds = emptyList()
        currentNames.clear()
        incomingRequests = emptyList()
        incomingInvites = emptyList()
        requestTimes = emptyMap()
        inviteTimes = emptyMap()
        acknowledgedAlerts.clear()
        activeAlert = null
        joinTarget = null
        friends = emptyList()
        requests = emptyList()
        invites = emptyList()
        found = emptyList()
        status = ""
    }

    /** Live directory listeners keep old friendships and pending invitations in sync after a rename. */
    private fun syncNameObservers() {
        val root = db?.reference ?: return
        val wanted = (friendIds + incomingRequests.map { it.uid } + incomingInvites.map { it.uid }).toSet()
        nameObservers.keys.toList().filter { it !in wanted }.forEach { stale ->
            nameObservers.remove(stale)?.let { (ref, listener) -> ref.removeEventListener(listener) }
            currentNames.remove(stale)
        }
        wanted.filter { it !in nameObservers }.forEach { member ->
            val ref = root.child("directory").child(member).child("nickname")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.getValue(String::class.java).orEmpty()
                    if (ArenaNickname.isValid(name)) currentNames[member] = ArenaNickname.normalize(name)
                    else currentNames.remove(member)
                    refreshNames()
                }
                override fun onCancelled(error: DatabaseError) {
                    status = "Arkadaş kullanıcı adları okunamadı: ${error.message}"
                }
            }
            nameObservers[member] = ref to listener
            ref.addValueEventListener(listener)
        }
        refreshNames()
    }

    private fun refreshNames() {
        friends = friendIds.map { FriendEntry(it, currentNames[it] ?: "Oyuncu") }
        requests = incomingRequests.map { it.copy(name = currentNames[it.uid] ?: it.name) }
        invites = incomingInvites.map { it.copy(name = currentNames[it.uid] ?: it.name) }
        refreshAlerts()
    }

    /** Live listeners run for the whole authenticated app, not just the Friends panel. */
    private fun refreshAlerts() {
        if (!enabled) { activeAlert = null; return }
        val current = requests.map { request ->
            SocialAlert(SocialAlertType.FRIEND_REQUEST, request.uid, request.name,
                requestTimes[request.uid] ?: 0L)
        } + invites.map { invite ->
            SocialAlert(SocialAlertType.ROOM_INVITE, invite.uid, invite.name,
                inviteTimes[invite.uid] ?: 0L, invite.code, invite.arenaType)
        }
        val keys = current.map { it.key }.toSet()
        // Removed/accepted items must disappear rather than block subsequent requests.
        if (activeAlert?.key !in keys) activeAlert = null
        val ongoing = activeAlert
        if (ongoing != null) {
            val refreshed = current.firstOrNull { it.key == ongoing.key }
            if (refreshed != null && refreshed != ongoing) activeAlert = refreshed
            return
        }
        activeAlert = current.asSequence()
            .filter { it.key !in acknowledgedAlerts }
            .sortedByDescending { it.timestamp }
            .firstOrNull()
    }

    fun closeAlert() {
        activeAlert?.let { acknowledgedAlerts += it.key }
        activeAlert = null
        refreshAlerts()
    }

    /** The room screen takes care of connecting and joining, including error reporting. */
    fun acceptRoomAlert() {
        val alert = activeAlert?.takeIf { it.type == SocialAlertType.ROOM_INVITE } ?: return
        joinTarget = SocialJoinTarget(alert.senderUid, alert.roomCode, alert.arenaType)
        closeAlert()
    }

    fun clearJoinTarget() { joinTarget = null }

    fun acceptFriendAlert() {
        val alert = activeAlert?.takeIf { it.type == SocialAlertType.FRIEND_REQUEST } ?: return
        acceptFriend(alert.senderUid)
        closeAlert()
    }

    fun declineAlert() {
        val alert = activeAlert ?: return
        when (alert.type) {
            SocialAlertType.FRIEND_REQUEST -> rejectFriend(alert.senderUid)
            SocialAlertType.ROOM_INVITE -> dismissInvite(alert.senderUid)
        }
        closeAlert()
    }

    private fun key(name: String) = ArenaNickname.normalize(name).lowercase(Locale.ROOT)

    private fun publishDirectory(name: String) {
        val currentUid = uid
        val root = db?.reference ?: return
        if (!ArenaNickname.isValid(name) || currentUid.isBlank()) return
        // The public directory contains only username and a normalized lookup value, never e-mail.
        root.child("directory").child(currentUid).setValue(mapOf(
            "nickname" to name, "search" to key(name)
        )).addOnFailureListener {
            if (uid == currentUid) status = "Kullanıcı dizini güncellenemedi. Firebase Rules ayarını kontrol et."
        }
    }

    fun saveName(input: String) {
        val name = ArenaNickname.normalize(input)
        val currentUid = uid
        val root = db?.reference ?: return
        if (!ArenaNickname.isValid(input)) {
            status = "Kullanıcı adı 2-24 karakter olmalı; harf, rakam, boşluk, nokta, _ ve - kullanılabilir."
            return
        }
        if (nameSaving || currentUid.isBlank()) return
        nameSaving = true
        root.updateChildren(mapOf(
            "/users/$currentUid/nickname" to name,
            "/directory/$currentUid" to mapOf("nickname" to name, "search" to key(name)),
        )).addOnCompleteListener { result ->
            if (uid != currentUid) return@addOnCompleteListener
            nameSaving = false
            status = if (result.isSuccessful) "Kullanıcı adın kaydedildi ve bütün bölümlerde kullanılacak."
                else "Kullanıcı adı kaydedilemedi. Veritabanı kurallarını kontrol et."
        }
    }

    fun searchName(input: String) {
        found = emptyList()
        val root = db?.reference ?: return
        val query = key(input)
        if (!ArenaNickname.isValid(input)) {
            status = "Arkadaş aramak için 2-24 karakterli tam kullanıcı adını gir."
            return
        }
        root.child("directory").orderByChild("search").equalTo(query).limitToFirst(20).get()
            .addOnSuccessListener { result ->
                found = result.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    val name = child.child("nickname").getValue(String::class.java).orEmpty()
                    if (id == uid || !ArenaNickname.isValid(name)) null else FriendEntry(id, name)
                }
                status = if (found.isEmpty()) "Bu kullanıcı adına sahip oyuncu bulunamadı." else "Oyuncular bulundu."
            }.addOnFailureListener { status = "Oyuncu aranamadı; Firebase Rules ayarını kontrol et." }
    }

    fun requestFriend(targetUid: String) {
        if (!enabled || targetUid == uid || targetUid.isBlank() || nickname.isBlank()) return
        if (friends.any { it.uid == targetUid }) { status = "Bu oyuncu zaten arkadaşın."; return }
        db!!.reference.updateChildren(mapOf(
            "/friendRequests/$targetUid/$uid" to mapOf(
                "senderName" to nickname, "createdAt" to ServerValue.TIMESTAMP
            ),
            "/friendOutbox/$uid/$targetUid" to true,
        )).addOnCompleteListener {
            status = if (it.isSuccessful) "Arkadaşlık isteği gönderildi." else "Arkadaşlık isteği gönderilemedi."
        }
    }

    fun acceptFriend(senderUid: String) {
        if (!enabled || requests.none { it.uid == senderUid }) return
        val currentUid = uid
        db!!.reference.updateChildren(mapOf(
            "/friends/$currentUid/$senderUid" to true,
            "/friends/$senderUid/$currentUid" to true,
            "/friendRequests/$currentUid/$senderUid" to null,
            "/friendOutbox/$senderUid/$currentUid" to null,
        )).addOnCompleteListener { result ->
            if (result.isSuccessful) {
                status = "Arkadaşlık isteği kabul edildi."
            } else status = "Arkadaşlık kabul edilemedi. Firebase Rules ayarını kontrol et."
        }
    }

    fun rejectFriend(senderUid: String) {
        if (!enabled || requests.none { it.uid == senderUid }) return
        db!!.reference.updateChildren(mapOf(
            "/friendRequests/$uid/$senderUid" to null,
            "/friendOutbox/$senderUid/$uid" to null,
        ))
    }

    fun removeFriend(friendUid: String) {
        if (!enabled || friends.none { it.uid == friendUid }) return
        db!!.reference.updateChildren(mapOf(
            "/friends/$uid/$friendUid" to null,
            "/friends/$friendUid/$uid" to null,
        )).addOnCompleteListener {
            status = if (it.isSuccessful) "Arkadaşlıktan çıkarıldı." else "Arkadaş kaldırılamadı."
        }
    }

    fun inviteFriend(friendUid: String, roomCode: String, arenaType: String = "legacy") {
        val code = RoomInvite.normalizedCode(roomCode)
        if (!enabled || !RoomInvite.isValid(code) || nickname.isBlank() ||
            friends.none { it.uid == friendUid }) return
        db!!.reference.child("roomInvites").child(friendUid).child(uid).setValue(mapOf(
            "code" to code, "senderName" to nickname, "sentAt" to ServerValue.TIMESTAMP,
            "arenaType" to if (arenaType == "premium") "premium" else "legacy"
        )).addOnCompleteListener {
            status = if (it.isSuccessful) "Oda daveti gönderildi." else "Oda daveti gönderilemedi."
        }
    }

    fun dismissInvite(senderUid: String) {
        if (!enabled) return
        db!!.reference.child("roomInvites").child(uid).child(senderUid).removeValue()
    }
}

@Composable
internal fun FriendsPanel(hub: FriendsHub, roomCode: String? = null,
    onJoin: ((String, String) -> Unit)? = null, premium: Boolean = false) {
    if (!hub.enabled) return
    var lookup by remember { mutableStateOf("") }
    val foreground = if (premium) Color.White else Color.Unspecified
    val action = if (premium) Color(0xFFE7C582) else Color.Unspecified
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Arkadaşlar · ${hub.friends.size}", color = foreground)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(lookup, { lookup = it.take(ArenaNickname.maximumLength) },
                modifier = Modifier.weight(1f), label = { Text("Kullanıcı adıyla ara", color = foreground) },
                singleLine = true, colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = foreground, unfocusedTextColor = foreground,
                    focusedBorderColor = action, unfocusedBorderColor = foreground,
                    cursorColor = action))
            TextButton(onClick = { hub.searchName(lookup) }, enabled = hub.nickname.isNotBlank()) { Text("Ara", color = action) }
        }
        hub.found.forEach { player ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(player.name, Modifier.weight(1f), color = foreground)
                TextButton(onClick = { hub.requestFriend(player.uid) }) { Text("İstek gönder", color = action) }
            }
        }
        if (hub.requests.isNotEmpty()) Text("Gelen arkadaşlık istekleri", color = foreground)
        hub.requests.forEach { player ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(player.name, Modifier.weight(1f), color = foreground)
                TextButton(onClick = { hub.acceptFriend(player.uid) }) { Text("Kabul et", color = action) }
                TextButton(onClick = { hub.rejectFriend(player.uid) }) { Text("Reddet", color = action) }
            }
        }
        hub.friends.forEach { friend ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(friend.name, Modifier.weight(1f), color = foreground)
                if (roomCode != null && RoomInvite.isValid(roomCode)) {
                    TextButton(onClick = { hub.inviteFriend(friend.uid, roomCode, if (premium) "premium" else "legacy") }) { Text("Odaya çağır", color = action) }
                }
                TextButton(onClick = { hub.removeFriend(friend.uid) }) { Text("Çıkar", color = action) }
            }
        }
        if (hub.invites.isNotEmpty()) Text("Gelen oda davetleri", color = foreground)
        hub.invites.forEach { invite ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${invite.name} seni Arena'ya davet ediyor", Modifier.weight(1f), color = foreground)
                if (onJoin != null) TextButton(onClick = {
                    onJoin(invite.uid, invite.code)
                }) { Text("Katıl", color = action) }
                TextButton(onClick = { hub.dismissInvite(invite.uid) }) { Text("Sil", color = action) }
            }
        }
        if (hub.status.isNotBlank()) Text(hub.status, color = foreground)
    }
}
