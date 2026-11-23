package com.kelimebulmacasi.a1

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/**
 * v189: user-uploaded illustrations, not generated placeholders.
 * Each name resolves to a bundled asset. Never load arbitrary paths.
 */
internal object Wq189IconCatalog {
    private val routes = mapOf(
        "home" to "home",
        "new_game" to "puzzle_abc_neon",
        "solo_puzzle" to "puzzle_builder",
        "puzzle" to "puzzle_builder",
        "packages" to "ready_folders",
        "folder" to "ready_folders",
        "categories" to "ready_folders",
        "verbs" to "verb_forms",
        "cards" to "learning_cycle",
        "library" to "learning_cycle",
        "notebook" to "learning_cycle",
        "learning" to "learning_cycle",
        "synonym" to "puzzle_abc_classic",
        "plan" to "study_checklist",
        "daily_plan" to "study_checklist",
        "daily_tasks" to "daily_tasks_neon",
        "assistant" to "speaking_neon",
        "ai_teacher" to "pronunciation_neon",
        "analytics" to "learning_brain_neon",
        "insights" to "learning_brain_classic",
        "listen" to "listen_write_neon",
        "dictation" to "listen_write_classic",
        "pronunciation" to "pronunciation_classic",
        "speak" to "speaking_classic",
        "story" to "storytelling_neon",
        "sentence" to "storytelling_classic",
        "review" to "reading_review_classic",
        "exam" to "reading_review_neon",
        "grammar" to "reading_review_classic",
        "success" to "daily_tasks_classic"
    )
    fun asset(key: String): String? = routes[key]
}

@Composable
internal fun Wq189UploadedArtwork(name: String, modifier: Modifier = Modifier): Boolean {
    val asset = Wq189IconCatalog.asset(name) ?: return false
    val context = LocalContext.current
    val artwork = remember(context.applicationContext, asset) {
        runCatching {
            context.applicationContext.assets.open("approved_icons_v189/$asset.webp").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (artwork != null) {
        Image(
            bitmap = artwork,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High
        )
    }
    return artwork != null
}
