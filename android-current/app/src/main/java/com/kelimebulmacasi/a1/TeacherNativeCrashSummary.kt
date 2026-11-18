package com.kelimebulmacasi.a1

import android.app.ApplicationExitInfo
import android.os.Build
import java.io.ByteArrayOutputStream

/**
 * Minimal, bounded parser for the Android native tombstone protobuf.
 * Schema: AOSP debuggerd/proto/tombstone.proto.
 *
 * Only abort_message (field 14), Cause.human_readable (15.1),
 * and the crashed thread's first module/function names (threads map 16)
 * are examined. Never export raw native tombstones: they can contain app
 * memory, user strings and even addresses. No network or telemetry.
 */
internal object TeacherNativeCrashSummary {
    private const val MAX_TRACE_BYTES = 2 * 1024 * 1024
    private const val MAX_FRAMES = 6

    private data class Field(val number: Int, val wire: Int, val offset: Int,
                             val end: Int, val scalar: Long = 0)

    private fun readVarint(bytes: ByteArray, at: Int, limit: Int): Pair<Long, Int>? {
        var value = 0L
        var pos = at
        for (shift in 0..63 step 7) {
            if (pos >= limit) return null
            val current = bytes[pos++].toInt() and 255
            value = value or ((current and 127).toLong() shl shift)
            if (current and 128 == 0) return value to pos
        }
        return null
    }

    private fun fields(bytes: ByteArray, from: Int = 0, to: Int = bytes.size): List<Field> {
        val result = ArrayList<Field>()
        var at = from
        while (at < to && result.size < 30_000) {
            val key = readVarint(bytes, at, to) ?: break
            at = key.second
            val num = (key.first ushr 3).toInt()
            val wire = (key.first and 7).toInt()
            if (num <= 0) break
            when (wire) {
                0 -> {
                    val value = readVarint(bytes, at, to) ?: break
                    result.add(Field(num, wire, at, value.second, value.first))
                    at = value.second
                }
                1, 5 -> {
                    val end = at + if (wire == 1) 8 else 4
                    if (end > to) break
                    result.add(Field(num, wire, at, end))
                    at = end
                }
                2 -> {
                    val size = readVarint(bytes, at, to) ?: break
                    at = size.second
                    if (size.first > (to - at).toLong()) break
                    val end = at + size.first.toInt()
                    result.add(Field(num, wire, at, end))
                    at = end
                }
                else -> break
            }
        }
        return result
    }

    private fun text(bytes: ByteArray, field: Field): String {
        if (field.wire != 2) return ""
        return runCatching {
            String(bytes, field.offset, field.end - field.offset, Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun safe(s: String, limit: Int = 160): String = s
        .replace(Regex("(?i)(?:/[^\\s:;,]+)+"), "[path]")
        .replace(Regex("[\\r\\n\\t\\p{Cntrl}]+"), " ")
        .replace(Regex("[^\\p{L}\\p{N}_.,:;()+=<>!#\\-\\[\\] /]"), "")
        .take(limit)

    private fun nativeFrames(bytes: ByteArray, message: Field, crashedTid: Long?): List<String> {
        val result = ArrayList<String>()
        val mapFields = fields(bytes, message.offset, message.end)
        val mapTid = mapFields.firstOrNull { it.number == 1 && it.wire == 0 }?.scalar
        // The tombstone contains all threads: never attribute a renderer
        // worker's frames to the thread that actually called abort().
        if (crashedTid != null && mapTid != crashedTid) return emptyList()
        for (entry in mapFields) {
            if (entry.number != 2 || entry.wire != 2) continue
            for (threadPart in fields(bytes, entry.offset, entry.end)) {
                if (threadPart.number != 4 || threadPart.wire != 2) continue
                val frameFields = fields(bytes, threadPart.offset, threadPart.end)
                val functionName = frameFields.firstOrNull { it.number == 4 }?.let {
                    safe(text(bytes, it), 110)
                }.orEmpty()
                val module = frameFields.firstOrNull { it.number == 6 }?.let {
                    text(bytes, it).substringAfterLast('/').substringAfterLast('\\')
                }.orEmpty()
                val safeModule = safe(module, 65)
                if (safeModule.isNotBlank() || functionName.isNotBlank()) {
                    result.add("$safeModule :: $functionName")
                }
                if (result.size >= MAX_FRAMES) return result
            }
        }
        return result
    }

    /** Pure parser also used by local JVM unit tests with synthetic protobuf data. */
    fun parse(bytes: ByteArray): String {
        val root = fields(bytes)
        val abort = root.firstOrNull { it.number == 14 && it.wire == 2 }?.let {
            safe(text(bytes, it), 180)
        }.orEmpty()
        val causes = root.filter { it.number == 15 && it.wire == 2 }.mapNotNull { entry ->
            fields(bytes, entry.offset, entry.end).firstOrNull {
                it.number == 1 && it.wire == 2
            }?.let { safe(text(bytes, it), 160) }
        }.filter { it.isNotBlank() }.take(2)

        // Only print a system assertion/abort, not arbitrary memory, log buffers,
        // command line, process names, full paths, addresses or user data.
        val safeAbort = abort.takeIf {
            listOf("filament", "gltfio", "assert", "morph", "buffer", "resource",
                   "vertex", "abort", "skin", "asset", "material", "engine")
                .any { term -> it.contains(term, ignoreCase = true) }
        }
        val crashedTid = root.firstOrNull { it.number == 6 && it.wire == 0 }?.scalar
        val stack = root.filter { it.number == 16 && it.wire == 2 }
            .asSequence()
            .flatMap { nativeFrames(bytes, it, crashedTid).asSequence() }
            .take(MAX_FRAMES)
            .toList()

        val summary = buildList {
            if (safeAbort != null) add("Native abort: $safeAbort")
            for (cause in causes) add("Native neden: $cause")
            if (stack.isNotEmpty()) add("Native fonksiyonlar: " + stack.joinToString("; "))
        }
        return summary.joinToString("\n").take(750)
    }

    fun fromExitInfo(info: ApplicationExitInfo): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            info.reason != ApplicationExitInfo.REASON_CRASH_NATIVE) return ""
        return runCatching {
            info.traceInputStream?.use { input ->
                val buf = ByteArray(8192)
                val out = ByteArrayOutputStream()
                while (out.size() < MAX_TRACE_BYTES) {
                    val got = input.read(buf, 0, minOf(buf.size, MAX_TRACE_BYTES - out.size()))
                    if (got <= 0) break
                    out.write(buf, 0, got)
                }
                parse(out.toByteArray())
            }.orEmpty()
        }.getOrDefault("")
    }
}
