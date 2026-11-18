package com.kelimebulmacasi.a1

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToLong

/** FSRS-6 memory model with its published default weights and a 90% recall target. */
internal object Fsrs {
    private val weights = doubleArrayOf(
        0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001,
        1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014,
        1.8729, 0.5425, 0.0912, 0.0658, 0.1542,
    )
    private const val MAX_INTERVAL = 36_500L
    private const val RETENTION = 0.90
    private val decay = weights[20]
    private val factor = RETENTION.pow(-1 / decay) - 1

    fun update(previous: ReviewMemory?, rating: ReviewRating, now: Long): ReviewMemory {
        val grade = rating.grade
        if (previous == null) {
            return ReviewMemory(weights[grade - 1], initialDifficulty(grade).coerceIn(1.0, 10.0), now)
        }

        val elapsedDays = (now - previous.reviewedAt).coerceAtLeast(0) / LearningSchedule.DAY
        val stability = previous.stability
        val difficulty = previous.difficulty
        val updatedStability = if (elapsedDays == 0L) {
            val multiplier = exp(weights[17] * (grade - 3 + weights[18])) * stability.pow(-weights[19])
            stability * if (rating == ReviewRating.Again) multiplier else multiplier.coerceAtLeast(1.0)
        } else {
            val recall = (1 + factor * elapsedDays / stability).pow(-decay)
            if (rating == ReviewRating.Again) {
                val forgotten = weights[11] * difficulty.pow(-weights[12]) *
                    ((stability + 1).pow(weights[13]) - 1) * exp(weights[14] * (1 - recall))
                minOf(forgotten, stability / exp(weights[17] * weights[18]))
            } else {
                val hard = if (rating == ReviewRating.Hard) weights[15] else 1.0
                val easy = if (rating == ReviewRating.Easy) weights[16] else 1.0
                stability * (1 + exp(weights[8]) * (11 - difficulty) * stability.pow(-weights[9]) *
                    (exp(weights[10] * (1 - recall)) - 1) * hard * easy)
            }
        }
        val damped = difficulty - weights[6] * (grade - 3) * (10 - difficulty) / 9
        val updatedDifficulty = weights[7] * initialDifficulty(4) + (1 - weights[7]) * damped
        return ReviewMemory(updatedStability.coerceIn(0.001, MAX_INTERVAL.toDouble()),
            updatedDifficulty.coerceIn(1.0, 10.0), now)
    }

    fun intervalDays(memory: ReviewMemory): Long = memory.stability.roundToLong().coerceIn(1, MAX_INTERVAL)

    private fun initialDifficulty(grade: Int): Double = weights[4] - exp(weights[5] * (grade - 1)) + 1
}
