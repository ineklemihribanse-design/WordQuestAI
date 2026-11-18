package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.security.SecureRandom

/** RTDB is the source of truth for lobby, board identity, solved cells and each player's answers.
 * The client validates vocabulary locally; competition scoring is NOT server-authoritative.
 */
internal data class PremiumOnlineMatch(
    val code: String,
    val hostUid: String,
    val guestUid: String,
    val hostName: String,
    val guestName: String,
    val phase: String,
    val mode: String,
    val boardId: String,
    val level: String,
    val topic: String,
    val size: Int,
    val seconds: Int,
    val quizSeed: Int,
    val turnUid: String,
    val turnUntil: Long,
    val solvedBy: Map<Int, String>,
    val answers: Map<String, Map<Int, Int>>,
    // Each word can be explicitly unknown to each player. Two independent "Bilmiyorum"
    // choices reveal the answer without awarding points.
    val unknownBy: Map<Int, Set<String>> = emptyMap(),
    val mistakesBy: Map<Int, Map<String, Int>> = emptyMap(),
) {
    val complete: Boolean get() = if (mode == "quiz") {
        hostUid.isNotBlank() && guestUid.isNotBlank() &&
            (answers[hostUid]?.size ?: 0) >= 12 && (answers[guestUid]?.size ?: 0) >= 12
    } else solvedBy.size >= size
    val cooperative: Boolean get() = mode == "team"
    val isCrossword: Boolean get() = mode != "quiz"
    fun score(uid: String, questions: List<PremiumQuestion>): Int = if (mode == "quiz") {
        val own = answers[uid] ?: emptyMap()
        own.count { (index, choice) -> questions.getOrNull(index)?.correct == choice } * 10 -
            own.count { (index, choice) -> choice >= 0 && questions.getOrNull(index)?.correct != choice } * 3
    } else solvedBy.count { it.value == uid } * 10 -
        mistakesBy.values.sumOf { wordMistakes -> wordMistakes[uid] ?: 0 } * 3
}

internal object PremiumOnlineLimits {
    val modes = setOf("duel", "team", "quiz")
    fun validConfig(level: String, size: Int, seconds: Int, mode: String): Boolean =
        level in PremiumArenaRules.allowedLevels &&
            seconds in PremiumArenaRules.allowedTimes && mode in modes &&
            (if (mode == "quiz") size == 12 else size in PremiumArenaRules.allowedSizes)
    fun otherPlayer(room: PremiumOnlineMatch, uid: String): String =
        if (room.hostUid == uid) room.guestUid else room.hostUid
    fun firstTurn(host: String, guest: String, chooseHost: Boolean): String =
        if (chooseHost) host else guest
    fun remainingSeconds(deadline: Long, serverNow: Long): Int =
        ((deadline - serverNow + 999L) / 1000L).coerceAtLeast(0).toInt()
    fun timedOut(deadline: Long, serverNow: Long): Boolean = deadline > 0L && serverNow >= deadline
    /** A wrong answer is also a failed attempt at this word. It costs points,
     * while the Bilmiyorum option does not. When both players have failed in
     * either way, the answer is revealed instead of returning to a blocked turn.
     */
    fun gaveUpOrFailed(match: PremiumOnlineMatch, index: Int, player: String): Boolean =
        player.isNotBlank() && (player in (match.unknownBy[index] ?: emptySet()) ||
            (match.mistakesBy[index]?.get(player) ?: 0) > 0)

    fun shouldReveal(match: PremiumOnlineMatch, index: Int): Boolean =
        match.isCrossword && match.phase == "playing" && index in 0 until match.size &&
            index !in match.solvedBy && match.hostUid.isNotBlank() &&
            match.guestUid.isNotBlank() && match.hostUid != match.guestUid &&
            gaveUpOrFailed(match, index, match.hostUid) &&
            gaveUpOrFailed(match, index, match.guestUid)
}

