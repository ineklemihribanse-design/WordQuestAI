package com.kelimebulmacasi.a1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.max

private val LenaInk = Color(0xFF124D31)
private val LenaMint = Color(0xFFE2F6E6)
private val LenaBorder = Color(0xFFCFE4D7)
private val LenaText = Color(0xFF192C24)
private val LenaMuted = Color(0xFF64776C)

private fun portraitFile(context: Context, profileKey: String): File {
    // File name contains no user-provided path, avoids traversal and
    // isolates one player's chosen picture from another player's picture.
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(profileKey.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    return File(File(context.filesDir, "lena_messenger_photos"), "$hash.png")
}

private fun loadPortrait(context: Context, profileKey: String): ImageBitmap? =
    runCatching {
        val file = portraitFile(context, profileKey)
        if (!file.exists()) return@runCatching null
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    }.getOrNull()

@Composable
internal fun rememberLenaUserPortrait(profileKey: String): MutableState<ImageBitmap?> {
    val context = LocalContext.current.applicationContext
    return remember(context, profileKey) {
        mutableStateOf(loadPortrait(context, profileKey))
    }
}

private fun saveChosenPortrait(context: Context, profileKey: String, uri: Uri): ImageBitmap {
    val resolver = context.contentResolver
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = resolver.openInputStream(uri)
        ?: throw IllegalArgumentException("Fotoğraf açılamadı")
    boundsStream.use { BitmapFactory.decodeStream(it, null, opts) }
    if (opts.outWidth <= 0 || opts.outHeight <= 0)
        throw IllegalArgumentException("Geçersiz fotoğraf")
    var sample = 1
    while (max(opts.outWidth, opts.outHeight) / sample > 1024) sample *= 2
    val decode = BitmapFactory.Options().apply { inSampleSize = sample }
    val source = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decode) }
        ?: throw IllegalArgumentException("Fotoğraf okunamadı")
    val maxSide = max(source.width, source.height)
    val scale = minOf(1.0f, 512.0f / maxSide)
    val scaled = if (scale >= 1.0f) source else Bitmap.createScaledBitmap(
        source, (source.width * scale).toInt().coerceAtLeast(1),
        (source.height * scale).toInt().coerceAtLeast(1), true
    )
    val dest = portraitFile(context, profileKey)
    dest.parentFile?.mkdirs()
    // Persist photo in app-private files and keep it through APK updates;
    // never upload or share it with the AI provider.
    FileOutputStream(dest).use { output ->
        if (!scaled.compress(Bitmap.CompressFormat.PNG, 95, output))
            throw IllegalStateException("Fotoğraf kaydedilemedi")
    }
    if (source !== scaled) source.recycle()
    return scaled.asImageBitmap()
}

@Composable
internal fun LenaActualAvatar(size: Dp, modifier: Modifier = Modifier) {
    // This image is generated during APK packaging from the actual released
    // 201-bone Lena GLB. Never substitute a generic AI-generated woman.
    Image(
        painter = painterResource(R.drawable.wq167_lena_portrait),
        contentDescription = "Gerçek 3D Lena öğretmeninin profil fotoğrafı",
        modifier = modifier.size(size).clip(CircleShape)
            .border(2.dp, LenaBorder, CircleShape),
        contentScale = ContentScale.Crop,
    )
}

@Composable
internal fun LenaUserAvatar(
    avatar: ImageBitmap?,
    size: Dp,
    modifier: Modifier = Modifier,
    initials: String = "S",
) {
    if (avatar != null) {
        Image(
            bitmap = avatar,
            contentDescription = "Seçtiğin profil fotoğrafı",
            modifier = modifier.size(size).clip(CircleShape)
                .border(2.dp, LenaBorder, CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.size(size).clip(CircleShape)
                .background(Color(0xFFE7EBF7))
                .border(2.dp, LenaBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials.take(2).uppercase(),
                color = LenaInk,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * .32f).sp,
            )
        }
    }
}

