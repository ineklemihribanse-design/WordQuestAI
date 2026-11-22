package com.kelimebulmacasi.a1

/** Pure layout calculations. Keyboard status must NEVER change crossword cell geometry. */
internal object Wq186ArenaGridPolicy {
    fun cellDp(availableWidthDp:Float, columns:Int, zoomed:Boolean):Float {
        val cols=columns.coerceAtLeast(1)
        val gap=if(availableWidthDp<500f)1f else 1.5f
        val fit=((availableWidthDp-8f-(cols-1)*gap)/cols).coerceIn(10f,60f)
        val minimum=if(availableWidthDp<500f)25f else 28f
        val enlarged=if(availableWidthDp<500f)36f else 46f
        return if(zoomed) maxOf(fit,enlarged) else maxOf(fit,minimum)
    }
    fun viewportDp(screenHeightDp:Float,boardHeightDp:Float,
                   keyboardVisible:Boolean):Float {
        val cap=if(keyboardVisible)
            (screenHeightDp*.31f).coerceIn(180f,300f)
        else (screenHeightDp*.72f).coerceIn(260f,900f)
        return minOf(cap,boardHeightDp.coerceAtLeast(1f))
    }
}
