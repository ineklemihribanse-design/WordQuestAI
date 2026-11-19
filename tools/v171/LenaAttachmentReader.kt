package com.kelimebulmacasi.a1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Xml
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine

/** Imports the user's explicitly selected files only. No raw photos/docs are sent to the AI. */
internal suspend fun readLenaSelectedAttachment(
    context: Context, uri: Uri, isImage: Boolean,
): LenaPreparedAttachment = withContext(Dispatchers.IO) {
    val filename = documentName(context, uri).take(90).ifBlank { if (isImage) "Görsel" else "Belge" }
    val mime = context.contentResolver.getType(uri).orEmpty()
    val content = when {
        isImage -> {
            val bitmap = readPhoto(context, uri)
            try { extractLenaOcr(bitmap) } finally { bitmap.recycle() }
        }
        isPdf(context, uri) -> readLenaPdfText(context, uri)
        filename.endsWith(".docx", true) ||
            mime.contains("wordprocessingml.document") -> readLenaDocx(context, uri)
        filename.endsWith(".txt", true) || mime.startsWith("text/") ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { input ->
                    val chars = CharArray(6000)
                    val count = input.read(chars)
                    if (count < 0) "" else String(chars, 0, count)
                }
            }.orEmpty()
        else -> throw IllegalArgumentException("Yalnızca görsel, PDF, TXT veya DOCX seçebilirsin.")
    }.trim().take(5500)
    require(content.isNotBlank()) {
        if (isImage) "Görselde okunabilir yazı bulunamadı. Şu an görselin yalnızca yazısını okuyabiliyorum."
        else "Belgelerde okunabilir metin bulunamadı."
    }
    LenaPreparedAttachment(filename, if (isImage) "Görsel" else "Belge", content)
}

private suspend fun extractLenaOcr(bitmap: Bitmap): String {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        suspendCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result ->
                    continuation.resumeWith(Result.success(result.text))
                }
                .addOnFailureListener { error ->
                    continuation.resumeWith(Result.failure(error))
                }
                .addOnCanceledListener {
                    continuation.resumeWith(Result.failure(
                        IllegalStateException("Tarama iptal edildi.")))
                }
        }
    } finally {
        recognizer.close()
    }
}

private suspend fun readLenaPdfText(context: Context, uri: Uri): String {
    val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
        ?: error("PDF açılamadı.")
    return PdfRenderer(descriptor).use { renderer ->
        val extracted = StringBuilder()
        val pages = minOf(3, renderer.pageCount)
        for (index in 0 until pages) {
            val bitmap = renderer.openPage(index).use { page ->
                val ratio = 1280f / maxOf(page.width, page.height).coerceAtLeast(1)
                Bitmap.createBitmap(
                    (page.width * ratio).toInt().coerceAtLeast(1),
                    (page.height * ratio).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                ).also { image ->
                    image.eraseColor(Color.WHITE)
                    page.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
            try { extracted.append(extractLenaOcr(bitmap)).append('\n') }
            finally { bitmap.recycle() }
            if (extracted.length >= 5500) break
        }
        extracted.toString()
    }
}

private fun readLenaDocx(context: Context, uri: Uri): String {
    val source = context.contentResolver.openInputStream(uri) ?: error("DOCX açılamadı.")
    return source.use { input ->
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    // Cap the uncompressed XML to avoid zip-bombs and oversized prompts.
                    val bytes = zip.readNBytes(512_000)
                    val parser = Xml.newPullParser().apply {
                        setInput(ByteArrayInputStream(bytes), "UTF-8")
                    }
                    val output = StringBuilder()
                    var event = parser.eventType
                    while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT &&
                        output.length < 6000) {
                        if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                            when (parser.name.substringAfter(':')) {
                                "t" -> output.append(parser.nextText()).append(' ')
                                "p" -> output.append('\n')
                            }
                        }
                        event = parser.next()
                    }
                    return@use output.toString()
                }
                entry = zip.nextEntry
            }
            throw IllegalArgumentException("DOCX içinde metin bulunamadı.")
        }
    }
}
