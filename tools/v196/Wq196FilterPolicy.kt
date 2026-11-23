package com.kelimebulmacasi.a1

/**
 * New Game filter selection: a topic and part-of-speech filter must NEVER
 * be combined. Multiple values within the chosen filter are supported.
 */
internal object Wq196FilterPolicy {
    const val TOPIC = "Konu"
    const val KIND = "Kelime Türü"

    fun mode(initialTopic: String?, initialKind: String?): String =
        if (initialTopic == null && initialKind != null) KIND else TOPIC

    fun activeTopics(mode: String, topics: Collection<String>): Set<String> =
        if (mode == TOPIC) topics.toSet() else emptySet()

    fun activeKinds(mode: String, kinds: Collection<String>): Set<String> =
        if (mode == KIND) kinds.toSet() else emptySet()
}
