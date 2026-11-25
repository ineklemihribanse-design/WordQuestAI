package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq117LenaMotionTest {
    @Test fun actualReplyIntentInBothLanguages() {
        val cases = mapOf(
            "Hello! How are you?" to Wq117LenaMotion.Intent.GREETING,
            "Hi there!" to Wq117LenaMotion.Intent.GREETING,
            "Well done! You got it." to Wq117LenaMotion.Intent.PRAISE,
            "Aferin! Çok iyi." to Wq117LenaMotion.Intent.PRAISE,
            "Teşekkür ederim." to Wq117LenaMotion.Intent.THANKS,
            "Not quite. Let's try again." to Wq117LenaMotion.Intent.CORRECTION,
            "Güzel deneme, tekrar deneyelim." to Wq117LenaMotion.Intent.CORRECTION,
            "Let me explain with an example." to Wq117LenaMotion.Intent.EXPLANATION,
            "What do you think?" to Wq117LenaMotion.Intent.QUESTION,
            "See you tomorrow!" to Wq117LenaMotion.Intent.FAREWELL,
            "This is not great." to Wq117LenaMotion.Intent.NEUTRAL,
            "I have a book." to Wq117LenaMotion.Intent.NEUTRAL,
            "Exactly! That's right." to Wq117LenaMotion.Intent.AGREEMENT
        )
        for ((reply,intent) in cases) assertEquals(reply,intent,Wq117LenaMotion.classify(reply))
    }
    @Test fun availableClipRotationAndPostureIsolation() {
        val names = setOf("Mixamo_Standing_Clap","Mixamo_Happy","Mixamo_Sitting_Clap","Mixamo_Waving")
        assertEquals("Mixamo_Standing_Clap",Wq117LenaMotion.choose("Excellent!",false,names,0))
        assertEquals("Mixamo_Happy",Wq117LenaMotion.choose("Excellent!",false,names,1))
        assertEquals("Mixamo_Sitting_Clap",Wq117LenaMotion.choose("Excellent!",true,names,0))
        assertNull(Wq117LenaMotion.choose("Hello!",true,names,0))
        assertNull(Wq117LenaMotion.choose("Hello!",false,emptySet(),0))
        assertNull(Wq117LenaMotion.choose("",false,names,0))
    }
}
