package wordquest.lena

/**
 * Pure Kotlin Lena animation-state controller. No dependency on a specific 3D renderer.
 * Integrators connect [AnimationPlayer] to their actual GLB animation runtime.
 * Alex and existing teacher assets are unaffected.
 */
enum class LenaState(val clip: String) {
    IDLE("01_Idle_Natural_Breathing"),
    GREETING("02_Greeting_Gentle_Wave"),
    SPEAKING("03_Talking_Hand_Gestures"),
    LISTENING("04_Listening_Nod"),
    THINKING("13_Thinking_Softly")
}

interface AnimationPlayer {
    fun hasClip(name: String): Boolean
    fun play(name: String, loop: Boolean, fadeSeconds: Float)
}

class LenaAnimationController(private val player: AnimationPlayer) {
    var currentState: LenaState? = null
        private set

    /** Returns true only when an available animation was requested. */
    fun transitionTo(state: LenaState): Boolean {
        if (state == currentState) return true
        val preferred = state.clip
        val selected = when {
            player.hasClip(preferred) -> preferred
            player.hasClip(LenaState.IDLE.clip) -> LenaState.IDLE.clip
            else -> return false
        }
        player.play(
            name = selected,
            loop = state != LenaState.GREETING || selected == LenaState.IDLE.clip,
            fadeSeconds = if (currentState == null) 0f else 0.25f
        )
        currentState = if (selected == preferred) state else LenaState.IDLE
        return true
    }

    fun onSpeechStart() = transitionTo(LenaState.SPEAKING)
    fun onSpeechEnd() = transitionTo(LenaState.IDLE)
    fun onListeningStart() = transitionTo(LenaState.LISTENING)
    fun onListeningEnd() = transitionTo(LenaState.IDLE)
    fun onThinkingStart() = transitionTo(LenaState.THINKING)
    fun onGreeting() = transitionTo(LenaState.GREETING)
    fun onGreetingFinished() = transitionTo(LenaState.IDLE)
}
