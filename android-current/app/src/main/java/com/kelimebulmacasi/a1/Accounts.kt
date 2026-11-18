package com.kelimebulmacasi.a1

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/** Firebase Realtime Database keys cannot contain ./#$[]; hash arbitrary game IDs safely. */
internal object CloudSyncKey {
    fun forProgress(gameId: String, deviceId: String): String = hex(gameId) + "_" + hex(deviceId).take(16)
    fun forCustom(gameId: String, deviceId: String): String = hex(gameId) + "_" + hex(deviceId).take(16)
    private fun hex(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

internal object CloudSyncMessage {
    const val pending = "Bulut bağlantısı kuruluyor. Kayıtlar cihazında güvende."
    const val offline = "İnternet bağlantısı yok. Kayıtlar cihazda saklanıyor."
    const val downloading = "Bulut kayıtları okunuyor…"
    const val ready = "Hesapla eşitlendi."
    const val writing = "Kayıtlar buluta gönderiliyor…"
    const val noPermission = "Firebase kullanıcı kayıtları için izin vermiyor. Kayıtlar cihazda güvende; Database → Rules ayarını kontrol et."
    const val failed = "Bulut eşitleme başarısız. Kayıtlar cihazda güvende."
    const val slow = "Bulut yanıtı gecikiyor. Kayıtlar cihazda güvende."
}

class Accounts(private val context: Context) {
    private val oauthSettings = context.getSharedPreferences("wordquest_oauth_config", Context.MODE_PRIVATE)
    val clientId: String get() = oauthSettings.getString("web_client_id", "").orEmpty().trim()
        .ifBlank { context.getString(R.string.cloud_web_client_id).trim() }
    fun configureWebId(webId: String): Boolean {
        val valid = webId.trim().matches(Regex("^[0-9]+-[A-Za-z0-9-]+\\.apps\\.googleusercontent\\.com$"))
        if (valid) oauthSettings.edit().putString("web_client_id", webId.trim()).apply()
        return valid
    }
    private val project = context.getString(R.string.cloud_project_id).trim()
    private val appId = context.getString(R.string.cloud_application_id).trim()
    private val key = context.getString(R.string.cloud_api_key).trim()
    private val databaseUrl = context.getString(R.string.cloud_database_url).trim()
    private val credentials by lazy { CredentialManager.create(context) }

    private val app = if (listOf(project, appId, key).any(String::isBlank)) null else {
        FirebaseApp.getApps(context).firstOrNull { it.name == "KelimeBulmacasi" }
            ?: FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                .setProjectId(project).setApplicationId(appId).setApiKey(key)
                .setDatabaseUrl(databaseUrl).build(), "KelimeBulmacasi")
    }
    private val auth = app?.let(FirebaseAuth::getInstance)
    // The user's configured service is Realtime Database, not Cloud Firestore.
    private val database: FirebaseDatabase? = app?.takeIf { databaseUrl.startsWith("https://") }
        ?.let { runCatching { FirebaseDatabase.getInstance(it, databaseUrl) }.getOrNull() }

    val available get() = auth != null
    val uid get() = auth?.currentUser?.uid
    val displayName get() = auth?.currentUser?.displayName ?: auth?.currentUser?.email
    val isGuest get() = auth?.currentUser?.isAnonymous == true
    val email get() = auth?.currentUser?.email
    internal fun socialDatabase(): FirebaseDatabase? = database

    /** Passwords go directly to Firebase Authentication, never SharedPreferences or RTDB. */
    suspend fun signInWithPassword(email: String, password: String): String? {
        val service = auth ?: return "Firebase bağlantısı bulunamadı."
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || password.isBlank())
            return "Geçerli e-posta ve şifre gir."
        return try {
            service.signInWithEmailAndPassword(email.trim(), password).awaitResult()
            null
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            "Giriş yapılamadı. E-posta ve şifreni kontrol et."
        }
    }

