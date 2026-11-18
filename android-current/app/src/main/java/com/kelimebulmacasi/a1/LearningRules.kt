package com.kelimebulmacasi.a1

import java.security.MessageDigest
import java.util.Locale

/** The learning layer is independent of Android storage so its rules can be tested on the JVM. */
internal enum class LearningKind(val label: String, val rating: ReviewRating? = null) {
    SOLVED("Doğru çözüm"),
    WRONG_CHECK("Yanlış deneme"),
    HINT("Harf ipucu"),
    UNKNOWN("Bilmiyorum"),
    REVIEW_GOOD("Hatırladım", ReviewRating.Good),
    REVIEW_AGAIN("Tekrar çalış", ReviewRating.Again),
    REVIEW_HARD("Zor", ReviewRating.Hard),
    REVIEW_EASY("Kolay", ReviewRating.Easy);

    companion object {
        fun forRating(rating: ReviewRating): LearningKind = entries.first { it.rating == rating }
    }
}

internal enum class ReviewRating(val grade: Int, val label: String) {
    Again(1, "Tekrar çalış"), Hard(2, "Zor"), Good(3, "Hatırladım"), Easy(4, "Kolay"),
}

internal data class ReviewMemory(val stability: Double, val difficulty: Double, val reviewedAt: Long)

internal data class LearningSignal(
    val kind: LearningKind,
    val entry: Entry,
    val resetAt: Long = 0L,
    val attempt: String = "",
    val assisted: Boolean = false,
    val boardUpdatedAt: Long = 0L,
)

internal data class LearningCard(
    val key: String,
    val answer: String,
    val clue: String,
    val context: String,
    val solved: Int = 0,
    val wrong: Int = 0,
    val hints: Int = 0,
    val remembered: Int = 0,
    val forgotten: Int = 0,
    val dueAt: Long = 0L,
    val lastAt: Long = 0L,
    val pinned: Boolean = false,
    val memory: ReviewMemory? = null,
)

internal fun stableHash(text: String): String = MessageDigest.getInstance("SHA-256")
    .digest(text.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it.toInt() and 0xff) }

/** Separate senses: for example RIGHT=sağ and RIGHT=hak are different cards. */
internal fun learningWordKey(entry: Entry): String {
    val clue = entry.clue.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
    return stableHash(englishLetters(entry.answer) + "\u001f" + clue)
}

internal fun learningEventId(gameId: String, signal: LearningSignal): String {
    val boardTime = signal.boardUpdatedAt.takeIf { it > 0 }?.toString().orEmpty()
    val detail = when (signal.kind) {
        LearningKind.WRONG_CHECK -> listOfNotNull(englishLetters(signal.attempt), boardTime.ifEmpty { null }).joinToString("\u001e")
        LearningKind.HINT, LearningKind.UNKNOWN -> boardTime
        else -> ""
    }
    return stableHash(listOf(gameId, signal.resetAt.toString(), learningWordKey(signal.entry),
        signal.kind.name, detail).joinToString("\u001f"))
}

internal val learningSearchLetters = listOf(
    "çÇ" to 'c', "ğĞ" to 'g', "ıİ" to 'i', "öÖ" to 'o', "şŞ" to 's', "üÜ" to 'u',
)

internal fun learningSearchTerm(raw: String): String = raw.trim()
    .map { letter -> learningSearchLetters.firstOrNull { letter in it.first }?.second ?: letter }
    .joinToString("").lowercase(Locale.ROOT)

internal object LearningSchedule {
    const val MINUTE = 60_000L
    const val DAY = 24L * 60L * MINUTE
    private const val QUICK_REVIEW = 10 * MINUTE

    fun next(card: LearningCard, kind: LearningKind, now: Long, assisted: Boolean = false): LearningCard {
        val update = card.copy(lastAt = now)
        val rating = kind.rating
        if (rating != null) return review(update, rating, now)
        return when (kind) {
            LearningKind.WRONG_CHECK -> update.copy(
                wrong = card.wrong + 1, dueAt = earlier(card.dueAt, now + QUICK_REVIEW),
            )
            LearningKind.HINT -> update.copy(
                hints = card.hints + 1, dueAt = earlier(card.dueAt, now + QUICK_REVIEW),
            )
            LearningKind.UNKNOWN -> update.copy(
                forgotten = card.forgotten + 1, dueAt = earlier(card.dueAt, now + QUICK_REVIEW),
            )
            LearningKind.SOLVED -> update.copy(
                solved = card.solved + 1,
                dueAt = when {
                    assisted -> earlier(card.dueAt, now + QUICK_REVIEW)
                    card.memory == null -> earlier(card.dueAt, now + DAY)
                    else -> card.dueAt
                },
            )
            else -> error("Missing review rating")
        }
    }

    fun intervalLabel(card: LearningCard, rating: ReviewRating, now: Long): String {
        if (rating == ReviewRating.Again) return "10 dk"
        return "${Fsrs.intervalDays(Fsrs.update(card.memory, rating, now))} gün"
    }

    private fun review(card: LearningCard, rating: ReviewRating, now: Long): LearningCard {
        val memory = Fsrs.update(card.memory, rating, now)
        val again = rating == ReviewRating.Again
        val interval = if (again) QUICK_REVIEW else Fsrs.intervalDays(memory) * DAY
        return card.copy(
            remembered = card.remembered + if (again) 0 else 1,
            forgotten = card.forgotten + if (again) 1 else 0,
            dueAt = now + interval,
            memory = memory,
        )
    }

    private fun earlier(old: Long, next: Long) = if (old <= 0) next else minOf(old, next)
}
