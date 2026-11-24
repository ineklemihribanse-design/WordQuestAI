package com.kelimebulmacasi.a1

internal object Wq213TypingContinuity {
    fun layoutActive(imeVisible: Boolean, typing: Boolean, wasShown: Boolean): Boolean =
        imeVisible || (typing && wasShown)

    fun advanceSolved(engine: Crossword, submittedIndex: Int): Boolean {
        if (engine.complete || submittedIndex !in engine.view().solvedIndices) return false
        // The existing engine picks the next unsolved clue and preserves scoring.
        if (engine.wordIndex != submittedIndex) return false
        engine.moveToNextWord()
        return engine.wordIndex != submittedIndex
    }
}
