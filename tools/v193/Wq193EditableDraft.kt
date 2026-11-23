package com.kelimebulmacasi.a1

/**
 * Draft-only policy: previously entered but unsolved letters are editable.
 * Only solved/revealed crossings protected by the Crossword engine are fixed.
 */
internal object Wq193EditableDraft {
    fun protectedPositions(
        visible: Map<Int, Char>,
        locked: Set<Int>
    ): Map<Int, Char> = visible.filterKeys { it in locked }

    fun normalizedText(text: String, maxLetters: Int): String =
        if (maxLetters <= 0) "" else englishLetters(text).take(maxLetters)

    /** Return true when a complete answer must remain open for correction. */
    fun needsCorrection(solved: Boolean): Boolean = !solved
}
