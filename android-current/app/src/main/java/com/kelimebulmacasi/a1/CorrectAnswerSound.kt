package com.kelimebulmacasi.a1

import android.content.Context

/** Shares the game/media sound engine with the Arena so effects work on phones. */
internal class CorrectAnswerSound(context: Context) : AutoCloseable {
    private val sounds = PremiumFeedbackSounds(context)
    fun play(enabled: Boolean, volume: Float) = sounds.playCorrect(enabled, volume)
    fun wrong(enabled: Boolean, volume: Float) = sounds.playWrong(enabled, volume)
    override fun close() = sounds.close()
}
