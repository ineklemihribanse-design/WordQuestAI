package com.kelimebulmacasi.a1

import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class Wq136RecoveryEngineTest {
    private val levels=listOf("A1","A2","B1","B2","C1","C2")
    private val vocabulary by lazy {
        val file=listOf(File("src/main/assets/arena-vocabulary-v1.json"),File("app/src/main/assets/arena-vocabulary-v1.json"),File("source/app/src/main/assets/arena-vocabulary-v1.json")).first{it.isFile}
        JSONArray(file.readText())
    }
    private fun settings(mode:String,size:Int)=JSONObject().put("mode",mode).put("levels",JSONArray(levels))
        .put("size",size).put("seconds",30).put("hints",3).put("retries",1).put("topic","Tüm konular")
    @Test fun nativeRecoverySixLevelsCrosswordAndQuiz(){
        for(mode in listOf("duel","team","quiz")){
            val size=if(mode=="quiz")20 else 50
            val raw=Wq136RecoveryEngine.generate(vocabulary,settings(mode,size),42)
            assertTrue(raw.toByteArray(Charsets.UTF_8).size in 2..48*1024)
            val r=JSONObject(raw);assertTrue(r.getBoolean("ok"))
            val a=if(mode=="quiz")r.getJSONArray("data") else r.getJSONObject("data").getJSONArray("words")
            assertEquals(size,a.length())
            val seen=mutableSetOf<String>();val used=mutableSetOf<String>()
            val cells=mutableMapOf<Pair<Int,Int>,Pair<Char,String>>()
            val links=List(size){mutableSetOf<Int>()}
            val owner=mutableMapOf<Pair<Int,Int>,Int>()
            for(n in 0 until size){
                if(mode=="quiz"){
                    val q=a.getJSONObject(n);val options=q.getJSONArray("options")
                    assertEquals(5,options.length());assertEquals(q.getString("answer"),options.getString(q.getInt("correct")))
                    assertEquals(5,(0 until options.length()).map{options.getString(it).lowercase()}.toSet().size)
                    assertTrue(seen.add(q.getString("answer").lowercase()));used.add(q.getString("level"))
                }else{
                    val w=a.getJSONArray(n);val answer=w.getString(0);val dir=w.getString(5)
                    assertTrue(seen.add(answer));used.add(w.getString(2))
                    assertTrue(answer.matches(Regex("[A-Z]{2,24}")));assertTrue(dir in listOf("A","D"))
                    answer.forEachIndexed{i,ch->
                        val y=w.getInt(3)+if(dir=="D")i else 0
                        val x=w.getInt(4)+if(dir=="A")i else 0
                        assertTrue(y in 0..79&&x in 0..79)
                        val k=y to x
                        val old=cells[k]
                        if(old!=null){assertEquals(old.first,ch);assertNotEquals(old.second,dir);val prev=owner.getValue(k);links[n].add(prev);links[prev].add(n)}
                        else {cells[k]=ch to dir;owner[k]=n}
                    }
                }
            }
            assertEquals(levels.toSet(),used)
            if(mode!="quiz"){
                val reached=mutableSetOf(0);val queue=java.util.ArrayDeque<Int>();queue.add(0)
                while(queue.isNotEmpty())links[queue.removeFirst()].forEach{if(reached.add(it))queue.add(it)}
                assertEquals(size,reached.size)
            }
        }
    }
    @Test fun refusesInsufficientQuestionCount(){
        try{Wq136RecoveryEngine.generate(vocabulary,settings("quiz",5),1);fail("Should refuse five questions for six levels")}
        catch(expected:IllegalArgumentException){}
    }
}
