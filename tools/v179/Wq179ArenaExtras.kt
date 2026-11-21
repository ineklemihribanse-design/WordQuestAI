package com.kelimebulmacasi.a1

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.util.Random

/** Shuffled, stable per participant and room. Never leaks crossword answers. */
internal object Wq179ArenaOrder {
    fun next(code:String, uid:String, count:Int, solved:Set<Int>, current:Int):Int {
        if(count<=0)return 0
        val order=(0 until count).shuffled(Random((code+"|"+uid).hashCode().toLong()))
        val start=order.indexOf(current)
        for(step in 1..count) {
            val candidate=order[(start+step+count)%count]
            if(candidate !in solved && candidate!=current) return candidate
        }
        return if(current in 0 until count) current else order.first()
    }
    fun hintPosition(code:String, index:Int, pattern:String):Int {
        val choices=pattern.indices.filter{pattern[it]=='.'}
        if(choices.isEmpty()) return -1
        // The first unopened letter is NOT automatically revealed.
        return choices[Random((code+"|H|"+index).hashCode().toLong()).nextInt(choices.size)]
    }
}

/** A free automatic word change per participant per room; survives screen recreation. */
internal object Wq179ArenaSkip {
    private fun key(room:String,uid:String)="free-skip-"+room+"-"+uid
    private fun store(ctx:Context)=ctx.getSharedPreferences("wordquest_arena_skips",Context.MODE_PRIVATE)
    fun used(ctx:Context,room:String,uid:String)=store(ctx).getBoolean(key(room,uid),false)
    fun consume(ctx:Context,room:String,uid:String) {
        store(ctx).edit().putBoolean(key(room,uid),true).apply()
    }
}

@Composable
internal fun Wq179ArenaWordChooser(
    puzzle:Puzzle, solved:Set<Int>, selected:Int,
    choose:(Int)->Unit
) {
    var open by rememberSaveable(puzzle.hashCode()){ mutableStateOf(false) }
    val unsolved=puzzle.words.size-solved.size
    OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(16.dp)) {
        Text("☷  TÜM KELİMELER  ·  $unsolved kaldı",fontWeight=FontWeight.Bold)
    }
    if(open) Dialog(onDismissRequest={open=false}) {
        Surface(shape=RoundedCornerShape(24.dp),color=Color(0xFFFAF7FF),
            modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Text("Tüm Kelimeler",fontWeight=FontWeight.ExtraBold,
                    color=Color(0xFF362555),fontSize=21.sp)
                Text("Bildiğin ipucunu seç. Çözülmüş sözcüklerin cevapları oyun sonunda gösterilir.",
                    color=Color(0xFF706388),fontSize=12.sp)
                Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    puzzle.words.indices.sortedWith(compareBy<Int> {puzzle.words[it].number}
                        .thenBy{puzzle.words[it].direction.name}).forEach{index ->
                        val w=puzzle.words[index]
                        val done=index in solved
                        val active=index==selected
                        Surface(onClick={if(!done){choose(index);open=false} else {}},
                            enabled=!done,shape=RoundedCornerShape(12.dp),
                            color=when {
                                done -> Color(0xFFE7F2E9)
                                active -> Color(0xFFE7DAFF)
                                else -> Color.White
                            },modifier=Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal=11.dp,vertical=10.dp)) {
                                Text(w.number.toString()+" "+(if(w.direction==Direction.Across)"YATAY" else "DİKEY")+
                                    " · "+w.cells.size+" harf"+
                                    if(done)"  ✓ ÇÖZÜLDÜ" else "",
                                    color=Color(0xFF7E53BC),fontSize=10.sp,
                                    fontWeight=FontWeight.Bold)
                                Text(w.entry.clue,color=Color(0xFF33284C),
                                    fontSize=14.sp,fontWeight=FontWeight.Medium)
                            }
                        }
                    }
                }
                TextButton(onClick={open=false},modifier=Modifier.align(Alignment.End)) {
                    Text("Kapat")
                }
            }
        }
    }
}

@Composable
internal fun Wq179ArenaClueHeader(puzzle:Puzzle, selected:Int) {
    val w=puzzle.words.getOrNull(selected) ?:return
    Surface(shape=RoundedCornerShape(17.dp),color=Color(0xFFEFE6FF),
        modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal=12.dp,vertical=9.dp),
            verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text("AKTİF İPUCU · "+w.number+" "+
                (if(w.direction==Direction.Across)"YATAY" else "DİKEY")+
                " · "+w.cells.size+" HARF",fontSize=11.sp,
                fontWeight=FontWeight.Black,color=Color(0xFF7952B3))
            Text(w.entry.clue,color=Color(0xFF2C2450),
                fontWeight=FontWeight.SemiBold,fontSize=15.sp)
        }
    }
}

