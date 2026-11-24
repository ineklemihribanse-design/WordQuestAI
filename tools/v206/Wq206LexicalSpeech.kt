package com.kelimebulmacasi.a1

import java.util.Locale

/** Crossword answers are displayed in capitals; those are not initials. */
internal object Wq206LexicalSpeech {
    fun key(text: String): String =
        text.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    fun shortWord(text: String): Boolean {
        val w = key(text)
        return w.length in 1..2 && w.all { it in 'a'..'z' }
    }

    fun useVerifiedRecording(words: List<String>, contains: (String) -> Boolean): Boolean {
        val keys = words.map(::key).filter(String::isNotBlank)
        return keys.any(::shortWord) && keys.all(contains)
    }

    // A custom word without an offline recording still uses the selected
    // device accent, but don't send uppercase crossword letters as initials.
    fun deviceText(words: List<String>): List<String> =
        words.map { if (shortWord(it)) key(it) else it }
}
