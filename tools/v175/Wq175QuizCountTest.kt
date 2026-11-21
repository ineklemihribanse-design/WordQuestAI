package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq175QuizCountTest {
    @Test fun realOptionsAndLegacyTwelveAccepted() {
        listOf(5,10,12,15,20).forEach { count ->
            assertTrue(PremiumOnlineLimits.validConfig("A1", count, 30, "quiz"))
        }
        assertFalse(PremiumOnlineLimits.validConfig("A1",4,30,"quiz"))
        assertFalse(PremiumOnlineLimits.validConfig("A1",30,30,"quiz"))
        assertTrue(PremiumOnlineLimits.validConfig("A1",20,30,"duel"))
    }
    private fun match(count:Int,host:Int,guest:Int)=PremiumOnlineMatch(
        "ABCDEF234","host","guest","A","B","playing","quiz","","A1",
        "Tüm konular",count,30,2,"host",0L,emptyMap(),
        mapOf("host" to (0 until host).associateWith{0},
              "guest" to (0 until guest).associateWith{0})
    )
    @Test fun roomEndsWhenBothPlayersFinishTheirSelectedQuestionCount() {
        assertFalse(match(5,5,4).complete)
        assertTrue(match(5,5,5).complete)
        assertFalse(match(20,20,19).complete)
        assertTrue(match(20,20,20).complete)
        assertTrue(match(12,12,12).complete)
    }
}
