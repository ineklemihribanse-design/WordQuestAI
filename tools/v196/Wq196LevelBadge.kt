package com.kelimebulmacasi.a1

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/**
 * The user-approved 3D book + graduation cap artwork has no printed level.
 * Keep the real dynamic level text (A1, A2, B1, B2, C1, C2 or YDS)
 * beside this same 30dp image in the unchanged crossword heading.
 */
@Composable
internal fun Wq196LevelBadge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(context.applicationContext) {
        runCatching {
            context.applicationContext.assets.open("wq196_level_badge.webp").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Seviye",
            modifier = modifier,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
        )
    } else {
        // A corrupt asset must not crash game UI.
        Wq175GlossIcon("learning", modifier)
    }
}
