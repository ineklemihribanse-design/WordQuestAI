package com.kelimebulmacasi.a1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Locally saved study paragraphs: NEVER auto-import scanned or AI text before review. */
internal data class Wq186StudyParagraph(
    val level: String, val turkish: String, val english: String, val origin: String,
)

internal class Wq186ParagraphStore(context: Context, profile: String) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "wq186_study_paragraphs_" + stableHash(profile).take(24),
        Context.MODE_PRIVATE,
    )

    fun read(): List<Wq186StudyParagraph> {
        return runCatching {
            val array = JSONArray(prefs.getString("entries", "[]") ?: "[]")
            buildList {
                for (i in 0 until minOf(120, array.length())) {
                    val item = array.optJSONObject(i) ?: continue
                    val lv = item.optString("level")
                    val tr = item.optString("tr").trim()
                    val en = item.optString("en").trim()
                    if (lv in englishLevels && tr.length in 10..3500 &&
                        en.length in 10..3500
                    ) add(Wq186StudyParagraph(lv, tr, en, item.optString("origin", "Elle")))
                }
            }
        }.getOrElse { emptyList() }
    }

    fun save(entry: Wq186StudyParagraph): Boolean {
        if (entry.level !in englishLevels || entry.english.trim().length !in 10..3500 ||
            entry.turkish.trim().length !in 10..3500
        ) return false
        val records = read()
        if (records.size >= 120) return false
        if (records.any { it.english.trim().equals(entry.english.trim(), ignoreCase = true) }) return false
        val payload = JSONArray()
        for (item in records + entry) {
            payload.put(JSONObject()
                .put("level", item.level).put("tr", item.turkish)
                .put("en", item.english).put("origin", item.origin))
        }
        return prefs.edit().putString("entries", payload.toString()).commit()
    }

    fun delete(english: String): Boolean {
        val remaining = read().filterNot { it.english == english }
        val payload = JSONArray()
        for (item in remaining) {
            payload.put(JSONObject().put("level", item.level)
                .put("tr", item.turkish).put("en", item.english).put("origin", item.origin))
        }
        return prefs.edit().putString("entries", payload.toString()).commit()
    }
}

private suspend fun wq186OcrBitmap(recognizer: TextRecognizer, bitmap: Bitmap): String =
    suspendCancellableCoroutine { promise ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                if (promise.isActive) promise.resume(result.text)
            }
            .addOnFailureListener { failure ->
                if (promise.isActive) promise.resumeWithException(failure)
            }
    }

private fun wq186Pages(specification: String, count: Int): List<Int> {
    val pages = specification.split(',').flatMap { section ->
        val item = section.trim()
        if ('-' in item) {
            val parts = item.split('-', limit = 2)
            val first = parts.getOrNull(0)?.toIntOrNull()
            val last = parts.getOrNull(1)?.toIntOrNull()
            if (first != null && last != null && first in 1..count &&
                last in first..minOf(count, first + 7)
            ) (first..last).toList() else emptyList()
        } else listOfNotNull(item.toIntOrNull())
    }
    return pages.filter { it in 1..count }.distinct().take(8).ifEmpty { listOf(1) }
}

/** PDF page rasterisation + MLKit OCR; photos and camera use existing scan helper.
 * At most 8 pages; user edits resulting text before saving the English/Turkish pair.
 */
internal suspend fun wq186ExtractParagraphText(
    context: Context, uri: Uri, pdf: Boolean, pages: String = "1",
): String = withContext(Dispatchers.IO) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        if (!pdf) {
            val bitmap = readPhoto(context, uri)
            try { wq186OcrBitmap(recognizer, bitmap).trim().take(3500) }
            finally { bitmap.recycle() }
        } else {
            val temp = File.createTempFile("wq186-paragraph-", ".pdf", context.cacheDir)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                } ?: error("PDF açılamadı.")
                val descriptor = ParcelFileDescriptor.open(
                    temp, ParcelFileDescriptor.MODE_READ_ONLY,
                )
                val renderer = try { PdfRenderer(descriptor) }
                catch (failure: Exception) { descriptor.close(); throw failure }
                renderer.use { document ->
                    val selected = wq186Pages(pages, document.pageCount)
                    val result = mutableListOf<String>()
                    for (pageNumber in selected) {
                        val bitmap = document.openPage(pageNumber - 1).use { page ->
                            val scale = 1700f / maxOf(page.width, page.height)
                            Bitmap.createBitmap(
                                (page.width * scale).roundToInt().coerceAtLeast(1),
                                (page.height * scale).roundToInt().coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888,
                            ).also {
                                it.eraseColor(Color.WHITE)
                                page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                        }
                        try { result += wq186OcrBitmap(recognizer, bitmap) }
                        finally { bitmap.recycle() }
                    }
                    result.joinToString("\n").trim().take(3500)
                }
            } finally {
                temp.delete()
            }
        }
    } finally {
        recognizer.close()
    }
}
