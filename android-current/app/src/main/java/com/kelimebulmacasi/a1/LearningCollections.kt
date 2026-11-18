package com.kelimebulmacasi.a1

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Organize learning events without duplicating spaced-repetition records. */
internal object LearningCollections {
    const val NEW_WORDS = "new-words"
    fun folderFor(gameId: String, kind: LearningKind): String? {
        if (kind.rating != null) return null
        if (gameId == "personal_review") return null
        return if (gameId.startsWith("premium-")) gameId else NEW_WORDS
    }
    fun status(kind: LearningKind): String = when (kind) {
        LearningKind.UNKNOWN -> "BILINMIYOR"
        LearningKind.WRONG_CHECK -> "YANLIS"
        else -> "YENI"
    }
    fun mergeStatus(old: String, incoming: String): String = when {
        (old.contains("BILINMIYOR") || incoming.contains("BILINMIYOR")) &&
            (old.contains("YANLIS") || incoming.contains("YANLIS")) -> "BILINMIYOR_YANLIS"
        old.contains("BILINMIYOR") || incoming.contains("BILINMIYOR") -> "BILINMIYOR"
        old.contains("YANLIS") || incoming.contains("YANLIS") -> "YANLIS"
        else -> "YENI"
    }
    fun defaultTitle(gameId: String, createdAt: Long): String =
        if (gameId == NEW_WORDS) "Yeni Kelimeler" else
            "${SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.forLanguageTag("tr-TR")).format(Date(createdAt))} · Arena"
    fun safeName(name: String): String = name.trim().replace(Regex("\\s+"), " ").take(64)
}

internal data class LearningFolder(
    val id: String, val title: String, val createdAt: Long, val total: Int, val wrong: Int,
    val unknown: Int, val kind: String,
)
