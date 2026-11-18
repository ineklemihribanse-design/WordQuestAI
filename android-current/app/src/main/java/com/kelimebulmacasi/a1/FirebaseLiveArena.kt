package com.kelimebulmacasi.a1

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.security.SecureRandom
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

/** Small, free-tier, two-device practice arena. It does not claim server-authoritative scoring. */
internal data class LiveQuestion(val text: String, val choices: List<String>, val correct: Int, val type: String)

/** Firebase is the default, low-bandwidth engine for the entire competition menu. */
internal enum class FirebaseCompetitionMode(val key: String, val label: String, val info: String) {
    Fast("fast", "Hızlı Kelime Yarışması", "Hızlı İngilizce kelime düellosu"),
    Duel("duel", "Bulmaca Düellosu", "Aynı ipuçlarını iki cihazda çöz"),
    Team("team", "Ortak Bulmaca", "İki kişi ortak doğru sayısını artırır"),
    Yds("yds", "YDS Düellosu", "Açıklamalı YDS tipi sorular"),
    Oral("oral", "Sözlü Sınav Alıştırması", "Sesli yanıtlama; AI hakemi ayrıca gerekir"),
    Daily("daily", "Günlük Meydan Okuma", "UTC gününe özel ortak soru seti"),
    Words("words", "Kelime", "Kısa kelime yarışması"),
    Grammar("grammar", "Gramer", "Kısa gramer yarışması"),
    Mixed("mixed", "Karma", "Kelime ve gramer birlikte");
    companion object { fun of(key: String) = entries.firstOrNull { it.key == key } ?: Mixed }
}

internal object SparkArenaLimits {
    const val roomsPerAccountPerUtcDay = 10
    const val millisPerDay = 86_400_000L
    fun utcDayIndex(nowMillis: Long = System.currentTimeMillis()): Long = nowMillis / millisPerDay
    fun utcDayIndexKey(nowMillis: Long = System.currentTimeMillis()): String = utcDayIndex(nowMillis).toString()
    // Turkey is UTC+03 throughout the year. The Firebase Rules use the same
    // shifted Unix day index, so quotas reset at local midnight, not 03:00.
    const val turkeyOffsetMillis = 10_800_000L
    fun turkeyQuotaDayIndex(nowMillis: Long = System.currentTimeMillis()): Long =
        Math.floorDiv(nowMillis + turkeyOffsetMillis, millisPerDay)
    fun turkeyQuotaDayIndexKey(nowMillis: Long = System.currentTimeMillis()): String =
        turkeyQuotaDayIndex(nowMillis).toString()
    const val maxAnswersPerPlayer = 12
    const val maxRecentRooms = 8
    /** Platform caps: the Spark plan also has its own hard resource limits. */
    const val maxConcurrentSparkConnections = 100
    fun utcDay(): String = LocalDate.now(ZoneOffset.UTC).toString()
}

/** Room codes are safe to copy and share as text; no third-party SDK required. */
internal object RoomInvite {
    private val pattern = Regex("[A-HJ-NP-Z2-9]{9}")
    fun normalizedCode(code: String): String = code.trim().uppercase(Locale.ROOT)
    fun isValid(code: String): Boolean = pattern.matches(normalizedCode(code))
    fun message(code: String): String = "WordQuest AI yarışmasına davetlisin!\nOda kodu: ${normalizedCode(code)}\nWordQuest AI → Arena → Odaya katıl"
}

/** Human-readable competition nickname; intentionally independent of the Google account name. */
internal object ArenaNickname {
    const val maximumLength = 24
    private val allowed = Regex("^[\\p{L}\\p{N}](?:[\\p{L}\\p{N} _.-]{0,22}[\\p{L}\\p{N}])?$")
    fun normalize(input: String): String = input.trim().replace(Regex("\\s+"), " ")
    fun isValid(input: String): Boolean {
        if (input.any { it.isISOControl() }) return false
        val value = normalize(input)
        return value.length in 2..maximumLength && allowed.matches(value)
    }
    fun safeDisplay(value: String, missing: String): String =
        value.takeIf(::isValid)?.let(::normalize) ?: missing
}

