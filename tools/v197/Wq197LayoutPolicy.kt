package com.kelimebulmacasi.a1

/** Pure, testable responsive metrics for New Game. */
internal object Wq197LayoutPolicy {
    fun columns(widthDp: Float, count: Int, longestLabel: Int): Int {
        if (count <= 0) return 0
        val desired = when {
            count <= 2 -> count
            longestLabel >= 17 -> if (widthDp >= 730f) 4 else 2
            longestLabel >= 12 -> when {
                widthDp >= 730f -> 5
                widthDp >= 480f -> 3
                else -> 2
            }
            widthDp >= 750f -> 6
            widthDp >= 540f -> 5
            else -> 3
        }
        return desired.coerceIn(1, count)
    }

    fun previewLimit(widthDp: Float) = when {
        widthDp >= 700f -> 12
        widthDp >= 430f -> 6
        else -> 4
    }

    fun preview(options: List<String>, selected: Set<String>, limit: Int): List<String> =
        (selected.toList() + options).distinct().take(limit.coerceAtLeast(0))
}
