package com.kelimebulmacasi.a1

import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.min

/**
 * Precomposed, face-registered 100-image 2D animation.
 * Unlike v1.0.43, NO live eye/lip/head photo patch overlays or crossfading:
 * each image is a complete frame from the same full-body person.
 * The full-body master remains bundled for a future full-body camera mode;
 * current view intentionally shows the waist-up crop with relaxed arms.
 */
@Composable
internal fun Teacher2DPortrait(
    motion: AvatarMode,
    mouthCue: AvatarMouthFrame,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val atlas = remember(context) {
        runCatching {
            context.assets.open("teacher2d/teacher2d_atlas_100.webp").use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
                    ?: error("100-frame teacher animation atlas could not be decoded")
            }
        }.getOrNull()
    }
    var nowMs by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var speechStartedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(motion) {
        speechStartedMs = if (motion == AvatarMode.SPEAKING)
            SystemClock.uptimeMillis() else 0L
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            nowMs = SystemClock.uptimeMillis()
            delay(36L)
        }
    }

    Box(modifier.background(Color(0xFF1A3645))) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFF1A3645))
            val image = atlas ?: return@Canvas
            val frame = Teacher2D100Timeline.select(
                motion, mouthCue, nowMs, speechStartedMs,
            ).coerceIn(0, 99)
            val cellW = 256
            val cellH = 384
            val fit = min(size.width / cellW, size.height / cellH)
            val width = max(1, (cellW * fit).toInt())
            val height = max(1, (cellH * fit).toInt())
            drawImage(
                image = image,
                srcOffset = IntOffset(frame % 10 * cellW, frame / 10 * cellH),
                srcSize = IntSize(cellW, cellH),
                dstOffset = IntOffset(((size.width - width) / 2f).toInt(),
                                      ((size.height - height) / 2f).toInt()),
                dstSize = IntSize(width, height),
            )
        }
    }
}