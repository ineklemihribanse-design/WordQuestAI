package com.kelimebulmacasi.a1

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherNativeCrashSummaryTest {
    private fun varint(value: Long): ByteArray {
        var v = value
        val bytes = ArrayList<Byte>()
        while (v >= 128) {
            bytes.add(((v and 127) or 128).toByte())
            v = v ushr 7
        }
        bytes.add(v.toByte())
        return bytes.toByteArray()
    }

    private fun field(number: Int, payload: ByteArray): ByteArray =
        varint((number * 8L + 2)).plus(varint(payload.size.toLong())).plus(payload)

    private fun string(number: Int, value: String): ByteArray =
        field(number, value.toByteArray(Charsets.UTF_8))

    private fun intField(number: Int, value: Long): ByteArray =
        varint(number * 8L).plus(varint(value))

    @Test fun parsesOnlyNativeAbortCauseAndModuleNames() {
        val frame = string(4, "gltfio::AssetLoader::createAsset") +
            string(6, "/system/lib64/libgltfio-jni.so")
        val thread = field(4, frame)
        val threadMapEntry = intField(1, 123).plus(field(2, thread))
        val wrongThreadFrame = string(4, "OtherThreadShouldNotAppear") +
            string(6, "libunrelated.so")
        val wrongThread = intField(1, 456).plus(field(2, field(4, wrongThreadFrame)))
        val cause = string(1, "Buffer size mismatch in gltfio")
        val proto = intField(6, 123) +
            string(14, "Filament assert: vertex buffer") +
            field(15, cause) +
            field(16, wrongThread) +
            field(16, threadMapEntry) +
            string(9, "--user-token-DO-NOT-EXPORT")
        val got = TeacherNativeCrashSummary.parse(proto)
        assertTrue(got.contains("Filament assert"))
        assertTrue(got.contains("Buffer size mismatch"))
        assertTrue(got.contains("libgltfio-jni.so"))
        assertTrue(got.contains("createAsset"))
        assertFalse(got.contains("OtherThreadShouldNotAppear"))
        assertFalse(got.contains("--user-token"))
        assertFalse(got.contains("/system/lib64/"))
    }

    @Test fun refusesArbitraryRawAbortsAndInvalidInput() {
        val message = string(14, "PRIVATE-MESSAGE: my unrelated user data")
        assertFalse(TeacherNativeCrashSummary.parse(message).contains("PRIVATE"))
        assertTrue(TeacherNativeCrashSummary.parse(byteArrayOf(0xff.toByte(), 0)).isEmpty())
    }
}
