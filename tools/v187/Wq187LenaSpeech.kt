package com.kelimebulmacasi.a1

/** A displayed translation is not necessarily something Lena should speak aloud. */
internal object Wq187LenaSpeech {
    private val parenthesis = Regex("""\s*[\(\（]([^\(\)\（\）]{1,180})[\)\）]""")
    private val turkishLetters = Regex("""[çğıöşüÇĞİÖŞÜ]""")
    private val turkishWords = Regex("""(?i)\b(şu|an|müsün|misin|için|bunu|anlamı|anlamına|demek|merhaba|sözcük|örnek|çevirisi|türkçesi|evet|hayır|burada|ben|sen|bir|nedir)\b""")

    private val unmistakableTurkish = Regex("""(?i)\b(merhaba|hadi|gidelim|selam|nasilsin|tamam|teşekkürler|lütfen)\b""")

    fun hasTurkishAside(value: String): Boolean =
        turkishLetters.containsMatchIn(value) || unmistakableTurkish.containsMatchIn(value) ||
            (turkishWords.findAll(value).count() >= 2)

    fun forSpeech(displayed: String, language: ConversationLanguage): String {
        val plain = displayed.replace(Regex("[*_#]"), "").trim()
        if (language != ConversationLanguage.English) return plain
        var omitted = false
        val cleaned = parenthesis.replace(plain) { match ->
            if (hasTurkishAside(match.groupValues[1])) {
                omitted = true
                ""
            } else match.value
        }.replace(Regex("""[ \t]{2,}"""), " ")
            .replace(Regex("""\s+([?.!,;:])"""), "$1")
            .trim()
        if (!omitted) return cleaned
        // Brief English pointer, pronounced by the already-selected English voice.
        val note = "You can read the Turkish explanation in parentheses if you'd like."
        return when {
            cleaned.isBlank() -> note
            cleaned.endsWith('.') || cleaned.endsWith('!') || cleaned.endsWith('?') -> "$cleaned $note"
            else -> "$cleaned. $note"
        }
    }
}
