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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Single human-looking waist-up 2D teacher, no WebView, OpenGL, SceneView,
 * Filament, GLB or subprocess. 21 hand-painted / generated and inspected
 * full-frame frames are bundled locally, never downloaded during a lesson.
 *
 * Seven real visibly different lip poses are chosen only from the current
 * Android TTS onRangeStart cue. This is word-range approximation, not phoneme
 * perfect audio analysis. An unrelated or stale utterance cannot move lips.
 * The three gentle hand poses crossfade instead of jerking between frames.
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
            context.assets.open("teacher2d/teacher2d_atlas.webp").use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
                    ?: error("Cannot decode the 2D teacher sprite")
            }
        }.getOrNull()
    }
    var frameMs by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var latestGestureCue by remember { mutableLongStateOf(0L) }
    var gestureAtMs by remember { mutableLongStateOf(0L) }
    var gesturePose by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        while (isActive) {
            frameMs = SystemClock.uptimeMillis()
            delay(40L) // 25 FPS; no audio playback or networking on the draw path.
        }
    }
    LaunchedEffect(motion, mouthCue.cueSequence) {
        if (motion == AvatarMode.SPEAKING &&
            mouthCue.cueSequence > 0L && mouthCue.cueSequence % 8L == 0L &&
            mouthCue.cueSequence != latestGestureCue) {
            latestGestureCue = mouthCue.cueSequence
            gesturePose = if ((mouthCue.cueSequence / 8L) % 2L == 0L) 1 else 2
            gestureAtMs = SystemClock.uptimeMillis()
        }
        if (motion != AvatarMode.SPEAKING) gestureAtMs = 0L
    }

    Box(modifier.background(Color(0xFF173A43))) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFF274E55), Color(0xFF112B35))),
                size = size,
            )
            val image = atlas ?: return@Canvas
            // Atlas is 7 columns × 3 rows, 512×768 pixels per pose.
            val width = 512
            val height = 768
            val scale = min(size.width / width, size.height / height)
            val destW = max(1, (width * scale).toInt())
            val destH = max(1, (height * scale).toInt())
            val left = ((size.width - destW) / 2f).toInt()
            val top = ((size.height - destH) / 2f).toInt()

            val elapsed = frameMs - mouthCue.cueAtMs
            val genuineCue = motion == AvatarMode.SPEAKING &&
                mouthCue.quality != LipSyncQuality.NONE &&
                mouthCue.cueAtMs > 0L && elapsed in 0L..180L &&
                mouthCue.weight > .05f
            val mouth = if (!genuineCue) 0 else when (mouthCue.viseme) {
                AvatarViseme.A -> 1
                AvatarViseme.E -> 2
                AvatarViseme.I -> 3
                AvatarViseme.O -> 4
                AvatarViseme.U -> 5
                AvatarViseme.MBP -> 6
                AvatarViseme.NEUTRAL -> 0
            }
            val elapsedGesture = frameMs - gestureAtMs
            // Natural eased gesture envelope; no abrupt or perpetual arm waving.
            val gestureMix = if (motion == AvatarMode.SPEAKING && gestureAtMs > 0L
                && elapsedGesture in 0L..1300L) {
                (sin(PI * elapsedGesture.toDouble() / 1300.0).toFloat()
                    .coerceIn(0f, 1f)) * .84f
            } else 0f
            val breath = (sin(frameMs.toDouble() / 1600.0) * 1.2f).toFloat()
            val dst = IntOffset(left, top + breath.toInt())
            val dims = IntSize(destW, destH)

            // Always draw the neutral arms, then crossfade the gesture variant.
            drawImage(
                image = image,
                srcOffset = IntOffset(mouth * width, 0),
                srcSize = IntSize(width, height),
                dstOffset = dst, dstSize = dims,
            )
            if (gestureMix > 0.005f) {
                drawImage(
                    image = image,
                    srcOffset = IntOffset(mouth * width, gesturePose * height),
                    srcSize = IntSize(width, height),
                    dstOffset = dst, dstSize = dims, alpha = gestureMix,
                )
            }

            // Small realistic eye-blink; hands and face otherwise stay calm.
            val blinkPhase = (frameMs + 1600L) % 5100L
            if (blinkPhase < 105L && motion != AvatarMode.ERROR) {
                val skin = Color(0xFFD5A18A)
                for (x in listOf(227f, 284f)) {
                    val centerX = left + x * scale
                    val centerY = top + (125f * scale) + breath
                    drawLine(
                        color = skin,
                        start = Offset(centerX - 11f * scale, centerY),
                        end = Offset(centerX + 11f * scale, centerY),
                        strokeWidth = max(1f, 5f * scale),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
