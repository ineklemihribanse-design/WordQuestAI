package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Wq196FilterPolicyTest {
    @Test fun defaultsToTopicEvenWithoutAFilter() {
        assertEquals(Wq196FilterPolicy.TOPIC, Wq196FilterPolicy.mode(null, null))
        assertEquals(Wq196FilterPolicy.TOPIC, Wq196FilterPolicy.mode("Nature", "Noun"))
        assertEquals(Wq196FilterPolicy.KIND, Wq196FilterPolicy.mode(null, "Noun"))
    }

    @Test fun topicModeNeverPassesWordKindsToGameGeneration() {
        val topics = Wq196FilterPolicy.activeTopics(Wq196FilterPolicy.TOPIC, listOf("Food", "Work"))
        val kinds = Wq196FilterPolicy.activeKinds(Wq196FilterPolicy.TOPIC, listOf("Noun", "Verb"))
        assertEquals(setOf("Food", "Work"), topics)
        assertTrue(kinds.isEmpty())
    }

    @Test fun kindModeNeverPassesTopicsToGameGeneration() {
        val topics = Wq196FilterPolicy.activeTopics(Wq196FilterPolicy.KIND, listOf("Food", "Travel"))
        val kinds = Wq196FilterPolicy.activeKinds(Wq196FilterPolicy.KIND, listOf("Verb", "Noun"))
        assertTrue(topics.isEmpty())
        assertEquals(setOf("Verb", "Noun"), kinds)
    }

    @Test fun unselectedModeKeepsWholeWordPoolAvailable() {
        assertTrue(Wq196FilterPolicy.activeTopics(Wq196FilterPolicy.TOPIC, emptyList()).isEmpty())
        assertTrue(Wq196FilterPolicy.activeKinds(Wq196FilterPolicy.KIND, emptyList()).isEmpty())
    }
}
