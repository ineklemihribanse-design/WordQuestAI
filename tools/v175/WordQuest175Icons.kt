package com.kelimebulmacasi.a1

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Assets generated at build time, not screenshots/emoji. Bitmap decode cached
 * per icon and density-independent Compose sizing keeps mobile views crisp.
 */
@Composable
internal fun Wq175GlossIcon(
    name: String, modifier: Modifier = Modifier.size(48.dp)
) {
    val context = LocalContext.current
    val image = remember(context, name) {
        runCatching {
            context.assets.open("wq175_icons/" + name + ".webp").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (image != null) {
        Image(bitmap = image, contentDescription = null,
            modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        Box(modifier.background(Color(0xFFEAE5F8), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) {
            Text("•", color = Color(0xFF6654A2))
        }
    }
}

internal object Wq175IconNames {
    fun menu(title: String): String {
        val s = title.lowercase(java.util.Locale.ROOT).replace("\u0307", "")
        return when {
            "öğrenme merkez" in s || "öneri" in s -> "insights"
            "ilerleme" in s || "istatistik" in s || "puanlarım" in s -> "analytics"
            "arkadaş" in s -> "friends"
            "profil" in s || "hesap" in s -> "account"
            "ayar" in s -> "settings"
            "yedek" in s || "dışa aktar" in s -> "backup"
            "geçmiş" in s -> "history"
            "sözlük" in s || "kelimelerim" in s -> "library"
            "aktif gün" in s || "seri" in s -> "streak"
            "mesaj" in s || "sohbet" in s -> "messages"
            "yanlışlarımdan" in s -> "mistakes"
            "kişisel ai öğretmen" in s -> "ai_teacher"
            "pdf" in s || "belge" in s || "dosya" in s -> "pdf"
            "kamera" in s -> "camera"
            "tarama" in s || "fotoğraf" in s || "görsel" in s -> "scan"
            "paragraf" in s || "okuma" in s -> "paragraph"
            "çeviri" in s -> "translate"
            "yakın anlam" in s || "eş anlam" in s -> "synonym"
            "tamamlama" in s -> "completion"
            "mini deneme" in s || "tam deneme" in s || "deneme" in s -> "exam"
            "yanlış" in s || "hata analiz" in s -> "review"
            "defter" in s -> "notebook"
            "süre" in s || "zamanlı" in s -> "timer"
            "başarı" in s || "sonuç" in s -> "success"
            "arena" in s || "yarış" in s -> "trophy"
            "yds" in s || "sınav" in s -> "yds"
            "kelime hafıza" in s || "tekrar" in s -> "cards"
            "konuşma" in s || "asistan" in s -> "assistant"
            "günlük" in s || "plan" in s -> "plan"
            "paket" in s -> "packages"
            "fiil" in s -> "verbs"
            "bulmaca" in s || "oyun" in s -> "puzzle"
            "gramer" in s || "dilbilgisi" in s -> "grammar"
            "hikâye" in s -> "story"
            "dinle" in s -> "listen"
            "cümle" in s -> "sentence"
            "başarı" in s -> "trophy"
            else -> "cards"
        }
    }
    fun practice(kind: PracticeKind) = when(kind) {
        PracticeKind.Dictation -> "listen"
        PracticeKind.Sentence -> "sentence"
        PracticeKind.Grammar -> "grammar"
        PracticeKind.Story -> "story"
        PracticeKind.Speak -> "speak"
    }
}