internal object LiveQuestionBank {
    private val all = listOf(
        LiveQuestion("'Elma' İngilizcede hangisidir?", listOf("orange", "apple", "pear", "peach"), 1, "words"),
        LiveQuestion("'Öğretmen' İngilizcede hangisidir?", listOf("doctor", "student", "teacher", "driver"), 2, "words"),
        LiveQuestion("'Çalışmak' fiili hangisidir?", listOf("sleep", "study", "forget", "sell"), 1, "words"),
        LiveQuestion("'Güvenilir' anlamına gelen sözcük?", listOf("reliable", "fragile", "anxious", "silent"), 0, "words"),
        LiveQuestion("'Despite' ile yakın anlamlı bağlaç?", listOf("because", "although", "in spite of", "so that"), 2, "words"),
        LiveQuestion("'Increase' sözcüğünün zıttı?", listOf("improve", "decrease", "include", "decide"), 1, "words"),
        LiveQuestion("She ___ to school every morning.", listOf("go", "goes", "going", "gone"), 1, "grammar"),
        LiveQuestion("I have lived here ___ 2020.", listOf("for", "since", "during", "until"), 1, "grammar"),
        LiveQuestion("If I ___ more time, I would travel.", listOf("have", "had", "has", "having"), 1, "grammar"),
        LiveQuestion("The report ___ yesterday.", listOf("was written", "has write", "writes", "writing"), 0, "grammar"),
        LiveQuestion("Neither John nor Mary ___ ready.", listOf("are", "were", "is", "be"), 2, "grammar"),
        LiveQuestion("He suggested ___ early.", listOf("leave", "leaving", "to leaving", "left"), 1, "grammar"),
    )
    private val words get() = all.filter { it.type == "words" }
    private val grammar get() = all.filter { it.type == "grammar" }
    private val mixed get() = all.sortedBy { if (it.type == "words") all.indexOf(it) % 6 * 2 else all.indexOf(it) % 6 * 2 + 1 }

    fun forMode(mode: String, utcDay: String = ""): List<LiveQuestion> = when (mode) {
        "words", "fast" -> words
        "grammar" -> grammar
        "duel", "team", "mixed" -> mixed
        "oral" -> grammar
        "yds" -> YdsContent.questions.take(12).map { LiveQuestion(it.question, it.options, it.answer, "yds") }
        "daily" -> {
            // A room stores its UTC day so both phones see precisely the same questions.
            val key = utcDay.ifBlank { SparkArenaLimits.utcDay() }
            val shift = key.hashCode().toUInt().toLong().rem(mixed.size).toInt()
            (mixed.drop(shift) + mixed.take(shift)).take(10)
        }
        else -> mixed
    }
    fun score(mode: String, answers: Map<Int, Int>, day: String = ""): Int {
        val questions = forMode(mode, day)
        return answers.count { (index, choice) -> index in questions.indices && questions[index].correct == choice } * 10
    }
    fun teamScore(mode: String, first: Map<Int, Int>, second: Map<Int, Int>, day: String = ""): Int {
        val questions = forMode(mode, day)
        return questions.indices.count { first[it] == questions[it].correct || second[it] == questions[it].correct } * 10
    }
}

