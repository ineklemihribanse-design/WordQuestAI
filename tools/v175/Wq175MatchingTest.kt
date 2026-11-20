package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq175MatchingTest {
    private fun row(level:String,english:String,turkish:String,kind:String="İsimler") =
        "$level\tGenel İngilizce\t$kind\t$english\t$turkish"

    @Test fun excludesDuplicateEnglishAndAmbiguousTurkish() {
        val input=sequenceOf(
            row("A1","APPLE","elma"),
            row("A1","APPLE","farklı elma"),
            row("A1","PEAR","armut"),
            row("A1","ORANGE","elma"),
            row("A1","BASKET","sepet (kap)"),
            row("C2","ADVOCATE","savunmak")
        )
        val found=Wq175MatchingBank.parse(input,"A1")
        assertEquals(2,found.size)
        assertEquals(setOf("APPLE","PEAR"),found.map{it.english}.toSet())
        assertTrue(found.all{it.level=="A1"})
    }

    @Test fun selectionRemainsDistinctAndBounded() {
        val list=(0 until 20).map {
            Wq175MatchPair("A1","İsimler","WORD$it","Anlam$it")
        }
        val selected=Wq175MatchingBank.prepare(list,8)
        assertEquals(8,selected.size)
        assertEquals(8,selected.map{it.id}.distinct().size)
        assertTrue(selected.all{it in list})
        val ambiguous=listOf(
            Wq175MatchPair("A1","İsimler","CAR","araba"),
            Wq175MatchPair("A1","İsimler","AUTO","araba"),
            Wq175MatchPair("A1","İsimler","BOOK","kitap"))
        assertEquals(2,Wq175MatchingBank.prepare(ambiguous,8).size)
    }

    @Test fun wrongAnswersAreNotArtificiallyMarkedCorrect() {
        val a=Wq175MatchPair("A1","İsimler","APPLE","elma")
        val b=Wq175MatchPair("A1","İsimler","PEAR","armut")
        assertNotEquals(a.id,b.id)
    }

    @Test fun levelAndWordTypeComeFromActualRows() {
        val items=Wq175MatchingBank.parse(sequenceOf(
            row("B2","ACHIEVE","başarmak","Fiiller"),
            row("A1","BOOK","kitap"),
            row("B2","TABLE","masa")
        ),"B2")
        assertEquals(2,items.size)
        assertEquals("Fiiller",items.first().kind)
    }
}