    suspend fun registerWithPassword(email: String, password: String): String? {
        val service = auth ?: return "Firebase bağlantısı bulunamadı."
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) return "Geçerli e-posta gir."
        if (password.length < 8) return "Şifre en az 8 karakter olmalı."
        return try {
            if (service.currentUser?.isAnonymous == true) {
                service.currentUser!!.linkWithCredential(
                    EmailAuthProvider.getCredential(email.trim(), password)
                ).awaitResult()
            } else service.createUserWithEmailAndPassword(email.trim(), password).awaitResult()
            null
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            "Kayıt yapılamadı. E-posta kullanımda olabilir; Firebase'de E-posta/Şifre girişini etkinleştir."
        }
    }

    @SuppressLint("CredentialManagerSignInWithGoogle")
    suspend fun signIn(): String? {
        val service = auth ?: return "Google girişi için Firebase yapılandırması gerekiyor."
        if (clientId.isBlank()) return "Google ile giriş için Firebase OAuth web istemcisi yapılandırılmamış."
        return try {
            val option = GetSignInWithGoogleOption.Builder(clientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = credentials.getCredential(context, request).credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                "Google hesabı doğrulanamadı."
            } else {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val google = GoogleAuthProvider.getCredential(token, null)
                if (service.currentUser?.isAnonymous == true) {
                    try { service.currentUser!!.linkWithCredential(google).awaitResult() }
                    catch (_: Exception) { service.signInWithCredential(google).awaitResult() }
                } else service.signInWithCredential(google).awaitResult()
                null
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: GetCredentialCancellationException) {
            null
        } catch (_: Exception) {
            "Google girişi tamamlanamadı. Bağlantını kontrol edip tekrar dene."
        }
    }

    fun signOut(scope: CoroutineScope) {
        auth?.signOut()
        scope.launch { runCatching { credentials.clearCredentialState(ClearCredentialStateRequest()) } }
    }

    /** Single-tap deletion: never opens the Google account chooser.
     * Firebase may reject stale authentication. In that case STOP before touching cloud data;
     * do not claim success or silently log the user out.
     */
    internal suspend fun deleteWordQuestAccount(): AccountDeletionResult {
        val service = auth ?: return AccountDeletionResult.Failed("Firebase hesabı kullanılamıyor.")
        val current = service.currentUser ?: return AccountDeletionResult.Failed("Oturum açık değil.")
        if (current.isAnonymous) return AccountDeletionResult.Failed("Misafir hesabında silinecek kayıtlı hesap yok.")
        val userId = current.uid
        val signedAt = current.metadata?.lastSignInTimestamp ?: 0L
        if (signedAt == 0L || System.currentTimeMillis() - signedAt > 4 * 60 * 1000L)
            return AccountDeletionResult.Failed("Güvenli silme için son girişin çok eski. Hesabından çıkıp yeniden giriş yaptıktan sonra tekrar dene.")
        if (service.currentUser?.uid != userId)
            return AccountDeletionResult.Failed("Oturum değişti. Silme durduruldu.")
        val db = database ?: return AccountDeletionResult.Failed("İnternet bağlantısı veya Firebase veritabanı yok.")
        try {
            // Remove reciprocal friendships, tracked outgoing requests and invitations first.
            // We only delete paths that RTDB security rules permit the account owner to remove.
            val root = db.reference
            val friendSnapshot = root.child("friends").child(userId).get().awaitResult()
            val outgoingSnapshot = root.child("friendOutbox").child(userId).get().awaitResult()
            val friends = friendSnapshot.children.mapNotNull { it.key }
            val recipients = outgoingSnapshot.children.mapNotNull { it.key }
            val deletes = mutableMapOf<String, Any?>(
                "/users/$userId" to null,
                "/directory/$userId" to null,
                "/friendRequests/$userId" to null,
                "/friendOutbox/$userId" to null,
                "/friends/$userId" to null,
                "/roomInvites/$userId" to null,
            )
            friends.forEach { friendUid ->
                deletes["/friends/$friendUid/$userId"] = null
                deletes["/roomInvites/$friendUid/$userId"] = null
            }
            recipients.forEach { recipient ->
                deletes["/friendRequests/$recipient/$userId"] = null
            }
            root.updateChildren(deletes).awaitResult()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return AccountDeletionResult.Failed("Bulut verileri silinemedi. Hesap korunuyor; Firebase kurallarını kontrol et.")
        }
        try {
            current.delete().awaitResult()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            return AccountDeletionResult.Failed("Firebase tekrar giriş gerektiriyor. Bulut verileri temizlendi ancak hesap silinemedi. Yeniden giriş yapıp tekrar dene.", cloudCleared = true)
        } catch (_: Exception) {
            return AccountDeletionResult.Failed("Bulut kayıtları temizlendi, ancak kimlik silinemedi. Tekrar giriş yapıp dene.", cloudCleared = true)
        }
        runCatching { credentials.clearCredentialState(ClearCredentialStateRequest()) }
        service.signOut()
        return AccountDeletionResult.Success(userId)
    }

    fun connect(
        uid: String,
        store: PlayerStore,
        scope: CoroutineScope,
        onChange: () -> Unit,
        onStatus: (String) -> Unit,
    ): CloudSync? = database?.let { db ->
        val preferences = context.getSharedPreferences("cloud_install", Context.MODE_PRIVATE)
        val device = preferences.getString("id", null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString("id", it).apply()
        }
        CloudSync(db, uid, device, store, scope, onChange, onStatus)
    }
}

