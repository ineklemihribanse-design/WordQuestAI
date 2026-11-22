package com.kelimebulmacasi.a1

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity

/** Safe optional original user art: phone portrait / wide tablet, both with game mode. */
@Composable
internal fun Wq183ArenaBackdrop(modifier: Modifier = Modifier, active: Boolean = false) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    val w = configuration.screenWidthDp
    val h = configuration.screenHeightDp
    val wideTablet = w >= 600 && h > 0 && w.toFloat() / h >= 1.2f
    val file = if (wideTablet) {
        if (active) "arena_tablet_13_16x10_koyu.webp" else "arena_tablet_13_16x10.webp"
    } else {
        if (active) "arena_telefon_9x19_5_koyu.webp" else "arena_telefon_9x19_5.webp"
    }
    val asset = "arena_v183/$file"
    val image = remember(context.applicationContext, asset, w, h) {
        runCatching {
            context.assets.open(asset).use { stream ->
                val opts = BitmapFactory.Options()
                val expected = (maxOf(w, h) * density).toInt().coerceAtLeast(1)
                // 2x, 4x etc downsampling avoids decoding needless full-res images.
                val nominalMax = if (wideTablet) 2560 else 2340
                var sample = 1
                while (nominalMax / (sample * 2) >= expected) sample *= 2
                opts.inSampleSize = sample
                BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
            }
        }.getOrNull()
    }
    Box(modifier) {
        if (image == null) {
            // Incomplete asset transfer must NEVER blank the existing Arena.
            Wq181ArenaHeroStage(Modifier.matchParentSize(), active = active)
        } else {
            Image(
                bitmap = image, contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            if (active) Canvas(Modifier.matchParentSize()) {
                drawRect(Brush.verticalGradient(
                    listOf(Color(0x7020163B), Color(0xAE160C2C))
                ))
            }
        }
    }
}
