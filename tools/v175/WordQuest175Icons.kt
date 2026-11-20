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
        val s = title.lowercase(java.util.Locale.ROOT)
        return when {
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
            "ilerleme" in s || "başarı" in s -> "trophy"
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