internal object AccountDeletionCopy {
    const val prompt = "Hesabını kalıcı olarak silmeyi onaylıyor musun?"
    const val confirm = "Evet, sil"
    const val cancel = "Vazgeç"
}

internal sealed interface AccountDeletionResult {
    data class Success(val uid: String) : AccountDeletionResult
    data class Failed(val message: String, val cloudCleared: Boolean = false) : AccountDeletionResult
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resumeWith(Result.success(it)) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWith(Result.failure(it)) }
        addOnCanceledListener { continuation.cancel() }
    }

/**
 * Realtime Database user sync. Each device owns its own per-game record to avoid overwrite
 * races. Shared progress is merged locally using the existing Progress.merge implementation.
 * Server rules MUST allow only auth.uid to read/write /users/$uid.
 */
class CloudSync(
    db: FirebaseDatabase,
    uid: String,
    private val device: String,
    private val store: PlayerStore,
    private val scope: CoroutineScope,
    private val onChange: () -> Unit,
    private val onStatus: (String) -> Unit,
) {
    private val user = db.reference.child("users").child(uid)
    private val listeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()
    private val pending = mutableSetOf<String>()
    private var batch: Job? = null
    private var stalled: Job? = null
    private var remoteProgressReady = false
    private var remoteCustomReady = false
    private var remoteSessionReady = false
    private var connected = false
    private var everConnected = false
    private var writesInFlight = 0
    private var syncFailed: String? = null
    private var closed = false
    private var slow = false

    private fun reportStatus() {
        if (closed) return
        onStatus(when {
            syncFailed != null -> syncFailed!!
            !connected -> if (everConnected) CloudSyncMessage.offline else CloudSyncMessage.pending
            !remoteProgressReady || !remoteCustomReady || !remoteSessionReady ->
                if (slow) CloudSyncMessage.slow else CloudSyncMessage.downloading
            writesInFlight > 0 -> CloudSyncMessage.writing
            else -> CloudSyncMessage.ready
        })
    }

    private fun watch(ref: DatabaseReference, onData: (DataSnapshot) -> Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!closed) onData(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                if (closed) return
                syncFailed = if (error.code == DatabaseError.PERMISSION_DENIED)
                    CloudSyncMessage.noPermission else CloudSyncMessage.failed
                reportStatus()
            }
        }
        ref.addValueEventListener(listener)
        listeners += ref to listener
    }

    init {
        reportStatus()
        // .info/connected distinguishes local cache reads from an active connection.
        watch(db.getReference(".info/connected")) { snapshot ->
            val nowConnected = snapshot.getValue(Boolean::class.java) == true
            if (nowConnected && !connected) {
                everConnected = true
                if (pending.isNotEmpty()) flush()
            }
            connected = nowConnected
            reportStatus()
        }
        watch(user.child("progress")) { snapshot ->
            var changed = false
            for (record in snapshot.children) {
                val id = record.child("gameId").getValue(String::class.java) ?: continue
                val remote = record.child("data").getValue(String::class.java)?.let {
                    runCatching { JSONObject(it).toProgress() }.getOrNull()
                } ?: continue
                val local = store.read(id)
                val merged = local.merge(remote)
                if (merged != local) {
                    store.save(id, merged)
                    changed = true
                }
            }
            if (changed) onChange()
            remoteProgressReady = true
            reportStatus()
        }
        watch(user.child("custom")) { snapshot ->
            val incoming = snapshot.children.mapNotNull { record ->
                record.child("data").getValue(String::class.java)?.let {
                    runCatching { JSONObject(it).toCustomGame() }.getOrNull()
                }
            }
            if (incoming.isNotEmpty() && store.saveCustom(incoming)) onChange()
            remoteCustomReady = true
            reportStatus()
        }
        watch(user.child("sessions")) { snapshot ->
            val latest = snapshot.children.maxByOrNull { it.child("openedAt").getValue(Long::class.java) ?: 0L }
            val openedAt = latest?.child("openedAt")?.getValue(Long::class.java) ?: 0L
            if (openedAt > store.lastGameAt) {
                store.recordLastGame(latest?.child("gameId")?.getValue(String::class.java), openedAt)
                onChange()
            }
            remoteSessionReady = true
            reportStatus()
        }
        // Never leave the UI indefinitely saying "syncing" when server access is denied/unavailable.
        stalled = scope.launch {
            delay(15_000)
            if (!closed && (!connected || !remoteProgressReady || !remoteCustomReady || !remoteSessionReady)) {
                slow = true
                reportStatus()
            }
        }
        store.progress().keys.forEach(pending::add)
        flush()
        store.savedGames().forEach(::saveCustom)
        saveSession()
    }

    private fun write(reference: DatabaseReference, value: Any) {
        if (closed) return
        writesInFlight++
        reportStatus()
        reference.setValue(value) { error, _ ->
            if (closed) return@setValue
            writesInFlight = (writesInFlight - 1).coerceAtLeast(0)
            if (error != null) {
                syncFailed = if (error.code == DatabaseError.PERMISSION_DENIED)
                    CloudSyncMessage.noPermission else CloudSyncMessage.failed
            }
            reportStatus()
        }
    }

    fun schedule(gameId: String) {
        if (closed) return
        pending.add(gameId)
        batch?.cancel()
        batch = scope.launch {
            delay(700)
            flush()
        }
    }

    fun flush() {
        if (closed) return
        batch?.cancel()
        batch = null
        for (id in pending.toList()) {
            val progress = store.read(id)
            write(user.child("progress").child(CloudSyncKey.forProgress(id, device)),
                mapOf("gameId" to id, "data" to progress.toJson().toString()))
        }
        pending.clear()
    }

    fun saveCustom(game: Game) {
        write(user.child("custom").child(CloudSyncKey.forCustom(game.id, device)),
            mapOf("data" to game.toJson().toString()))
    }

    fun saveSession() {
        val id = store.lastGame ?: return
        write(user.child("sessions").child(CloudSyncKey.forProgress("session", device)),
            mapOf("gameId" to id, "openedAt" to store.lastGameAt))
    }

    fun close() {
        // Local data persists even when a pending server write does not complete.
        batch?.cancel()
        stalled?.cancel()
        closed = true
        listeners.forEach { (reference, listener) -> reference.removeEventListener(listener) }
        listeners.clear()
    }
}
