package com.kelimebulmacasi.a1

import kotlin.math.roundToInt

/** Visual-only geometry for tablet focus. Phone and normal overview use v192 unchanged. */
internal object Wq199TabletFocus {
    fun enabled(smallestWidthDp: Int, enlarged: Boolean): Boolean =
        smallestWidthDp >= 600 && enlarged

    fun keyboardBoardHeightDp(availableDp: Float, originalDp: Float): Float =
        minOf(originalDp, (availableDp - 120f).coerceAtLeast(1f))

    fun tileDp(
        desiredDp: Float, widthDp: Float, heightDp: Float,
        wordColumns: Int, wordRows: Int, gapDp: Float
    ): Float {
        if (wordColumns < 1 || wordRows < 1 || !widthDp.isFinite() ||
            !heightDp.isFinite() || !desiredDp.isFinite() || !gapDp.isFinite())
            return 0f
        // Include border/padding and a conservative allowance for rounding
        // each cell to physical pixels. The active word fits on BOTH axes.
        val stepAllowance = gapDp.coerceAtLeast(0f) + 1f
        val fitX = (widthDp - 14f) / wordColumns - stepAllowance
        val fitY = (heightDp - 14f) / wordRows - stepAllowance
        return minOf(desiredDp, fitX, fitY).coerceAtLeast(0.5f)
    }

    fun scrollPx(
        first: Int, last: Int, pitchPx: Float, cellPx: Float,
        viewportPx: Int, maxScrollPx: Int, insetPx: Float = 0f
    ): Int {
        if (first < 0 || last < first || !pitchPx.isFinite() ||
            !cellPx.isFinite() || pitchPx <= 0f || cellPx <= 0f ||
            viewportPx <= 0 || maxScrollPx <= 0 ||
            maxScrollPx == Int.MAX_VALUE) return 0
        val start = first * pitchPx + insetPx
        val end = last * pitchPx + cellPx + insetPx
        return ((start + end - viewportPx) / 2f).roundToInt().coerceIn(0, maxScrollPx)
    }
}
