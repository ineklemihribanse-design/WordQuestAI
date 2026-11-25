package com.kelimebulmacasi.a1

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random
import kotlin.math.abs

/** Offline recovery engine. Only public dictionary input; same wire format and native validation. */
internal object Wq136RecoveryEngine {
    private data class Word(val answer:String,val meaning:String,val level:String)
    private data class Pos(val r:Int,val c:Int)
    private data class Mark(val ch:Char,val dirs:String)
    private data class Placed(val w:Word,val r:Int,val c:Int,val d:String)
    private data class Candidate(val index:Int,val r:Int,val c:Int,val d:String,val cost:Int)
    fun generate(words:JSONArray,s:JSONObject,seed:Int,checkCancelled:()->Unit = {}):String {
        checkCancelled()
        val mode=s.getString("mode")
        val levels=s.getJSONArray("levels").let{a->List(a.length()){a.getString(it)}}
        val size=s.getInt("size")
        require(mode in setOf("duel","team","quiz") && size in (if(mode=="quiz")listOf(5,10,12,15,20) else listOf(20,30,40,50)))
        require(levels.isNotEmpty() && size>=levels.size)
        val topic=s.getString("topic")
        val source=(0 until words.length()).map{words.getJSONObject(it)}
        val valid=source.filter{it.getString("level") in levels && it.getString("answer").isNotBlank() && it.getString("meaning").isNotBlank()}
        val all=valid.map{Word(it.getString("answer").trim(),it.getString("meaning"),it.getString("level"))}
        val targets=valid.filter{topic=="Tüm konular"||topic=="all"||it.getString("category")==topic}
            .map{Word(it.getString("answer").trim(),it.getString("meaning"),it.getString("level"))}
            .filter{mode=="quiz"||it.answer.matches(Regex("[A-Za-z]{2,24}"))}
            .sortedWith(compareBy<Word>{it.level}.thenBy{it.answer.lowercase()}.thenBy{it.meaning})
            .distinctBy{it.answer.lowercase()}
        fun pool(rng:Random):List<Word>{
            val groups=levels.associateWith{l->targets.filter{it.level==l}.shuffled(rng).toMutableList()}
            for(l in levels)require(groups.getValue(l).isNotEmpty()){"Seçilen seviyede uygun kelime yok: "+l}
            val result=mutableListOf<Word>();val order=levels.shuffled(rng)
            while(result.size<size){
                val before=result.size
                for(l in order)if(result.size<size){val group=groups.getValue(l);if(group.isNotEmpty())result.add(group.removeAt(group.lastIndex))}
                require(before!=result.size){"Seçilen havuzda yeterli farklı kelime yok."}
            }
            return result
        }
        if(mode=="quiz"){
            val rng=Random(seed);val output=JSONArray()
            for(w in pool(rng)){
                checkCancelled()
                val wrong=all.asSequence().filter{!it.answer.equals(w.answer,true)&&!it.meaning.equals(w.meaning,true)&&
                    !it.meaning.substringBefore(',').trim().equals(w.meaning.substringBefore(',').trim(),true)}
                    .map{it.answer}.distinctBy{it.lowercase()}.toList().shuffled(rng).take(4)
                require(wrong.size==4){"Beş seçenek için yeterli farklı kelime yok."}
                val options=(wrong+w.answer).shuffled(rng)
                output.put(JSONObject().put("prompt",w.meaning).put("options",JSONArray(options)).put("correct",options.indexOf(w.answer))
                    .put("answer",w.answer).put("level",w.level))
            }
            return JSONObject().put("ok",true).put("data",output).toString()
        }
        repeat(48){attempt->
                checkCancelled()
            val pending=pool(Random(seed+attempt*7919)).map{it.copy(answer=it.answer.uppercase(java.util.Locale.ROOT))}
                .sortedByDescending{it.answer.length}.toMutableList()
            val grid=HashMap<Pos,Mark>();val placed=mutableListOf<Placed>()
            fun put(w:Word,r:Int,c:Int,d:String){
                val dr=if(d=="D")1 else 0;val dc=1-dr
                w.answer.forEachIndexed{i,ch->val key=Pos(r+i*dr,c+i*dc);grid[key]=Mark(ch,(grid[key]?.dirs?:"")+d)}
                placed.add(Placed(w,r,c,d))
            }
            fun validPlacement(w:Word,r:Int,c:Int,d:String):Boolean{
                val dr=if(d=="D")1 else 0;val dc=1-dr
                if(Pos(r-dr,c-dc) in grid || Pos(r+w.answer.length*dr,c+w.answer.length*dc) in grid)return false
                var crosses=0
                w.answer.forEachIndexed{i,ch->
                    val y=r+i*dr;val x=c+i*dc;val old=grid[Pos(y,x)]
                    if(old!=null){if(old.ch!=ch||d in old.dirs)return false;crosses++}
                    else if(Pos(y-dc,x-dr) in grid || Pos(y+dc,x+dr) in grid)return false
                }
                return crosses>0
            }
            put(pending.removeAt(0),0,0,"A")
            while(pending.isNotEmpty()){
                checkCancelled()
                val minR=grid.keys.minOf{it.r};val maxR=grid.keys.maxOf{it.r}
                val minC=grid.keys.minOf{it.c};val maxC=grid.keys.maxOf{it.c}
                var best:Candidate?=null
                for((wi,w) in pending.withIndex())for((pos,mark) in grid){
                    if(mark.dirs.length!=1)continue
                    val d=if(mark.dirs=="A")"D" else "A";val dr=if(d=="D")1 else 0;val dc=1-dr
                    for(i in w.answer.indices){
                        if(w.answer[i]!=mark.ch)continue
                        val r=pos.r-i*dr;val c=pos.c-i*dc
                        val h=maxOf(maxR,r+(w.answer.length-1)*dr)-minOf(minR,r)+1
                        val width=maxOf(maxC,c+(w.answer.length-1)*dc)-minOf(minC,c)+1
                        if(h>80||width>80)continue
                        val cost=h*width+abs(h-width)*3
                        if(cost>=(best?.cost?:Int.MAX_VALUE))continue
                        if(validPlacement(w,r,c,d))best=Candidate(wi,r,c,d,cost)
                    }
                }
                val next=best?:break
                put(pending.removeAt(next.index),next.r,next.c,next.d)
            }
            if(pending.isEmpty()){
                val minR=placed.minOf{it.r};val minC=placed.minOf{it.c}
                val result=JSONArray()
                placed.forEach{p->result.put(JSONArray().put(p.w.answer).put(p.w.meaning).put(p.w.level).put(p.r-minR).put(p.c-minC).put(p.d))}
                return JSONObject().put("ok",true).put("data",JSONObject().put("levels",JSONArray(levels)).put("words",result)).toString()
            }
        }
        error("Seçilen havuzdan tam ve bağlı bulmaca üretilemedi. Başka konu veya kelime sayısı seç.")
    }
}
