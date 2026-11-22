package com.kelimebulmacasi.a1

/**
 * The on-screen response preserves written Turkish translations, whereas TTS
 * reads only the surrounding conversation. Never send parentheses to the
 * engine or to range-based lip cues. This is not phoneme-accurate lip sync.
 */
internal object Wq187SpokenReply {
    private val decoration = Regex("[*_\x60#]+")
    private val spaces = Regex("[\\t ]+")
    private val newlines = Regex("\\n{3,}")

    fun prepare(written: String, language: ConversationLanguage): String {
        val text = decoration.replace(written,"")
        val spoken = StringBuilder(text.length)
        var depth = 0
        var removedParenthetical = false
        for (char in text) {
            when(char) {
                '(' -> { depth++; removedParenthetical = true }
                ')' -> if(depth>0) depth-- else spoken.append(char)
                else -> if(depth==0) spoken.append(char)
            }
        }
        val clear = newlines.replace(spaces.replace(spoken.toString()," ").trim(), "\n\n")
            .trim(' ','\n',',',';',':','-')
        if(clear.isEmpty()) return ""
        if (!removedParenthetical) return clear
        val guidance = if(language==ConversationLanguage.English)
            "You can see the Turkish meaning in parentheses on screen."
            else "Türkçe açıklamayı mesajdaki parantez içinde görebilirsin."
        return clear.trimEnd('.')+". "+guidance
    }
}
