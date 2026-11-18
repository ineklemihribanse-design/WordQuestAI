package com.kelimebulmacasi.a1

import java.io.Closeable
import java.io.IOException
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal const val ARENA_SCHEME = BuildConfig.ARENA_SCHEME
internal val arenaCodePattern = Regex("WQ-[0-9A-F]{8}")
internal fun arenaCode(text: String): String = arenaCodePattern.find(text.uppercase())?.value.orEmpty()
internal fun JSONObject.rows(name: String): List<JSONObject> = optJSONArray(name).objects()
internal fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull(::optJSONObject)
internal fun JSONObject.obj(name: String) = optJSONObject(name) ?: JSONObject()
internal fun JSONObject.text(name: String) = if (isNull(name)) "" else optString(name)

internal data class ArenaEndpoints(val supabase: String, val publicKey: String, val worker: String) {
    companion object {
        fun validated(supabase: String, key: String, worker: String): ArenaEndpoints {
            fun httpsOrigin(value: String, domain: String): String {
                val trimmed = value.trim().trimEnd('/')
                val uri = runCatching { URI(trimmed) }.getOrNull()
                require(uri?.scheme == "https" && uri.host?.endsWith(domain) == true &&
                    uri.userInfo == null && uri.port == -1 && uri.rawPath.isNullOrBlank() &&
                    uri.rawQuery == null && uri.rawFragment == null)
                return trimmed
            }
            val token = key.trim()
            val legacy = runCatching {
                val body = token.split('.')[1]
                JSONObject(String(java.util.Base64.getUrlDecoder().decode(body), Charsets.UTF_8)).optString("role") == "anon"
            }.getOrDefault(false)
            require(token.length in 16..4096 && (token.startsWith("sb_publishable_") || legacy))
            return ArenaEndpoints(httpsOrigin(supabase, ".supabase.co"), token, httpsOrigin(worker, ".workers.dev"))
        }
    }
}

internal data class ArenaTokens(val access: String, val refresh: String, val expires: Long, val id: String) {
    constructor(json: JSONObject) : this(
        json.getString("access_token"), json.getString("refresh_token"),
        System.currentTimeMillis() + json.optLong("expires_in", 3_600) * 1000,
        json.getJSONObject("user").getString("id"),
    )
}

internal enum class ArenaMode(val id: String, val label: String, val caption: String) {
    Fast("fast", "Hızlı Kelime Yarışması", "Aynı ipuçlarıyla İngilizce kelime bul"),
    Duel("duel", "Bulmaca Düellosu", "Aynı kare bulmacayı ayrı tahtalarda çöz"),
    Team("team", "Ortak Bulmaca", "Bir tahtayı birlikte tamamla"),
    Yds("yds", "YDS Düellosu", "Akademik kelimeler ve özgün YDS soruları"),
    Oral("oral", "AI Sözlü Sınavı", "Sırayla İngilizce yanıt ver"),
    Daily("daily", "Günlük Meydan Okuma", "Bugünün ortak B1 paketi");
    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: Fast }
}

internal data class ArenaPack(val mode: ArenaMode, val level: String, val topic: String, val counts: List<Int>) {
    constructor(data: JSONObject) : this(
        ArenaMode.of(data.getString("mode")), data.getString("level"), data.getString("topic"),
        data.getJSONArray("counts").let { a -> List(a.length(), a::getInt) },
    )
}

internal class ArenaProblem(val code: String, val status: Int = 0) : IOException(code) {
    val description: String get() = when (code) {
        "arena_access" -> "Bu hesap yarışma için yetkilendirilmemiş veya bu odaya ait değil."
        "arena_auth" -> "Oturum açılamadı. E-postanı ve şifreni kontrol et."
        "arena_expired" -> "Davet kodunun süresi doldu."
        "arena_missing" -> "Oda bulunamadı. Davet kodunu kontrol et."
        "arena_full" -> "Oda dolu veya yarışma başlamış."
        "arena_pack", "arena_settings" -> "Bu ayarlarla soru paketi bulunamadı."
        "arena_room_limit" -> "Bu saat için oda oluşturma sınırına ulaştın."
        "arena_chat_limit" -> "Çok sık mesaj gönderiyorsun. Bir dakika sonra yeniden dene."
        "arena_quota" -> "Bugünkü AI kotası dolmuş olabilir. Kesin cevaplı yarışmalar devam edebilir."
        "arena_ai_config", "arena_provider" -> "Bu rol için sunucuda etkin bir ücretsiz sağlayıcı ve model seç."
        "arena_ai_down", "arena_ai_empty", "arena_grade" -> "Yapay zekâ değerlendirmesi alınamadı. Yeniden deneyebilirsin."
        "arena_active" -> "Yarışma sırasında öğretmende genel çalışma başlıklarını kullanabilirsin."
        "arena_stage" -> "Bu işlem yarışmanın şu anki aşamasında kullanılamıyor."
        "arena_busy" -> "Hakem zaten bir değerlendirme hazırlıyor."
        "arena_already" -> "Hakemin değerlendirmesi ortak sohbette."
        "arena_turn", "arena_question" -> "Soru değişti. Ekrandaki güncel soruyu kullan."
        "arena_answer" -> "Geçerli bir cevap yaz."
        "arena_message" -> "Mesajı kontrol et ve biraz kısalt."
        "arena_consent" -> "AI değerlendirmesi için veri paylaşımına izin ver."
        "arena_hint" -> "İpucu eğitim modunda kullanılabilir."
        "arena_service" -> "Yarışma sunucusuna ulaşılamadı. Bağlantıyı ve sunucu ayarlarını kontrol et."
        else -> "Bağlantı kurulamadı. İnterneti ve yarışma ayarlarını kontrol et."
    }
}

