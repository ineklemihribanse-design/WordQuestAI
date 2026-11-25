package com.kelimebulmacasi.a1

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.resume
import java.io.ByteArrayOutputStream

/** Room-only, consented high-resolution preview. Never transmits original ZIP. */
internal object Wq128RoomHd {
    private const val MAX_TEXT=180_050
    private const val MAX_RAW=130_000
    private const val MAX_EDGE=1024
    private val images=mutableStateMapOf<String,Bitmap>()
    fun remoteImage(id:String):Bitmap?=images[id]
    fun receive(id:String,image:Bitmap?) { if(image==null)images.remove(id) else images[id]=image }
    fun field(side:String)=if(side=="host")"hostMascotHd" else "guestMascotHd"

    /** Strip transparent margins, never crop opaque pixels or stretch proportions. */
    fun trim(b:Bitmap):Bitmap {
        if(!b.hasAlpha() || b.width !in 1..2048 || b.height !in 1..2048) return b
        val row=IntArray(b.width)
        var x0=b.width;var y0=b.height;var x1=-1;var y1=-1
        for(y in 0 until b.height) {
            b.getPixels(row,0,b.width,0,y,b.width,1)
            for(x in row.indices) if((row[x] ushr 24)>0) {
                if(x<x0)x0=x;if(x>x1)x1=x;if(y<y0)y0=y;if(y>y1)y1=y
            }
        }
        if(x1<x0||y1<y0)return b
        val margin=(maxOf(x1-x0+1,y1-y0+1)*.015f).toInt().coerceAtLeast(2)
        x0=(x0-margin).coerceAtLeast(0);y0=(y0-margin).coerceAtLeast(0)
        x1=(x1+margin).coerceAtMost(b.width-1);y1=(y1+margin).coerceAtMost(b.height-1)
        return Bitmap.createBitmap(b,x0,y0,x1-x0+1,y1-y0+1)
    }

    fun encode(id:String,source:Bitmap):String? {
        if(!Wq183MascotStore.isCustomId(id)||source.width !in 1..2048||source.height !in 1..2048)return null
        return runCatching {
            val b=trim(source)
            for(edge in listOf(1024,768,640,512,384)) {
                val scale=minOf(1f,edge.toFloat()/maxOf(b.width,b.height))
                val small=Bitmap.createScaledBitmap(b,(b.width*scale).toInt().coerceAtLeast(1),(b.height*scale).toInt().coerceAtLeast(1),true)
                try {
                    for(quality in listOf(92,85,75,65)) {
                        val out=ByteArrayOutputStream()
                        if(!small.compress(Bitmap.CompressFormat.WEBP,quality,out))continue
                        val raw=out.toByteArray()
                        if(raw.size !in 80..MAX_RAW)continue
                        val encoded="$id:"+Base64.encodeToString(raw,Base64.NO_WRAP)
                        if(encoded.length<=MAX_TEXT)return@runCatching encoded
                    }
                } finally { if(small!==b)small.recycle() }
            }
            null
        }.getOrNull()
    }
    fun decode(id:String,payload:String?):Bitmap? {
        if(!Wq183MascotStore.isCustomId(id)||payload==null||payload.length>MAX_TEXT||!payload.startsWith("$id:"))return null
        return runCatching {
            val b64=payload.substring(id.length+1)
            if(b64.length !in 100..173336||!b64.matches(Regex("[A-Za-z0-9+/=]+")))return@runCatching null
            val raw=Base64.decode(b64,Base64.NO_WRAP)
            if(raw.size !in 80..MAX_RAW||String(raw,0,4,Charsets.US_ASCII)!="RIFF"||String(raw,8,4,Charsets.US_ASCII)!="WEBP")return@runCatching null
            val opts=BitmapFactory.Options().apply {inJustDecodeBounds=true}
            BitmapFactory.decodeByteArray(raw,0,raw.size,opts)
            if(opts.outWidth !in 1..MAX_EDGE||opts.outHeight !in 1..MAX_EDGE)return@runCatching null
            BitmapFactory.decodeByteArray(raw,0,raw.size)
        }.getOrNull()
    }
}

/** Keep legacy 160px channel for older clients; HD is optional and private to room participants. */
@Composable
internal fun Wq128RoomHdBridge(room:DatabaseReference?,side:String?,myId:String?,rivalId:String?,
    selected:String,allowed:Boolean,connected:Boolean) {
    val context=LocalContext.current
    val ownField=side?.let(Wq128RoomHd::field)
    val rivalField=Wq128RoomHd.field(if(side=="host")"guest" else "host")
    var payload by remember(room,rivalId){mutableStateOf<String?>(null)}
    DisposableEffect(room,rivalField,rivalId) {
        val ref=if(room!=null&&Wq183MascotStore.isCustomId(rivalId))room.child(rivalField) else null
        val listener=if(ref!=null)object:ValueEventListener {
            override fun onDataChange(snapshot:DataSnapshot){payload=snapshot.getValue(String::class.java)}
            override fun onCancelled(error:DatabaseError){payload=null}
        } else null
        if(ref!=null&&listener!=null)ref.addValueEventListener(listener)
        onDispose {
            if(ref!=null&&listener!=null)ref.removeEventListener(listener)
            if(rivalId!=null)Wq128RoomHd.receive(rivalId,null)
        }
    }
    LaunchedEffect(room,rivalId,payload) {
        if(rivalId!=null) {
            val value=withContext(Dispatchers.Default){Wq128RoomHd.decode(rivalId,payload)}
            currentCoroutineContext().ensureActive()
            Wq128RoomHd.receive(rivalId,value)
        }
    }
    LaunchedEffect(room,ownField,myId,selected,allowed,connected) {
        if(room==null||ownField==null)return@LaunchedEffect
        val node=room.child(ownField)
        if(!allowed||!Wq183MascotStore.isCustomId(selected)||myId!=selected) {
            node.removeValue();node.onDisconnect().cancel()
            return@LaunchedEffect
        }
        if(!connected)return@LaunchedEffect
        val value=withContext(Dispatchers.Default) {
            Wq183MascotStore.picture(context,selected,false)?.let {Wq128RoomHd.encode(selected,it)}
        } ?: return@LaunchedEffect
        val registered=suspendCancellableCoroutine<Boolean>{c->
            node.onDisconnect().removeValue().addOnCompleteListener {if(c.isActive)c.resume(it.isSuccessful)}
        }
        if(registered&&connected){
            currentCoroutineContext().ensureActive();node.setValue(value)
        }
    }
    DisposableEffect(room,ownField) {
        onDispose {if(room!=null&&ownField!=null){room.child(ownField).removeValue();room.child(ownField).onDisconnect().cancel()}}
    }
}
