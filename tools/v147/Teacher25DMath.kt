package com.kelimebulmacasi.a1

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 2.5D deformation model. NO frame atlas, image switching, or dislocated
 * eye/mouth photo patches. One 256x384 human texture is deformed continuously
 * by a shared 33x49 vertex mesh. All regions are anchored in that bitmap's
 * own coordinates: head 128,70; lip centre 122,86; shoulders 128,155.
 *
 * This is the deformation-engine prototype, not a fully rigged artist-authored
 * face model: a flat neutral texture cannot reveal hidden teeth, tongue,
 * or unseen side-of-face geometry. Those require layered artwork/mesh masks.
 */
internal object Teacher25DMath {
    const val WIDTH = 256
    const val HEIGHT = 384
    const val SEG_X = 32
    const val SEG_Y = 48
    const val FLOAT_COUNT = (SEG_X + 1) * (SEG_Y + 1) * 2

    internal data class Pose(
        val jaw: Float = 0f,      // [0, 1] phoneme mouth aperture
        val blink: Float = 0f,    // [0, 1] eyelid deformation
        val breath: Float = 0f,   // [-1, 1], shoulder movement < 0.35px
        val nod: Float = 0f,      // [-1, 1], face centre movement < .3px
        val gesture: Float = 0f,  // [-1, 1], one arm at a time
    )

    private fun clamp01(n: Float): Float = n.coerceIn(0f, 1f)
    private fun bell(d: Float, radius: Float): Float {
        val n = d / radius
        return if (abs(n) >= 1f) 0f else (1f - n * n) * (1f - n * n)
    }

    fun buildVertices(p: Pose, out: FloatArray) {
        require(out.size == FLOAT_COUNT) { "Bad 2.5D vertex buffer length" }
        val jaw = clamp01(p.jaw)
        val blink = clamp01(p.blink)
        var i = 0
        for (row in 0..SEG_Y) {
            val y = row.toFloat() * HEIGHT / SEG_Y
            for (col in 0..SEG_X) {
                val x = col.toFloat() * WIDTH / SEG_X
                // Head-space transform falls to 0 on hairline/neck boundaries.
                val faceMask = bell((x - 128f) / 1.8f, 90f) *
                    bell((y - 80f) / 1.25f, 78f)
                val mouthMask = bell(x - 122f, 29f) * bell(y - 90f, 22f)
                // Upper lip rises, lower lip/chin descends continuously.
                val lipVertical = if (y >= 86f) 2.7f else -0.9f
                val eyeL = bell(x - 109f, 18f) * bell(y - 64f, 13f)
                val eyeR = bell(x - 140f, 18f) * bell(y - 64f, 13f)
                val eyelid = max(eyeL, eyeR)
                val eyeVertical = if (y < 64f) 2.2f else -2.2f
                val torso = bell(x - 128f, 82f) * bell(y - 218f, 94f)
                val rightArm = bell(x - 214f, 34f) * bell(y - 255f, 130f)
                val leftArm = bell(x - 42f, 34f) * bell(y - 255f, 130f)
                val gest = p.gesture.coerceIn(-1f, 1f)
                val armMovement = if (gest > 0f) rightArm * gest else leftArm * -gest
                out[i++] = x +
                    faceMask * p.nod.coerceIn(-1f, 1f) * .30f +
                    armMovement * 1.5f
                out[i++] = y +
                    mouthMask * lipVertical * jaw +
                    eyelid * eyeVertical * blink +
                    torso * p.breath.coerceIn(-1f, 1f) * .32f -
                    armMovement * 1.5f
            }
        }
    }

    /** Interpolate motion independently from frame rate, avoiding vertex jitter. */
    fun approach(current: Float, target: Float, deltaMs: Long, halfLifeMs: Float): Float {
        val elapsed = deltaMs.coerceIn(0L, 100L).toFloat()
        val decay = exp(-.6931472f * elapsed / max(1f, halfLifeMs))
        return current * decay + target * (1f - decay)
    }

    /** Natural, infrequent short eye closure; never frozen closed for seconds. */
    fun eyelid(nowMs: Long): Float {
        val t = ((nowMs + 317L) % 4400L).toFloat()
        if (t >= 180f) return 0f
        return when {
            t < 65f -> t / 65f
            t < 110f -> 1f
            else -> (180f - t) / 70f
        }.coerceIn(0f, 1f)
    }

    /** Approximate mouth cues only while speech AUDIO is genuinely playing. */
    fun mouthTarget(mode: AvatarMode, cue: AvatarMouthFrame, nowMs: Long, speechSince: Long): Float {
        if (mode != AvatarMode.SPEAKING || speechSince <= 0L) return 0f
        val cueAge = nowMs - cue.cueAtMs
        if (cue.quality != LipSyncQuality.NONE && cue.cueAtMs > 0L &&
            cueAge in 0L..280L) {
            val shape = when (cue.viseme) {
                AvatarViseme.NEUTRAL, AvatarViseme.MBP -> 0f
                AvatarViseme.I, AvatarViseme.E -> .38f
                AvatarViseme.U -> .43f
                AvatarViseme.O -> .7f
                AvatarViseme.A -> .92f
            }
            val envelope = 1f - .45f * (cueAge.toFloat() / 280f)
            return shape * envelope
        }
        // Only a playback-gated fallback. Never claim phoneme-perfect sync.
        val elapsed = (nowMs - speechSince).coerceAtLeast(0L)
        return (.12f + .54f * abs(sin(elapsed / 137.0))).toFloat()
    }

    fun breath(nowMs: Long): Float = sin(nowMs / 2600.0).toFloat()
    fun nod(nowMs: Long, speaking: Boolean): Float =
        (sin(nowMs / (if (speaking) 1430.0 else 3100.0)) *
            (if (speaking) .55 else .2)).toFloat()

    /** Most of the time BOTH hands remain down, sometimes ONE arm gestures. */
    fun gesture(nowMs: Long, speechSince: Long, speaking: Boolean): Float {
        if (!speaking || speechSince <= 0L) return 0f
        val elapsed = (nowMs - speechSince).coerceAtLeast(0L)
        val cycle = elapsed % 6500L
        if (cycle < 2300L || cycle > 3600L) return 0f
        val phase = (cycle - 2300L).toDouble() / 1300.0
        val direction = if ((elapsed / 6500L) % 2L == 0L) 1.0 else -1.0
        return (direction * .55 * sin(PI * phase)).toFloat()
    }
}
