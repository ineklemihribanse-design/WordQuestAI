package com.kelimebulmacasi.a1

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/** Upgrade optional v182 atlas to true 512px originals when verified pack is staged. */
@Composable
internal fun Wq183HighResArtwork(name: String, modifier: Modifier): Boolean {
    val context = LocalContext.current
    // Never resolve arbitrary user paths: only approved atlas catalog names.
    if (!Wq182Icons.has(name)) return false
    val bitmap = remember(context.applicationContext, name) {
        runCatching {
            context.assets.open("approved_icons_v183/$name.webp").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
    return bitmap != null
}
