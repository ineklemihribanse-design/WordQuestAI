package com.kelimebulmacasi.a1

/**
 * 100-frame 2D timeline. Every frame was rendered from ONE portrait, so eyes,
 * cheeks, mouth and hair never have independently generated face coordinates.
 * Viseme cues are word-range approximations, not phoneme-perfect lip sync.
 */
internal object Teacher2D100Timeline {
    private val fallback = intArrayOf(1, 2, 4, 1, 3, 5, 2, 6, 4, 1, 2, 6)

    fun select(mode: AvatarMode, cue: AvatarMouthFrame, nowMs: Long, speechStartedMs: Long): Int {
        val now = nowMs.coerceAtLeast(0L)
        if (mode == AvatarMode.SPEAKING && speechStartedMs > 0L) {
            // Breath and natural blink do not re-layer face parts; a 150-ms
            // pre-rendered full-face blink frame briefly closes the mouth.
            val blink = (now + 731L) % 4700L
            if (blink < 165L) return when {
                blink < 55L -> 4
                blink < 110L -> 5
                else -> 6
            }
            val cueAge = now - cue.cueAtMs
            if (cue.quality != LipSyncQuality.NONE && cue.cueAtMs > 0L &&
                cue.weight > .025f && cueAge in 0L..320L) {
                val group = when (cue.viseme) {
                    AvatarViseme.A -> 0
                    AvatarViseme.E -> 1
                    AvatarViseme.I -> 2
                    AvatarViseme.O -> 3
                    AvatarViseme.U -> 4
                    AvatarViseme.MBP, AvatarViseme.NEUTRAL -> 5
                }
                val phase = (cueAge / 32L).toInt().coerceIn(0,9)
                return 10 + group * 10 + phase
            }
            // Some Android TTS engines omit onRangeStart. Fallback is enabled
            // ONLY between actual TTS onStart and onDone/onStop callbacks.
            val elapsed = (now - speechStartedMs).coerceAtLeast(0L)
            val group = fallback[((elapsed / 260L) % fallback.size).toInt()] - 1
            val phase = ((elapsed % 260L) / 26L).toInt().coerceIn(0,9)
            return 10 + group * 10 + phase
        }
        // Blink lasts ~165 ms, NOT multiple 400-ms slides as in the earlier
        // prototype. A normal idle gaze stays open for the remaining cycle.
        val blink = (now + 731L) % 4700L
        if (blink < 165L) {
            val blinkStep = when {
                blink < 55L -> 0
                blink < 110L -> 1
                else -> 2
            }
            return when (mode) {
                AvatarMode.LISTENING -> 74 + blinkStep
                AvatarMode.THINKING -> 85 + blinkStep
                else -> 4 + blinkStep
            }
        }
        return when (mode) {
            AvatarMode.LISTENING -> listOf(70, 71, 72, 73, 77, 78, 79)[((now / 380L) % 7L).toInt()]
            AvatarMode.THINKING -> listOf(80, 81, 82, 83, 84, 88, 89)[((now / 400L) % 7L).toInt()]
            AvatarMode.GREETING -> 90 + ((now / 145L) % 10L).toInt()
            else -> listOf(0, 1, 2, 3, 7, 8, 9)[((now / 460L) % 7L).toInt()]
        }
    }
}