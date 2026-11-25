package com.kelimebulmacasi.a1

import java.text.Normalizer
import java.util.Locale

/** Local interpretation of the actual AI reply being read aloud.
 * No provider call, no model edits, no guessed phoneme or sentence timing.
 */
internal object Wq117LenaMotion {
    enum class Intent { GREETING, PRAISE, THANKS, AGREEMENT, CORRECTION, EXPLANATION, QUESTION, FAREWELL, NEUTRAL }
    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").replace('ı','i')
            .replace(Regex("[*_#]"), "").replace(Regex("\\s+"), " ").trim()
    private fun starts(s: String, words: String) = Regex("^(?:$words)(?=\\b|[!,.?])").containsMatchIn(s)
    fun classify(reply: String): Intent {
        val s = normalize(reply.take(12000))
        if (s.isBlank()) return Intent.NEUTRAL
        return when {
            starts(s,"not quite|not exactly|almost|good try|nice try|try again|let'?s try again|tam degil|bir daha deneyelim|guzel deneme|kucuk bir duzeltme") -> Intent.CORRECTION
            starts(s,"hello|hi there|hi|hey there|good morning|good afternoon|good evening|merhaba|selam|gunaydin|iyi aksamlar") -> Intent.GREETING
            starts(s,"well done|excellent|fantastic|great job|good job|perfect|bravo|congratulations|you got it|that'?s correct|harika|aferin|tebrikler|cok iyi|dogru cevap") -> Intent.PRAISE
            starts(s,"thank you|thanks|i appreciate|tesekkur ederim|tesekkurler|sag ol") -> Intent.THANKS
            starts(s,"goodbye|bye|see you|take care|gorusuruz|hosca kal") -> Intent.FAREWELL
            starts(s,"yes[,!]|exactly|absolutely|i agree|that'?s right|evet[,!]|kesinlikle|aynen") -> Intent.AGREEMENT
            starts(s,"let me explain|let'?s look|for example|imagine|first[,!]|the difference|here'?s how|ornegin|mesela|aciklayayim|ilk olarak") -> Intent.EXPLANATION
            s.endsWith("?") && s.length <= 260 -> Intent.QUESTION
            else -> Intent.NEUTRAL
        }
    }
    /** Choose a single appropriate native clip at the start of a reply.
     * No locomotion, sitting/standing transitions, negative expressions or overlapping clips.
     * Seated speech stays seated; all 38 motions remain manually selectable.
     */
    fun openingChoices(intent: Intent, seated: Boolean): List<String> {
        val names = if (seated) when (intent) {
            Intent.PRAISE -> listOf("Sitting_Clap")
            else -> emptyList()
        } else when (intent) {
            Intent.GREETING -> listOf("Standing_Greeting","Waving")
            Intent.PRAISE -> listOf("Standing_Clap","Happy","Clapping")
            Intent.THANKS -> listOf("Thankful")
            Intent.AGREEMENT -> listOf("Agreeing","Head_Nod_Yes")
            Intent.CORRECTION -> listOf("Head_Gesture","Thoughtful_Head_Nod")
            Intent.EXPLANATION -> listOf("Pointing","Talking-1","Head_Gesture")
            Intent.QUESTION -> listOf("Head_Gesture","Talking-2")
            Intent.FAREWELL -> listOf("Waving")
            Intent.NEUTRAL -> emptyList()
        }
        return names.map { "Mixamo_$it" }
    }
    fun choose(reply: String, seated: Boolean, available: Set<String>, cursor: Int): String? {
        val choices = openingChoices(classify(reply), seated).filter { it in available }
        if (choices.isEmpty()) return null
        return choices[(cursor.coerceAtLeast(0)) % choices.size]
    }
}
