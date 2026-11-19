package com.kelimebulmacasi.a1

/** Added in v1.0.61 to repair Lena v6's unweighted facial bones.
 * Unlike the legacy face bones these are real glTF mesh morph targets.
 * Both clips touch ONLY weights; the body animation touches rotation.
 * Actual Android TTS playback status determines which facial loop is active.
 */
internal object LenaFaceAnimationClips {
    private const val IDLE = "Lena_V161_Face_IdleBlink"
    private const val TALK = "Lena_V161_Face_TalkBlink"

    fun select(mode: AvatarMode, available: Set<String>): String? {
        val wanted = if (mode == AvatarMode.SPEAKING) TALK else IDLE
        return wanted.takeIf { it in available }
    }
}
