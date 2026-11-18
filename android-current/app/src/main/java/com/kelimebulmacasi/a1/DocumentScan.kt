package com.kelimebulmacasi.a1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine

internal fun documentName(context: Context, uri: Uri): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: "Belge"

internal fun isPdf(context: Context, uri: Uri): Boolean =
    context.contentResolver.getType(uri) == "application/pdf" || documentName(context, uri).endsWith(".pdf", true)

internal suspend fun scanPairs(
    context: Context,
    uri: Uri,
    pdf: Boolean,
    pages: String,
    photoRegion: PhotoRegion?,
    onPage: suspend (Int, Int) -> Unit,
): List<WordPair> = withContext(Dispatchers.IO) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        val pairs = if (pdf) {
            scanPdf(context, uri, pages, recognizer, onPage)
        } else {
            onPage(1, 1)
            withBitmap(readPhoto(context, uri, photoRegion)) { scanPage(recognizer, it, tryAllRotations = true).pairs }
        }
        val distinct = linkedMapOf<String, WordPair>()
        for (pair in pairs) {
            val cleaned = preserveEnglishPhraseSpacing(pair.answer)
            val corrected = pair.copy(answer = cleaned, review = pair.review || cleaned != pair.answer)
            val key = englishLetters(corrected.answer)
            val existing = distinct[key]
            if (existing == null || (!existing.valid && corrected.valid)) distinct[key] = corrected
        }
        distinct.values.toList()
    } finally {
        recognizer.close()
    }
}

