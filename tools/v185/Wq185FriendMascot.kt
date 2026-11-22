package com.kelimebulmacasi.a1

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/** Explicitly opted-in thumbnail only. Source ZIPs never leave the device. */
internal object Wq185FriendMascot {
    private const val PREFS = "wq185_friend_mascot_sharing"
    private fun key(profile: String) = MessageDigest.getInstance("SHA-256")
        .digest(profile.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }.take(32)

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun consent(context: Context, profile: String): Boolean =
        profile.isNotBlank() && prefs(context).getBoolean(key(profile), false)

    fun setConsent(context: Context, profile: String, value: Boolean) {
        require(profile.isNotBlank())
        prefs(context).edit().putBoolean(key(profile), value).apply()
    }

    fun keyFor(profile: String) = key(profile)

    /** Guard against displaying arbitrary unbounded network images. */
    fun decode(preview: String?): Bitmap? {
        if (preview == null || preview.length !in 100..18050) return null
        val id = preview.substringBefore(':')
        if (!Wq183MascotStore.isCustomId(id)) return null
        return Wq183ArenaShare.decodePreview(id, preview)
    }

    fun makePreview(context: Context, profile: String, avatar: String): String? {
        if (!consent(context, profile) || !Wq183MascotStore.isCustomId(avatar) ||
            !Wq183MascotStore.exists(context, profile, avatar)) return null
        val source = Wq183MascotStore.picture(context, avatar, false) ?: return null
        return try { Wq183ArenaShare.encodePreview(avatar, source) }
               finally { source.recycle() }
    }

    fun validPreview(avatar: String, preview: String?): Boolean =
        Wq183MascotStore.isCustomId(avatar) && preview != null &&
        preview.length in 100..18050 && preview.startsWith("$avatar:") &&
        decode(preview) != null
}

@Composable
internal fun rememberWq185FriendConsent(profileKey: String): State<Boolean> {
    val context = LocalContext.current
    val prefs = remember(context.applicationContext) { Wq185FriendMascot.prefs(context) }
    val prefKey = remember(profileKey) { Wq185FriendMascot.keyFor(profileKey) }
    val value = remember(profileKey) {
        mutableStateOf(Wq185FriendMascot.consent(context, profileKey))
    }
    DisposableEffect(prefs, prefKey) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == prefKey)
                value.value = Wq185FriendMascot.consent(context, profileKey)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return value
}

/** Reactive when mascot/consent changes, plus periodic minimal friend overview. */
@Composable
internal fun Wq185FriendProfilePublisher(session: AppSession) {
    val context = LocalContext.current
    val selected by rememberWq2DSelection(session.profileKey)
    val consent by rememberWq185FriendConsent(session.profileKey)
    LaunchedEffect(session.accountName, session.profileKey, selected, consent) {
        while (session.accountName != null && session.social.enabled) {
            val preview = if (consent && Wq183MascotStore.isCustomId(selected))
                withContext(Dispatchers.Default) {
                    Wq185FriendMascot.makePreview(context, session.profileKey, selected)
                } else null
            // Custom art has a separate optional field; builtin avatar remains
            // valid in older friendProfiles rules. Removing consent drops the field.
            session.social.publishOverview(
                selected, session.totalPoints, session.finishedCount, preview)
            delay(120000L)
        }
    }
}

@Composable
internal fun Wq185FriendSharingSetting(profileKey: String, online: Boolean) {
    val context = LocalContext.current
    val consent by rememberWq185FriendConsent(profileKey)
    Row(
        Modifier.fillMaxWidth().padding(vertical=5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Maskotumu arkadaşlarıma göster",
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Yalnızca karşılıklı arkadaşların küçük bir önizleme görür. " +
                "Asıl ZIP paylaşılmaz. Dilediğin zaman kapatabilirsin.",
                fontSize = 11.sp, color = Color(0xFF63576C))
        }
        Switch(checked = consent && online, enabled = online,
            onCheckedChange = { Wq185FriendMascot.setConsent(context, profileKey, it) })
    }
}

@Composable
internal fun Wq185ProfileSelector(selected: String, onClick: () -> Unit) {
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    val custom = Wq183MascotStore.isCustomId(selected)
    val portraitHeight = if (custom) {
        if (tablet) 305.dp else 242.dp
    } else if (tablet) 200.dp else 156.dp
    val portraitWidth = if (custom) {
        if (tablet) 198.dp else 150.dp
    } else if (tablet) 170.dp else 125.dp
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), color = Color(0xFFF2EAFB)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp, vertical=4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Wq2DPortrait(selected, Modifier.width(portraitWidth).height(portraitHeight))
            Column(Modifier.weight(1f),
                verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("Seçili maskot", fontWeight=FontWeight.Bold,
                    fontSize=16.sp, color=Color(0xFF36205F))
                Text(Wq2DMascots.label(selected), fontSize=13.sp,
                    color=Color(0xFF655183))
                Text("Karakteri değiştir  ✎", fontSize=12.sp,
                    color=Color(0xFF395B57))
            }
        }
    }
}
