package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq206LexicalSpeechTest {
    @Test fun capitalizedShortPuzzleAnswersUseVerifiedRecordings() {
        val known = setOf("us","do","hi","my","we","go","no","be","it")
        for (word in known) {
            assertTrue(Wq206LexicalSpeech.useVerifiedRecording(listOf(word.uppercase()), known::contains))
        }
        assertEquals(listOf("us","do","hi","my"), Wq206LexicalSpeech.deviceText(listOf("US","DO","HI","MY")))
    }
    @Test fun existingDeviceModeForLongWordsIsPreserved() {
        assertFalse(Wq206LexicalSpeech.useVerifiedRecording(listOf("WATER")) { true })
        assertEquals(listOf("WATER"), Wq206LexicalSpeech.deviceText(listOf("WATER")))
    }
    @Test fun batchPreservesOrderAndDoesNotDropMissingCustomWords() {
        val known = setOf("us","water")
        assertTrue(Wq206LexicalSpeech.useVerifiedRecording(listOf("US","water"), known::contains))
        assertFalse(Wq206LexicalSpeech.useVerifiedRecording(listOf("US","unlisted"), known::contains))
        assertEquals(listOf("us","unlisted"), Wq206LexicalSpeech.deviceText(listOf("US","unlisted")))
        assertFalse(Wq206LexicalSpeech.useVerifiedRecording(emptyList(), known::contains))
    }
}
