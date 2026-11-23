package com.kelimebulmacasi.a1

import kotlin.math.roundToInt

/** Pure geometry for the mobile crossword's overview/focus modes.
 * This is visual-only: no change to words, scoring or saved progress.
 */
internal object Wq192Viewport {
    fun overviewTileDp(widthDp: Float, heightDp: Float, columns: Int, rows: Int): Float {
        if (!widthDp.isFinite() || !heightDp.isFinite() ||
            widthDp <= 12f || heightDp <= 12f || columns <= 0 || rows <= 0) return 0f
        return minOf((widthDp - 12f) / columns, (heightDp - 12f) / rows)
            .coerceAtLeast(0f)
    }

    /** Center an entire selected word (not only its initial cell). */
    fun focusScrollPx(
        firstCell: Int,
        lastCell: Int,
        tilePx: Float,
        viewportPx: Float,
        maxScrollPx: Int
    ): Int {
        if (firstCell < 0 || lastCell < firstCell || !tilePx.isFinite() ||
            !viewportPx.isFinite() || tilePx <= 0f || viewportPx <= 0f ||
            maxScrollPx <= 0) return 0
        val focusCenterPx = (firstCell.toFloat() + lastCell + 1f) * tilePx * 0.5f
        return (focusCenterPx - viewportPx * 0.5f)
            .roundToInt().coerceIn(0, maxScrollPx)
    }
}
