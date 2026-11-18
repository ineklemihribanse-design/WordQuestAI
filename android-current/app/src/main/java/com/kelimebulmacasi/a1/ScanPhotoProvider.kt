package com.kelimebulmacasi.a1

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File

class ScanPhotoProvider : FileProvider()

internal class TakeScanPhoto : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent = super.createIntent(context, input).apply {
        clipData = ClipData.newRawUri("scan photo", input)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}

internal fun createScanPhoto(context: Context): File {
    val folder = File(context.cacheDir, "scan_photos")
    folder.mkdirs()
    val cutoff = System.currentTimeMillis() - 86_400_000L
    folder.listFiles()?.filter { it.lastModified() < cutoff }?.forEach(File::delete)
    return File.createTempFile("scan-", ".jpg", folder)
}

internal fun scanPhotoUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, context.packageName + ".scan_photos", file)
