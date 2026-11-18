package com.kelimebulmacasi.a1

import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class ArenaSession(app: Application) : AndroidViewModel(app) {
    private val preferences = app.getSharedPreferences("arena_v1", Context.MODE_PRIVATE)
    private var endpoints = readEndpoints()
    private var api = endpoints?.let(::ArenaApi)
    private val tokenLock = Mutex()
    private var tokens: ArenaTokens? = readTokens()
    private var generation = 0
    private var observing: Job? = null
    private var requestedCode = ""
    private var actionJob: Job? = null
    private var aiJob: Job? = null
    private var lastSync = 0L
    private var serverAt = 0L
    private var stampAt = 0L
    var foreground: Boolean = true
        private set

    var pendingInvite by mutableStateOf("")
        private set
    var configured by mutableStateOf(endpoints != null)
        private set
    var profile by mutableStateOf<JSONObject?>(null)
        private set
    var packs by mutableStateOf<List<ArenaPack>>(emptyList())
        private set
    var room by mutableStateOf<JSONObject?>(null)
        private set
    var olderChat by mutableStateOf<List<JSONObject>>(emptyList())
        private set
    var history by mutableStateOf(JSONObject())
        private set
    var tutor by mutableStateOf<List<JSONObject>>(emptyList())
        private set
    var review by mutableStateOf<List<JSONObject>>(emptyList())
        private set
    var signedIn by mutableStateOf(tokens != null)
        private set
    var busy by mutableStateOf(false)
        private set
    var aiBusy by mutableStateOf(false)
        private set
    var live by mutableStateOf(false)
        private set
    var connected by mutableStateOf(false)
        private set
    var status by mutableStateOf("")
        private set
    var aiStatus by mutableStateOf("")
        private set

    fun settingValues(): List<String> = listOf(endpoints?.supabase.orEmpty(), endpoints?.publicKey.orEmpty(), endpoints?.worker.orEmpty())
    fun invite(value: String) { if (arenaCode(value).isNotBlank()) pendingInvite = arenaCode(value) }
    fun useInvite() { pendingInvite = "" }
    fun clearStatus() { status = ""; aiStatus = "" }

    fun configure(url: String, key: String, worker: String, done: () -> Unit) {
        val next = runCatching { ArenaEndpoints.validated(url, key, worker) }.getOrNull()
        if (next == null) {
            status = "Supabase proje adresi, yayımlanabilir anahtar ve workers.dev adresini kontrol et."
            return
        }
        if (next != endpoints) {
            signOut()
            endpoints = next
            api = ArenaApi(next)
            preferences.edit().putString("supabase", next.supabase).putString("public", next.publicKey)
                .putString("worker", next.worker).apply()
        }
        configured = true
        done()
    }

    fun testWorker() = run { apiOrThrow().health(); status = "AI sunucusu erişilebilir." }

    fun signIn(email: String, password: String, done: () -> Unit) = run {
        val service = apiOrThrow()
        val login = service.signIn(email.trim(), password)
        val info = service.db(login.access, "profile", JSONObject()) as JSONObject
        storeTokens(login)
        profile = info
        signedIn = true
        busy = false
        done()
    }

    fun signOut() {
        generation++
        actionJob?.cancel()
        aiJob?.cancel()
        observe("")
        tokens = null
        signedIn = false
        profile = null
        packs = emptyList()
        history = JSONObject()
        tutor = emptyList()
        review = emptyList()
        room = null
        olderChat = emptyList()
        busy = false
        aiBusy = false
        status = ""
        aiStatus = ""
        preferences.edit().remove("session").apply()
        api?.close()
    }

    fun loadHome() = run {
        profile = request("profile") as JSONObject
    }

    fun loadCreate() = run {
        packs = (request("catalog") as JSONArray).objects().map(::ArenaPack)
    }
    fun loadHistory() = run { history = request("history") as JSONObject }
    fun loadStats() = run {
        history = request("history") as JSONObject
        review = (request("study") as JSONArray).objects()
    }
    fun loadTutor() = run { tutor = (request("tutor-history") as JSONArray).objects() }

    fun create(pack: ArenaPack, count: Int, seconds: Int, training: Boolean, referee: Boolean, done: () -> Unit) = run {
        val data = JSONObject().put("mode", pack.mode.id).put("level", pack.level).put("topic", pack.topic)
            .put("size", count).put("seconds", seconds).put("training", training).put("referee", referee)
        receive(request("create", data) as JSONObject)
        done()
    }

    fun join(value: String, done: () -> Unit) {
        val code = arenaCode(value)
        if (code.isBlank()) { status = "WQ ile başlayan oda kodunun tamamını gir."; return }
        run {
            receive(request("join", JSONObject().put("code", code)) as JSONObject)
            useInvite()
            done()
        }
    }

    fun moveToRematch(done: () -> Unit) = run {
        receive(request("rematch", roomData()) as JSONObject)
        done()
    }

    fun updateRoom(action: String, extra: JSONObject = JSONObject(), complete: () -> Unit = {}) = run {
        val data = extra.put("code", room?.text("code").orEmpty())
        receive(request(action, data) as JSONObject)
        complete()
    }

    fun saveAi(model: JSONObject) = run {
        sendAi("config", model)
        profile = request("profile") as JSONObject
        status = "Tercihler kaydedildi."
    }

    fun tutorRequest(code: String, text: String, topic: String, done: () -> Unit) = runAi {
        val data = JSONObject().put("message", text).put("topic", topic).put("code", code).put("consent", true)
        val turn = sendAi("tutor", data)
        tutor = (tutor + turn).distinctBy { it.optLong("id") }.takeLast(80)
        done()
    }

    fun loadOlderChat(before: Long) = run {
        val data = roomData().put("before", before)
        val rows = (request("chat-older", data) as JSONArray).objects()
        if (data.text("code") == room?.text("code")) olderChat = rows + olderChat
    }

    fun referee(action: String, question: String = "", done: () -> Unit = {}) = runAi {
        val data = roomData().put("consent", true).put("question", question)
        sendAi(action, data)
        val code = data.getString("code")
        val update = request("state", JSONObject().put("code", code)) as JSONObject
        if (room?.text("code") == code) { receive(update); done() }
    }

    fun time() = if (stampAt == 0L) System.currentTimeMillis() else serverAt + SystemClock.elapsedRealtime() - stampAt

    fun pause() {
        foreground = false
        observing?.cancel()
        observing = null
        live = false
        connected = false
    }
    fun resume() {
        foreground = true
        if (requestedCode.isNotBlank()) observe(requestedCode)
    }

    fun observe(code: String) {
        if (code == requestedCode && observing?.isActive == true) return
        requestedCode = code
        observing?.cancel()
        observing = null
        live = false
        connected = false
        if (code.isEmpty() || !foreground) return
        val before = generation
        observing = viewModelScope.launch {
            var watcher: ArenaWatch? = null
            var openedAt = 0L
            var pulseAt = 0L
            var socketToken = ""
            var backoffMs = 1_000L
            var countdownFetched = false
            val codeData = JSONObject().put("code", code)
            try {
                while (currentCoroutineContext().isActive && before == generation) {
                    try {
                        val state = request("state", codeData) as JSONObject
                        if (before != generation || requestedCode != code) break
                        receive(state)
                        backoffMs = 1_000
                        val phase = state.text("phase")
                        val token = access()
                        if (watcher?.ended != false || socketToken != token ||
                            (!watcher.subscribed && SystemClock.elapsedRealtime() - openedAt > 25_000)) {
                            watcher?.close()
                            watcher = apiOrThrow().watch(token, state.getString("id"))
                            socketToken = token
                            openedAt = SystemClock.elapsedRealtime()
                            pulseAt = openedAt
                        }
                        val activeWatch = watcher
                        live = activeWatch.subscribed
                        val untilStart = state.optLong("startMs") - time()
                        val untilEnd = state.optLong("endMs") - time()
                        val next = when {
                            phase == "playing" && untilStart > -800 && !countdownFetched -> (untilStart + 180).coerceAtLeast(150)
                            phase == "playing" && untilEnd > 0 -> minOf(untilEnd + 170, if (live) 22_000L else 4_000L)
                            phase == "finished" -> if (live) 24_000L else 8_000L
                            else -> if (live) 22_000L else 4_000L
                        }
                        val event = withTimeoutOrNull(next.coerceIn(180, 24_000)) { activeWatch.changes.receiveCatching() }
                        if (event == null && phase == "playing" && untilStart > -800) countdownFetched = true
                        if (SystemClock.elapsedRealtime() - pulseAt > 19_000) {
                            activeWatch.pulse()
                            pulseAt = SystemClock.elapsedRealtime()
                        }
                    } catch (problem: CancellationException) {
                        throw problem
                    } catch (problem: Exception) {
                        connected = false
                        live = false
                        if (problem is ArenaProblem && problem.status == 401) { signOut(); break }
                        if (problem is ArenaProblem && problem.code in setOf("arena_missing", "arena_access")) {
                            status = problem.description
                            break
                        }
                        watcher?.close()
                        watcher = null
                        delay(backoffMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(20_000)
                    }
                }
            } finally {
                watcher?.close()
            }
        }
    }

    private fun receive(state: JSONObject) {
        val current = room
        val at = state.optLong("serverMs")
        if (current?.text("id") == state.text("id") && at < lastSync) return
        if (current?.text("id") != state.text("id")) olderChat = emptyList()
        lastSync = at
        serverAt = at
        stampAt = SystemClock.elapsedRealtime()
        room = state
        connected = true
    }
    private fun roomData() = JSONObject().put("code", room?.text("code").orEmpty())
    private fun apiOrThrow() = api ?: throw ArenaProblem("arena_service")
    private suspend fun request(action: String, data: JSONObject = JSONObject()): Any {
        val result = try { apiOrThrow().db(access(), action, data) }
        catch (problem: ArenaProblem) {
            if (problem.status != 401) throw problem
            apiOrThrow().db(access(force = true), action, data)
        }
        currentCoroutineContext().ensureActive()
        return result
    }
    private suspend fun sendAi(path: String, data: JSONObject) = apiOrThrow().ai(access(), path, data)
    private suspend fun access(force: Boolean = false): String = tokenLock.withLock {
        val current = tokens ?: throw ArenaProblem("arena_auth", 401)
        if (!force && System.currentTimeMillis() < current.expires - 60_000) return@withLock current.access
        val before = generation
        val renewed = apiOrThrow().refresh(current.refresh)
        if (before != generation || current.id != renewed.id) throw ArenaProblem("arena_auth", 401)
        storeTokens(renewed)
        renewed.access
    }

    private fun run(task: suspend () -> Unit) {
        if (busy) return
        busy = true
        status = ""
        actionJob = job(task, { status = it }, { busy = false })
    }

    private fun runAi(task: suspend () -> Unit) {
        if (aiBusy) return
        aiBusy = true
        aiStatus = ""
        aiJob = job(task, { aiStatus = it }, { aiBusy = false })
    }

    private fun job(task: suspend () -> Unit, message: (String) -> Unit, finish: () -> Unit): Job {
        val before = generation
        return viewModelScope.launch {
            try { task() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (problem: Exception) {
                if (before == generation) {
                    if (problem is ArenaProblem && problem.status == 401 && signedIn) signOut()
                    message((problem as? ArenaProblem ?: ArenaProblem("arena_service")).description)
                }
            } finally { if (before == generation) finish() }
        }
    }

    private fun readEndpoints(): ArenaEndpoints? = runCatching {
        ArenaEndpoints.validated(preferences.getString("supabase", "").orEmpty(), preferences.getString("public", "").orEmpty(),
            preferences.getString("worker", "").orEmpty())
    }.getOrNull()

    private fun readTokens(): ArenaTokens? = runCatching {
        val stored = preferences.getString("session", null) ?: return null
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, localKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        val data = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
        ArenaTokens(data.getString("access"), data.getString("refresh"), data.getLong("expires"), data.getString("id"))
    }.getOrNull()

    private fun storeTokens(value: ArenaTokens) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, localKey())
        val payload = JSONObject().put("access", value.access).put("refresh", value.refresh).put("expires", value.expires)
            .put("id", value.id).toString().toByteArray(Charsets.UTF_8)
        val stored = Base64.encodeToString(cipher.iv + cipher.doFinal(payload), Base64.NO_WRAP)
        preferences.edit().putString("session", stored).apply()
        tokens = value
    }

    private fun localKey(): SecretKey {
        val alias = "wordquest_arena_session"
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    override fun onCleared() {
        pause()
        api?.close()
        super.onCleared()
    }
}
