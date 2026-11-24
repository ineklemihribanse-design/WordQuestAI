package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq215ImeExitTest {
    @Test fun actualClosingRestoresOverviewBeforeLastFrame() {
        val closing = Wq215ImeExit.isClosing(true, 400, 0)
        assertTrue(closing)
        assertFalse(Wq215ImeExit.typingLayout(true, closing, 0, true, true))
        assertFalse(Wq215ImeExit.typingLayout(false, closing, 0, true, true))
    }
    @Test fun openingAndVisibleKeyboardDoNotExitTyping() {
        assertFalse(Wq215ImeExit.isClosing(true,0,400))
        assertFalse(Wq215ImeExit.isClosing(true,400,400))
        assertTrue(Wq215ImeExit.typingLayout(true,false,400,true,true))
        assertTrue(Wq215ImeExit.typingLayout(false,false,400,true,true))
    }
    @Test fun finalHiddenFrameDoesNotLatchOldLayout() {
        assertFalse(Wq215ImeExit.typingLayout(false,false,0,true,true))
        assertFalse(Wq215ImeExit.typingLayout(false,false,0,false,false))
    }
    @Test fun oldAndroidUsesActualVisibility() {
        assertFalse(Wq215ImeExit.isClosing(false,400,0))
        assertTrue(Wq215ImeExit.typingLayout(true,false,0,true,true))
        assertFalse(Wq215ImeExit.typingLayout(false,false,0,true,true))
    }
}
