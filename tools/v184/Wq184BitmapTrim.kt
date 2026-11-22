package com.kelimebulmacasi.a1

import android.graphics.Bitmap
import android.graphics.Color

/** Crops only transparent outer padding of locally imported art; no face retouching.
 * Uses one scanline of memory instead of copying the entire pixel array.
 * Existing built-in characters and stored files are never modified.
 */
internal object Wq184BitmapTrim {
    fun crop(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha() || bitmap.isRecycled) return bitmap
        val w = bitmap.width
        val h = bitmap.height
        if (w < 1 || h < 1) return bitmap
        val row = IntArray(w)
        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        for (y in 0 until h) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                // Soft hair edges count too, but fully transparent shadows don't.
                if (Color.alpha(row[x]) > 8) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) return bitmap
        val pad = (minOf(w, h) / 100).coerceAtLeast(3)
        val x0 = (minX - pad).coerceAtLeast(0)
        val y0 = (minY - pad).coerceAtLeast(0)
        val x1 = (maxX + pad + 1).coerceAtMost(w)
        val y1 = (maxY + pad + 1).coerceAtMost(h)
        if (x0 == 0 && y0 == 0 && x1 == w && y1 == h) return bitmap
        return Bitmap.createBitmap(bitmap, x0, y0, x1 - x0, y1 - y0)
    }
}