@Composable
internal fun LenaMessengerProfileHeader(
    profileName: String,
    profileKey: String,
    aiReady: Boolean,
    userAvatar: MutableState<ImageBitmap?>,
) {
    val context = LocalContext.current.applicationContext
    var photoError by remember(profileKey) { mutableStateOf<String?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching { saveChosenPortrait(context, profileKey, uri) }
                .onSuccess { userAvatar.value = it; photoError = null }
                .onFailure { photoError = "Fotoğraf yüklenemedi. Başka bir görsel deneyebilirsin." }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.horizontalGradient(listOf(Color.White, Color(0xFFF7FBF7), Color(0xFFE8F8EB))))
            .border(1.dp, LenaBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 17.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            LenaActualAvatar(92.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Lena", fontFamily = FontFamily.Serif, fontSize = 29.sp,
                        color = LenaInk, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(9.dp))
                    Text("♛", color = Color(0xFFD1A22D), fontSize = 23.sp)
                }
                Text("İngilizce Öğretmeni · AI Asistan", color = LenaText, fontSize = 12.sp,
                    maxLines = 2)
                Row(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(if (aiReady) Color(0xFFE8FAE8) else Color(0xFFFFF6E9))
                        .border(1.dp, if (aiReady) Color(0xFF9CD5A5) else Color(0xFFEBD7B3),
                            RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape)
                        .background(if (aiReady) Color(0xFF2ACB50) else Color(0xFFCF9B35)))
                    Text(if (aiReady) "Çevrimiçi" else "AI anahtarı gerekli",
                        color = LenaInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(Modifier.width(1.dp).height(66.dp).background(LenaBorder))
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    LenaUserAvatar(
                        avatar = userAvatar.value, size = 64.dp,
                        modifier = Modifier.clickable { pickPhoto.launch("image/*") },
                        initials = profileName.trim().take(1).ifBlank { "S" },
                    )
                    Box(
                        Modifier.align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFFDDF2DE))
                            .border(1.dp, Color.White, CircleShape)
                            .clickable { pickPhoto.launch("image/*") }
                            .padding(5.dp),
                    ) {
                        Image(painterResource(R.drawable.wq167_camera),
                            contentDescription = "Fotoğraf seç",
                            modifier = Modifier.size(14.dp))
                    }
                }
                Text(profileName, color = LenaInk, fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = {
                    if (userAvatar.value != null) {
                        portraitFile(context, profileKey).delete()
                        userAvatar.value = null
                    } else pickPhoto.launch("image/*")
                }, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)) {
                    Text(if (userAvatar.value == null) "Fotoğraf ekle" else "Fotoğrafı kaldır",
                        color = LenaInk, fontSize = 10.sp)
                }
            }
        }
        if (photoError != null) Text(photoError.orEmpty(),
            color = Color(0xFFC64A4A), fontSize = 11.sp)
    }
}

