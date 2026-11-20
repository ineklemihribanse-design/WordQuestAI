package com.kelimebulmacasi.a1

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.result.contract.ActivityResultContracts
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Tests the existing Android document/camera transport without claiming
 * that physical camera hardware or ML Kit OCR has been exercised.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class Wq175MediaSafetyTest {
    @Test fun pdfSelectionKeepsExplicitPagesAndDeduplicates() {
        assertEquals(listOf(1,2,3,6), pageNumbers("1-3, 2, 6", 9))
        assertEquals(listOf(2,3,4,7), pageNumbers("2–4, 7", 9))
    }

    @Test fun blankPageSelectionUsesEntireDocument() {
        assertEquals(listOf(1,2,3), pageNumbers("", 3))
    }

    @Test fun invalidPdfPagesAreRejectedInsteadOfSilentlySkipping() {
        for (selection in listOf("0", "5", "3-2", "a", "1-7", "1,,3")) {
            try {
                pageNumbers(selection, 4)
                fail("Accepted invalid PDF selection: $selection")
            } catch (_: IllegalArgumentException) {
                // Expected: no partial selection is written to the user's notebook.
            }
        }
    }

    @Test fun cameraCaptureIntentCarriesTemporaryUriGrants() {
        val context: Context = RuntimeEnvironment.getApplication()
        val file = createScanPhoto(context)
        try {
            val uri = scanPhotoUri(context, file)
            assertEquals("content", uri.scheme)
            assertTrue(uri.authority.orEmpty().endsWith(".scan_photos"))
            val intent = TakeScanPhoto().createIntent(context, uri)
            assertEquals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE, intent.action)
            val needed = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            assertEquals(needed, intent.flags and needed)
            assertNotNull(intent.clipData)
        } finally {
            file.delete()
        }
    }

    @Test fun cameraImageCanBeReadViaItsGrantedFileProviderUri() {
        val context: Context = RuntimeEnvironment.getApplication()
        val file = createScanPhoto(context)
        val source = Bitmap.createBitmap(80, 48, Bitmap.Config.ARGB_8888)
        try {
            source.eraseColor(android.graphics.Color.WHITE)
            file.outputStream().use { stream ->
                assertTrue(source.compress(Bitmap.CompressFormat.JPEG, 92, stream))
            }
            val decoded = readPhoto(context, scanPhotoUri(context, file))
            try {
                assertEquals(80, decoded.width)
                assertEquals(48, decoded.height)
            } finally {
                decoded.recycle()
            }
        } finally {
            source.recycle()
            file.delete()
        }
    }
}
