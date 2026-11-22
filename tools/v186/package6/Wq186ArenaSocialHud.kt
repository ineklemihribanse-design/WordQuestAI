package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay

/** Firebase events are room-scoped and each player can write ONLY their own events. */
@Composable
internal fun Wq186ArenaSocialHud(
    session: AppSession,
    match: PremiumOnlineMatch,
    myUid: String,
    isHost: Boolean,
    hostName: String,
    guestName: String,
    hostScore: Int,
    guestScore: Int
) {
    val database = session.accounts.socialDatabase()
    val room = remember(database, match.code) {
        database?.reference?.child("premiumRooms")?.child(match.code)
    }
    val extra = remember(room) { room?.child("package6") }
    var emoji by remember(match.code) { mutableStateOf(mapOf<String, Pair<String, Long>>()) }
    var votes by remember(match.code) { mutableStateOf(emptySet<String>()) }
    var presence by remember(match.code) { mutableStateOf(emptyMap<String, Boolean>()) }
    var disconnectedAt by remember(match.code) { mutableStateOf(emptyMap<String,Long>()) }
    var tick by remember(match.code) { mutableLongStateOf(System.currentTimeMillis()) }
    var emojiOpen by remember(match.code) { mutableStateOf(false) }
    var profileOpen by remember(match.code) { mutableStateOf<String?>(null) }
    var askFinish by remember(match.code) { mutableStateOf(false) }
    var notice by remember(match.code) { mutableStateOf("") }

    DisposableEffect(extra, database, match.code, myUid, match.phase) {
        if (extra == null || database == null || myUid.isBlank()) {
            onDispose { }
        } else {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    emoji = snapshot.child("emoji").children.mapNotNull { user ->
                        val id = user.key ?: return@mapNotNull null
                        val symbol = user.child("symbol").getValue(String::class.java) ?: return@mapNotNull null
                        val at = (user.child("sentAt").value as? Number)?.toLong() ?: 0L
                        if (Wq186ArenaFairPlay.validEmoji(symbol)) id to (symbol to at) else null
                    }.toMap()
                    votes = snapshot.child("finishVotes").children.mapNotNull { it.key
                        ?.takeIf { key -> it.getValue(Boolean::class.java) == true }
                    }.toSet()
                    disconnectedAt = snapshot.child("presence").children.mapNotNull { user ->
                        val id=user.key ?: return@mapNotNull null
                        val at=(user.child("at").value as? Number)?.toLong() ?: 0L
                        id to at
                    }.toMap()
                    presence = snapshot.child("presence").children.mapNotNull { user ->
                        val id = user.key ?: return@mapNotNull null
                        val online = user.child("connected").getValue(Boolean::class.java)
                        if (online == null) null else id to online
                    }.toMap()
                }
                override fun onCancelled(error: DatabaseError) {
                    notice = "Arena bağlantısı: " + error.message
                }
            }
            extra.addValueEventListener(listener)
            val connection = database.reference.child(".info/connected")
            val ownPresence = extra.child("presence").child(myUid)
            val connectionListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.getValue(Boolean::class.java) == true &&
                        match.phase in listOf("lobby", "playing")) {
                        ownPresence.onDisconnect().setValue(mapOf(
                            "connected" to false,
                            "at" to ServerValue.TIMESTAMP
                        ))
                        ownPresence.setValue(mapOf(
                            "connected" to true,
                            "at" to ServerValue.TIMESTAMP
                        ))
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    notice = "Bağlantı durumu alınamadı."
                }
            }
            connection.addValueEventListener(connectionListener)
            onDispose {
                extra.removeEventListener(listener)
                connection.removeEventListener(connectionListener)
            }
        }
    }

    LaunchedEffect(match.phase, votes, isHost, room, match.hostUid, match.guestUid) {
        if (isHost && room != null && match.phase == "playing" &&
            match.hostUid.isNotBlank() && match.guestUid.isNotBlank() &&
            votes.containsAll(listOf(match.hostUid, match.guestUid))) {
            // Both participants explicitly voted. Existing server rules restrict
            // the 'playing' -> 'finished' transition to the host.
            room.child("phase").setValue("finished").addOnFailureListener {
                notice = "Ortak bitirme sonucu kaydedilemedi."
            }
        }
    }

    val opponentUid = if (isHost) match.guestUid else match.hostUid
    val opponent = if (isHost) guestName else hostName
    val disconnected = opponentUid.isNotBlank() &&
        presence[opponentUid] == false
    LaunchedEffect(disconnected) {
        while(disconnected) {
            tick=System.currentTimeMillis()
            delay(1000)
        }
    }
    val offlineAt=disconnectedAt[opponentUid] ?: tick
    val waitingSeconds=((Wq186ArenaFairPlay.DISCONNECT_GRACE_MS -
        (tick-offlineAt).coerceAtLeast(0)).coerceAtLeast(0L)/1000L)
    val latest = emoji[opponentUid]
    val showEmoji = latest != null &&
        System.currentTimeMillis() - latest.second in 0L..8000L

    if (match.phase == "playing" || match.phase == "lobby") {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            Box {
                OutlinedButton(onClick = { emojiOpen = true },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal=9.dp,vertical=3.dp)) {
                    Text("😊  Emoji", fontSize=12.sp, color=Color(0xFFFBF3FF))
                }
                DropdownMenu(expanded=emojiOpen,onDismissRequest={emojiOpen=false}) {
                    Row(Modifier.horizontalScroll(rememberScrollState())
                        .padding(horizontal=6.dp,vertical=3.dp)) {
                        Wq186ArenaFairPlay.allowedEmojis.forEach { e ->
                            TextButton(onClick={
                                emojiOpen=false
                                if (myUid.isNotBlank() && match.phase=="playing") {
                                    extra?.child("emoji")?.child(myUid)?.setValue(
                                        mapOf("symbol" to e,"sentAt" to ServerValue.TIMESTAMP))
                                        ?.addOnFailureListener { notice="Emoji gönderilemedi." }
                                }
                            }) {Text(e,fontSize=20.sp)}
                        }
                    }
                }
            }
            OutlinedButton(onClick={profileOpen=opponentUid},
                shape=RoundedCornerShape(16.dp),
                contentPadding=PaddingValues(horizontal=8.dp,vertical=3.dp)) {
                Text("Oyuncu profili",fontSize=11.sp,color=Color(0xFFFBF3FF))
            }
            if (showEmoji) Text(latest!!.first,fontSize=25.sp)
            Spacer(Modifier.weight(1f))
            if (match.phase=="playing") TextButton(onClick={
                if(myUid in votes)extra?.child("finishVotes")?.child(myUid)?.removeValue()
                else askFinish=true
            }) {
                Text(if(myUid in votes)"Teklifi geri çek" else "Bitirme teklifi",
                    color=Color(0xFFFFDF9B),fontSize=11.sp)
            }
        }
        if (disconnected) Text(
            if(waitingSeconds>0)
                "$opponent çevrimdışı. Oda korunuyor · yeniden bağlanma için $waitingSeconds sn."
            else "$opponent henüz bağlanmadı. Maç açık; bağlantı gelince devam edebilir.",
            color=Color(0xFFFFDF9B),fontSize=12.sp)
        if (match.phase=="playing" && opponentUid in votes && myUid !in votes) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Rakibin birlikte bitirmeyi teklif etti.",
                    modifier=Modifier.weight(1f),color=Color(0xFFFFDF9B),
                    fontSize=12.sp)
                TextButton(onClick={
                    extra?.child("finishVotes")?.child(myUid)?.setValue(true)
                        ?.addOnFailureListener { notice="Teklif kabul edilemedi." }
                }) { Text("Kabul et",color=Color.White) }
            }
        }
        if (myUid in votes && opponentUid !in votes && match.phase=="playing")
            Text("Maçı bitirme teklifin karşı oyuncunun onayını bekliyor.",
                color=Color(0xFFE2D4FF),fontSize=11.sp)
        if (notice.isNotBlank()) Text(notice,color=Color(0xFFFFCCB4),fontSize=11.sp)
    }

    if (askFinish) AlertDialog(
        onDismissRequest={askFinish=false},
        title={Text("Maçı birlikte bitir") },
        text={Text("İki oyuncu kabul ederse maç, mevcut puanlarla tamamlanır. Tek taraflı çıkış olmaz.")},
        confirmButton={TextButton(onClick={
            askFinish=false
            if(myUid.isNotBlank() && myUid !in votes && match.phase=="playing")
                extra?.child("finishVotes")?.child(myUid)?.setValue(true)
                    ?.addOnFailureListener {notice="Bitirme teklifi gönderilemedi."}
        }) {Text("Teklifi gönder")}},
        dismissButton={TextButton(onClick={askFinish=false}) {Text("Devam et")}}
    )

    val viewedUid=profileOpen
    if (viewedUid!=null) {
        val host=viewedUid==match.hostUid
        val avatar=if(host)match.hostAvatar else match.guestAvatar
        val name=if(host)hostName else guestName
        val score=if(host)hostScore else guestScore
        AlertDialog(
            onDismissRequest={profileOpen=null},
            title={Text("Oyuncu profili")},
            text={
                Column(horizontalAlignment=Alignment.CenterHorizontally,
                    modifier=Modifier.fillMaxWidth()) {
                    Wq2DPortrait(avatar.orEmpty().ifBlank { "piko" },
                        Modifier.width(105.dp).height(160.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(name,fontWeight=FontWeight.Bold)
                    Text("★ $score puan")
                    Text(if(presence[viewedUid]==false)"Bağlantı kesildi" else "Arena oyuncusu")
                }
            },
            confirmButton={TextButton(onClick={profileOpen=null}){Text("Kapat")}}
        )
    }
}
