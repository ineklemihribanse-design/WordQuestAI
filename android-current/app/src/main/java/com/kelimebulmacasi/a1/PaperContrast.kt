package com.kelimebulmacasi.a1

import kotlin.math.max
import kotlin.math.roundToInt

// Estimate the paper's local brightness, then make shadowed and lit areas
// comparable. The grid is small so only two pixel rows are needed in memory.
internal fun normalizePaper(
    width: Int,
    height: Int,
    read: (Int, IntArray) -> Unit,
    write: (Int, IntArray) -> Unit,
) {
    val step = (minOf(width, height) / 20).coerceIn(20, 58)
    val columns = (width + step - 1) / step
    val rows = (height + step - 1) / step
    val histograms = Array(rows * columns) { IntArray(16) }
    val pixels = IntArray(width)
    for (y in 0 until height) {
        read(y, pixels)
        for (x in pixels.indices) {
            histograms[y / step * columns + x / step][luma(pixels[x]) / 16]++
        }
    }
    val lighting = histograms.map { bins ->
        val threshold = bins.sum() * .88
        var count = 0
        val bucket = bins.indexOfFirst { count += it; count >= threshold }
        (bucket.coerceAtLeast(0) * 16 + 10).coerceAtLeast(55)
    }
    for (y in 0 until height) {
        read(y, pixels)
        val yFraction = (y.toFloat() / step - .5f).coerceIn(0f, (rows - 1).toFloat())
        val top = yFraction.toInt()
        val bottom = minOf(top + 1, rows - 1)
        for (x in pixels.indices) {
            val xFraction = (x.toFloat() / step - .5f).coerceIn(0f, (columns - 1).toFloat())
            val left = xFraction.toInt()
            val right = minOf(left + 1, columns - 1)
            val paperTop = between(lighting[top * columns + left], lighting[top * columns + right], xFraction - left)
            val paperBottom = between(lighting[bottom * columns + left], lighting[bottom * columns + right], xFraction - left)
            val paper = paperTop + (paperBottom - paperTop) * (yFraction - top)
            val grey = ((luma(pixels[x]) * 236f / max(paper, 45f) - 30f) * 1.20f).roundToInt().coerceIn(0, 255)
            pixels[x] = 0xff000000.toInt() or (grey shl 16) or (grey shl 8) or grey
        }
        write(y, pixels)
    }
}

private fun between(a: Int, b: Int, part: Float) = a + (b - a) * part

private fun luma(pixel: Int) = (
    (pixel ushr 16 and 255) * 77 + (pixel ushr 8 and 255) * 150 + (pixel and 255) * 29
) shr 8
