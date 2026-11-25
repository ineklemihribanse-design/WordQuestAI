package com.kelimebulmacasi.a1

/** Pure render-clock controller. One native clip and, during a transition only,
 * one previous clip. No synthetic joint edits, overlapping players or T-pose reset.
 * The last displayed pose is the start of every fade. Rapid changes coalesce
 * until the current fade is complete, rather than snapping to an unmixed clip.
 */
internal class Wq118LenaBlend {
    data class Clip(val index: Int, val duration: Float, val loop: Boolean)
    data class Sample(val current: Int, val time: Float,
                      val previous: Int? = null, val previousTime: Float = 0f,
                      val alpha: Float = 1f)
    private data class Fade(val old: Clip, val oldTime: Float, val seconds: Float,
                            var elapsed: Float = 0f)
    private var clip: Clip? = null
    private var time = 0f
    private var fade: Fade? = null
    private var pending: Clip? = null
    private var lastFrame: Long? = null

    fun request(index: Int, duration: Float, loop: Boolean, replay: Boolean = false) {
        require(index >= 0 && duration.isFinite() && duration > 0f)
        val next = Clip(index, duration, loop)
        if (!replay && next == clip) {
            pending = null
            return
        }
        if (!replay && next == pending) return
        pending = next
    }

    private fun begin(next: Clip) {
        val old = clip
        fade = old?.let {
            Fade(it, time, minOf(.60f, it.duration * .40f, next.duration * .40f))
        }
        clip = next
        time = 0f
    }

    fun frame(nanos: Long): Sample? {
        val oldFrame = lastFrame
        lastFrame = nanos
        val dt = if (oldFrame == null || nanos <= oldFrame) 0f
            else ((nanos - oldFrame) / 1_000_000_000.0).toFloat().coerceIn(0f,.10f)
        val next = pending
        if (clip == null || (fade == null && next != null)) {
            if (next == null) return null
            pending = null
            begin(next)
            // Exactly preserve the outgoing pose on the first rendered frame.
            return sample()
        }
        val current = clip ?: return null
        val f = fade
        if (f != null) {
            f.elapsed = (f.elapsed + dt).coerceAtMost(f.seconds)
            time = (time + dt).coerceAtMost(current.duration)
            if (f.elapsed >= f.seconds) fade = null
            return sample()
        }
        if (time >= current.duration && current.loop) {
            begin(current)
            return sample()
        }
        time = (time + dt).coerceAtMost(current.duration)
        return sample()
    }

    private fun sample(): Sample? {
        val c = clip ?: return null
        val f = fade ?: return Sample(c.index, time)
        val t = (f.elapsed / f.seconds).coerceIn(0f,1f)
        val eased = t*t*(3f-2f*t)
        return Sample(c.index,time,f.old.index,
            (f.oldTime + f.elapsed).coerceAtMost(f.old.duration),eased)
    }
}
