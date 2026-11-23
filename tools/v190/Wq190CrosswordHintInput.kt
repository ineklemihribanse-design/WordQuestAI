package com.kelimebulmacasi.a1

/**
 * A revealed/crossing letter belongs to the puzzle, not to an edit text's
 * untrusted draft. Users type only the missing letters; existing full-word
 * answers remain supported for backward compatibility.
 */
internal object Wq190CrosswordHintInput {
    fun typedPositions(
        total: Int,
        locked: Map<Int, Char>,
        draft: String
    ): Map<Int, Char> {
        if (total <= 0) return emptyMap()
        val letters = draft.filter(Char::isLetter).uppercase()
        val open = (0 until total).filter { it !in locked }
        val slots = if (letters.length == total) (0 until total).toList() else open
        return slots.take(letters.length).mapIndexedNotNull { index, slot ->
            if (slot in locked || index >= letters.length) null
            else slot to letters[index]
        }.toMap()
    }

    fun completedWord(
        total: Int,
        locked: Map<Int, Char>,
        draft: String
    ): String? {
        if (total <= 0 || locked.keys.any { it !in 0 until total }) return null
        val letters = draft.filter(Char::isLetter).uppercase()
        val missing = (0 until total).filter { it !in locked }
        if (letters.length != missing.size && letters.length != total) return null
        val result = CharArray(total) { '_' }
        val positions = if (letters.length == total) (0 until total).toList() else missing
        positions.forEachIndexed { i, index -> result[index] = letters[i] }
        locked.forEach { (i, char) -> result[i] = char }
        return result.concatToString().takeIf { '_' !in it }
    }

    fun canSubmit(total: Int, locked: Map<Int, Char>, draft: String): Boolean =
        completedWord(total, locked, draft) != null
}
