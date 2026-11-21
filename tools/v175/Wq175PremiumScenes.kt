package com.kelimebulmacasi.a1

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/** Real approved scene names from the user's ZIP, never from rejected later mockups. */
internal object Wq175PremiumScenes {
    fun file(slug: String): String? = when (slug) {
        "ana_sayfa" -> "01_ana_sayfa_calisma_odasi"
        "arena" -> "02_arena_turnuva_sahnesi"
        "yds_akademi" -> "03_yds_akademi_kutuphane"
        "bulmacalarim" -> "04_bulmacalar_yaratici_atolye"
        "hazir_paketler" -> "05_paketler_kitaplik"
        "gunluk_plan", "gunluk_planim" -> "06_gunluk_plan_basari_odasi"
        "kelime_hafizam", "eslestirmelerim" -> "07_kelime_kartlari_kitaplik"
        "yeni_oyun" -> "08_kelime_oyunu_harfler"
        "calisma" -> "09_calisma_odasi_dikey"
        "konusma_asistani" -> "10_ai_asistan_ogrenme_sahnesi"
        "kelime_yaz" -> "12_bulmacalar_yazi_alani_sag"
        "ilerleme" -> "14_ilerleme_basari_kosesi"
        else -> null
    }
}

/** Artwork remains decorative and separate from accessible, clickable Compose content.
 * A dark scrim is essential for legible labels on colorful 3D backgrounds.
 */
@Composable
internal fun Wq175SceneBackdrop(
    slug: String,
    modifier: Modifier = Modifier,
    darkScrim: Boolean = true
) {
    val context = LocalContext.current
    val file = Wq175PremiumScenes.file(slug)
    val bitmap = remember(context, file) {
        runCatching {
            file?.let { name ->
                context.assets.open("wq175_user_art/scenes/$name.webp").use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }
        }.getOrNull()
    }
    Box(modifier) {
        if(bitmap != null) {
            Image(bitmap=bitmap,contentDescription=null,
                modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(
                    Color(0xFF123D56),Color(0xFF6750A5),Color(0xFFAA81C9)))))
        }
        if(darkScrim) Box(Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(
                Color(0xC7111A36),Color(0xA3281E52),
                Color(0x40201844),Color(0x080B1839)))))
    }
}