/** All Firebase connection information is client configuration. No server credentials are used. */
internal class FirebaseLiveArena(private val context: Context) {
    var state by mutableStateOf<RoomState?>(null)
        private set
    var status by mutableStateOf("Firebase yarışması için bağlan.")
        private set
    var busy by mutableStateOf(false)
        private set
    var signedIn by mutableStateOf(false)
        private set
    var roomCode by mutableStateOf("")
        private set
    var playerName by mutableStateOf("")
        private set
    var nameLoaded by mutableStateOf(false)
        private set
    var nameSaved by mutableStateOf(false)
        private set
    var nameSaving by mutableStateOf(false)
        private set
    private var database: FirebaseDatabase? = null
    private var auth: FirebaseAuth? = null
    // Room recents are local conveniences. Quotas live exclusively under the Firebase user UID.
    private val local = context.getSharedPreferences("firebase_arena_budget", Context.MODE_PRIVATE)
    private val nicknameLocal = context.getSharedPreferences("firebase_arena_nicknames", Context.MODE_PRIVATE)
    private var nicknameRef: DatabaseReference? = null
    private var nicknameListener: ValueEventListener? = null
    private var listener: ValueEventListener? = null
    private var roomRef: DatabaseReference? = null
    private var quotaRef: DatabaseReference? = null
    private var quotaListener: ValueEventListener? = null
    private var quotaDayKey: String? = null
    private var timeOffsetRef: DatabaseReference? = null
    private var timeOffsetListener: ValueEventListener? = null
    private var serverOffsetMillis: Long = 0L
    private fun serverNow(): Long = System.currentTimeMillis() + serverOffsetMillis
    private fun watchServerTime() {
        stopServerTime()
        val node = database?.reference?.child(".info/serverTimeOffset") ?: return
        timeOffsetRef = node
        timeOffsetListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                serverOffsetMillis = (snapshot.value as? Number)?.toLong() ?: 0L
                refreshQuotaForToday()
            }
            override fun onCancelled(error: DatabaseError) { /* local time remains fallback */ }
        }.also(node::addValueEventListener)
    }
    private fun stopServerTime() {
        timeOffsetRef?.let { node -> timeOffsetListener?.let(node::removeEventListener) }
        timeOffsetRef = null
        timeOffsetListener = null
    }
    fun refreshQuotaForToday() {
        if (!connected || myUid.isBlank()) return
        val today = SparkArenaLimits.turkeyQuotaDayIndexKey(serverNow())
        if (quotaDayKey != today) watchAccountQuota()
    }
    var usedRoomsToday by mutableIntStateOf(0)
        private set
    var quotaLoaded by mutableStateOf(false)
        private set

    data class RoomState(
        val host: String, val guest: String, val mode: String, val phase: String, val utcDay: String,
        val hostAnswers: Map<Int, Int>, val guestAnswers: Map<Int, Int>,
        val hostName: String = "", val guestName: String = "",
    )

    val myUid: String get() = auth?.currentUser?.uid.orEmpty()
    val isHost: Boolean get() = state?.host == myUid && myUid.isNotBlank()
    val connected: Boolean get() = database != null && signedIn
    val isRegisteredAccount: Boolean get() = auth?.currentUser?.isAnonymous == false && auth?.currentUser != null
    val remainingRoomsToday: Int get() = (SparkArenaLimits.roomsPerAccountPerUtcDay - usedRoomsToday).coerceAtLeast(0)
    val recentRooms: List<String> get() = local.getString("rooms", "").orEmpty().split(',')
        .filter(RoomInvite::isValid).take(SparkArenaLimits.maxRecentRooms)
    private fun rememberRoom(code: String) {
        val recent = (listOf(code) + recentRooms).distinct().take(SparkArenaLimits.maxRecentRooms)
        local.edit().putString("rooms", recent.joinToString(",")).apply()
    }

    private fun stopQuotaListening() {
        val ref = quotaRef
        val active = quotaListener
        if (ref != null && active != null) ref.removeEventListener(active)
        quotaRef = null
        quotaListener = null
        quotaDayKey = null
        quotaLoaded = false
        usedRoomsToday = 0
    }

    private fun stopNicknameListening() {
        val ref = nicknameRef
        val active = nicknameListener
        if (ref != null && active != null) ref.removeEventListener(active)
        nicknameRef = null
        nicknameListener = null
        nameLoaded = false
        nameSaved = false
        nameSaving = false
        playerName = ""
    }

    /** The Firebase UID is the nickname's owner: a second device gets the same value. */
    private fun watchNickname() {
        stopNicknameListening()
        val uid = myUid
        val db = database ?: return
        if (uid.isBlank()) return
        val cached = nicknameLocal.getString(uid, "").orEmpty()
        if (ArenaNickname.isValid(cached)) playerName = ArenaNickname.normalize(cached)
        val ref = db.reference.child("users").child(uid).child("nickname")
        nicknameRef = ref
        nicknameListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remote = snapshot.getValue(String::class.java).orEmpty()
                if (ArenaNickname.isValid(remote)) {
                    playerName = ArenaNickname.normalize(remote)
                    nameSaved = true
                    nicknameLocal.edit().putString(uid, playerName).apply()
                    syncLiveRoomName()
                } else {
                    // Do not silently upload old local values or claim they are synced.
                    playerName = nicknameLocal.getString(uid, "").orEmpty()
                        .takeIf(ArenaNickname::isValid).orEmpty()
                    nameSaved = false
                }
                nameLoaded = true
            }
            override fun onCancelled(error: DatabaseError) {
                nameLoaded = false
                nameSaved = false
                status = "Kullanıcı adı okunamadı: ${error.message}. Firebase Rules iznini kontrol et."
            }
        }.also(ref::addValueEventListener)
    }

    fun savePlayerName(rawName: String) {
        if (!connected || !nameLoaded || nameSaving) return
        val normalized = ArenaNickname.normalize(rawName)
        if (!ArenaNickname.isValid(rawName)) {
            status = "Kullanıcı adı 2-24 karakter olmalı; harf, rakam, boşluk, nokta, _ veya - kullan."
            return
        }
        val uid = myUid
        val ref = nicknameRef ?: return
        nameSaving = true
        ref.setValue(normalized).addOnCompleteListener { result ->
            nameSaving = false
            if (result.isSuccessful && uid == myUid) {
                playerName = normalized
                nameSaved = true
                nicknameLocal.edit().putString(uid, normalized).apply()
                status = "Yarışma kullanıcı adın kaydedildi. İki cihazda da kullanılabilir."
            } else {
                status = "Kullanıcı adı kaydedilemedi. Firebase kullanıcı kurallarını kontrol et."
            }
        }
    }

    private fun watchAccountQuota() {
        stopQuotaListening()
        val userUid = myUid
        val db = database ?: return
        if (userUid.isBlank()) return
        val day = SparkArenaLimits.turkeyQuotaDayIndexKey(serverNow())
        val ref = db.reference.child("roomQuota").child(userUid).child(day)
        quotaDayKey = day
        quotaRef = ref
        quotaListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Ignore a callback from yesterday's listener during midnight rollover.
                if (quotaDayKey != day || myUid != userUid) return
                usedRoomsToday = snapshot.childrenCount.toInt().coerceAtMost(SparkArenaLimits.roomsPerAccountPerUtcDay)
                quotaLoaded = true
            }
            override fun onCancelled(error: DatabaseError) {
                quotaLoaded = false
                status = "Hesap bazlı oda kotası okunamadı: ${error.message}. Firebase kota kurallarını güncelle."
            }
        }.also(ref::addValueEventListener)
    }

    fun connect(url: String, onReady: (() -> Unit)? = null) {
        val trimmed = url.trim().trimEnd('/')
        if (!Regex("https://[a-zA-Z0-9.-]+(?:firebasedatabase[.]app|firebaseio[.]com)").matches(trimmed)) {
            status = "Realtime Database adresi geçersiz. Firebase Console'dan veritabanı URL'sini kopyala."
            return
        }
        val id = context.getString(R.string.cloud_project_id)
        val appId = context.getString(R.string.cloud_application_id)
        val api = context.getString(R.string.cloud_api_key)
        if (id.isBlank() || appId.isBlank() || api.isBlank()) {
            status = "Firebase yapılandırması bu APK'da bulunmuyor."
            return
        }
        try {
            val app = FirebaseApp.getApps(context).firstOrNull { it.name == "KelimeBulmacasi" }
                ?: FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                    .setProjectId(id).setApplicationId(appId).setApiKey(api).setDatabaseUrl(trimmed).build(), "KelimeBulmacasi")
            checkNotNull(app)
            database = FirebaseDatabase.getInstance(app, trimmed)
            auth = FirebaseAuth.getInstance(app)
            busy = true
            val existing = auth!!.currentUser
            if (existing != null) {
                signedIn = true
                busy = false
                status = "Bağlandı. Oda kurabilir veya davet kodu girebilirsin."
                watchServerTime()
                watchAccountQuota()
                watchNickname()
                onReady?.invoke()
            } else {
                auth!!.signInAnonymously().addOnCompleteListener { task ->
                    busy = false
                    signedIn = task.isSuccessful
                    status = if (task.isSuccessful) "Anonim oturum hazır. Oda açabilirsin." else
                        "Anonim giriş başarısız. Firebase Authentication'da Anonymous etkin mi?"
                    if (task.isSuccessful) {
                        watchServerTime()
                        watchAccountQuota()
                        watchNickname()
                        onReady?.invoke()
                    }
                }
            }
        } catch (_: Exception) {
            busy = false
            status = "Firebase başlatılamadı. Veritabanı adresini kontrol et."
        }
    }

    fun create(mode: String) {
        val db = database ?: return
        if (!connected || busy) return
        if (!nameLoaded || !nameSaved || !ArenaNickname.isValid(playerName)) {
            status = "Önce yarışma kullanıcı adını kaydet."
            return
        }
        if (!isRegisteredAccount) {
            status = "Oda oluşturmak için kayıtlı hesabınla giriş yap. Misafir olarak odalara katılabilirsin."
            return
        }
        if (!quotaLoaded) {
            status = "Hesabının günlük oda hakkı Firebase'den okunuyor."
            return
        }
        val dayIndex = SparkArenaLimits.turkeyQuotaDayIndex(serverNow())
        if (quotaDayKey != dayIndex.toString()) {
            refreshQuotaForToday()
            status = "Türkiye saatiyle yeni gün başladı; oda hakkın yenileniyor."
            return
        }
        if (remainingRoomsToday <= 0) {
            status = "Hesabının günlük 10 yeni oda hakkı doldu. Mevcut odalara katılabilirsin."
            return
        }
        if (FirebaseCompetitionMode.entries.none { it.key == mode }) {
            status = "Bilinmeyen yarışma türü."
            return
        }
        val uid = myUid
        if (uid.isBlank()) return
        busy = true
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rng = SecureRandom()
        val code = (1..9).map { chars[rng.nextInt(chars.length)] }.joinToString("")
        val transportMode = when (mode) {
            "fast", "duel", "team" -> "words"
            "oral" -> "grammar"
            else -> "mixed"
        }
        val quota = db.reference.child("roomQuota").child(uid).child(dayIndex.toString())
        quota.get().addOnSuccessListener { snapshot ->
            val free = (0 until SparkArenaLimits.roomsPerAccountPerUtcDay)
                .map(Int::toString).filter { !snapshot.child(it).exists() }
            if (free.isEmpty()) {
                busy = false
                status = "Hesabının günlük 10 oda hakkı doldu."
            } else reserveQuotaSlot(quota, free, 0, code) { slot ->
                if (slot == null) {
                    busy = false
                    status = "Günlük hesap kotası doldu veya Firebase oda kotası kuralları eksik."
                } else {
                    // Room creation requires a matching immutable quota reservation on Firebase.
                    val initial = mapOf(
                        "hostUid" to uid, "hostName" to playerName,
                        "mode" to transportMode, "gameType" to mode,
                        "phase" to "lobby", "createdAt" to ServerValue.TIMESTAMP,
                        "utcDay" to SparkArenaLimits.utcDay(),
                        "quotaDay" to dayIndex, "quotaSlot" to slot,
                    )
                    db.reference.child("rooms").child(code).setValue(initial).addOnCompleteListener { result ->
                        busy = false
                        if (result.isSuccessful) {
                            rememberRoom(code)
                            observe(code)
                        } else status = "Oda açılamadı. Yeni Firebase kota kurallarını yayımla: ${result.exception?.localizedMessage.orEmpty()}"
                    }
                }
            }
        }.addOnFailureListener { error ->
            busy = false
            status = "Hesap kotasına erişilemedi: ${error.localizedMessage.orEmpty()}"
        }
    }

    private fun reserveQuotaSlot(
        quota: DatabaseReference, slots: List<String>, position: Int, code: String,
        done: (String?) -> Unit,
    ) {
        if (position >= slots.size) { done(null); return }
        val slot = slots[position]
        quota.child(slot).runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                if (data.value != null) return Transaction.abort()
                data.value = code
                return Transaction.success(data)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                when {
                    error != null -> {
                        status = "Firebase hesap kota izni reddedildi: ${error.message}"
                        done(null)
                    }
                    committed -> done(slot)
                    else -> reserveQuotaSlot(quota, slots, position + 1, code, done)
                }
            }
        }, false)
    }

    fun join(rawCode: String, onJoined: (() -> Unit)? = null) {
        if (!connected || busy) return
        if (!nameLoaded || !nameSaved || !ArenaNickname.isValid(playerName)) {
            status = "Odaya kullanıcı adınla katılmak için önce adını kaydet."
            return
        }
        val code = RoomInvite.normalizedCode(rawCode)
        if (!RoomInvite.isValid(code)) { status = "9 karakterli geçerli oda kodunu gir."; return }
        val uid = myUid
        val chosenName = playerName
        busy = true
        val ref = database!!.reference.child("rooms").child(code)
        ref.child("guestUid").setValue(uid).addOnCompleteListener { joinResult ->
            if (!joinResult.isSuccessful) {
                busy = false
                status = "Odaya katılamadın. Kod yanlış, oda dolu veya güvenlik kuralları eksik."
            } else {
                // Joining an existing lobby and attaching a name are separate child writes.
                // Existing v1.0.7 rooms remain compatible with this flow.
                ref.child("guestName").setValue(chosenName).addOnCompleteListener { nameResult ->
                    busy = false
                    rememberRoom(code)
                    observe(code)
                    if (nameResult.isSuccessful) onJoined?.invoke()
                    if (!nameResult.isSuccessful) {
                        status = "Odaya katıldın, fakat kullanıcı adın gönderilemedi. Yeni Firebase Rules kurallarını yayımla."
                    }
                }
            }
        }
    }

    private fun observe(code: String) {
        closeRoom()
        roomCode = code
        rememberRoom(code)
        val ref = database!!.reference.child("rooms").child(code)
        roomRef = ref
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) { status = "Oda bulunamadı."; return }
                val host = snapshot.child("hostUid").getValue(String::class.java).orEmpty()
                val guest = snapshot.child("guestUid").getValue(String::class.java).orEmpty()
                val mode = snapshot.child("gameType").getValue(String::class.java)
                    ?: snapshot.child("mode").getValue(String::class.java) ?: "mixed"
                val phase = snapshot.child("phase").getValue(String::class.java) ?: "lobby"
                val utcDay = snapshot.child("utcDay").getValue(String::class.java).orEmpty()
                fun answers(uid: String): Map<Int, Int> {
                    if (uid.isBlank()) return emptyMap()
                    return snapshot.child("answers").child(uid).children.mapNotNull { child ->
                        val index = child.key?.toIntOrNull()
                        val value = child.getValue(Long::class.java)?.toInt()
                        if (index != null && value != null) index to value else null
                    }.toMap()
                }
                val hostName = snapshot.child("hostName").getValue(String::class.java).orEmpty()
                val guestName = snapshot.child("guestName").getValue(String::class.java).orEmpty()
                state = RoomState(host, guest, mode, phase, utcDay, answers(host), answers(guest),
                    hostName, guestName)
                syncLiveRoomName()
                status = "Canlı oda bağlandı."
            }
            override fun onCancelled(error: DatabaseError) {
                status = "Oda dinleme reddedildi. Firebase veritabanı güvenlik kurallarını kontrol et."
            }
        }.also(ref::addValueEventListener)
    }

    /** Keep the active lobby/player label synchronized with the account's nickname. */
    private fun syncLiveRoomName() {
        val current = state ?: return
        if (!nameSaved || !ArenaNickname.isValid(playerName) ||
            current.phase !in setOf("lobby", "playing")) return
        val target = when (myUid) {
            current.host -> "hostName".takeIf { current.hostName != playerName }
            current.guest -> "guestName".takeIf { current.guestName != playerName }
            else -> null
        } ?: return
        roomRef?.child(target)?.setValue(playerName)?.addOnFailureListener {
            status = "Odadaki kullanıcı adı güncellenemedi. Firebase kurallarını kontrol et."
        }
    }

    fun start() {
        if (!isHost || state?.guest.isNullOrBlank()) return
        roomRef?.child("phase")?.setValue("playing")?.addOnFailureListener {
            status = "Oyun başlatılamadı."
        }
    }

    fun answer(index: Int, choice: Int) {
        val s = state ?: return
        if (s.phase != "playing" || choice !in 0..3 || index !in LiveQuestionBank.forMode(s.mode, s.utcDay).indices) return
        val own = if (isHost) s.hostAnswers else s.guestAnswers
        if (own.containsKey(index)) return
        roomRef?.child("answers")?.child(myUid)?.child(index.toString())?.setValue(choice)
            ?.addOnFailureListener { status = "Cevap kaydedilemedi. Tekrar dene." }
    }

    fun finish() {
        if (!isHost) return
        roomRef?.child("phase")?.setValue("finished")?.addOnFailureListener {
            status = "Sonuç tamamlanamadı."
        }
    }

    fun note(message: String) { status = message }
    fun openRecent(code: String) { if (connected) observe(code) }
    fun leave() { closeRoom(); status = "Odadan ayrıldın." }
    private fun closeRoom() {
        val previous = roomRef
        val active = listener
        if (previous != null && active != null) previous.removeEventListener(active)
        roomRef = null
        listener = null
        state = null
        roomCode = ""
    }
    fun close() { closeRoom(); stopQuotaListening(); stopNicknameListening(); stopServerTime() }
}