internal class ArenaApi(private val endpoints: ArenaEndpoints) {
    private val http = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(47, TimeUnit.SECONDS).callTimeout(50, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()

    suspend fun signIn(email: String, password: String) =
        ArenaTokens(JSONObject(auth("password", JSONObject().put("email", email).put("password", password))))

    suspend fun refresh(token: String) = ArenaTokens(JSONObject(auth("refresh_token", JSONObject().put("refresh_token", token))))

    private suspend fun auth(grant: String, data: JSONObject): String = call(
        Request.Builder().url(endpoints.supabase + "/auth/v1/token?grant_type=" + grant)
            .header("apikey", endpoints.publicKey).post(data.toString().toRequestBody(json)).build(), "arena_auth",
    )

    suspend fun db(access: String, action: String, data: JSONObject): Any = org.json.JSONTokener(call(
        Request.Builder().url(endpoints.supabase + "/rest/v1/rpc/arena_rpc")
            .header("apikey", endpoints.publicKey).header("Authorization", "Bearer $access")
            .post(JSONObject().put("action", action).put("data", data).toString().toRequestBody(json)).build(),
    )).nextValue()

    suspend fun health() {
        val data = JSONObject(call(Request.Builder().url(endpoints.worker + "/health").build()))
        if (data.optString("app") != "wordquest-arena" || data.optInt("protocol") != 1) throw ArenaProblem("arena_service")
    }

    suspend fun ai(access: String, path: String, data: JSONObject): JSONObject = JSONObject(call(
        Request.Builder().url(endpoints.worker + "/v1/" + path)
            .header("Authorization", "Bearer $access").post(data.toString().toRequestBody(json)).build(),
    ))

    private suspend fun call(request: Request, defaultError: String = "arena_service"): String = withContext(Dispatchers.IO) {
        http.newCall(request).execute().use { response ->
            val body = response.body ?: throw ArenaProblem(defaultError, response.code)
            val source = body.source()
            if (source.request(524_289)) throw ArenaProblem("arena_service")
            val text = source.readUtf8()
            if (!response.isSuccessful) {
                val details = runCatching { JSONObject(text) }.getOrNull()
                val error = details?.text("error")?.ifBlank { details.text("message") }.orEmpty()
                throw ArenaProblem(
                    if (error.startsWith("arena_")) error else if (response.code == 429) "arena_quota" else defaultError,
                    response.code,
                )
            }
            text
        }
    }

    fun watch(access: String, room: String): ArenaWatch = ArenaWatch(endpoints, http, access, room)
    fun close() {
        http.dispatcher.cancelAll()
        http.connectionPool.evictAll()
    }

    private companion object { val json = "application/json; charset=utf-8".toMediaType() }
}

internal class ArenaWatch(
    endpoints: ArenaEndpoints,
    http: OkHttpClient,
    private val access: String,
    private val room: String,
) : WebSocketListener(), Closeable {
    val changes = Channel<Unit>(Channel.CONFLATED)
    @Volatile var subscribed = false
        private set
    @Volatile var ended = false
        private set
    private val socket: WebSocket
    private var ref = 1L

    init {
        require(room.matches(Regex("[0-9a-f-]{36}")))
        val url = endpoints.supabase.replaceFirst("https:", "wss:") +
            "/realtime/v1/websocket?vsn=1.0.0&apikey=" + endpoints.publicKey
        socket = http.newWebSocket(Request.Builder().url(url).build(), this)
    }
    override fun onOpen(webSocket: WebSocket, response: Response) {
        val filter = JSONObject().put("event", "UPDATE").put("schema", "public")
            .put("table", "arena_events").put("filter", "room_id=eq.$room")
        val config = JSONObject().put("broadcast", JSONObject().put("self", false).put("ack", false))
            .put("presence", JSONObject().put("key", "").put("enabled", false))
            .put("postgres_changes", JSONArray().put(filter)).put("private", false)
        webSocket.send(frame("realtime:arena-$room", "phx_join", JSONObject().put("config", config).put("access_token", access), 1))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        if (text.length > 32_768) return
        val event = runCatching { JSONObject(text) }.getOrNull() ?: return
        when (event.optString("event")) {
            "phx_reply" -> if (event.optString("ref") == "1") {
                subscribed = event.obj("payload").optString("status") == "ok"
                if (subscribed) changes.trySend(Unit) else stop()
            }
            "postgres_changes" -> changes.trySend(Unit)
            "phx_error", "phx_close" -> stop()
        }
    }

    // Supabase uses Phoenix application heartbeats in addition to its WebSocket transport.
    fun pulse() {
        if (!ended) socket.send(frame("phoenix", "heartbeat", JSONObject(), ++ref))
    }

    private fun frame(topic: String, event: String, data: JSONObject, ref: Long) =
        JSONObject().put("topic", topic).put("event", event).put("payload", data).put("ref", ref.toString())
            .put("join_ref", if (topic == "phoenix") JSONObject.NULL else "1").toString()

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = stop()
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = stop()
    private fun stop() {
        ended = true
        subscribed = false
        changes.trySend(Unit)
    }
    override fun close() {
        stop()
        socket.cancel()
        changes.close()
    }
}
