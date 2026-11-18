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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.min

/**
 * A genuine LAYERED portrait, not a full-frame photo slideshow.
 *
 * Body/arms: four deformation poses smoothly crossfaded.
 * Head: independent transparent layer gently rotates about neck.
 * Lips: seven actual photographed facial expression patches, composited
 * onto moving head based on Android TTS text range when present.
 * Eyes: real half-closed and closed-lid images, NOT fake painted lines.
 *
 * The voice still originates from the existing Gemini/OpenAI → Android TTS
 * pipeline. For TTS engines lacking onRangeStart, mouth motion runs only
 * between real onPlaying start and stop, and approximates—not claims—phonemes.
 */
@Composable
internal fun Teacher2DPortrait(
    motion: AvatarMode,
    mouthCue: AvatarMouthFrame,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val body = remember(context) {
        runCatching {
            context.assets.open("teacher2d/teacher2d_body.webp").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
                    ?: error("Invalid 2D body rig")
            }
        }.getOrNull()
    }
    val head = remember(context) {
        runCatching {
            context.assets.open("teacher2d/teacher2d_head.webp").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
                    ?: error("Invalid 2D face rig")
            }
        }.getOrNull()
    }
    val mouths = remember(context) {
        runCatching {
            context.assets.open("teacher2d/teacher2d_mouth.webp").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
                    ?: error("Invalid 2D visemes")
            }
        }.getOrNull()
    }
    val eyes = remember(context) {
        runCatching {
            context.assets.open("teacher2d/teacher2d_eyes.webp").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
                    ?: error("Invalid 2D eyelids")
            }
        }.getOrNull()
    }
    var clockMs by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var speechStartedAtMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(motion) {
        speechStartedAtMs = if (motion == AvatarMode.SPEAKING) {
            SystemClock.uptimeMillis()
        } else 0L
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            clockMs = SystemClock.uptimeMillis()
            delay(33L)
        }
    }

    Box(modifier.background(Color(0xFF193C45))) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFF234C55), Color(0xFF15323B))),
                size = size,
            )
            val b = body ?: return@Canvas
            val h = head ?: return@Canvas
            val m = mouths ?: return@Canvas
            val e = eyes ?: return@Canvas

            val referenceWidth = 384
            val referenceHeight = 512
            val fit = min(
                size.width / referenceWidth,
                size.height / referenceHeight,
            )
            val dstWidth = max(1, (referenceWidth * fit).toInt())
            val dstHeight = max(1, (referenceHeight * fit).toInt())
            val x0 = ((size.width - dstWidth) / 2).toInt()
            val y0 = ((size.height - dstHeight) / 2).toInt()
            val bodyDst = IntOffset(x0, y0)
            val bodySize = IntSize(dstWidth, dstHeight)

            // Torso and arms are an independently sampled sprite layer.
            drawImage(
                image = b,
                srcOffset = IntOffset(0, 0),
                srcSize = IntSize(384, 512),
                dstOffset = bodyDst,
                dstSize = bodySize,
            )
            val gesture = Teacher2DAnimator.gesture(motion, clockMs, speechStartedAtMs)
            if (gesture.pose != 0 && gesture.alpha > .002f) {
                drawImage(
                    image = b,
                    srcOffset = IntOffset(0, gesture.pose * 512),
                    srcSize = IntSize(384, 512),
                    dstOffset = bodyDst,
                    dstSize = bodySize,
                    alpha = gesture.alpha,
                )
            }

            // The whole face—including hair and neck—actually moves independent
            // of the torso. Lips and eyelids inherit the SAME head transform.
            val headTilt = Teacher2DAnimator.headTiltDegrees(motion, clockMs)
            val headBob = Teacher2DAnimator.headBobPx(motion, clockMs)
            val bob = (headBob * fit).toInt()
            val headDst = IntOffset(x0, y0 + bob)
            val headSize = IntSize(dstWidth, max(1, (264f * fit).toInt()))
            val pivot = Offset(x0 + 192f * fit, y0 + 239f * fit)
            withTransform({ rotate(degrees = headTilt, pivot = pivot) }) {
                drawImage(
                    image = h,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(384, 264),
                    dstOffset = headDst,
                    dstSize = headSize,
                )

                val mouthIndex = Teacher2DAnimator.mouthIndex(
                    motion, mouthCue, clockMs, speechStartedAtMs,
                )
                if (mouthIndex != 0) {
                    drawImage(
                        image = m,
                        srcOffset = IntOffset(mouthIndex * 133, 0),
                        srcSize = IntSize(133, 77),
                        dstOffset = IntOffset(x0 + (126f * fit).toInt(),
                            y0 + (128f * fit).toInt() + bob),
                        dstSize = IntSize(max(1, (133f * fit).toInt()),
                            max(1, (77f * fit).toInt())),
                    )
                }
                val blink = Teacher2DAnimator.blinkIndex(clockMs)
                if (blink != 0) {
                    drawImage(
                        image = e,
                        srcOffset = IntOffset(blink * 177, 0),
                        srcSize = IntSize(177, 55),
                        dstOffset = IntOffset(x0 + (102f * fit).toInt(),
                            y0 + (86f * fit).toInt() + bob),
                        dstSize = IntSize(max(1, (177f * fit).toInt()),
                            max(1, (55f * fit).toInt())),
                    )
                }
            }
        }
    }
}
