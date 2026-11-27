package com.kelimebulmacasi.a1

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

internal object Wq150ModeCatalog {
    val kinds=linkedMapOf("duel" to "Bulmaca Düellosu","quiz" to "5 Şıklı Düello",
        "coop" to "Ortak Bulmaca","team" to "Takım Oyunu")
    private val competition=linkedMapOf("classic" to "Klasik","streak" to "Seri",
        "limited" to "Sınırlı Seri","speed" to "Hız","strategy" to "Strateji")
    val cefr=listOf("A1","A2","B1","B2","C1","C2")
    val yds=listOf("easy","medium","hard")
    fun modes(kind:String):Map<String,String> = when(kind){
        "duel","team" -> competition
        "quiz" -> competition.filterKeys{it!="strategy"}+("simultaneous" to "Eş Zamanlı")
        "coop" -> linkedMapOf("alternating" to "Sıralı","free" to "Serbest",
            "double" to "Çift Cevap","timed" to "Süreli","perfect" to "Kusursuz")
        else -> emptyMap()
    }
    fun levels(scale:String)=if(scale=="yds")yds else cefr
    fun sizes(format:String)=if(format=="quiz")listOf(5,10,12,15,20,30,50,70) else listOf(10,20,30,40,50,70)
}
internal data class Wq150Settings(
    val kind:String="duel",val mode:String="classic",val format:String="crossword",
    val scale:String="cefr",val levels:List<String> = listOf("A1"),
    val topic:String?=null,val wordType:String?=null,val count:Int=20,val seconds:Int=30,
    val hints:Int=3,val retries:Int=0,val skipLimit:Int=2,val limit:Int=3,val maxMistakes:Int=3
) {
    companion object {
        fun fromJson(j:JSONObject):Wq150Settings {
            val levels=j.optJSONArray("levels")
            val result=Wq150Settings(kind=j.optString("kind","duel"),mode=j.optString("mode","classic"),
                format=j.optString("format","crossword"),scale=j.optString("scale","cefr"),
                levels=if(levels==null)listOf("A1") else (0 until levels.length()).map{levels.optString(it)},
                topic=j.optString("topic").takeIf{!j.isNull("topic")&&it.isNotBlank()},
                wordType=j.optString("wordType").takeIf{!j.isNull("wordType")&&it.isNotBlank()},
                count=j.optInt("count",20),seconds=j.optInt("seconds",30),hints=j.optInt("hints",3),
                retries=j.optInt("retries",0),skipLimit=j.optInt("skipLimit",2),limit=j.optInt("limit",3),
                maxMistakes=j.optInt("maxMistakes",3))
            require(result.valid())
            return result
        }
    }
    fun changeKind(value:String):Wq150Settings {
        require(value in Wq150ModeCatalog.kinds)
        val nextFormat=if(value=="quiz")"quiz" else "crossword"
        return copy(kind=value,mode=Wq150ModeCatalog.modes(value).keys.first(),format=nextFormat,
            count=if(count in Wq150ModeCatalog.sizes(nextFormat))count else 20)
    }
    fun changeScale(value:String):Wq150Settings {
        require(value=="cefr"||value=="yds")
        return copy(scale=value,levels=listOf(Wq150ModeCatalog.levels(value).first()),topic=null,wordType=null)
    }
    fun changeMode(value:String):Wq150Settings {
        require(value in Wq150ModeCatalog.modes(kind))
        return copy(mode=value,seconds=if(kind=="coop"&&value=="timed"&&seconds==0)300 else seconds)
    }
    fun changeFormat(value:String):Wq150Settings {
        require(kind=="team"&&value in listOf("quiz","crossword"))
        return copy(format=value,count=if(count in Wq150ModeCatalog.sizes(value))count else 20)
    }
    fun toggleLevel(value:String):Wq150Settings {
        require(value in Wq150ModeCatalog.levels(scale))
        val selected=if(value in levels){if(levels.size==1)levels else levels-value}else levels+value
        return copy(levels=Wq150ModeCatalog.levels(scale).filter{it in selected})
    }
    fun valid():Boolean {
        val id=Regex("^[A-Za-z0-9_-]{1,100}$")
        return kind in Wq150ModeCatalog.kinds && mode in Wq150ModeCatalog.modes(kind) &&
            format in listOf("quiz","crossword") && (kind=="team"||format==if(kind=="quiz")"quiz" else "crossword") &&
            scale in listOf("cefr","yds") && levels.isNotEmpty() && levels.distinct().size==levels.size &&
            levels.all{it in Wq150ModeCatalog.levels(scale)} &&
            (topic==null||id.matches(topic)) && (wordType==null||id.matches(wordType)) &&
            (topic==null||wordType==null) && count in (if(format=="quiz")5..70 else 10..70) &&
            count>=levels.size && seconds in 0..(if(kind=="coop"&&mode=="timed")3600 else 600) &&
            !(kind=="coop"&&mode=="timed"&&seconds==0) && hints in 0..20 && retries in 0..3 &&
            skipLimit in 0..5 && limit in listOf(2,3,5) && maxMistakes in 1..20
    }
    fun json():JSONObject {
        require(valid())
        return JSONObject().put("kind",kind).put("mode",mode).put("format",format).put("scale",scale)
            .put("levels",JSONArray(levels)).put("topic",topic?:JSONObject.NULL).put("wordType",wordType?:JSONObject.NULL)
            .put("count",count).put("seconds",seconds).put("hints",hints).put("retries",retries)
            .put("skipLimit",skipLimit).put("limit",limit).put("maxMistakes",maxMistakes)
    }
}
internal data class Wq150Catalog(val data:JSONObject){
    private fun groups(key:String):List<JSONObject>{
        val a=data.optJSONArray(key)?:return emptyList()
        return (0 until a.length()).mapNotNull{a.optJSONObject(it)}
    }
    fun options(key:String):Map<String,String> = groups(key).associate{it.optString("id") to it.optString("name")}
    private fun counts(s:Wq150Settings):JSONObject?{
        val filter=if(s.topic!=null)groups("topics").firstOrNull{it.optString("id")==s.topic}
            else if(s.wordType!=null)groups("wordTypes").firstOrNull{it.optString("id")==s.wordType}
            else null
        val base=if(s.topic!=null||s.wordType!=null)filter?.optJSONObject("counts") else data.optJSONObject("all")
        return base?.optJSONObject(s.scale)
    }
    fun coverage(s:Wq150Settings):List<Int> = s.levels.map{counts(s)?.optJSONObject(it)?.optInt(s.format,0)?:0}
    fun supports(s:Wq150Settings):Boolean{
        val c=coverage(s)
        return c.isNotEmpty()&&c.all{it>0}&&c.sum()>=s.count
    }
    fun hasYds():Boolean=listOf("easy","medium","hard").any{level->
        (data.optJSONObject("all")?.optJSONObject("yds")?.optJSONObject(level)?.optInt("crossword")?:0)>0
    }
    companion object{
        fun read(context:android.content.Context):Wq150Catalog?=runCatching{
            val j=JSONObject(context.assets.open("wq150_arena_catalog.json").bufferedReader().use{it.readText()})
            require(j.optInt("schema")==150);Wq150Catalog(j)
        }.getOrNull()
    }
}
@Composable
internal fun Wq150MatchSettingsDialog(initial:Wq150Settings,topics:Map<String,String>,wordTypes:Map<String,String>,
    available:Boolean,onDismiss:()->Unit,onApply:(Wq150Settings)->Unit,catalog:Wq150Catalog?=null) {
    var s by remember(initial){mutableStateOf(initial)}
    MaterialTheme(colorScheme=Wq122Scheme) {
        Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
            Box(Modifier.fillMaxSize().background(Color(0xFF150B3E))){
                Wq122SettingsBackdrop(Modifier.matchParentSize())
                Column(Modifier.align(Alignment.TopCenter).widthIn(max=1000.dp).fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(12.dp),
                    verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        TextButton(onClick=onDismiss){Text("‹ Geri",color=Wq122Gold)}
                        Wq122Art("settings",Modifier.size(36.dp))
                        Text("Maç Ayarları",Modifier.weight(1f),color=Wq122Gold,fontWeight=FontWeight.Black,
                            style=MaterialTheme.typography.headlineSmall)
                    }
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement=Arrangement.spacedBy(8.dp)){
                Wq150Choices("Oyun türü",Wq150ModeCatalog.kinds,s.kind){s=s.changeKind(it)}
                Wq150Choices("Oyun biçimi",Wq150ModeCatalog.modes(s.kind),s.mode){s=s.changeMode(it)}
                if(s.kind=="team")Wq150Choices("2'ye 2 · içerik",mapOf("crossword" to "Bulmaca","quiz" to "5 Şıklı"),s.format){s=s.changeFormat(it)}
                if(s.mode=="limited")Wq150Choices("Seri sınırı",listOf(2,3,5).associate{it.toString() to "$it doğru"},s.limit.toString()){s=s.copy(limit=it.toInt())}
                if(s.kind=="coop"&&s.mode=="perfect")Wq150Choices("Ortak yanlış hakkı",listOf(1,2,3,5).associate{it.toString() to it.toString()},s.maxMistakes.toString()){s=s.copy(maxMistakes=it.toInt())}
                val scales=if(catalog==null||catalog.hasYds())mapOf("cefr" to "CEFR","yds" to "YDS")else mapOf("cefr" to "CEFR")
                Wq150Choices("Seviye sistemi",scales,s.scale){s=s.changeScale(it)}
                if(catalog!=null&&!catalog.hasYds())Text("YDS içerikleri henüz aktarılmadı.",color=Color(0xFFFFD6A1))
                Text("Seviyeler · birden fazla seçilebilir",color=Wq122Gold,fontWeight=FontWeight.Bold)
                Wq150ModeCatalog.levels(s.scale).chunked(3).forEach{row->
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                        row.forEach{level->Wq122Choice(when(level){"easy"->"Kolay";"medium"->"Orta";"hard"->"Zor";else->level},
                            level in s.levels,Modifier.weight(1f)){s=s.toggleLevel(level)}}
                    }
                }
                Wq150Choices("Konu",mapOf("all" to "Tümü")+topics,s.topic?:"all"){s=s.copy(topic=it.takeUnless{x->x=="all"},wordType=null)}
                if(wordTypes.isNotEmpty()||catalog==null)
                    Wq150Choices("Kelime türü",mapOf("all" to "Tümü")+wordTypes,s.wordType?:"all"){s=s.copy(wordType=it.takeUnless{x->x=="all"},topic=null)}
                else Text("Kelime türü etiketleri henüz aktarılmadı.",color=Color(0xFFFFD6A1))
                Wq150Choices(if(s.format=="quiz")"Soru sayısı" else "Kelime sayısı",
                    Wq150ModeCatalog.sizes(s.format).associate{it.toString() to it.toString()},s.count.toString()){s=s.copy(count=it.toInt())}
                val timed=s.kind=="coop"&&s.mode=="timed"
                Wq150Choices(if(timed)"Ortak maç süresi" else "Soru süresi",
                    (if(timed)listOf(180,300,600,900)else listOf(0,20,30,45,60)).associate{it.toString() to if(it==0)"Süresiz" else if(timed)"${it/60} dk" else "$it sn"},s.seconds.toString()){s=s.copy(seconds=it.toInt())}
                Wq150Choices("İpucu hakkı",listOf(0,3,5,8,12,20).associate{it.toString() to it.toString()},s.hints.toString()){s=s.copy(hints=it.toInt())}
                if(s.kind=="duel"){
                    Wq150Choices("Tekrar hakkı",(0..3).associate{it.toString() to it.toString()},s.retries.toString()){s=s.copy(retries=it.toInt())}
                    Wq150Choices("Kelimeyi geçme hakkı",(0..5).associate{it.toString() to it.toString()},s.skipLimit.toString()){s=s.copy(skipLimit=it.toInt())}
                }
                if(catalog!=null&&!catalog.supports(s))Text("Seçilen seviyeler ve filtre için yeterli içerik yok. Soru sayısını azalt veya filtreyi değiştir.",color=Color(0xFFFFD6A1))
                if(!available)Text("İşlem sürüyor veya yeniden denenmesi gerekiyor.",color=Color(0xFFFFD6A1))
                    }
                    Wq122Primary("Ayarları uygula",available&&s.valid()&&(catalog?.supports(s)!=false),{onApply(s)})
                }
            }
        }
    }
}
@Composable
private fun Wq150Choices(title:String,values:Map<String,String>,selected:String,onPick:(String)->Unit){
    Text(title,color=Wq122Gold,fontWeight=FontWeight.Bold)
    values.entries.chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
        row.forEach{(key,label)->Wq122Choice(label,key==selected,Modifier.weight(1f)){onPick(key)}}
        if(row.size==1)Spacer(Modifier.weight(1f))
    }}
}
