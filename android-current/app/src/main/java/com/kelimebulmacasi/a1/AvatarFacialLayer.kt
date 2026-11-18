package com.kelimebulmacasi.a1

/**
 * Single-owner facial compositor for native SceneView. All ARKit face clips
 * write the same 52 glTF morph weights. Two face clips playing at once do not
 * add together: the last applied clip overwrites the first. Choose ONE
 * precomposed mouth+blink clip, while body clips remain independent.
 *
 * This returns a clip name, not a Filament object, so it can be JVM-tested.
 * Actual device rendering and lip-sync fidelity still require QA.
 */
internal class AvatarFacialLayer {
    private var actorId: String = ""
    private var epochMs: Long = -1L

    /** Composited face clip name for this frame, always a valid static asset. */
    fun frame(
        actor: String,
        mode: AvatarMode,
        cue: AvatarMouthFrame,
        nowMs: Long,
    ): String {
        if (actor != actorId || epochMs < 0L || nowMs < epochMs) {
            actorId = actor
            epochMs = nowMs.coerceAtLeast(0L)
        }

        // Starting another speech response sets a new cue timestamp; never
        // display the previous session's vowel in LISTENING, IDLE or THINKING.
        val elapsed = if (nowMs >= cue.cueAtMs) nowMs - cue.cueAtMs else Long.MAX_VALUE
        val mouthActive = mode == AvatarMode.SPEAKING &&
            cue.quality != LipSyncQuality.NONE &&
            cue.cueAtMs > 0L &&
            cue.weight > 0f &&
            elapsed in 0L..170L

        // Short asymmetric blink that is consistent on a given actor.
        // All actors blink even while listening or speaking. There are no
        // external timers and the face remains neutral on app suspension.
        val period = when(actor) {
            "Deniz" -> 4100L
            "Mert" -> 5200L
            else -> 4650L
        }
        val seedOffset = when(actor) {
            "Deniz" -> 900L
            "Mert" -> 1400L
            else -> 500L
        }
        val blinkPhase = ((nowMs - epochMs + seedOffset) % period + period) % period
        val blink = mode != AvatarMode.ERROR && blinkPhase < 260L

        val vowel = if (mouthActive) cue.viseme.name else "NEUTRAL"
        return "Teacher_Viseme_" + vowel + if (blink) "_Blink" else ""
    }

    fun reset() {
        actorId = ""
        epochMs = -1L
    }
}
