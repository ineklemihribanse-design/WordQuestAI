package com.kelimebulmacasi.a1

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object Wq150SoundPacks{
    val names=linkedMapOf("crystal" to "Kristal","stadium" to "Stadyum","digital" to "Dijital","soft" to "Yumuşak","silent" to "Sessiz")
    val descriptions=mapOf("crystal" to "Parlak, kısa çanlar ve ahşap tıklar","stadium" to "Tok perküsyon ve zafer fanfarı","digital" to "Modern, hafif elektronik efektler","soft" to "Sakin ve düşük yoğunluklu sesler","silent" to "Arena seslerini tamamen kapat")
    val events=listOf("correct","wrong","tick","urgent","ambient")
    fun normalize(value:String?)=value?.takeIf{it in names}?:"crystal"
    private fun prefs(context:Context)=context.getSharedPreferences("wordquest_arena_audio_179",Context.MODE_PRIVATE)
    fun selected(context:Context)=normalize(prefs(context).getString("sound_pack","crystal"))
    fun select(context:Context,value:String){prefs(context).edit().putString("sound_pack",normalize(value)).apply()}
}
// Confirmed answers must not be interrupted by clock or ambient cues.
internal fun wq150SoundPriority(event:String)=when(event){
    "correct","wrong"->3
    "tick","urgent"->2
    "ambient"->1
    else->0
}
internal fun wq150SoundVolume(event:String)=when(event){
    "ambient"->.30f
    "tick"->.42f
    "urgent"->.62f
    else->.85f
}
internal class Wq150SoundPlayer(context:Context):AutoCloseable{
    private val pool=runCatching{
        SoundPool.Builder().setMaxStreams(2).setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    }.getOrNull()
    private val resourceIds=mapOf(
        "crystal_correct" to R.raw.wq150_crystal_correct,
        "crystal_wrong" to R.raw.wq150_crystal_wrong,
        "crystal_tick" to R.raw.wq150_crystal_tick,
        "crystal_urgent" to R.raw.wq150_crystal_urgent,
        "crystal_ambient" to R.raw.wq150_crystal_ambient,
        "stadium_correct" to R.raw.wq150_stadium_correct,
        "stadium_wrong" to R.raw.wq150_stadium_wrong,
        "stadium_tick" to R.raw.wq150_stadium_tick,
        "stadium_urgent" to R.raw.wq150_stadium_urgent,
        "stadium_ambient" to R.raw.wq150_stadium_ambient,
        "digital_correct" to R.raw.wq150_digital_correct,
        "digital_wrong" to R.raw.wq150_digital_wrong,
        "digital_tick" to R.raw.wq150_digital_tick,
        "digital_urgent" to R.raw.wq150_digital_urgent,
        "digital_ambient" to R.raw.wq150_digital_ambient,
        "soft_correct" to R.raw.wq150_soft_correct,
        "soft_wrong" to R.raw.wq150_soft_wrong,
        "soft_tick" to R.raw.wq150_soft_tick,
        "soft_urgent" to R.raw.wq150_soft_urgent,
        "soft_ambient" to R.raw.wq150_soft_ambient
    )
    private val samples=mutableMapOf<String,Int>()
    private val loaded=mutableSetOf<Int>()
    private var stream=0
    private var priority=0
    private var protectedUntil=0L
    private var closed=false
    init{
        pool?.setOnLoadCompleteListener{_,id,status->if(!closed&&status==0)loaded.add(id)}
        Wq150SoundPacks.names.keys.filter{it!="silent"}.forEach{pack->
            Wq150SoundPacks.events.forEach{event->
                val key=pack+"_"+event
                val res=resourceIds.getValue(key)
                if(res!=0)runCatching{pool?.load(context,res,1)?.let{samples[key]=it}}
            }
        }
    }
    fun stop(){
        if(!closed&&stream!=0)runCatching{pool?.stop(stream)}
        stream=0;priority=0;protectedUntil=0L
    }
    fun play(pack:String,event:String){
        if(closed)return
        if(pack=="silent"){stop();return}
        val id=samples[pack+"_"+event]?:return
        if(id !in loaded)return
        val now=android.os.SystemClock.elapsedRealtime()
        val next=wq150SoundPriority(event)
        if(next<priority&&now<protectedUntil)return
        stop()
        val volume=wq150SoundVolume(event)
        stream=runCatching{pool?.play(id,volume,volume,next,0,1f)?:0}.getOrDefault(0)
        if(stream!=0){
            priority=next
            protectedUntil=now+when(next){3->1000L;2->300L;else->0L}
        }
    }
    override fun close(){if(!closed){stop();closed=true;pool?.release();loaded.clear()}}
}
@Composable
internal fun Wq150SoundPackSettings(){
    val context=LocalContext.current
    var selected by remember(context){mutableStateOf(Wq150SoundPacks.selected(context))}
    val player=remember(context){Wq150SoundPlayer(context)}
    DisposableEffect(player){onDispose{player.close()}}
    Text("Arena ses paketi",fontSize=14.sp,color=Color(0xFF3C3258))
    Wq150SoundPacks.names.entries.toList().chunked(2).forEach{row->
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            row.forEach{(id,label)->
                OutlinedButton(onClick={player.stop();selected=id;Wq150SoundPacks.select(context,id);player.play(id,"correct")},
                    modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp,vertical=4.dp)){
                    Text((if(selected==id)"✓ " else "")+label,fontSize=13.sp)
                }
            }
            if(row.size==1)Spacer(Modifier.weight(1f))
        }
    }
    Text(Wq150SoundPacks.descriptions[selected].orEmpty(),fontSize=12.sp,color=Color(0xFF77708B))
    if(selected!="silent"){
        Text("Ön dinleme",fontSize=12.sp,color=Color(0xFF3C3258))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            listOf("correct" to "Doğru","wrong" to "Yanlış","tick" to "Saat").forEach{(event,label)->
                TextButton(onClick={player.stop();player.play(selected,event)},modifier=Modifier.weight(1f),contentPadding=PaddingValues(4.dp)){Text(label,fontSize=12.sp)}
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            TextButton(onClick={player.stop();player.play(selected,"urgent")},modifier=Modifier.weight(1f)){Text("Son 3 saniye")}
            TextButton(onClick={player.stop();player.play(selected,"ambient")},modifier=Modifier.weight(1f)){Text("Ortam sesi")}
        }
    }
}