internal object Wq179ArenaSoundPrefs {
    private const val FILE="wordquest_arena_audio_179"
    private fun prefs(context:Context)=context.getSharedPreferences(FILE,Context.MODE_PRIVATE)
    fun ambient(context:Context)=prefs(context).getBoolean("ambient",true)
    fun clock(context:Context)=prefs(context).getBoolean("clock",true)
    fun setAmbient(context:Context,on:Boolean){prefs(context).edit().putBoolean("ambient",on).apply()}
    fun setClock(context:Context,on:Boolean){prefs(context).edit().putBoolean("clock",on).apply()}
}

/** Original synthetic arena effects; no copyrighted or continuous audio assets. */
internal class Wq179ArenaTones: AutoCloseable {
    private val quiet=runCatching{ToneGenerator(AudioManager.STREAM_MUSIC,23)}.getOrNull()
    private val loud=runCatching{ToneGenerator(AudioManager.STREAM_MUSIC,60)}.getOrNull()
    private var closed=false
    fun ambient(){
        if(!closed)runCatching{quiet?.startTone(ToneGenerator.TONE_PROP_BEEP,85)}
    }
    fun tick(seconds:Int){
        if(closed || seconds !in 1..10) return
        runCatching {
            if(seconds<=3) loud?.startTone(ToneGenerator.TONE_DTMF_8,140)
            else quiet?.startTone(ToneGenerator.TONE_DTMF_1,75)
        }
    }
    override fun close(){if(!closed){closed=true;quiet?.release();loud?.release()}}
}

@Composable
internal fun Wq179ArenaGameAudio(isPlaying:Boolean,seconds:Int?,effectsEnabled:Boolean){
    val context=LocalContext.current
    val ambient=remember(context){Wq179ArenaSoundPrefs.ambient(context)}
    val clock=remember(context){Wq179ArenaSoundPrefs.clock(context)}
    val tones=remember{Wq179ArenaTones()}
    DisposableEffect(tones){onDispose{tones.close()}}
    LaunchedEffect(isPlaying,effectsEnabled,ambient){
        while(isPlaying && effectsEnabled && ambient){
            tones.ambient()
            delay(12000L)
        }
    }
    LaunchedEffect(isPlaying,seconds,effectsEnabled,clock){
        if(isPlaying && effectsEnabled && clock && seconds!=null && seconds in 1..10){
            tones.tick(seconds)
        }
    }
}

@Composable
internal fun Wq179ArenaAudioSettings(){
    val context=LocalContext.current
    var ambient by remember(context){mutableStateOf(Wq179ArenaSoundPrefs.ambient(context))}
    var clock by remember(context){mutableStateOf(Wq179ArenaSoundPrefs.clock(context))}
    Text("Arena sesleri",fontWeight=FontWeight.Bold,color=Color(0xFF3C3258))
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text("Arena atmosfer sesi",modifier=Modifier.weight(1f),fontSize=14.sp)
        Switch(checked=ambient,onCheckedChange={ambient=it;Wq179ArenaSoundPrefs.setAmbient(context,it)})
    }
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text("Son 10 saniye saat sesi",modifier=Modifier.weight(1f),fontSize=14.sp)
        Switch(checked=clock,onCheckedChange={clock=it;Wq179ArenaSoundPrefs.setClock(context,it)})
    }
    Text("Doğru / yanlış seslerini üstteki oyun efektleri anahtarından kapatabilirsin.",
        fontSize=11.sp,color=Color(0xFF77708B))
}


/** Stadium lights under the live game, without moving the layout. */
@Composable
internal fun Wq179ArenaStageBackground(modifier:Modifier=Modifier){
    androidx.compose.foundation.Canvas(modifier){
        drawRect(Brush.verticalGradient(listOf(
            Color(0xFFF6F1FF),Color(0xFFF8F6FF),Color(0xFFFFFCFF))))
        val top=androidx.compose.ui.geometry.Offset(size.width*.5f,size.height*.02f)
        drawCircle(Color(0x148D54C8),radius=size.width*.90f,center=top)
        drawCircle(Color(0x0DFFBD75),radius=size.width*.52f,center=top)
        drawLine(Color(0x1F8550B7),
            androidx.compose.ui.geometry.Offset(size.width*.09f,0f),
            androidx.compose.ui.geometry.Offset(size.width*.27f,size.height*.23f),5f)
        drawLine(Color(0x1F8550B7),
            androidx.compose.ui.geometry.Offset(size.width*.91f,0f),
            androidx.compose.ui.geometry.Offset(size.width*.73f,size.height*.23f),5f)
    }
}
