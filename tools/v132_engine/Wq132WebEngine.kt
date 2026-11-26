package com.kelimebulmacasi.a1

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import org.json.JSONObject
import org.json.JSONArray
import java.security.MessageDigest

/** The only input to the isolated web game engine is public bundled vocabulary and bounded settings.
 * No device files, account info, credentials, network, native bridge or arbitrary commands. */
internal object Wq132WebEngine {
    const val PATH="web/arena_engine.html"
    const val MAX_BYTES=128*1024
    private const val CSP="default-src 'none'; script-src 'unsafe-inline'; style-src 'none'; img-src 'none'; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'"
    fun validate(bytes:ByteArray):String {
        require(bytes.size in 200..MAX_BYTES)
        val html=String(bytes,Charsets.UTF_8)
        require(html.startsWith("<!DOCTYPE html>")&&html.contains(CSP)&&html.contains("window.wqGenerate"))
        return html
    }
    const val MAX_ROUND_BYTES=48*1024
    data class Round(val board:PremiumArenaBoard?=null,val questions:List<PremiumQuestion> = emptyList(),
        val snapshot:String="",val snapshotId:String="",val wordLevels:Map<String,String> = emptyMap())
    fun snapshotId(snapshot:String):String {
        val bytes=snapshot.toByteArray(Charsets.UTF_8)
        require(bytes.size in 2..MAX_ROUND_BYTES){"Ortak oyun verisi sınır dışı ("+bytes.size+" bayt, en fazla "+MAX_ROUND_BYTES+" bayt)"}
        return "web:"+MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt() and 255)}
    }
    private data class Input(val words:JSONArray,val settings:JSONObject,val seed:Int)
    private fun input(c:Context,mode:String,levels:List<String>,topic:String,size:Int,seconds:Int,hints:Int,retries:Int,seed:Int):Input {
        require(mode in setOf("duel","team","quiz"))
        val pool=Wq186ArenaPresetStore.LEVELS.filter{it in levels}
        require(pool.isNotEmpty()&&pool.size<=size)
        val all=JSONArray(c.assets.open("arena-vocabulary-v1.json").bufferedReader().use{it.readText()})
        val filtered=JSONArray()
        for(i in 0 until all.length()){
            val w=all.getJSONObject(i)
            if(w.getString("level") in pool && (mode=="quiz" || topic==PremiumArenaRules.allTopics || topic=="all" || w.getString("category")==topic))filtered.put(JSONObject().put("answer",w.getString("answer"))
                .put("meaning",w.getString("meaning")).put("category",w.getString("category")).put("level",w.getString("level")))
        }
        val settings=JSONObject().put("mode",mode).put("levels",JSONArray(pool)).put("topic",topic)
            .put("size",size).put("seconds",seconds).put("hints",hints).put("retries",retries)
        return Input(filtered,settings,seed)
    }
    private fun decode(input:Input,result:String):Round {
        require(result.isNotBlank()){"Oyun motorundan boş yanıt geldi."}
        val response=JSONObject(result)
        require(response.optBoolean("ok")){response.optString("error","Oyun üretilemedi")}
        val roundId=snapshotId(result)
        val s=input.settings
        val levels=s.getJSONArray("levels").let{a->List(a.length()){a.getString(it)}}
        val size=s.getInt("size")
        val topic=s.getString("topic")
        val records=(0 until input.words.length()).map{input.words.getJSONObject(it)}
        fun matches(answer:String,meaning:String,level:String,topicRequired:Boolean)=records.any{
            it.getString("answer").equals(answer,true)&&it.getString("meaning")==meaning&&it.getString("level")==level&&
                (!topicRequired||topic==PremiumArenaRules.allTopics||topic=="all"||it.getString("category")==topic)
        }
        if(s.getString("mode")=="quiz"){
            val a=response.getJSONArray("data");require(a.length()==size)
            val questions=List(size){n->
                val q=a.getJSONObject(n);val options=q.getJSONArray("options").let{List(it.length()){j->it.getString(j)}}
                val answer=q.getString("answer");val meaning=q.getString("prompt");val level=q.getString("level");val correct=q.getInt("correct")
                require(options.size==5&&options.distinctBy{it.lowercase()}.size==5&&correct in options.indices&&options[correct]==answer)
                require(level in levels&&matches(answer,meaning,level,true)&&options.all{option->records.any{it.getString("answer").equals(option,true)}})
                PremiumQuestion("$meaning — İngilizcesi hangisidir?",options,correct,answer,level)
            }
            require(questions.map{it.answer.lowercase()}.distinct().size==size&&questions.map{it.level}.toSet()==levels.toSet())
            return Round(questions=questions,snapshot=result,snapshotId=roundId)
        }
        val words=response.getJSONObject("data").getJSONArray("words")
        require(words.length()==size)
        val entries=mutableListOf<Entry>();val places=mutableListOf<Placement>()
        val seen=mutableSetOf<String>();val used=mutableSetOf<String>();val wordLevels=mutableMapOf<String,String>()
        val cells=mutableMapOf<Pair<Int,Int>,Pair<Char,MutableSet<String>>>()
        val owners=mutableMapOf<Pair<Int,Int>,MutableSet<Int>>()
        val links=List(size){mutableSetOf<Int>()}
        for(n in 0 until size){
            val w=words.getJSONArray(n);require(w.length()==6)
            val answer=w.getString(0);val meaning=w.getString(1);val level=w.getString(2)
            val r=w.getInt(3);val c=w.getInt(4);val d=w.getString(5)
            require(answer.matches(Regex("[A-Z]{2,24}"))&&meaning.length<=1000&&level in levels&&matches(answer,meaning,level,true))
            require(seen.add(answer)&&d in setOf("A","D")&&r in 0..79&&c in 0..79)
            used.add(level);wordLevels[answer]=level
            val dr=if(d=="D")1 else 0;val dc=1-dr
            answer.forEachIndexed{j,ch->
                val y=r+j*dr;val x=c+j*dc;require(y in 0..79&&x in 0..79)
                val k=y to x;val old=cells[k]
                if(old!=null){require(old.first==ch&&d !in old.second);old.second.add(d)}else cells[k]=ch to mutableSetOf(d)
                val list=owners.getOrPut(k){mutableSetOf()}
                list.forEach{links[n].add(it);links[it].add(n)};list.add(n)
            }
            entries.add(Entry(answer,meaning,""));places.add(Placement(Cell(r,c),if(d=="A")Direction.Across else Direction.Down))
        }
        require(used==levels.toSet())
        val reached=mutableSetOf(0);val queue=java.util.ArrayDeque<Int>();queue.add(0)
        while(queue.isNotEmpty())links[queue.removeFirst()].forEach{if(reached.add(it))queue.add(it)}
        require(reached.size==size)
        return Round(board=PremiumArenaBoard(roundId,levels.first(),topic,Puzzle.create(entries,places)),snapshot=result,snapshotId=roundId,wordLevels=wordLevels)
    }
    /** Guests use the exact frozen host payload, never rerun a potentially updated engine.
     * The digest covers its bytes; the existing native board parser checks all entries. */
    fun decodeShared(c:Context,mode:String,levels:List<String>,topic:String,size:Int,seconds:Int,hints:Int,retries:Int,
        id:String,payload:String):Round {
        require(id==snapshotId(payload)){"Ortak oyun verisi doğrulanamadı"}
        return decode(input(c,mode,levels,topic,size,seconds,hints,retries,0),payload)
    }
    // Stability release: no WebView or JavaScript participates in game creation.
    // Preserve the same validated round format and SHA-256 for Firebase.
    suspend fun generate(c:Context,mode:String,levels:List<String>,topic:String,size:Int,seconds:Int,hints:Int,retries:Int,seed:Int):Round =
        withContext(Dispatchers.Default) {
            val scope=currentCoroutineContext()
            scope.ensureActive()
            val input=input(c,mode,levels,topic,size,seconds,hints,retries,seed)
            val result=Wq136RecoveryEngine.generate(input.words,input.settings,input.seed){scope.ensureActive()}
            scope.ensureActive()
            decode(input,result)
        }
}
