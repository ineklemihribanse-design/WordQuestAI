package com.kelimebulmacasi.a1

import android.graphics.Canvas as NativeCanvas
import android.graphics.Color as NativeColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.max

/**
 * Resolution independent reconstruction of the approved green/lilac wave art.
 * Same 4:3 landscape and 9:16 portrait crop as v202, aligned at top center.
 * Drawn in actual device pixels (no scaled bitmap or extra bitmap allocation).
 * Only New Game uses this decoration.
 */
@Composable
internal fun Wq203NewGameBackground(landscape: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val aspect = if (landscape) 4f / 3f else 9f / 16f
        val artWidth = max(size.width, size.height * aspect)
        val artHeight = artWidth / aspect
        drawIntoCanvas { target ->
            val c = target.nativeCanvas
            val saved = c.save()
            c.clipRect(0f, 0f, size.width, size.height)
            c.translate((size.width - artWidth) / 2f, 0f)
            c.scale(artWidth / 1000f, artHeight / 1000f)
            paintBackground(c, landscape)
            c.restoreToCount(saved)
        }
    }
}

private fun rgb(s: String) = NativeColor.parseColor(s)

private fun paintBackground(c: NativeCanvas, landscape: Boolean) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    fun linear(y0: Float, y1: Float, vararg colors: String) {
        p.shader = LinearGradient(0f, y0, 0f, y1,
            colors.map(::rgb).toIntArray(), null, Shader.TileMode.CLAMP)
    }
    fun fill(path: Path, color: String) {
        p.shader = null
        p.color = rgb(color)
        c.drawPath(path, p)
        p.color = NativeColor.WHITE
    }
    fun wave(offset: Float): Path = Path().apply {
        moveTo(0f, 0f)
        if (landscape) {
            lineTo(0f, 134f + offset)
            cubicTo(152f, 32f + offset, 318f, 109f + offset, 493f, 187f + offset)
            cubicTo(668f, 262f + offset, 819f, 303f + offset, 1000f, 115f + offset)
        } else {
            lineTo(0f, 126f + offset)
            cubicTo(153f, 43f + offset, 302f, 116f + offset, 491f, 180f + offset)
            cubicTo(668f, 249f + offset, 804f, 214f + offset, 1000f, 143f + offset)
        }
        lineTo(1000f, 0f)
        close()
    }
    // Pearl-lilac field and warm center glow, with no visible raster texels.
    linear(100f, 1000f, "#F0E3FC", "#F4E9FC", "#EDE0FB")
    c.drawRect(0f, 0f, 1000f, 1000f, p)
    p.shader = RadialGradient(485f, if (landscape) 480f else 355f,
        530f, intArrayOf(rgb("#FFF5E9"), rgb("#30FFF3E9"), rgb("#00FFF3E9")),
        floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
    c.drawRect(0f, 0f, 1000f, 1000f, p)
    val lower = Path().apply {
        moveTo(0f, if (landscape) 670f else 820f)
        cubicTo(215f, 690f, 297f, 984f, 565f, 928f)
        cubicTo(750f, 890f, 867f, 727f, 1000f, 735f)
        lineTo(1000f, 1000f); lineTo(0f, 1000f); close()
    }
    fill(lower, "#20BCA2EE")
    val front = Path().apply {
        moveTo(0f, if (landscape) 875f else 960f)
        cubicTo(220f, 802f, 350f, 1010f, 575f, 910f)
        cubicTo(790f, 804f, 914f, 855f, 1000f, 918f)
        lineTo(1000f, 1000f); lineTo(0f, 1000f); close()
    }
    fill(front, "#24C6ADEE")
    // Pale translucent ribbon with thin warm highlight, then the green field.
    linear(0f, 300f, "#8BB990", "#D5E5B8", "#FFF5D5")
    c.drawPath(wave(if (landscape) 9f else 22f), p)
    p.shader = LinearGradient(0f, 0f, 1000f, 260f,
        intArrayOf(rgb("#164B3A"), rgb("#195B43"), rgb("#124736")),
        floatArrayOf(0f, .48f, 1f), Shader.TileMode.CLAMP)
    val green = wave(0f)
    c.drawPath(green, p)
    val clip = c.save()
    c.clipPath(green)
    val sweep = Path().apply {
        moveTo(0f, 0f); lineTo(if (landscape) 340f else 310f, 0f)
        cubicTo(230f, 34f, 128f, 82f, 0f, 112f)
        close()
    }
    fill(sweep, "#1745AB7B")
    // Quiet crossword motif, matching the existing art rather than game board.
    val cellW = if (landscape) 32f else 44f
    val cellH = cellW * if (landscape) 4f / 3f else 9f / 16f
    val stepX = cellW + 3f
    val stepY = cellH + 3f
    val motif = if (landscape) listOf(
        0 to 0, 0 to 1, 0 to 2, 1 to 1, 2 to 1,
        8 to 0, 8 to 1, 8 to 2, 8 to 3, 9 to 2, 10 to 2, 11 to 2,
        12 to 1, 12 to 2, 12 to 3, 12 to 4, 13 to 1, 14 to 1, 15 to 1,
        17 to 3, 18 to 3, 19 to 3, 19 to 2, 19 to 1, 20 to 2, 21 to 2
    ) else listOf(
        4 to 0, 4 to 1, 4 to 2, 4 to 3, 4 to 4, 4 to 5,
        0 to 2, 1 to 2, 2 to 2, 3 to 2, 5 to 3, 6 to 3, 7 to 3,
        7 to 2, 7 to 4, 8 to 4, 9 to 4, 2 to 4, 2 to 5, 3 to 5
    )
    p.shader = null
    p.color = rgb("#185FAD82")
    val x0 = if (landscape) 120f else 490f
    for ((col, row) in motif) {
        val x = x0 + col * stepX
        val y = row * stepY - 5f
        c.drawRoundRect(x, y, x + cellW, y + cellH, 4f, 4f, p)
    }
    p.color = NativeColor.WHITE
    // A soft green light near the wave, clipped to its silhouette.
    p.shader = RadialGradient(230f, 155f, 310f,
        intArrayOf(rgb("#305DC178"), rgb("#005DC178")), null, Shader.TileMode.CLAMP)
    c.drawRect(0f, 0f, 1000f, 360f, p)
    c.restoreToCount(clip)
}
