package com.kelimebulmacasi.a1

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * Per-room, opt-in avatar thumbnail transport over the ALREADY USED Firebase RTDB.
 * No new Firebase Storage bucket, new paid API or upload of the original ZIP.
 * The room is readable by authenticated host/guest only, subject to deployed RTDB rules.
 */
internal object Wq183ArenaShare {
    private const val MAX_TEXT = 18_050
    private const val MAX_RAW = 13_000
    private const val MAX_EDGE = 160
    private const val PREFS = "wq183_arena_mascot_consent"
    private val remote = mutableStateMapOf<String, Bitmap>()

    private fun prefKey(profile: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(profile.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }.take(32)

    fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun consent(context: Context, profile: String): Boolean =
        profile.isNotBlank() && preferences(context).getBoolean(prefKey(profile), false)

    fun setConsent(context: Context, profile: String, enabled: Boolean) {
        require(profile.isNotBlank())
        preferences(context).edit().putBoolean(prefKey(profile), enabled).apply()
    }

    fun consentKey(profile: String): String = prefKey(profile)

    fun remoteImage(id: String): Bitmap? =
        if (Wq183MascotStore.isCustomId(id)) remote[id] else null

    fun removeRemote(id: String?) {
        if (id != null) remote.remove(id)
    }

    /** Only decode bounded, valid WebP content that matches an exact room avatar ID. */
    fun decodePreview(avatarId: String, payload: String?): Bitmap? {
        if (!Wq183MascotStore.isCustomId(avatarId) ||
            payload.isNullOrBlank() || payload.length > MAX_TEXT ||
            !payload.startsWith("$avatarId:")) return null
        val base64 = payload.substring(avatarId.length + 1)
        if (base64.length !in 100..17_500 ||
            !base64.matches(Regex("[A-Za-z0-9+/=]+"))) return null
        return runCatching {
            val raw = Base64.decode(base64, Base64.NO_WRAP)
            if (raw.size !in 80..MAX_RAW) return@runCatching null
            if (String(raw, 0, 4, Charsets.US_ASCII) != "RIFF" ||
                String(raw, 8, 4, Charsets.US_ASCII) != "WEBP") return@runCatching null
            val check = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(raw, 0, raw.size, check)
            if (check.outWidth !in 1..MAX_EDGE || check.outHeight !in 1..MAX_EDGE)
                return@runCatching null
            BitmapFactory.decodeByteArray(raw, 0, raw.size)
        }.getOrNull()
    }

    /** Original source remains local; export only a strict 160px-or-less lossy WebP. */
    fun encodePreview(id: String, bitmap: Bitmap): String? {
        if (!Wq183MascotStore.isCustomId(id) ||
            bitmap.width !in 1..2048 || bitmap.height !in 1..2048) return null
        return runCatching {
            for (edge in listOf(160, 128, 96, 80)) {
                val scale = minOf(1f, edge.toFloat() / maxOf(bitmap.width, bitmap.height))
                val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val small = Bitmap.createScaledBitmap(bitmap, width, height, true)
                try {
                    for (quality in listOf(65, 48, 32)) {
                        val buffer = ByteArrayOutputStream()
                        if (!small.compress(Bitmap.CompressFormat.WEBP, quality, buffer))
                            continue
                        val raw = buffer.toByteArray()
                        if (raw.size in 80..MAX_RAW) {
                            val encoded = "$id:" + Base64.encodeToString(raw, Base64.NO_WRAP)
                            if (encoded.length <= MAX_TEXT) return@runCatching encoded
                        }
                    }
                } finally {
                    if (small !== bitmap) small.recycle()
                }
            }
            null
        }.getOrNull()
    }

    fun receive(id: String, data: String?) {
        val decoded = decodePreview(id, data)
        if (decoded == null) remote.remove(id) else remote[id] = decoded
    }

    fun side(match: PremiumOnlineMatch, uid: String): String? = when(uid) {
        match.hostUid -> "host"
        match.guestUid -> "guest"
        else -> null
    }

    fun previewField(side: String): String = if (side == "host")
        "hostMascotPreview" else "guestMascotPreview"
}

@Composable
internal fun rememberWq183ShareConsent(profileKey: String): State<Boolean> {
    val context = LocalContext.current
    val prefs = remember(context.applicationContext) { Wq183ArenaShare.preferences(context) }
    val key = remember(profileKey) { Wq183ArenaShare.consentKey(profileKey) }
    val state = remember(profileKey, key) {
        mutableStateOf(Wq183ArenaShare.consent(context, profileKey))
    }
    DisposableEffect(prefs, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key)
                state.value = Wq183ArenaShare.consent(context, profileKey)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

/** Side-effect bridge: attach only to an AUTHENTICATED match participant. */
@Composable
internal fun Wq183ArenaShareBridge(match: PremiumOnlineMatch?, session: AppSession) {
    val context = LocalContext.current
    val selected by rememberWq2DSelection(session.profileKey)
    val allowed by rememberWq183ShareConsent(session.profileKey)
    val uid = session.accounts.uid.orEmpty()
    val side = if (match != null) Wq183ArenaShare.side(match, uid) else null
    val myId = if (side == "host") match?.hostAvatar else match?.guestAvatar
    val rivalId = if (side == "host") match?.guestAvatar else match?.hostAvatar
    val otherSide = if (side == "host") "guest" else "host"
    val room = remember(match?.code, uid, side) {
        if (side != null && match != null && match.code.isNotBlank())
            session.accounts.socialDatabase()
                ?.reference?.child("premiumRooms")?.child(match.code)
        else null
    }
    val myField = if (side != null) Wq183ArenaShare.previewField(side) else null
    val otherField = Wq183ArenaShare.previewField(otherSide)

    DisposableEffect(room, otherField, rivalId) {
        val source: DatabaseReference? =
            if (room != null && Wq183MascotStore.isCustomId(rivalId))
                room.child(otherField) else null
        val listener = if (source != null && rivalId != null)
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Wq183ArenaShare.receive(
                        rivalId, snapshot.getValue(String::class.java))
                }
                override fun onCancelled(error: DatabaseError) {
                    Wq183ArenaShare.removeRemote(rivalId)
                }
            } else null
        if (source != null && listener != null) source.addValueEventListener(listener)
        onDispose {
            if (source != null && listener != null) source.removeEventListener(listener)
            Wq183ArenaShare.removeRemote(rivalId)
        }
    }

    // A denied write is NON-FATAL: both players still use the built-in piko image.
    LaunchedEffect(room, myField, myId, selected, allowed) {
        if (room == null || myField == null) return@LaunchedEffect
        val node = room.child(myField)
        if (!allowed || !Wq183MascotStore.isCustomId(selected)) {
            // A user who withdraws consent can retract an existing room preview.
            node.removeValue()
            node.onDisconnect().cancel()
            return@LaunchedEffect
        }
        // Avatar field is server-authored and owner-only. Only publish when it matches.
        if (myId != selected) return@LaunchedEffect
        val value = withContext(Dispatchers.Default) {
            val image = Wq183MascotStore.picture(context, selected, false)
            image?.let { Wq183ArenaShare.encodePreview(selected, it) }
        } ?: return@LaunchedEffect
        node.onDisconnect().removeValue()
        node.setValue(value)
    }

    // Clear owned preview if the player explicitly leaves a room or closes screen.
    DisposableEffect(room, myField) {
        onDispose {
            if (room != null && myField != null) {
                room.child(myField).removeValue()
                room.child(myField).onDisconnect().cancel()
            }
        }
    }
}
