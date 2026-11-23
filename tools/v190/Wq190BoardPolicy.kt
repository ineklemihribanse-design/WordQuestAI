package com.kelimebulmacasi.a1

/** Keep the entire crossword visible within a landscape tablet column.
 * Unlike the previous implementation, IME appearance does not cut this
 * already allocated grid height in half.
 */
internal object Wq190BoardPolicy {
    fun fittedHeightDp(
        allocatedWidthDp: Float,
        availableHeightDp: Float,
        rows: Int,
        columns: Int
    ): Float {
        if (rows < 1 || columns < 1 ||
            !allocatedWidthDp.isFinite() || !availableHeightDp.isFinite() ||
            allocatedWidthDp <= 0f || availableHeightDp <= 0f
        ) return 0f
        val aspectHeight = allocatedWidthDp * rows.toFloat() / columns.toFloat()
        val viewportHeight = (availableHeightDp - 12f).coerceAtLeast(1f)
        return minOf(aspectHeight, viewportHeight)
    }
}
