package com.kelimebulmacasi.a1

/** Pure, deterministic match summary. Never mix a rival's mistakes into this player's notebook. */
internal data class PremiumReviewWord(
    val index: Int, val entry: Entry, val wrongAttempts: Int, val unknown: Boolean,
) {
    val needsReview: Boolean get() = wrongAttempts > 0 || unknown
}

internal data class PremiumMatchSummary(
    val correct: Int, val wrong: Int, val unknown: Int, val review: List<PremiumReviewWord>,
    val all: List<PremiumReviewWord> = review,
) {
    val wrongWords: Int get() = review.count { it.wrongAttempts > 0 }
    val reviewedWords: Int get() = review.count(PremiumReviewWord::needsReview)
}

internal object PremiumMatchReview {
    fun crossword(match: PremiumOnlineMatch, entries: List<Entry>, uid: String): PremiumMatchSummary {
        val records = entries.mapIndexed { index, entry ->
            PremiumReviewWord(index, entry,
                match.mistakesBy[index]?.get(uid)?.coerceAtLeast(0) ?: 0,
                uid in (match.unknownBy[index] ?: emptySet()))
        }
        return PremiumMatchSummary(
            correct = match.solvedBy.count { it.value == uid },
            wrong = records.sumOf { it.wrongAttempts },
            unknown = records.count { it.unknown },
            review = records.filter { it.needsReview },
            all = records,
        )
    }

    fun quiz(match: PremiumOnlineMatch, questions: List<PremiumQuestion>, uid: String): PremiumMatchSummary {
        val answers = match.answers[uid].orEmpty()
        val records = questions.mapIndexedNotNull { index, q ->
            val selected = answers[index] ?: return@mapIndexedNotNull null
            val entry = Entry(q.answer, q.prompt.substringBefore(" — "), "Arena bilgi yarışması")
            PremiumReviewWord(index, entry,
                if (selected >= 0 && selected != q.correct) 1 else 0, selected < 0)
        }
        return PremiumMatchSummary(
            correct = questions.indices.count { index -> answers[index] == questions[index].correct },
            wrong = records.sumOf { it.wrongAttempts },
            unknown = records.count { it.unknown },
            review = records.filter { it.needsReview },
            all = records,
        )
    }

    fun prompt(summary: PremiumMatchSummary): String {
        val words = summary.review.take(18).joinToString("; ") { record ->
            "${record.entry.answer}: ${record.entry.clue}" +
                if (record.unknown) " (bilmiyorum)" else " (yanlış)"
        }
        return ("Bu WordQuest AI yarışmasının ardından bana Türkiye Türkçesiyle İngilizce öğret. " +
            "Doğru: ${summary.correct}, yanlış deneme: ${summary.wrong}, bilmiyorum: ${summary.unknown}. " +
            "Tekrar çalışacağım kelimeler: ${words.ifBlank { "Yeni kelimelerle kısa bir tekrar yapalım" }}. " +
            "Her kelimenin anlamını, telaffuzunu ve iki örnek cümlesini göster; ardından beni sınava al.")
            .take(1900)
    }
}
