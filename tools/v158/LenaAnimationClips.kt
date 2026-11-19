package com.kelimebulmacasi.a1

/** Exact animation names from the shipped Lena v6 GLB.
 * Missing clips fall back to the idle animation rather than crashing.
 */
internal object LenaAnimationClips {
    private const val IDLE = "01_Idle_Natural_Breathing"

    fun select(motion: AvatarMode, available: Set<String>): String? {
        val desired = when (motion) {
            AvatarMode.GREETING -> "02_Greeting_Gentle_Wave"
            AvatarMode.LISTENING -> "04_Listening_Nod"
            AvatarMode.THINKING -> "13_Thinking_Softly"
            AvatarMode.SPEAKING -> "03_Talking_Hand_Gestures"
            AvatarMode.ERROR, AvatarMode.IDLE -> IDLE
        }
        return when {
            desired in available -> desired
            IDLE in available -> IDLE
            else -> null
        }
    }
}
