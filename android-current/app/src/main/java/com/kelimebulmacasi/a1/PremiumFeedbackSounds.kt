package com.kelimebulmacasi.a1

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlin.math.sqrt

/** Short original game effects on the phone's *media/game* audio channel.
 * MediaPlayer.create returns fully prepared clips; SoundPool.load is asynchronous,
 * which previously dropped the first tap (and some phone ringer modes silenced it).
 * Every failure is contained so a muted/unavailable sound never blocks the game.
 */
internal class PremiumFeedbackSounds(context: Context) : AutoCloseable {
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val appContext = context.applicationContext
    private val correctPlayer = player(R.raw.correct_soft)
    private val wrongPlayer = player(R.raw.wrong_soft)
    private val victoryPlayer = player(R.raw.victory_soft)
    private var closed = false

    private fun player(resId: Int): MediaPlayer? = runCatching {
        MediaPlayer.create(appContext, resId, attributes, 0)
    }.getOrNull()

    private fun play(player: MediaPlayer?, enabled: Boolean, volume: Float) {
        if (closed || !GameEffectPolicy.canPlay(enabled, volume) || player == null) return
        // Perceived loudness follows a curved slider; effects remain gentle yet audible
        // on phone speakers while the exact 0% setting remains fully silent.
        val gain = sqrt(volume.coerceIn(0f, 1f))
        runCatching {
            player.setVolume(gain, gain)
            player.seekTo(0)
            player.start()
        }
    }
    fun playCorrect(enabled: Boolean, volume: Float) = play(correctPlayer, enabled, volume)
    fun playWrong(enabled: Boolean, volume: Float) = play(wrongPlayer, enabled, volume)
    fun correct(preferences: WordQuestPreferences) =
        playCorrect(preferences.correctSoundEnabled, preferences.correctSoundVolume)
    fun wrong(preferences: WordQuestPreferences) =
        playWrong(preferences.correctSoundEnabled, preferences.correctSoundVolume)
    fun victory(preferences: WordQuestPreferences) =
        play(victoryPlayer, preferences.correctSoundEnabled, preferences.correctSoundVolume)
    override fun close() {
        if (closed) return
        closed = true
        listOfNotNull(correctPlayer, wrongPlayer, victoryPlayer).forEach { runCatching { it.release() } }
    }
}

internal object GameEffectPolicy {
    fun canPlay(enabled: Boolean, volume: Float): Boolean = enabled && volume > 0f
}