private suspend fun scanPdf(
    context: Context,
    uri: Uri,
    selection: String,
    recognizer: TextRecognizer,
    onPage: suspend (Int, Int) -> Unit,
): List<WordPair> {
    val temporary = File.createTempFile("puzzle-", ".pdf", context.cacheDir)
    try {
        context.contentResolver.openInputStream(uri)?.use { source ->
            temporary.outputStream().use { source.copyTo(it) }
        } ?: error("Dosya açılamadı.")
        val descriptor = ParcelFileDescriptor.open(temporary, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = try {
            PdfRenderer(descriptor)
        } catch (error: Throwable) {
            descriptor.close()
            throw error
        }
        return renderer.use {
            val pages = pageNumbers(selection, it.pageCount)
            val result = mutableListOf<WordPair>()
            for ((index, number) in pages.withIndex()) {
                onPage(index + 1, pages.size)
                currentCoroutineContext().ensureActive()
                val bitmap = it.openPage(number - 1).use { page ->
                    val scale = 2160f / maxOf(page.width, page.height)
                    Bitmap.createBitmap(
                        (page.width * scale).roundToInt().coerceAtLeast(1),
                        (page.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888,
                    ).also { image ->
                        image.eraseColor(Color.WHITE)
                        page.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
                result += withBitmap(bitmap) { image -> scanPage(recognizer, image, tryAllRotations = false).pairs }
            }
            result
        }
    } finally {
        temporary.delete()
    }
}

internal fun readPhoto(context: Context, uri: Uri, region: PhotoRegion? = null): Bitmap {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Bu görsel açılamadı." }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 3650) sample *= 2
    val bitmap = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: error("Bu görsel açılamadı.")
    val orientation = runCatching {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull()
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
    }
    if (region != null) matrix.postRotate(region.rotation.toFloat())
    val rotated = if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated !== bitmap) bitmap.recycle()
    if (region == null) return rotated
    val left = (region.left * rotated.width).roundToInt().coerceIn(0, rotated.width - 1)
    val top = (region.top * rotated.height).roundToInt().coerceIn(0, rotated.height - 1)
    val right = (region.right * rotated.width).roundToInt().coerceIn(left + 1, rotated.width)
    val bottom = (region.bottom * rotated.height).roundToInt().coerceIn(top + 1, rotated.height)
    val cropped = Bitmap.createBitmap(rotated, left, top, right - left, bottom - top)
    if (cropped !== rotated) rotated.recycle()
    return cropped
}

private suspend fun <T> withBitmap(bitmap: Bitmap, block: suspend (Bitmap) -> T): T =
    try { block(bitmap) } finally { bitmap.recycle() }

private data class PageReading(val lines: List<ScanLine>, val width: Int, val height: Int)

private suspend fun readLines(recognizer: TextRecognizer, bitmap: Bitmap, rotation: Int = 0): PageReading {
    val scanned = recognizer.process(InputImage.fromBitmap(bitmap, rotation)).awaitScan()
    val lines = scanned.textBlocks.flatMap { it.lines }.mapNotNull { line ->
        line.boundingBox?.let { ScanLine(line.text, it.left, it.top, it.right, it.bottom) }
    }
    return if (rotation == 90 || rotation == 270) PageReading(lines, bitmap.height, bitmap.width)
        else PageReading(lines, bitmap.width, bitmap.height)
}

private suspend fun scanPage(recognizer: TextRecognizer, bitmap: Bitmap, tryAllRotations: Boolean): PairReading {
    var best = PairReading(emptyList(), 0)
    var bestReading: PageReading? = null
    var bestRotation = 0
    val rotations = if (bitmap.width >= bitmap.height) listOf(0, 270, 90, 180) else listOf(0, 90, 270, 180)
    for ((index, rotation) in rotations.withIndex()) {
        currentCoroutineContext().ensureActive()
        val reading = readLines(recognizer, bitmap, rotation)
        val result = recognizePairs(reading.lines)
        if (result.tableScore > best.tableScore) {
            best = result
            bestReading = reading
            bestRotation = rotation
        }
        val valid = best.pairs.count(WordPair::valid)
        if ((!tryAllRotations && index == 0 && valid >= 5) || (tryAllRotations && valid >= 20 && best.tableScore >= 145)) break
    }
    val reading = bestReading
    if (tryAllRotations && reading != null && best.pairs.count(WordPair::valid) >= 4) {
        val crop = focusOnTable(reading, best)
        if (crop != null) {
            val rotated = if (bestRotation == 0) bitmap else Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(bestRotation.toFloat()) }, true,
            )
            try {
                val closeup = Bitmap.createBitmap(rotated, crop.left, crop.top, crop.width(), crop.height())
                try {
                    val contrast = Bitmap.createBitmap(closeup.width, closeup.height, Bitmap.Config.ARGB_8888)
                    withBitmap(contrast) { image ->
                        normalizePaper(closeup.width, closeup.height,
                            read = { y, pixels -> closeup.getPixels(pixels, 0, pixels.size, 0, y, pixels.size, 1) },
                            write = { y, pixels -> image.setPixels(pixels, 0, pixels.size, 0, y, pixels.size, 1) },
                        )
                        val enhanced = readLines(recognizer, image)
                        val clearer = listOf(recognizePairs(enhanced.lines), recognizePairs(joinTableFragments(enhanced.lines)))
                            .maxBy(PairReading::tableScore)
                        if (clearer.tableScore > best.tableScore) best = clearer
                    }
                } finally {
                    if (closeup !== rotated) closeup.recycle()
                }
            } finally {
                if (rotated !== bitmap) rotated.recycle()
            }
        }
    }
    return best
}

private fun focusOnTable(page: PageReading, reading: PairReading): Rect? {
    val answers = reading.pairs.map { englishLetters(it.answer) }.toSet()
    val matching = page.lines.filter { englishLetters(it.text) in answers }
    if (matching.size < 4) return null
    val left = matching.map(ScanLine::left).sorted()[matching.size / 2]
    val top = matching.minOf(ScanLine::top)
    val bottom = matching.maxOf(ScanLine::bottom)
    val size = matching.map(ScanLine::height).sorted()[matching.size / 2].coerceAtLeast(12)
    val clues = reading.pairs.filter(WordPair::valid).map { it.clue.lowercase() }.toSet()
    val translations = page.lines.filter { line ->
        line.left > left && line.top in (top - size * 3)..(bottom + size * 5) &&
            line.text.trimStart(' ', ':', '|').trim().lowercase() in clues
    }
    if (translations.size < 3) return null
    val lastX = translations.map(ScanLine::right).sorted().let { it[(it.size * .8).toInt().coerceAtMost(it.lastIndex)] }
    return Rect(
        (left - size * 2).coerceAtLeast(0), (top - size * 4).coerceAtLeast(0),
        (lastX + size * 4).coerceAtMost(page.width), (bottom + size * 9).coerceAtMost(page.height),
    ).takeIf { it.width() in (size * 8)..(page.width * .95).toInt() && it.height() >= size * 10 }
}

// Keep the image alive until the native detector finishes, including when the editor closes.
private suspend fun <T> Task<T>.awaitScan(): T = suspendCoroutine { continuation ->
    addOnSuccessListener { continuation.resumeWith(Result.success(it)) }
    addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
    addOnCanceledListener { continuation.resumeWith(Result.failure(kotlinx.coroutines.CancellationException())) }
}