private fun copyRoomCode(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("WordQuest AI oda kodu", RoomInvite.normalizedCode(code)))
}

private fun shareRoomCode(context: Context, code: String, whatsappOnly: Boolean) {
    val base = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, RoomInvite.message(code))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        if (whatsappOnly) {
            context.startActivity(base.setPackage("com.whatsapp"))
        } else {
            context.startActivity(Intent.createChooser(base, "Oda davetini paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    } catch (_: ActivityNotFoundException) {
        // WhatsApp / WhatsApp Business not installed: choose any installed sharing app.
        context.startActivity(Intent.createChooser(base.setPackage(null), "Oda davetini paylaş")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
internal fun FirebaseLiveArenaScreen(session: AppSession, back: () -> Unit, legacyServer: () -> Unit, onAccount: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { context.getSharedPreferences("firebase_room_config", Context.MODE_PRIVATE) }
    val client = remember { FirebaseLiveArena(context.applicationContext) }
    var url by remember { mutableStateOf(settings.getString("database_url", context.getString(R.string.cloud_database_url))
        ?: context.getString(R.string.cloud_database_url)) }
    var enteredCode by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(FirebaseCompetitionMode.Fast.key) }
    var selection by remember { mutableIntStateOf(0) }
    var oauthId by remember { mutableStateOf(session.oauthWebClientId) }
    var showOAuthSetup by remember { mutableStateOf(false) }
    val room = client.state
    val selected = FirebaseCompetitionMode.of(mode)
    val oralRecognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val message = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        val questions = room?.let { LiveQuestionBank.forMode(it.mode, it.utcDay) }.orEmpty()
        val question = questions.getOrNull(selection)
        if (result.resultCode == Activity.RESULT_OK && question != null) {
            val normalized = message.lowercase(Locale.ROOT).trim().replace(Regex("[^a-z ]"), "")
            val matched = question.choices.indexOfFirst { choice ->
                choice.lowercase(Locale.ROOT).trim().replace(Regex("[^a-z ]"), "") == normalized
            }
            if (matched >= 0) client.answer(selection, matched)
            else client.note("Konuşma eşleştirilemedi; istersen aşağıdaki şıklardan seç.")
        }
    }
    DisposableEffect(client) { onDispose { client.close() } }
    BackHandler(room != null) { client.leave() }
    LaunchedEffect(url, session.accountName) { if (url.isNotBlank()) client.connect(url) }
    // Rebind the daily quota on midnight even if the screen has stayed open.
    LaunchedEffect(client) {
        while (true) { client.refreshQuotaForToday(); kotlinx.coroutines.delay(1_000L) }
    }
    // Receiving an invite from anywhere opens this screen and joins automatically
    // after Firebase and the signed-in user's nickname are ready.
    val joinTarget = session.social.joinTarget
    LaunchedEffect(joinTarget, client.connected, client.nameSaved, client.busy) {
        if (joinTarget != null && client.connected && client.nameSaved && !client.busy) {
            session.social.clearJoinTarget() // exactly one join attempt per tap
            client.join(joinTarget.roomCode) {
                session.social.dismissInvite(joinTarget.senderUid)
            }
        }
    }
    ScreenPage("AI Yarışma Merkezi", "Firebase Spark · ücretsiz çevrimiçi yarışmalar", back) {
        BodyText("Varsayılan bağlantı Firebase. İkinci oyuncu kendi cihazından oda koduyla katılır; bu cihazda ikinci oyuncu profili gerekmez.")
        if (session.accountName != null) BodyText("Hesap: ${session.profileName}", Forest)
        else {
            if (session.oauthWebClientId.isNotBlank()) {
                PrimaryAction(if (session.signInBusy) "Google girişi açılıyor…" else "Google hesabıyla giriş",
                    session::signIn, session.accountAvailable && !session.signInBusy)
            } else BodyText("Google OAuth için Web istemci kimliği henüz kayıtlı değil. Anonim Firebase oturumu ile ücretsiz yarışmaya devam edebilirsin.")
            session.accountError?.let { BodyText(it, ErrorInk) }
            BodyText("E-posta/şifre hesabıyla giriş için ana ekrandaki Hesap menüsünü kullanabilirsin.")
        }
        if (showOAuthSetup) {
            OutlinedTextField(oauthId, { oauthId = it.take(180) }, Modifier.fillMaxWidth(),
                label = { Text("Firebase OAuth Web Client ID") }, singleLine = true)
            PrimaryAction("OAuth kimliğini bu cihazda kaydet", {
                if (session.configureOAuth(oauthId)) showOAuthSetup = false
            }, !session.signInBusy && session.isValidOAuthId(oauthId))
            BodyText("Google ile giriş Firebase Authentication'da etkin ve uygulama SHA-1/SHA-256 kayıtlı olmalı. Sadece .apps.googleusercontent.com ile biten WEB istemci kimliğini gir; sır veya şifre girme.")
        }
        TextButton(onClick = { showOAuthSetup = !showOAuthSetup }) {
            Text(if (showOAuthSetup) "OAuth ayarını kapat" else "Google OAuth ayarları")
        }
        HorizontalDivider()
        BodyText("Kullanıcı adı Hesap bölümünden belirlenir ve tüm yarışma odalarında aynı ad kullanılır.")
        TextButton(onClick = onAccount) { Text("Hesap ve kullanıcı adı") }
        if (client.nameSaved) BodyText("Oyuncu: ${client.playerName}", Forest)
        else BodyText(if (session.accountName == null) "Arkadaşlarınla yarışmak için önce Hesap bölümünden giriş yapıp kullanıcı adını kaydet."
            else if (client.nameLoaded) "Hesap bölümünden bir kullanıcı adı belirle." else "Kullanıcı adı okunuyor…")
        HorizontalDivider()
        BodyText(if (client.isRegisteredAccount) {
            if (client.quotaLoaded) "Hesabının bugünkü oda hakkı: ${client.remainingRoomsToday}/${SparkArenaLimits.roomsPerAccountPerUtcDay} · Türkiye saatiyle 00.00'da yenilenir."
            else "Hesabının günlük oda hakkı Firebase'den kontrol ediliyor..."
        } else "Oda oluşturmak için kayıtlı hesabınla giriş yap. Misafir olarak mevcut odalara katılabilirsin.")
        if (!client.connected) {
            BodyText("Oda açmak için Google veya e-posta/şifre hesabınla giriş yap. Misafir olarak mevcut odalara katılabilirsin.")
            PrimaryAction("Firebase'e bağlan", {
                settings.edit().putString("database_url", url.trim()).apply()
                client.connect(url)
            }, url.isNotBlank() && !client.busy)
            TextButton(onClick = { showOAuthSetup = !showOAuthSetup }) { Text("Bağlantı ayarlarını görüntüle") }
            if (showOAuthSetup) OutlinedTextField(url, { url = it.take(250) }, Modifier.fillMaxWidth(),
                label = { Text("Realtime Database URL") }, singleLine = true)
        } else if (room == null) {
            BodyText("Yarışma türünü seç")
            FirebaseCompetitionMode.entries.filter { it !in setOf(FirebaseCompetitionMode.Words,
                FirebaseCompetitionMode.Grammar, FirebaseCompetitionMode.Mixed) }.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RadioButton(selected = selected == item, onClick = { mode = item.key })
                    Column {
                        TextButton(onClick = { mode = item.key }) { Text(item.label) }
                        BodyText(item.info)
                    }
                }
            }
            PrimaryAction("Firebase odası oluştur", { client.create(mode) },
                !client.busy && client.isRegisteredAccount && client.quotaLoaded &&
                    client.nameSaved && client.remainingRoomsToday > 0)
            HorizontalDivider()
            OutlinedTextField(enteredCode, { enteredCode = it.take(14) }, Modifier.fillMaxWidth(),
                label = { Text("Arkadaşının 9 karakterli oda kodu") }, singleLine = true)
            PrimaryAction("Odaya katıl", { client.join(enteredCode) },
                !client.busy && client.nameSaved && RoomInvite.isValid(enteredCode))
            if (client.recentRooms.isNotEmpty()) {
                BodyText("Son odalar (yalnızca bu cihaz)")
                client.recentRooms.forEach { code ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { client.openRecent(code) }) { Text(code) }
                        TextButton(onClick = { copyRoomCode(context, code); client.note("Oda kodu kopyalandı.") }) { Text("Kopyala") }
                        TextButton(onClick = { shareRoomCode(context, code, false) }) { Text("Paylaş") }
                    }
                }
            }
        } else {
            val questions = LiveQuestionBank.forMode(room.mode, room.utcDay)
            val own = if (client.isHost) room.hostAnswers else room.guestAnswers
            val other = if (client.isHost) room.guestAnswers else room.hostAnswers
            val me = LiveQuestionBank.score(room.mode, own, room.utcDay)
            val rival = LiveQuestionBank.score(room.mode, other, room.utcDay)
            val team = LiveQuestionBank.teamScore(room.mode, own, other, room.utcDay)
            val teamMode = room.mode == FirebaseCompetitionMode.Team.key
            val hostLabel = ArenaNickname.safeDisplay(room.hostName, "Oda sahibi")
            val guestLabel = ArenaNickname.safeDisplay(room.guestName,
                if (room.guest.isBlank()) "Bekleniyor" else "Misafir")
            val myLabel = if (client.isHost) hostLabel else guestLabel
            val rivalLabel = if (client.isHost) guestLabel else hostLabel
            BodyText("Oda kodu: ${client.roomCode} · ${FirebaseCompetitionMode.of(room.mode).label}")
            BodyText("Oyuncular: ${hostLabel} (kurucu) · ${guestLabel}")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { copyRoomCode(context, client.roomCode); client.note("Oda kodu kopyalandı.") }) { Text("Kopyala") }
                OutlinedButton(onClick = { shareRoomCode(context, client.roomCode, false) }) { Text("Paylaş") }
            }
            BodyText(if (teamMode) "Ortak puan: $team" else "$myLabel: $me · $rivalLabel: $rival")
            when (room.phase) {
                "lobby" -> {
                    BodyText(if (room.guest.isBlank()) "Arkadaşın kendi Android cihazından kullanıcı adıyla katılsın." else "$guestLabel odaya katıldı.")
                    if (client.isHost) PrimaryAction("Yarışmayı başlat", client::start, room.guest.isNotBlank())
                    else BodyText("Oda sahibi başlatınca başlayacak.")
                }
                "playing" -> {
                    if (questions.isNotEmpty()) {
                        val index = selection.coerceIn(0, questions.lastIndex)
                        val question = questions[index]
                        val completedByTeam = teamMode && (own[index] == question.correct || other[index] == question.correct)
                        BodyText("${index + 1}/${questions.size} · ${question.text}")
                        if (room.mode == FirebaseCompetitionMode.Oral.key && !own.containsKey(index)) {
                            ArenaOutline("Sesle yanıtla (cihazdaki ses tanıma)") {
                                try {
                                    oralRecognizer.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB"))
                                } catch (_: ActivityNotFoundException) { client.note("Bu cihazda konuşma tanıma bulunamadı.") }
                            }
                        }
                        if (own.containsKey(index) || completedByTeam) {
                            BodyText(if (completedByTeam || own[index] == question.correct) "✓ Doğru" else
                                "Doğru yanıt: ${question.choices[question.correct]}")
                            if (room.mode == "yds") {
                                YdsContent.questions.getOrNull(index)?.let { BodyText("Açıklama: ${it.explanation}") }
                            }
                        } else {
                            question.choices.forEachIndexed { choice, label ->
                                OutlinedButton(onClick = { client.answer(index, choice) }, Modifier.fillMaxWidth()) { Text(label) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { selection = (index - 1).coerceAtLeast(0) }, enabled = index > 0) { Text("Önceki soru") }
                            OutlinedButton(onClick = { selection = (index + 1).coerceAtMost(questions.lastIndex) },
                                enabled = index < questions.lastIndex) { Text("Sonraki soruyu seç") }
                        }
                        if (client.isHost && (own.size + other.size >= questions.size * 2 || team == questions.size * 10)) {
                            PrimaryAction("Sonuçları kesinleştir", client::finish)
                        }
                    }
                }
                "finished" -> {
                    BodyText(if (teamMode) "Takım toplamı: $team" else "Yarışma sonucu · $myLabel: $me · $rivalLabel: $rival")
                    PrimaryAction("Rövanş odası oluştur", { client.leave(); client.create(room.mode) },
                        client.isRegisteredAccount && client.quotaLoaded && client.remainingRoomsToday > 0)
                }
            }
            OutlinedButton(onClick = client::leave, modifier = Modifier.fillMaxWidth()) { Text("Odadan ayrıl") }
        }
        if (session.social.enabled) {
            HorizontalDivider()
            BodyText("Arkadaşlık istekleri ve oda davetleri")
            FriendsPanel(
                session.social,
                roomCode = client.roomCode.takeIf { room != null && client.isHost && room.phase == "lobby" },
                onJoin = { senderUid, code ->
                    client.join(code) { session.social.dismissInvite(senderUid) }
                },
            )
        }
        if (client.status.isNotBlank()) BodyText(client.status)
        HorizontalDivider()
        ArenaOutline("Sunucuya bağlan · Alternatif yarışma altyapısı", action = legacyServer)
        BodyText("Eski sunucu/AI hakemi ayarları kaldırılmadı. Firebase günlük ücretsiz sınırlara uyar; AI hakemliği ve güvenilir sunucu tarafı puan doğrulaması ayrıca yapılandırılmadan aktif değildir.")
    }
}
