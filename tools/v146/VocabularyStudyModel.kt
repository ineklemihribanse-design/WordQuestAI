package com.kelimebulmacasi.a1

import java.util.Locale
import kotlin.math.min

/** Small, deterministic answer checker; never needs an API key or cloud access. */
internal enum class VocabularyGrade { CORRECT, CLOSE, WRONG }

internal object VocabularyAnswerChecker {
    private val englishAliases = mapOf(
        "grandmother" to listOf("grandma", "granny"),
        "grandfather" to listOf("grandpa", "granddad"),
        "mom" to listOf("mother", "mum"),
        "mother" to listOf("mom", "mum"),
        "father" to listOf("dad", "daddy"),
        "dad" to listOf("father", "daddy")
    )

    fun normalize(raw: String, english: Boolean): String {
        val locale = if (english) Locale.ROOT else Locale.forLanguageTag("tr-TR")
        return raw.trim().replace('’', '\'')
            .replace(Regex("\\s+"), " ")
            .trimEnd('.', '!', '?')
            .lowercase(locale)
            .trim()
    }

    fun variants(expected: String, english: Boolean): Set<String> {
        val clean = normalize(expected, english)
        val parts = if (english) Regex("\\s*(?:/|;|\\bor\\b)\\s*", RegexOption.IGNORE_CASE)
                    else Regex("\\s*(?:/|;|\\bveya\\b)\\s*", RegexOption.IGNORE_CASE)
        val alternatives = parts.split(expected).map { normalize(it, english) }.filter { it.isNotBlank() }
        val words = if (alternatives.size > 1) alternatives else listOf(clean)
        return (words + (if (english) words.flatMap { englishAliases[it].orEmpty() } else emptyList()))
            .map { normalize(it, english) }.filter { it.isNotBlank() }.toSet()
    }

    fun grade(input: String, expected: String, english: Boolean): VocabularyGrade {
        val text = normalize(input, english)
        if (text.isEmpty()) return VocabularyGrade.WRONG
        val variants = variants(expected, english)
        if (text in variants) return VocabularyGrade.CORRECT
        return if (variants.any { it.length >= 4 && text.length >= 4 &&
                    distanceAtMostTwo(text, it) <= (if (it.length > 5) 2 else 1) })
            VocabularyGrade.CLOSE else VocabularyGrade.WRONG
    }

    private fun distanceAtMostTwo(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 2) return 3
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = min(min(current[j] + 1, previous[j + 1] + 1), previous[j] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }
}

/** Only unambiguous, reviewed labels. Unknown or multi-purpose words stay unclassified. */
internal enum class VocabularyType(val label: String) {
    NOUN("İsim"), VERB("Fiil"), ADJECTIVE("Sıfat"), ADVERB("Zarf")
}

internal object VocabularyWordTypes {
    private val definitions = mapOf(
        VocabularyType.NOUN to """
            grandmother grandfather mother father sister brother family school teacher student
            house cat dog apple city country friend happiness child children person people
            woman man table chair kitchen bedroom computer garden library hospital
        """.trimIndent(),
        VocabularyType.VERB to """
            learn read write speak listen eat drink understand remember forget improve
            decide explain arrive discover borrow lend describe achieve develop
        """.trimIndent(),
        VocabularyType.ADJECTIVE to """
            beautiful happy sad important different difficult easy interesting possible
            correct careful expensive dangerous useful famous comfortable intelligent
        """.trimIndent(),
        VocabularyType.ADVERB to """
            quickly slowly carefully usually always never often sometimes silently
        """.trimIndent()
    )
    private val dictionary: Map<String, VocabularyType> = buildMap {
        for ((type, words) in definitions) {
            for (word in words.split(Regex("\\s+")).filter { it.isNotBlank() }) {
                require(!containsKey(word)) { "Ambiguous word type: $word" }
                put(word, type)
            }
        }
    }

    fun get(answer: String): VocabularyType? = dictionary[VocabularyAnswerChecker.normalize(answer, true)]
    fun label(answer: String): String? = get(answer)?.label
}

internal data class VocabularyStudyConfig(
    val folderId: String,
    val filter: String,
    val mode: String,
    val direction: String,
    val count: Int,
    val seed: Int
)

internal fun vocabularyStudyCards(
    session: AppSession, folderId: String, filter: String
): List<LearningCard> =
    if (folderId != "all") session.learningFolderCards(folderId, filter)
    else session.searchLearning("", 0, 5000).filter { card ->
        when (filter) {
            "BILINMIYOR" -> card.forgotten > 0
            "YANLIS" -> card.wrong > 0
            "YENI" -> card.solved > 0 && card.forgotten == 0 && card.wrong == 0
            else -> true
        }
    }
