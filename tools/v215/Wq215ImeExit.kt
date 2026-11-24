package com.kelimebulmacasi.a1

internal object Wq215ImeExit {
    fun isClosing(supportsAnimation: Boolean, sourcePx: Int, targetPx: Int): Boolean =
        supportsAnimation && sourcePx > 0 && targetPx == 0

    fun typingLayout(visible: Boolean, closing: Boolean, targetPx: Int,
        typing: Boolean, wasShown: Boolean): Boolean =
        !closing && (visible || (targetPx > 0 && typing && wasShown))
}
