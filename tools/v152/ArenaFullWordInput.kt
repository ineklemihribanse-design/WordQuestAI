package com.kelimebulmacasi.a1

/**
 * UI input now takes the ENTIRE word; already solved crossing letters stay
 * immutable in the board. The typed characters occupy their true positions.
 *
 * Example: C.... + "CLOSE" -> "CLOSE", never "CCLOS".
 */
internal fun crosswordFullWordPreview(pattern: String, typed: String): String {
    val letters = englishLetters(typed)
    return buildString(pattern.length) {
        pattern.forEachIndexed { index, locked ->
            append(if (locked != '.') locked else letters.getOrNull(index) ?: '.')
        }
    }
}

/** No partial words are submitted as incorrect answers by accident. */
internal fun crosswordFullWordReady(length: Int, typed: String): Boolean =
    length > 0 && englishLetters(typed).length == length

/** A submitted answer must match all letters, including revealed crossings. */
internal fun crosswordFullWordMatches(pattern: String, typed: String, expected: String): Boolean {
    val letters = englishLetters(typed)
    return crosswordFullWordReady(pattern.length, typed) &&
        letters == expected &&
        pattern.indices.all { i -> pattern[i] == '.' || pattern[i] == letters[i] }
}
