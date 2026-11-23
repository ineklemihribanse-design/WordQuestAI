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

    /** Phone grid height is bounded on BOTH axes. Reserve room for the
     * header, clue and editor when the IME is hidden; when it is shown,
     * keep a readable board and let the existing grid scroll if necessary.
     * No crossword cell or stored progress is changed by this policy.
     */
    fun phoneHeightDp(
        allocatedWidthDp: Float,
        availableHeightDp: Float,
        rows: Int,
        columns: Int,
        keyboardVisible: Boolean
    ): Float {
        if (rows < 1 || columns < 1 ||
            !allocatedWidthDp.isFinite() || !availableHeightDp.isFinite() ||
            allocatedWidthDp <= 0f || availableHeightDp <= 0f
        ) return 0f
        val naturalHeight = allocatedWidthDp * rows.toFloat() / columns.toFloat()
        val reserved = if (keyboardVisible) 172f else 285f
        val minimumBoard = if (keyboardVisible) 160f else 170f
        val allocated = (availableHeightDp - reserved).coerceAtLeast(minimumBoard)
        return minOf(naturalHeight, allocated)
    }
}
