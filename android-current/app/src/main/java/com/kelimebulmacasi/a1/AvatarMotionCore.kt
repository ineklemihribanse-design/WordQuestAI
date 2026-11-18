package com.kelimebulmacasi.a1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** These values describe what the *real* microphone and audio player are doing. */
internal enum class AvatarMode { IDLE, GREETING, LISTENING, THINKING, SPEAKING, ERROR }
internal enum class AvatarViseme { NEUTRAL, A, E, I, O, U, MBP }
internal enum class AvatarGesture { REST, OPEN_PALM, EXPLAIN, SMALL_NOD, WAVE }
internal enum class LipSyncQuality { NONE, TEXT_RANGE_APPROXIMATION, TIMED_VISEME, AUDIO_ENVELOPE }

internal data class AvatarMouthFrame(
    val viseme: AvatarViseme = AvatarViseme.NEUTRAL,
    val weight: Float = 0f,
    val quality: LipSyncQuality = LipSyncQuality.NONE,
    val cueSequence: Long = 0L,
    val cueAtMs: Long = 0L,
)

internal data class AvatarMotionFrame(
    val mode: AvatarMode,
    val gesture: AvatarGesture,
    val blink: Float,
    val headTiltDegrees: Float,
    val breath: Float,
)

/**
 * Compose-observable lifecycle reducer: never derives SPEAKING from the AI response
 * being received or the 'play' button being tapped. Wait for actual audio onStart.
 * One session at a time; late audio callbacks from a previous session are ignored.
 */
internal class AvatarStateController(initialActor: String) {
    var actorId: String = initialActor
        private set
    // Compose observes actual microphone/TTS lifecycle state. Without
    // snapshot state, a GREETING -> IDLE transition may not redraw the model.
    var mode: AvatarMode by mutableStateOf(AvatarMode.IDLE)
        private set
    var activeAudioId: Long? = null
        private set
    private var generation = 0L
    private var rejectedId: Long? = null

    fun selectActor(id: String) {
        if (id == actorId) return
        rejectedId = activeAudioId
        actorId = id
        activeAudioId = null
        mode = AvatarMode.IDLE
        generation++
    }
    fun micStarted() {
        activeAudioId = null
        mode = AvatarMode.LISTENING
        generation++
    }
    fun micCancelled() {
        if (mode == AvatarMode.LISTENING) mode = AvatarMode.IDLE
    }
    fun responsePending() {
        activeAudioId = null
        mode = AvatarMode.THINKING
        generation++
    }
    fun responseAvailable() {
        if (mode == AvatarMode.THINKING) mode = AvatarMode.IDLE
    }
    fun failure() {
        activeAudioId = null
        mode = AvatarMode.ERROR
        generation++
    }
    fun clearError() {
        if (mode == AvatarMode.ERROR) mode = AvatarMode.IDLE
    }
    fun greeting() {
        if (mode == AvatarMode.IDLE) mode = AvatarMode.GREETING
    }
    fun greetingFinished() {
        if (mode == AvatarMode.GREETING) mode = AvatarMode.IDLE
    }
    fun audioStarted(turnId: Long): Boolean {
        if (turnId == rejectedId || mode == AvatarMode.LISTENING) return false
        if (activeAudioId != null && activeAudioId != turnId) return false
        activeAudioId = turnId
        mode = AvatarMode.SPEAKING
        generation++
        return true
    }
    fun audioFinished(turnId: Long?) {
        if (turnId == null || activeAudioId == turnId) {
            activeAudioId = null
            if (mode == AvatarMode.SPEAKING) mode = AvatarMode.IDLE
            generation++
        }
    }
    fun currentGeneration(): Long = generation
}

/**
 * The mouth is **not** driven by invented phoneme timestamps.
 * Accept authentic timed visemes from a capable engine if available.
 * Android TTS onRangeStart only supports TEXT_RANGE_APPROXIMATION;
 * no engine guarantee of phoneme timing should ever be inferred.
 */
internal class AvatarLipSyncController {
    var activeTurnId: Long? = null
        private set
    private var previous = AvatarMouthFrame()
    private var cueAtMs: Long = 0L
    private var cueSequence: Long = 0L