@Composable
private fun LenaModePill(
    name: String,
    iconId: Int,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    val bg = if (selected) Color(0xFF229650) else Color(0xFFFFFEFD)
    val fg = if (selected) Color.White else LenaInk
    val icon: @Composable () -> Unit = {
        Image(
            painter = painterResource(iconId), contentDescription = null,
            colorFilter = if (selected) ColorFilter.tint(Color.White) else null,
            modifier = Modifier.size(19.dp),
        )
    }
    if (onClick == null) {
        Row(
            Modifier.clip(shape).background(bg)
                .border(1.dp, if (selected) Color(0xFF3FBB66) else LenaBorder, shape)
                .padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            icon()
            Text(name, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        OutlinedButton(
            onClick = onClick, shape = shape,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = bg, contentColor = fg,
            ),
            modifier = Modifier.border(1.dp,
                if (selected) Color(0xFF3FBB66) else LenaBorder, shape),
        ) {
            icon()
            Spacer(Modifier.width(7.dp))
            Text(name, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun LenaMessengerModeBar(
    selected: TeacherMode,
    onSelect: (TeacherMode) -> Unit,
    onVocabulary: () -> Unit,
) {
    // The first view only highlights Messages. Choosing a teaching mode
    // visually moves the active highlight to that mode; no double-active pills.
    var showModeHighlight by remember { mutableStateOf(false) }
    // Same order as the approved reference. Modes keep their actual callbacks.
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LenaModePill("Mesajlar", R.drawable.wq167_chat, selected = !showModeHighlight) { showModeHighlight = false }
        LenaModePill("Pratik", R.drawable.wq167_practice, showModeHighlight && selected == TeacherMode.Practice) {
            showModeHighlight = true
            onSelect(TeacherMode.Practice)
        }
        LenaModePill("Dilbilgisi", R.drawable.wq167_grammar, showModeHighlight && selected == TeacherMode.Grammar) {
            showModeHighlight = true
            onSelect(TeacherMode.Grammar)
        }
        LenaModePill("Kelime", R.drawable.wq167_word, onClick = onVocabulary)
        LenaModePill("Kişisel Ders", R.drawable.wq167_lesson, showModeHighlight && selected == TeacherMode.Lesson) {
            showModeHighlight = true
            onSelect(TeacherMode.Lesson)
        }
        LenaModePill("Hata Düzelt", R.drawable.wq167_correction,
            showModeHighlight && selected == TeacherMode.Correction) {
            showModeHighlight = true
            onSelect(TeacherMode.Correction)
        }
    }
}

/** Decorative speaking indicator, never a fabricated TTS progress or duration. */
@Composable
internal fun LenaVoiceWaveform(active: Boolean) {
    val bars = listOf(6, 11, 9, 17, 25, 15, 24, 13, 20, 12, 8, 14, 6)
    Row(
        modifier = Modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bars.forEach { height ->
            Box(
                Modifier.width(3.dp).height((if (active) height else (height / 2 + 3)).dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Color(0xFF2CBA68) else Color(0xFFA7DCB7))
            )
        }
    }
}

/** Functional, duration-free TTS strip. Playback position is not fabricated:
 * Android's device speech engines do not expose a portable elapsed-time API.
 */
@Composable
internal fun LenaMessageAudioBar(
    playing: Boolean,
    speechRate: Float,
    onToggle: () -> Unit,
    onRateChange: () -> Unit,
) {
    val shape=RoundedCornerShape(24.dp)
    Row(
        modifier=Modifier.fillMaxWidth().clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFF5FAE9), Color(0xFFE6F8DC))))
            .border(1.dp, Color(0xFFD7EBD5), shape)
            .padding(horizontal=7.dp, vertical=1.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(7.dp),
    ) {
        TextButton(onClick=onToggle, contentPadding=PaddingValues(3.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF239B48)),
                contentAlignment=Alignment.Center) {
                Image(painterResource(if (playing) R.drawable.wq167_pause
                    else R.drawable.wq167_play), contentDescription=null,
                    colorFilter=ColorFilter.tint(Color.White),
                    modifier=Modifier.size(20.dp))
            }
        }
        LenaVoiceWaveform(playing)
        Text(if (playing) "Durdur" else "Dinle", color=LenaInk,
            fontSize=11.sp, fontWeight=FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        TextButton(onClick=onRateChange,
            contentPadding=PaddingValues(horizontal=5.dp,vertical=2.dp),
            modifier=Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White)) {
            Text(String.format(java.util.Locale.ROOT, "%.1f×", speechRate),
                color=LenaInk,fontSize=11.sp,fontWeight=FontWeight.Bold)
        }
        Image(painterResource(R.drawable.wq167_audio), contentDescription=null,
            modifier=Modifier.size(19.dp))
    }
}

/** A real local bookmark, stored per learner and message, not a decorative icon. */
@Composable
internal fun LenaMessageBookmark(profileKey: String, turnId: Long) {
    val context=LocalContext.current.applicationContext
    val key=remember(profileKey,turnId) {
        MessageDigest.getInstance("SHA-256")
            .digest((profileKey+":"+turnId).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    val store=remember(context) {
        context.getSharedPreferences("wordquest_lena_bookmarks",Context.MODE_PRIVATE)
    }
    var saved by remember(key) { mutableStateOf(store.getBoolean(key,false)) }
    TextButton(onClick={ saved=!saved;store.edit().putBoolean(key,saved).apply() },
        contentPadding=PaddingValues(4.dp)) {
        Image(painterResource(R.drawable.wq167_bookmark),
            colorFilter=if (saved) ColorFilter.tint(Color(0xFF2FB35A)) else null,
            contentDescription=if (saved) "Kaydedildi" else "Mesajı kaydet",
            modifier=Modifier.size(19.dp))
    }
}
