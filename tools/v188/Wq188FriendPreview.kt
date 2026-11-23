package com.kelimebulmacasi.a1

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

/** Friend-only HD thumbnail using the existing owner-only, mutual-friend-readable
 * avatarPreview field and its unchanged max 18,050-character Firebase validation.
 * Original ZIP and source bitmap remain entirely on the sender's device.
 * Does not change Arena's low-bandwidth 160px preview protocol.
 */
internal object Wq188FriendPreview {
    private const val MAX_SIDE = 1024
    private const val MAX_RAW = 13000
    private const val MAX_TEXT = 18050
    private val alphabet = Regex("[A-Za-z0-9+/=]+")

    fun decode(id:String, preview:String?):Bitmap? {
        if(!Wq183MascotStore.isCustomId(id) ||
            preview==null || preview.length !in 140..MAX_TEXT ||
            !preview.startsWith("$id:")) return null
        val token=preview.substring(id.length+1)
        if(token.length !in 100..17500 || !alphabet.matches(token)) return null
        return runCatching {
            val raw=Base64.decode(token, Base64.NO_WRAP)
            if(raw.size !in 80..MAX_RAW ||
                String(raw,0,4,Charsets.US_ASCII)!="RIFF" ||
                String(raw,8,4,Charsets.US_ASCII)!="WEBP") return@runCatching null
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
            BitmapFactory.decodeByteArray(raw,0,raw.size,bounds)
            if(bounds.outWidth !in 1..MAX_SIDE ||
                bounds.outHeight !in 1..MAX_SIDE) return@runCatching null
            BitmapFactory.decodeByteArray(raw,0,raw.size)
        }.getOrNull()
    }

    fun encode(id:String, original:Bitmap):String? {
        if(!Wq183MascotStore.isCustomId(id) ||
            original.width !in 1..2048 || original.height !in 1..2048) return null
        return runCatching {
            var best:String?=null
            var bestScore=-1
            // Preserve the mutual-friend permission and 18,050-char storage cap.
            // Prefer a sharper mid-size thumbnail over a blurry huge one.
            for(edge in listOf(1024, 896, 768, 640, 512, 448, 384, 320, 256, 192, 160)) {
                val scale=minOf(1f,edge.toFloat()/maxOf(original.width,original.height))
                val width=(original.width*scale).toInt().coerceAtLeast(1)
                val height=(original.height*scale).toInt().coerceAtLeast(1)
                val small=if(width==original.width && height==original.height) original
                          else Bitmap.createScaledBitmap(original,width,height,true)
                try {
                    for(quality in listOf(92,84,76,68,60,50,40,32)) {
                        val stream=ByteArrayOutputStream()
                        if(!small.compress(Bitmap.CompressFormat.WEBP,quality,stream)) continue
                        val raw=stream.toByteArray()
                        if(raw.size !in 80..MAX_RAW) continue
                        val value="$id:"+Base64.encodeToString(raw,Base64.NO_WRAP)
                        if(value.length > MAX_TEXT) continue
                        val score=maxOf(width,height)*quality*quality
                        if(score>bestScore) {
                            best=value
                            bestScore=score
                        }
                        break
                    }
                } finally {
                    if(small !== original) small.recycle()
                }
            }
            best
        }.getOrNull()
    }
}
