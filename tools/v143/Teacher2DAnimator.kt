package com.kelimebulmacasi.a1

import kotlin.math.PI
import kotlin.math.sin

/**
 * Visual timing plan for the REAL layered 2D rig. Inputs are actual lifecycle
 * mode, Android TTS onRangeStart cues and monotonic elapsed time.
 *
 * When an Android TTS engine emits no onRangeStart callbacks, the mouth must
 * still move while Android onStart reports that speech is playing. That path
 * is explicitly only a playback-gated approximation, NOT phoneme alignment.
 */
internal object Teacher2DAnimator {
    private val fallbackVisemes = intArrayOf(0, 1, 2, 4, 2, 6, 3, 1, 5, 2, 0, 4, 1, 3, 2, 6)

    fun mouthIndex(
        mode: AvatarMode,
        cue: AvatarMouthFrame,
        nowMs: Long,
        speechStartedAtMs: Long,
    ): Int {
        if (mode != AvatarMode.SPEAKING || speechStartedAtMs <= 0L) return 0
        val cueAge = nowMs - cue.cueAtMs
        if (cue.quality != LipSyncQuality.NONE &&
            cue.cueAtMs > 0L && cueAge in 0L..350L &&
            cue.weight > .025f) {
            return when (cue.viseme) {
                AvatarViseme.NEUTRAL -> 0
                AvatarViseme.A -> 1
                AvatarViseme.E -> 2
                AvatarViseme.I -> 3
                AvatarViseme.O -> 4
                AvatarViseme.U -> 5
                AvatarViseme.MBP -> 6
            }
        }
        // No device range events? Keep visible lips active ONLY during onStart→onDone.
        // Irregular 125-ms patterns reduce mechanical opening and closing.
        val slot = ((nowMs - speechStartedAtMs).coerceAtLeast(0L) / 125L)
        return fallbackVisemes[(slot % fallbackVisemes.size).toInt()]
    }

    /** 4.4-second natural eyelid cycle: half, closed, half, open. */
    fun blinkIndex(nowMs: Long): Int = when ((nowMs + 1187L) % 4400L) {
        in 0L..60L -> 1
        in 61L..125L -> 2
        in 126L..185L -> 1
        else -> 0
    }

    internal data class Gesture(val pose: Int, val alpha: Float)

    fun gesture(
        mode: AvatarMode,
        nowMs: Long,
        speechStartedAtMs: Long,
    ): Gesture {
        if (mode != AvatarMode.SPEAKING || speechStartedAtMs <= 0L) {
            return Gesture(0, 0f)
        }
        val elapsed = (nowMs - speechStartedAtMs).coerceAtLeast(0L)
        val cycle = elapsed % 4300L
        if (cycle < 350L || cycle > 2550L) return Gesture(0, 0f)
        val phase = (cycle - 350L).toFloat() / 2200f
        val amplitude = sin(PI * phase).toFloat().coerceIn(0f, 1f)
        val pose = 1 + ((elapsed / 4300L) % 3L).toInt()
        return Gesture(pose, amplitude * .88f)
    }

    fun headTiltDegrees(mode: AvatarMode, nowMs: Long): Float {
        val speed = if (mode == AvatarMode.SPEAKING) 770.0 else 1880.0
        val amplitude = if (mode == AvatarMode.SPEAKING) 1.1 else .55
        return (sin(nowMs / speed) * amplitude).toFloat()
    }

    fun headBobPx(mode: AvatarMode, nowMs: Long): Float {
        val speed = if (mode == AvatarMode.SPEAKING) 1150.0 else 2450.0
        val amplitude = if (mode == AvatarMode.SPEAKING) 1.7 else .8
        return (sin(nowMs / speed) * amplitude).toFloat()
    }
}
