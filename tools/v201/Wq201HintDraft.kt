package com.kelimebulmacasi.a1

/** Only transient editor state. Real hints are still produced and saved by the engine. */
internal object Wq201HintDraft {
    fun authoritative(
        visible: Map<Int, Char>, locked: Set<Int>, revealed: Map<Int, Char>
    ): Map<Int, Char> = visible.filter { (i, value) ->
        i in locked || revealed[i] == value
    }

    fun remap(total: Int, oldLocked: Map<Int, Char>, newLocked: Map<Int, Char>,
              draft: String): String {
        if (draft.length == total) return draft
        val oldTyped = Wq190CrosswordHintInput.typedPositions(total, oldLocked, draft)
        return (0 until total).mapNotNull { index ->
            if (index in newLocked) null else oldTyped[index]
        }.joinToString("")
    }

    fun newlyRevealed(before: Map<Int, Char>, after: Map<Int, Char>): Map<Int, Char> =
        after.filter { (i, value) -> before[i] != value }
}
