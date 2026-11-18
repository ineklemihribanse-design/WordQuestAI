package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import org.json.JSONObject

/**
 * WordQuest's transparent, offline IPA lookup. The underlying eSpeak phoneme output
 * was generated from the SAME profiles as the bundled OGG clips; it is not a
 * human-reviewed dictionary and must not be presented as one. Ambiguous words
 * without a selected sense are intentionally absent. Other words not in the
 * supported library produce no fabricated transcription.
 */
internal object WordIpaLexicon {
    internal data class Entry(
        val us: String,
        val uk: String,
        val turkishUS: String?,
        val turkishUK: String?,
        val mightHaveOtherReadings: Boolean,
        val cmuUsListed: Boolean,
    )

    @Volatile private var cache: Map<String, Entry>? = null

    fun lookup(context: Context, word: String): Entry? {
        val dictionary = cache ?: synchronized(this) {
            cache ?: load(context.applicationContext).also { cache = it }
        }
        return dictionary[OfflineAccentPlayer.normalize(word)]
    }

    private fun load(context: Context): Map<String, Entry> = runCatching {
        context.assets.open("pronunciation/ipa.json").bufferedReader(Charsets.UTF_8).use { file ->
            val all = JSONObject(file.readText()).getJSONObject("entries")
            buildMap {
                val keys = all.keys()
                while (keys.hasNext()) {
                    val word = keys.next()
                    val source = all.getJSONObject(word)
                    val us = source.optString("us").trim()
                    val uk = source.optString("uk").trim()
                    if (us.isBlank() || uk.isBlank()) continue
                    put(word, Entry(us, uk,
                        source.optString("tr_us").takeIf(String::isNotBlank),
                        source.optString("tr_uk").takeIf(String::isNotBlank),
                        source.optBoolean("possible_multiple_readings", false),
                        source.optBoolean("cmu_us", false)))
                }
            }
        }
    }.getOrDefault(emptyMap())
}

@Composable
internal fun WordIpa(word: String, preferences: WordQuestPreferences, modifier: Modifier = Modifier) {
    if (!preferences.showIpa) return
    val context = LocalContext.current
    val item = WordIpaLexicon.lookup(context, word)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (item == null) {
            Text("IPA: Bu sözcük için doğrulanabilir tek bir okunuş yok.", color = Muted, fontSize = 12.sp)
        } else {
            Text("US /${item.us}/", color = Ink, fontFamily = FontFamily.Default, fontSize = 14.sp)
            Text("UK /${item.uk}/", color = Ink, fontFamily = FontFamily.Default, fontSize = 14.sp)
            if (preferences.showTurkishApprox) {
                if (item.turkishUS != null) Text("Yaklaşık TR (US): ${item.turkishUS}", color = Muted, fontSize = 12.sp)
                if (item.turkishUK != null) Text("Yaklaşık TR (UK): ${item.turkishUK}", color = Muted, fontSize = 12.sp)
                if (item.turkishUS == null && item.turkishUK == null)
                    Text("Bu sözcük için denetlenmiş Türkçe yaklaşık okunuş yok.", color = Muted, fontSize = 12.sp)
            }
            if (item.mightHaveOtherReadings) {
                Text("Başka okunuşları olabilir; anlamına göre sözlükten doğrula.", color = Muted, fontSize = 11.sp)
            }
        }
    }
}
