package com.kelimebulmacasi.a1

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.min

/**
 * Real-time 2.5D single-mesh prototype, NOT a 100-photo slideshow.
 *
 * Exactly ONE full-body master image is decoded and cropped for today's
 * waist-up display. Native Android Canvas.drawBitmapMesh reshapes its pixels.
 * No SceneView/Filament, no independent face patch compositing and no
 * allocated 3200x4800 full-frame atlas.
 */
@Composable
internal fun Teacher25DPortrait(
    motion: AvatarMode,
    mouthCue: AvatarMouthFrame,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val image = remember(context) {
        runCatching {
            context.assets.open("teacher2d/teacher2d_fullbody_master.webp").use {
                val master = BitmapFactory.decodeStream(it)
                    ?: error("Cannot read 2.5D source image")
                require(master.width == 1024 && master.height == 1536)
                val crop = Bitmap.createBitmap(master, 205, 0, 614, 921)
                val thumb = Bitmap.createScaledBitmap(
                    crop, Teacher25DMath.WIDTH, Teacher25DMath.HEIGHT, true)
                if (crop !== master) crop.recycle()
                master.recycle()
                thumb
            }
        }.getOrNull()
    }
    val vertices = remember { FloatArray(Teacher25DMath.FLOAT_COUNT) }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    var now by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var speechSince by remember { mutableLongStateOf(0L) }
    var mouth by remember { mutableFloatStateOf(0f) }
    val latestMode by rememberUpdatedState(motion)
    val latestCue by rememberUpdatedState(mouthCue)

    LaunchedEffect(motion) {
        speechSince = if (motion == AvatarMode.SPEAKING)
            SystemClock.uptimeMillis() else 0L
        if (motion != AvatarMode.SPEAKING) mouth = 0f
    }
    LaunchedEffect(Unit) {
        var prev = SystemClock.uptimeMillis()
        while (isActive) {
            val time = SystemClock.uptimeMillis()
            val delta = time - prev
            prev = time
            val target = Teacher25DMath.mouthTarget(
                latestMode, latestCue, time, speechSince,
            )
            mouth = Teacher25DMath.approach(mouth, target, delta, 48f)
            now = time
            delay(33L)
        }
    }

    Box(modifier.background(Color(0xFF1A3645))) {
        Canvas(Modifier.fillMaxSize()) {
            val source = image ?: return@Canvas
            val speaking = motion == AvatarMode.SPEAKING
            Teacher25DMath.buildVertices(
                Teacher25DMath.Pose(
                    jaw = mouth,
                    blink = Teacher25DMath.eyelid(now),
                    breath = Teacher25DMath.breath(now),
                    nod = Teacher25DMath.nod(now, speaking),
                    gesture = Teacher25DMath.gesture(now, speechSince, speaking),
                ), vertices,
            )
            val fit = min(
                size.width / Teacher25DMath.WIDTH.toFloat(),
                size.height / Teacher25DMath.HEIGHT.toFloat(),
            )
            val width = Teacher25DMath.WIDTH * fit
            val height = Teacher25DMath.HEIGHT * fit
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                val save = native.save()
                native.translate((size.width - width) / 2f, (size.height - height) / 2f)
                native.scale(fit, fit)
                native.drawBitmapMesh(source, Teacher25DMath.SEG_X,
                    Teacher25DMath.SEG_Y, vertices, 0, null, 0, paint)
                native.restoreToCount(save)
            }
        }
    }
}