internal class PremiumOnlineRoom(private val context: Context, private val session: AppSession) {
    private val database: FirebaseDatabase? get() = session.accounts.socialDatabase()
    private val uid: String get() = session.accounts.uid.orEmpty()
    private val nickname: String get() = session.social.nickname
    private val root get() = database?.reference
    private val roomBase get() = root?.child("premiumRooms")
    var match by mutableStateOf<PremiumOnlineMatch?>(null)
        private set
    var status by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var roomCode by mutableStateOf("")
        private set
    var usedToday by mutableStateOf(0)
        private set
    var quotaLoaded by mutableStateOf(false)
        private set
    private var ref: DatabaseReference? = null
    private var listener: ValueEventListener? = null
    private var quotaRef: DatabaseReference? = null
    private var quotaListener: ValueEventListener? = null
    private var quotaDayKey: String? = null
    private var timeOffsetRef: DatabaseReference? = null
    private var timeOffsetListener: ValueEventListener? = null
    var serverTimeOffset by mutableLongStateOf(0L)
        private set
    fun serverNow(wallTime: Long = System.currentTimeMillis()): Long = wallTime + serverTimeOffset

    fun listenServerTime() {
        timeOffsetRef?.let { old -> timeOffsetListener?.let(old::removeEventListener) }
        val offset = database?.reference?.child(".info/serverTimeOffset") ?: return
        timeOffsetRef = offset
        timeOffsetListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                serverTimeOffset = (snapshot.value as? Number)?.toLong() ?: 0L
            }
            override fun onCancelled(error: DatabaseError) { /* cached offset is still usable */ }
        }.also(offset::addValueEventListener)
    }
    private val rng = SecureRandom()
    private val saved = context.getSharedPreferences("premium_online_last_room", Context.MODE_PRIVATE)

    val isHost: Boolean get() = match?.hostUid == uid && uid.isNotBlank()
    val ready: Boolean get() = database != null && uid.isNotBlank() &&
        nickname.isNotBlank() && session.accountName != null
    val canCreate: Boolean get() = ready && !session.accounts.isGuest && quotaLoaded &&
        quotaDayKey == SparkArenaLimits.turkeyQuotaDayIndexKey(serverNow()) && usedToday < 10 && !busy
    val lastRoom: String get() = saved.getString("last_room_$uid", "").orEmpty()

    fun listenQuota() {
        quotaRef?.let { old -> quotaListener?.let(old::removeEventListener) }
        quotaRef = null
        quotaListener = null
        quotaLoaded = false
        usedToday = 0
        val database = database ?: return
        if (uid.isBlank()) return
        val day = SparkArenaLimits.turkeyQuotaDayIndexKey(serverNow())
        quotaDayKey = day
        val current = database.reference.child("roomQuota").child(uid).child(day)
        quotaRef = current
        quotaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (quotaDayKey != day) return
                usedToday = snapshot.childrenCount.toInt().coerceAtMost(10)
                quotaLoaded = true
            }
            override fun onCancelled(error: DatabaseError) {
                quotaLoaded = false
                status = "Oda kotası okunamadı: ${error.message}"
            }
        }.also(current::addValueEventListener)
    }

    fun refreshQuotaForToday() {
        if (uid.isNotBlank() && database != null &&
            quotaDayKey != SparkArenaLimits.turkeyQuotaDayIndexKey(serverNow())) listenQuota()
    }

    private fun randomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (0 until 9).map { chars[rng.nextInt(chars.length)] }.joinToString("")
    }

    fun create(mode: String, level: String, topic: String, size: Int, seconds: Int, boardId: String) {
        refreshQuotaForToday()
        if (!canCreate) { status = "Oda oluşturmak için hesabınla giriş yap ve kullanıcı adını kaydet."; return }
        if (!PremiumOnlineLimits.validConfig(level, size, seconds, mode) ||
            (mode != "quiz" && boardId.isBlank())) {
            status = "Seçilen oyun ayarları geçersiz."; return
        }
        val database = database ?: return
        busy = true
        status = "Çevrim içi oda hazırlanıyor…"
        val myUid = uid
        val day = SparkArenaLimits.turkeyQuotaDayIndexKey(serverNow())
        val quota = database.reference.child("roomQuota").child(myUid).child(day)
        quota.get().addOnSuccessListener { snapshot ->
            val free = (0..9).map(Int::toString).filter { !snapshot.child(it).exists() }
            if (free.isEmpty()) { busy = false; status = "Günlük oda hakkın doldu."; return@addOnSuccessListener }
            val code = randomCode()
            reserve(quota, free, 0, code) { slot ->
                if (slot == null) { busy = false; status = "Oda kotası ayrılamadı."; return@reserve }
                val initial = mapOf<String, Any>(
                    "hostUid" to myUid, "hostName" to nickname, "mode" to mode,
                    "phase" to "lobby", "level" to level, "topic" to topic.take(75),
                    "size" to size, "seconds" to seconds, "boardId" to boardId,
                    "quizSeed" to rng.nextInt(Int.MAX_VALUE), "turn" to mapOf(
                        "uid" to myUid,
                        "until" to 0L, // Lobby never consumes a player's turn or timer.
                    ),
                    "createdAt" to ServerValue.TIMESTAMP, "quotaDay" to day.toLong(),
                    "quotaSlot" to slot,
                )
                database.reference.child("premiumRooms").child(code).setValue(initial)
                    .addOnCompleteListener { done ->
                        busy = false
                        if (done.isSuccessful) { status = "Oda oluşturuldu."; observe(code) }
                        else status = "Oda açılamadı: ${done.exception?.localizedMessage.orEmpty()} · Premium Firebase kurallarını yayımla."
                    }
            }
        }.addOnFailureListener { e ->
            busy = false; status = "Oda kotasına erişilemedi: ${e.localizedMessage.orEmpty()}"
        }
    }

    private fun reserve(quota: DatabaseReference, slots: List<String>, position: Int, code: String,
                        complete: (String?) -> Unit) {
        if (position >= slots.size) { complete(null); return }
        quota.child(slots[position]).runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                if (data.value != null) return Transaction.abort()
                data.value = code
                return Transaction.success(data)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, data: DataSnapshot?) {
                when {
                    error != null -> { status = "Firebase kota izni reddedildi: ${error.message}"; complete(null) }
                    committed -> complete(slots[position])
                    else -> reserve(quota, slots, position + 1, code, complete)
                }
            }
        }, false)
    }

    fun join(input: String, onJoined: (() -> Unit)? = null) {
        if (!ready || busy) { status = "Önce Hesap bölümünde giriş yap ve kullanıcı adını kaydet."; return }
        val code = RoomInvite.normalizedCode(input)
        if (!RoomInvite.isValid(code)) { status = "Geçerli 9 karakterli oda kodu gir."; return }
        val ref = roomBase?.child(code) ?: return
        busy = true
        status = "Odaya bağlanılıyor…"
        ref.get().addOnSuccessListener { snapshot ->
            val host = snapshot.child("hostUid").getValue(String::class.java).orEmpty()
            val guest = snapshot.child("guestUid").getValue(String::class.java).orEmpty()
            if (host == uid || guest == uid) {
                busy = false; observe(code); onJoined?.invoke()
            } else if (host.isNotBlank() && guest.isBlank()) {
                takeGuestSeat(ref, code, onJoined)
            } else {
                busy = false; status = "Oda dolu veya bulunamadı."
            }
        }.addOnFailureListener {
            // Parent read is denied before joining; guestUid child write is permitted by rules.
            takeGuestSeat(ref, code, onJoined)
        }
    }

    private fun takeGuestSeat(ref: DatabaseReference, code: String, onJoined: (() -> Unit)?) {
        ref.child("guestUid").setValue(uid).addOnCompleteListener { result ->
            if (!result.isSuccessful) {
                busy = false
                status = "Odaya katılamadın: Kod geçersiz, oda dolu veya Firebase kuralları eksik."
                return@addOnCompleteListener
            }
            ref.child("guestName").setValue(nickname).addOnCompleteListener { nameResult ->
                busy = false
                if (nameResult.isSuccessful) {
                    status = "Odaya katıldın."; observe(code); onJoined?.invoke()
                } else {
                    status = "Oyuncu adı Firebase'e kaydedilemedi."; observe(code)
                }
            }
        }
    }

    private fun observe(code: String) {
        detach()
        val next = roomBase?.child(code) ?: return
        roomCode = code
        saved.edit().putString("last_room_$uid", code).apply()
        ref = next
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) { match = null; status = "Oda bulunamadı."; return }
                fun childString(key: String) = snapshot.child(key).getValue(String::class.java).orEmpty()
                fun childInt(key: String) = snapshot.child(key).getValue(Long::class.java)?.toInt() ?: 0
                fun choices(player: DataSnapshot): Map<Int, Int> = player.children.mapNotNull { answer ->
                    val index = answer.key?.toIntOrNull()
                    val value = answer.getValue(Long::class.java)?.toInt()
                    if (index != null && value != null) index to value else null
                }.toMap()
                val answers = snapshot.child("answers").children.mapNotNull { player ->
                    player.key?.let { it to choices(player) }
                }.toMap()
                val solved = snapshot.child("solved").children.mapNotNull { entry ->
                    val index = entry.key?.toIntOrNull()
                    val owner = entry.getValue(String::class.java)
                    if (index != null && owner != null) index to owner else null
                }.toMap()
                val unknown = snapshot.child("unknown").children.mapNotNull { word ->
                    val index = word.key?.toIntOrNull() ?: return@mapNotNull null
                    val players = word.children.filter { it.getValue(Boolean::class.java) == true }
                        .mapNotNull { it.key }.toSet()
                    index to players
                }.toMap()
                val mistakes = snapshot.child("mistakes").children.mapNotNull { word ->
                    val index = word.key?.toIntOrNull() ?: return@mapNotNull null
                    val players = word.children.mapNotNull { player ->
                        val count = (player.value as? Number)?.toInt()
                        if (player.key != null && count != null) player.key!! to count else null
                    }.toMap()
                    index to players
                }.toMap()
                match = PremiumOnlineMatch(code, childString("hostUid"), childString("guestUid"),
                    childString("hostName"), childString("guestName"), childString("phase"),
                    childString("mode"), childString("boardId"), childString("level"),
                    childString("topic"), childInt("size"), childInt("seconds"), childInt("quizSeed"),
                    snapshot.child("turn").child("uid").getValue(String::class.java).orEmpty(),
                    snapshot.child("turn").child("until").getValue(Long::class.java) ?: 0L,
                    solved, answers, unknown, mistakes)
            }
            override fun onCancelled(error: DatabaseError) {
                status = "Oda bağlantısı reddedildi: ${error.message}"
                match = null
            }
        }.also(next::addValueEventListener)
    }

    fun start() {
        val m = match ?: return
        if (!isHost || m.guestUid.isBlank() || m.phase != "lobby" || busy) return
        val room = ref ?: return
        busy = true
        status = "Oyuncular hazırlanıyor…"
        fun begin() {
            room.child("phase").setValue("playing").addOnCompleteListener { result ->
                busy = false
                status = if (result.isSuccessful) "Oyun başladı!" else
                    "Yarışma başlatılamadı: ${result.exception?.localizedMessage.orEmpty()}"
            }
        }
        if (m.mode != "duel") { begin(); return }
        // Select a player only after BOTH participants have entered the lobby.
        // Updating the turn before 'phase' avoids starting a round with an expired clock.
        val first = PremiumOnlineLimits.firstTurn(m.hostUid, m.guestUid, rng.nextBoolean())
        val expiry = if (m.seconds == 0) 0L else serverNow() + m.seconds * 1000L
        room.child("turn").setValue(mapOf("uid" to first, "until" to expiry))
            .addOnCompleteListener { result ->
                if (result.isSuccessful) begin() else {
                    busy = false
                    status = "İlk sıra seçilemedi. Yeni Arena Firebase kurallarını yayımla."
                }
            }
    }

    fun markSolved(index: Int) {
        val m = match ?: return
        if (!m.isCrossword || m.phase != "playing" || index !in 0 until m.size ||
            m.solvedBy.containsKey(index) || (!m.cooperative && m.turnUid != uid)) return
        val current = ref ?: return
        current.child("solved").child(index.toString()).runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                if (data.value != null) return Transaction.abort()
                data.value = uid
                return Transaction.success(data)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) status = "Cevap kaydedilemedi: ${error.message}"
                else if (committed) {
                    status = "Doğru cevap! İki oyuncunun bulmacası güncellendi."
                    if (!m.cooperative) renewOrPass(uid)
                    if (m.solvedBy.size + 1 >= m.size && isHost) finish()
                }
            }
        }, false)
    }

    /** "Bilmiyorum" is free. Wrong submissions are also failed attempts,
     * but are separately recorded as -3 points. The next failed answer from
     * the other player reveals the word automatically.
     */
    fun dontKnow(index: Int) {
        val m = match ?: return
        if (!m.isCrossword || m.phase != "playing" || index !in 0 until m.size ||
            index in m.solvedBy || (!m.cooperative && m.turnUid != uid)) return
        if (uid in (m.unknownBy[index] ?: emptySet())) {
            status = "Bu kelime için Bilmiyorum yanıtın zaten kaydedildi."
            if (!m.cooperative) pass()
            return
        }
        ref?.child("unknown")?.child(index.toString())?.child(uid)?.setValue(true)
            ?.addOnCompleteListener { result ->
                if (!result.isSuccessful) {
                    status = "Bilmiyorum kaydedilemedi. Firebase kurallarını güncelle."
                } else {
                    status = "Bilmiyorum kaydedildi. Rakibin de bilemezse cevap açılacak."
                    checkForReveal(index)
                    if (!m.cooperative) pass()
                }
            }
    }

    /** Room listener includes BOTH unknown and mistakes, including writes from
     * the opposite device. Both clients may try; the solved transaction ensures
     * only one reveal can succeed.
     */
    fun syncFailedAttemptReveals() {
        val m = match ?: return
        if (!m.isCrossword || m.phase != "playing") return
        (0 until m.size).forEach { index ->
            if (PremiumOnlineLimits.shouldReveal(m, index)) revealWord(index)
        }
    }

    /** Read latest authoritative answers after the Firebase write: avoids
     * waiting on a stale Compose snapshot when the two inputs arrive together.
     */
    private fun checkForReveal(index: Int) {
        val m = match ?: return
        ref?.get()?.addOnSuccessListener { room ->
            if (room.child("solved").child(index.toString()).exists() ||
                room.child("phase").getValue(String::class.java) != "playing") return@addOnSuccessListener
            fun failed(player: String): Boolean = player.isNotBlank() &&
                (room.child("unknown").child(index.toString()).child(player)
                    .getValue(Boolean::class.java) == true ||
                 ((room.child("mistakes").child(index.toString()).child(player).value as? Number)
                    ?.toInt() ?: 0) > 0)
            if (m.hostUid != m.guestUid && failed(m.hostUid) && failed(m.guestUid))
                revealWord(index)
        }?.addOnFailureListener {
            // The room's live listener also checks for the same two responses.
        }
    }

    private fun revealWord(index: Int) {
        val roomRef = ref ?: return
        val m = match ?: return
        if (!PremiumOnlineLimits.shouldReveal(m, index)) {
            // A write callback may have fresher server data than local listeners;
            // a matching server state is checked separately by checkForReveal.
            // Triggering this transaction is safe: Firebase rules require both
            // players' recorded failed attempts before accepting "reveal".
            if (m.phase != "playing" || index in m.solvedBy) return
        }
        roomRef.child("solved").child(index.toString()).runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                if (data.value != null) return Transaction.abort()
                data.value = "reveal" // neither player earns points for a revealed word
                return Transaction.success(data)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, data: DataSnapshot?) {
                when {
                    error != null -> status = "Cevap açılamadı: ${error.message}. Firebase kurallarını kontrol et."
                    committed -> {
                        status = "İkiniz de kelimeyi çözemediğiniz için cevap açıldı. Yanlış cevap cezası korunur."
                        if (m.solvedBy.size + 1 >= m.size && isHost) finish()
                        else if (!m.cooperative) pass() // never renew the failing player's turn
                    }
                }
            }
        }, false)
    }

    /** Store each incorrect submission as a bounded, authenticated penalty event.
     * A wrong guess deducts 3 points; then the opponent gets the turn in duel mode.
     */
    fun wrong(index: Int) {
        val m = match ?: return
        if (!m.isCrossword || m.phase != "playing" || index !in 0 until m.size ||
            index in m.solvedBy || (!m.cooperative && m.turnUid != uid)) return
        ref?.child("mistakes")?.child(index.toString())?.child(uid)
            ?.runTransaction(object : Transaction.Handler {
                override fun doTransaction(data: MutableData): Transaction.Result {
                    val count = (data.value as? Number)?.toInt() ?: 0
                    if (count >= 5) return Transaction.abort()
                    data.value = count + 1
                    return Transaction.success(data)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, current: DataSnapshot?) {
                    status = when {
                        error != null -> "Yanlış cevap puana işlenemedi: ${error.message}"
                        committed -> "Yanlış cevap: -3 puan. ${if (m.cooperative) "Tekrar dene." else "Sıra rakibe geçiyor."}"
                        else -> "Bu kelimede en fazla 5 yanlış deneme yapılabilir."
                    }
                    if (error == null && committed) {
                        checkForReveal(index)
                        if (!m.cooperative) pass()
                    }
                }
            }, false)
    }

    fun answer(index: Int, choice: Int) {
        val m = match ?: return
        if (m.mode != "quiz" || m.phase != "playing" || index !in 0..11 || choice !in -1..4 ||
            m.answers[uid]?.containsKey(index) == true) return
        ref?.child("answers")?.child(uid)?.child(index.toString())?.setValue(choice)
            ?.addOnFailureListener { status = "Cevap kaydedilemedi." }
    }

    fun pass() {
        val m = match ?: return
        if (m.phase != "playing" || m.mode != "duel" || m.guestUid.isBlank() || m.turnUid != uid) return
        changeTurn(m, PremiumOnlineLimits.otherPlayer(m, uid), onlyWhenExpired = false)
    }

    fun passIfExpired() {
        val m = match ?: return
        if (m.phase != "playing" || m.mode != "duel" || m.guestUid.isBlank() ||
            !PremiumOnlineLimits.timedOut(m.turnUntil, serverNow())) return
        // Either connected device may advance an expired turn. A Firebase transaction
        // guarantees that simultaneous attempts cannot pass the turn twice.
        changeTurn(m, PremiumOnlineLimits.otherPlayer(m, m.turnUid), onlyWhenExpired = true)
    }

    private fun renewOrPass(newUid: String) {
        val m = match ?: return
        if (m.phase == "playing" && m.mode == "duel" && m.turnUid == uid) {
            changeTurn(m, newUid, onlyWhenExpired = false)
        }
    }

    private fun changeTurn(m: PremiumOnlineMatch, newUid: String, onlyWhenExpired: Boolean) {
        if (newUid.isBlank() || uid !in listOf(m.hostUid, m.guestUid)) return
        val oldUid = m.turnUid
        val oldUntil = m.turnUntil
        val refTurn = ref?.child("turn") ?: return
        refTurn.runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                // Do not apply an old timer or user action after the opponent has taken their turn.
                val currentUid = data.child("uid").getValue(String::class.java)
                val currentUntil = data.child("until").getValue(Long::class.java)
                if (currentUid != oldUid || currentUntil != oldUntil) return Transaction.abort()
                if (onlyWhenExpired && !PremiumOnlineLimits.timedOut(oldUntil, serverNow()))
                    return Transaction.abort()
                data.child("uid").value = newUid
                data.child("until").value = if (m.seconds == 0) 0L else serverNow() + m.seconds * 1000L
                return Transaction.success(data)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, data: DataSnapshot?) {
                if (error != null) status = "Sıra yenilenemedi: ${error.message}"
            }
        }, false)
    }

    fun finish() {
        val m = match ?: return
        if (!isHost || m.phase != "playing" || !m.complete) return
        ref?.child("phase")?.setValue("finished")?.addOnFailureListener {
            status = "Sonuç kaydedilemedi."
        }
    }

    fun note(message: String) { status = message }
    fun leave() { detach(); status = "Odadan ayrıldın." }
    private fun detach() {
        ref?.let { current -> listener?.let(current::removeEventListener) }
        ref = null; listener = null; match = null; roomCode = ""
    }
    fun close() {
        detach()
        quotaRef?.let { current -> quotaListener?.let(current::removeEventListener) }
        quotaRef = null; quotaListener = null
        timeOffsetRef?.let { current -> timeOffsetListener?.let(current::removeEventListener) }
        timeOffsetRef = null; timeOffsetListener = null
    }
}
