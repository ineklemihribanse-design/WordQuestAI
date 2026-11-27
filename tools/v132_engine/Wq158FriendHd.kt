package com.kelimebulmacasi.a1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Optional friend-only detail image, not the source ZIP. Old clients keep the small preview. */
internal object Wq158FriendHd {
    const val MAX_TEXT=160100
    private const val MAX_RAW=120000
    data class Pairing(val preview:String?,val hd:String?)
    fun prepare(context:Context,profile:String,avatar:String):Pairing{
        if(!Wq185FriendMascot.consent(context,profile)||!Wq183MascotStore.isCustomId(avatar)||
            !Wq183MascotStore.exists(context,profile,avatar))return Pairing(null,null)
        val original=Wq183MascotStore.picture(context,avatar,false)?:return Pairing(null,null)
        return try{
            val preview=Wq188FriendPreview.encode(avatar,original)
            Pairing(preview,if(preview!=null)encode(avatar,original)else null)
        }finally{original.recycle()}
    }
    fun encode(id:String,source:Bitmap):String?{
        if(!Wq183MascotStore.isCustomId(id)||source.width !in 1..2048||source.height !in 1..2048)return null
        return runCatching{
            val longest=maxOf(source.width,source.height)
            for(edge in listOf(1024,896,768,640,512,384,256).map{minOf(it,longest)}.distinct()){
                val scale=edge.toFloat()/longest
                val b=if(edge==longest)source else Bitmap.createScaledBitmap(source,
                    (source.width*scale).toInt().coerceAtLeast(1),(source.height*scale).toInt().coerceAtLeast(1),true)
                try{
                    for(q in listOf(94,88,82,76,70,64)){
                        val out=ByteArrayOutputStream()
                        if(!b.compress(Bitmap.CompressFormat.WEBP,q,out))continue
                        val raw=out.toByteArray()
                        if(raw.size !in 80..MAX_RAW)continue
                        val value="$id:"+Base64.encodeToString(raw,Base64.NO_WRAP)
                        if(value.length<=MAX_TEXT)return@runCatching value
                    }
                }finally{if(b!==source)b.recycle()}
            }
            null
        }.getOrNull()
    }
    fun decode(expectedPreview:String,payload:String?):Bitmap?{
        val id=expectedPreview.substringBefore(':')
        if(!Wq183MascotStore.isCustomId(id)||payload==null||payload.length !in 140..MAX_TEXT||
            !payload.startsWith("$id:"))return null
        return runCatching{
            val token=payload.substringAfter(':')
            if(!Regex("[A-Za-z0-9+/=]+").matches(token))return@runCatching null
            val raw=Base64.decode(token,Base64.NO_WRAP)
            if(raw.size !in 80..MAX_RAW||String(raw,0,4,Charsets.US_ASCII)!="RIFF"||
                String(raw,8,4,Charsets.US_ASCII)!="WEBP")return@runCatching null
            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeByteArray(raw,0,raw.size,bounds)
            if(bounds.outWidth !in 1..1024||bounds.outHeight !in 1..1024)return@runCatching null
            BitmapFactory.decodeByteArray(raw,0,raw.size)
        }.getOrNull()
    }
}
private object Wq158DetailCache{
    private val cache=object:android.util.LruCache<String,ImageBitmap>(8*1024*1024){
        override fun sizeOf(key:String,value:ImageBitmap)=value.width*value.height*4
    }
    @Synchronized fun load(uid:String,preview:String,payload:String):ImageBitmap?{
        val digest=java.security.MessageDigest.getInstance("SHA-256")
            .digest((preview+":"+payload).toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
        val key="$uid:$digest"
        cache.get(key)?.let{return it}
        return Wq158FriendHd.decode(preview,payload)?.asImageBitmap()?.also{cache.put(key,it)}
    }
}
@Composable
internal fun Wq158RememberFriendHd(hub:FriendsHub,entry:FriendEntry):ImageBitmap?{
    val preview=entry.avatarPreview
    var payload by remember(hub,entry.uid,preview){mutableStateOf<String?>(null)}
    DisposableEffect(hub,entry.uid,preview){
        val close=if(preview!=null)hub.observeFriendHd(entry.uid,preview){payload=it}else ({})
        onDispose{close()}
    }
    // No cached HD is displayed until the current authorized listener supplies it.
    val data=payload
    var image by remember(entry.uid,preview,data){mutableStateOf<ImageBitmap?>(null)}
    LaunchedEffect(entry.uid,preview,data){
        if(preview!=null&&data!=null)image=withContext(Dispatchers.IO){Wq158DetailCache.load(entry.uid,preview,data)}
    }
    return image
}
