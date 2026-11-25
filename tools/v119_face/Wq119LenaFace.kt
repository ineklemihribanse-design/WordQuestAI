package com.kelimebulmacasi.a1

import kotlin.math.exp

/** Six weights in the model's exact targetNames order. Independent of body
 * animation; real range/viseme callbacks only, no invented speech timing. */
internal class Wq119LenaFace {
    private val weights = FloatArray(6)
    private var previousMs = -1L
    private var blinkAt = -1L
    private var blinkIndex = 0
    private val intervals = longArrayOf(4200, 5300, 3700, 6100, 4600)
    fun frame(nowMs: Long, speaking: Boolean, viseme: String, amount: Float,
              cueAtMs: Long, quality: String, smile: Boolean,
              enabled: Boolean = true): FloatArray {
        if (previousMs < 0 || nowMs < previousMs) {
            weights.fill(0f)
            previousMs = nowMs
            blinkAt = nowMs + intervals[0]
            blinkIndex = 0
        }
        val dt = (nowMs - previousMs).coerceIn(0L, 80L).toFloat()
        previousMs = nowMs
        val fresh = enabled && speaking && quality != "NONE" && cueAtMs > 0 &&
            nowMs - cueAtMs in 0L..190L && amount.isFinite()
        val index = if (fresh) when(viseme) {
            "A" -> 0
            "E", "I" -> 1
            "O" -> 2
            "U" -> 3
            else -> -1 // closed lips for MBP/neutral
        } else -1
        val voice = if (index >= 0) amount.coerceIn(0f,1f) else 0f
        val tau = if (fresh) 48f else 38f
        val k = 1f - exp(-dt / tau)
        for (i in 0..3) {
            val target = if (i == index) voice else 0f
            weights[i] += (target - weights[i]) * k
            if (weights[i] < .003f) weights[i] = 0f
        }
        val targetSmile = if(enabled && smile) (if(speaking) .16f else .32f) else 0f
        weights[4] += (targetSmile - weights[4]) * (1f - exp(-dt / 220f))
        if (weights[4] < .003f) weights[4] = 0f
        if (nowMs > blinkAt + 190L) {
            blinkIndex = (blinkIndex+1) % intervals.size
            blinkAt = nowMs + intervals[blinkIndex]
        }
        val phase = nowMs - blinkAt
        val blink = when {
            phase in 0L..70L -> phase / 70f
            phase in 71L..185L -> 1f - (phase-70L)/115f
            else -> 0f
        }.coerceIn(0f,1f)
        weights[5] = if(enabled) blink*blink*(3f-2f*blink) else 0f
        if(!enabled) weights.fill(0f)
        return weights
    }
}
