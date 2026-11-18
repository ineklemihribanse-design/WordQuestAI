package com.kelimebulmacasi.a1

import android.graphics.Canvas as NativeCanvas
import android.graphics.Color as AColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlin.math.abs
import kotlin.math.sin

/**
 * Device-native, GPU-independent visible teacher.
 *
 * The previous WebView canvas could report WebGL NO_ERROR while outputting
 * zero visible character pixels; it also hid its own fallback at that point.
 * This 2D, softly shaded animated portrait is a guaranteed non-WebView
 * alternative until a separately verified rigged GLB character renderer ships.
 *
 * Never put API keys, student text or microphone data in visual rendering.
 */
@Composable
internal fun TeacherFallback2D(
    actor: TeacherAvatar,
    speaking: Boolean,
    listening: Boolean,
    thinking: Boolean,
    speechRate: Float,
    modifier: Modifier = Modifier,
) {
    val animation = rememberInfiniteTransition(label = "teacher-avatar")
    val beat by animation.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing), repeatMode = RepeatMode.Restart,
        ), label = "face-motion",
    )
    val palette = when (actor.name) {
        "Deniz" -> AvatarPalette("#D9A588", "#3A303B", "#526EA0", "#DDE4F0", "#654D48", false, false)
        "Mert" -> AvatarPalette("#BF9277", "#292B32", "#67557E", "#D6D1C3", "#53433A", false, true)
        else -> AvatarPalette("#E6B9A0", "#4B2E2B", "#127A73", "#C0D6C5", "#5D4237", true, false)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val c = drawContext.canvas.nativeCanvas
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
        fun shade(color: String, dark: String = color): Pair<Int, Int> =
            AColor.parseColor(color) to AColor.parseColor(dark)
        fun oval(x: Float, y: Float, w: Float, h: Float, upper: Int, lower: Int = upper) {
            p.reset(); p.isAntiAlias = true
            p.shader = LinearGradient(x, y, x + w * .6f, y + h, upper, lower, Shader.TileMode.CLAMP)
            c.drawOval(RectF(x, y, x + w, y + h), p)
            p.shader = null
        }
        fun round(x: Float, y: Float, w: Float, h: Float, rad: Float, top: Int, bottom: Int = top) {
            p.reset(); p.isAntiAlias = true
            p.shader = LinearGradient(x, y, x + w * .5f, y + h, top, bottom, Shader.TileMode.CLAMP)
            c.drawRoundRect(RectF(x, y, x + w, y + h), rad, rad, p)
            p.shader = null
        }
        fun polygon(color: Int, path: (Path) -> Unit) {
            val q = Path(); path(q)
            p.reset(); p.isAntiAlias = true; p.color = color
            c.drawPath(q, p)
        }

        val skin = AColor.parseColor(palette.skin)
        val hair = AColor.parseColor(palette.hair)
        val jacket = AColor.parseColor(palette.jacket)
        val shirt = AColor.parseColor(palette.shirt)
        val eye = AColor.parseColor(palette.eye)
        val scale = minOf(size.height / 535f, size.width / 720f).coerceAtLeast(.2f)
        val save = c.save()
        c.translate(size.width * .5f, size.height * .53f + sin(beat * 6.283185f) * 3.5f * scale)
        c.scale(scale, scale)

        // Subtle portrait halo, shoulders and tailored jacket with separate collar.
        oval(-245f, -180f, 490f, 485f, 0x222BA39D, 0x002BA39D)
        oval(-235f, 79f, 470f, 428f, jacket, AColor.parseColor("#0F303A"))
        oval(-230f, 65f, 155f, 300f, jacket, AColor.parseColor("#092B39"))
        oval(75f, 65f, 155f, 300f, jacket, AColor.parseColor("#092B39"))
        polygon(shirt) {
            it.moveTo(-67f, 94f); it.lineTo(70f, 94f); it.lineTo(108f, 336f)
            it.lineTo(-113f, 336f); it.close()
        }
        round(-30f, 63f, 60f, 92f, 23f, skin, AColor.parseColor("#C38A75"))
        polygon(AColor.parseColor("#0E4748")) {
            it.moveTo(-92f, 100f); it.lineTo(-22f, 144f); it.lineTo(-110f, 315f)
            it.lineTo(-164f, 180f); it.close()
        }
        polygon(AColor.parseColor("#174E51")) {
            it.moveTo(92f, 100f); it.lineTo(22f, 144f); it.lineTo(110f, 315f)
            it.lineTo(164f, 180f); it.close()
        }

        // Hair behind face, ears and tapered jaw.
        oval(-124f, -210f, 248f, 312f, hair, AColor.parseColor("#251B25"))
        if (!palette.shortHair) {
            oval(-133f, -122f, 67f, 277f, hair, AColor.parseColor("#251B25"))
            oval(67f, -122f, 66f, 275f, hair, AColor.parseColor("#251B25"))
        }
        oval(-113f, -59f, 45f, 75f, skin, AColor.parseColor("#BB816C"))
        oval(68f, -59f, 45f, 75f, skin, AColor.parseColor("#BB816C"))
        oval(-102f, -179f, 204f, 274f, AColor.parseColor("#F1CCAE"), skin)
        oval(-92f, -65f, 184f, 164f, skin, AColor.parseColor("#D49C86"))

        // Side highlights, brows, animated eyelids and moving pupils.
        oval(-83f, -68f, 40f, 18f, AColor.parseColor("#F0C4A4"), AColor.parseColor("#E2A78C"))
        oval(43f, -68f, 40f, 18f, AColor.parseColor("#F0C4A4"), AColor.parseColor("#E2A78C"))
        round(-75f, -86f, 53f, 8f, 6f, hair)
        round(22f, -86f, 53f, 8f, 6f, hair)
        val blink = beat > .81f && beat < .855f
        for (side in listOf(-1f, 1f)) {
            val x = if (side < 0) -67f else 25f
            oval(x, -57f, 43f, if (blink) 4f else 27f, AColor.parseColor("#FFF6ED"))
            if (!blink) {
                oval(x + 14f, -56f, 19f, 25f, eye, AColor.parseColor("#302422"))
                oval(x + 22f, -53f, 7f, 8f, AColor.WHITE)
            }
        }

        // Bridge, softly lit nose, lips and speech-driven jaw movement.
        oval(-12f, -38f, 26f, 59f, AColor.parseColor("#E8B29A"), AColor.parseColor("#C58B78"))
        oval(-19f, 13f, 38f, 13f, AColor.parseColor("#CE9782"))
        val mouthHeight = if (speaking) 6f + (15f * abs(sin(beat * 6.283185f * 5f * speechRate.coerceIn(.6f, 1.4f)))) else 5f
        oval(-30f, 45f, 60f, mouthHeight, AColor.parseColor("#753F47"))
        oval(-30f, 47f + mouthHeight, 60f, 8f, AColor.parseColor("#C07978"))
        if (listening) {
            oval(-80f, 6f, 30f, 9f, AColor.parseColor("#E9A99A"))
            oval(50f, 6f, 30f, 9f, AColor.parseColor("#E9A99A"))
        }

        // Natural layered hairline with actor-specific silhouette.
        if (palette.shortHair) {
            oval(-104f, -203f, 208f, 91f, hair, AColor.parseColor("#25262B"))
            oval(-104f, -153f, 79f, 42f, hair)
        } else {
            oval(-111f, -203f, 222f, 79f, hair, AColor.parseColor("#34252B"))
            oval(-107f, -167f, 106f, 54f, hair, AColor.parseColor("#34252B"))
            oval(34f, -171f, 81f, 44f, hair)
        }

        if (palette.glasses) {
            p.reset(); p.isAntiAlias = true; p.color = AColor.parseColor("#795C51")
            p.style = Paint.Style.STROKE; p.strokeWidth = 5f
            c.drawRoundRect(RectF(-86f, -66f, -12f, -19f), 16f, 16f, p)
            c.drawRoundRect(RectF(12f, -66f, 86f, -19f), 16f, 16f, p)
            c.drawLine(-12f, -49f, 12f, -49f, p)
        }
        c.restoreToCount(save)
    }
}

private data class AvatarPalette(
    val skin: String,
    val hair: String,
    val jacket: String,
    val shirt: String,
    val eye: String,
    val glasses: Boolean,
    val shortHair: Boolean,
)
