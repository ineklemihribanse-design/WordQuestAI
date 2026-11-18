package com.kelimebulmacasi.a1

/**
 * Subtle real-speech pacing for Ethan's existing licensed human Talk clip.
 *
 * Uses ONLY actual Android TTS range callback cues from AvatarMouthFrame.
 * No claims about emotions, phoneme accuracy, listening, or hidden thoughts.
 * The animator remains the presentation layer; Gemini generates the words.
 *
 * A real speech callback can briefly energise a gesture. An old callback
 * from a previous turn cannot keep the model animated once speech ends.
 */
internal class EthanPresenceRhythm {
    private var lastCueSequence = -1L
    private var lastCueTimeMs = -1L
    private var lastStrongBeatMs = -1L

    fun speed(mode: AvatarMode, cue: AvatarMouthFrame, nowMs: Long): Float {
        if (mode != AvatarMode.SPEAKING) {
            reset()
            return 1f
        }

        val validCue = cue.quality != LipSyncQuality.NONE &&
            cue.weight > 0f &&
            cue.cueAtMs > 0L &&
            cue.cueAtMs <= nowMs &&
            nowMs - cue.cueAtMs <= 450L

        if (validCue && cue.cueSequence != lastCueSequence) {
            lastCueSequence = cue.cueSequence
            lastCueTimeMs = nowMs
            if (lastStrongBeatMs < 0L || nowMs - lastStrongBeatMs >= 1700L) {
                lastStrongBeatMs = nowMs
            }
        }

        // Approximate cadence rather than falsely treating a word-range
        // callback as accurate audio energy or a recognized phoneme.
        if (lastCueTimeMs < 0L) return 1f
        val sinceWord = (nowMs - lastCueTimeMs).coerceAtLeast(0L)
        return when {
            sinceWord < 220L && nowMs - lastStrongBeatMs < 350L -> 1.055f
            sinceWord < 380L -> 1.015f
            sinceWord > 1200L -> .96f
            else -> 1f
        }
    }

    fun reset() {
        lastCueSequence = -1L
        lastCueTimeMs = -1L
        lastStrongBeatMs = -1L
    }
}