    fun start(id: Long) {
        activeTurnId = id
        previous = AvatarMouthFrame()
        cueAtMs = 0L
    }
    fun stop(id: Long? = null) {
        if (id == null || activeTurnId == id) {
            activeTurnId = null
            previous = AvatarMouthFrame()
            cueAtMs = 0L
        }
    }
    fun push(
        turnId: Long,
        viseme: AvatarViseme,
        atMs: Long,
        quality: LipSyncQuality,
        strength: Float = 1f,
    ): Boolean {
        if (turnId != activeTurnId || atMs < cueAtMs ||
            quality == LipSyncQuality.NONE) return false
        cueAtMs = atMs
        cueSequence++
        previous = AvatarMouthFrame(viseme,strength.coerceIn(0f,1f),quality,cueSequence,atMs)
        return true
    }
    fun frame(nowMs: Long): AvatarMouthFrame {
        if (activeTurnId == null || cueAtMs == 0L) return AvatarMouthFrame()
        val elapsed = (nowMs - cueAtMs).coerceAtLeast(0L)
        val window = if (previous.quality == LipSyncQuality.TIMED_VISEME) 190L else 150L
        val fade = (1f - elapsed.toFloat()/window).coerceIn(0f,1f)
        return previous.copy(weight=previous.weight * fade)
    }
    companion object {
        /** Coarse mouth pose from an actual range callback; NOT phoneme lip sync. */
        fun fromTextRange(text: String): AvatarViseme {
            val sample = text.trim().lowercase()
            if (sample.isEmpty()) return AvatarViseme.NEUTRAL
            val consonant = sample.firstOrNull { it.isLetter() }
            if (consonant in listOf('m','b','p')) return AvatarViseme.MBP
            return when (sample.firstOrNull { it in "aeiou" }) {
                'a' -> AvatarViseme.A
                'e' -> AvatarViseme.E
                'i' -> AvatarViseme.I
                'o' -> AvatarViseme.O
                'u' -> AvatarViseme.U
                else -> AvatarViseme.NEUTRAL
            }
        }
    }
}

/**
 * Small deterministic gestures, avoiding choreographed dances/repeated waves.
 * With no active voice these are idle/blink only.
 */
internal class AvatarGestureController {
    fun frame(
        mode: AvatarMode, elapsedMs: Long, actor: String,
    ): AvatarMotionFrame {
        val time = elapsedMs.coerceAtLeast(0L).toFloat()/1000f
        val period = when (actor) {
            "Deniz" -> 3.8f // Sophia is a little more energetic
            "Mert" -> 6.6f  // Ethan more reserved
            else -> 5.1f     // Emma patient/explanatory
        }
        val phase = time % period
        val gesture = when (mode) {
            AvatarMode.GREETING -> if (time < 1.6f) AvatarGesture.WAVE else AvatarGesture.REST
            AvatarMode.SPEAKING -> when {
                phase > 1.4f && phase < 2.15f -> AvatarGesture.OPEN_PALM
                phase > 3.0f && phase < 3.5f -> AvatarGesture.EXPLAIN
                else -> AvatarGesture.REST
            }
            AvatarMode.LISTENING -> if (phase > 2f && phase < 2.22f)
                AvatarGesture.SMALL_NOD else AvatarGesture.REST
            else -> AvatarGesture.REST
        }
        val blinkCycle = (time + when(actor){"Deniz"->1.0f;"Mert"->.4f;else->0f}) % 4.8f
        val blink = if (blinkCycle >= 4.64f) 1f else 0f
        val nod = if (gesture==AvatarGesture.SMALL_NOD) 4f else 0f
        val head = nod + when (mode) {
            AvatarMode.SPEAKING -> sin(time*1.3f)*1.4f
            AvatarMode.THINKING -> 2.5f
            else -> sin(time*.7f)*.5f
        }
        return AvatarMotionFrame(
            mode=mode,
            gesture=gesture,
            blink=blink,
            headTiltDegrees=head,
            breath=.5f+.5f*sin(time*1.2f),
        )
    }
}

/** Camera settings expressed as source-independent full-body/face presets. */
internal class AvatarCameraController {
    enum class Preset { FULL_BODY, WAIST, FACE }
    var preset=Preset.FULL_BODY
        private set
    var orbitYawDegrees=0f
        private set
    var zoom=1f
        private set

    fun changePreset(next: Preset) { preset=next;zoom=1f }
    fun drag(deltaDegrees: Float) {
        orbitYawDegrees=((orbitYawDegrees+deltaDegrees)%360f+360f)%360f
    }
    fun pinch(factor: Float) {
        if (factor.isFinite() && factor>0f) zoom=(zoom*factor).coerceIn(.55f,2.5f)
    }
    fun reset() {
        preset=Preset.FULL_BODY
        zoom=1f
        orbitYawDegrees=0f
    }
}
