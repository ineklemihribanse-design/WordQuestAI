package com.kelimebulmacasi.a1

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.WindowCompat
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.shadow
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
            count=if(count in (if(nextFormat=="quiz")5..70 else 10..70))count else 20)
    }
    fun changeScale(value:String):Wq150Settings {
        require(value=="cefr"||value=="yds")
        if(scale==value)return this
        return copy(scale=value,levels=listOf(Wq150ModeCatalog.levels(value).first()),topic=null,wordType=null)
    }
    fun changeMode(value:String):Wq150Settings {
        require(value in Wq150ModeCatalog.modes(kind))
        val wasTimed=kind=="coop"&&mode=="timed"
        val willBeTimed=kind=="coop"&&value=="timed"
        return copy(mode=value,seconds=when{
            willBeTimed&&!wasTimed -> 300
            wasTimed&&!willBeTimed -> 30
            else -> seconds
        })
    }
    fun changeFormat(value:String):Wq150Settings {
        require(kind=="team"&&value in listOf("quiz","crossword"))
        return copy(format=value,count=if(count in (if(value=="quiz")5..70 else 10..70))count else 20)
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
/** Scope system bar appearance to Arena and restore it when leaving.
 * MainActivity already enables edge-to-edge. Fullscreen dialogs opt in separately. */
@Suppress("DEPRECATION")
@Composable
internal fun Wq150ArenaBars(settingsPage:Boolean=false){
    val view=LocalView.current
    val context=view.context
    val window=remember(view){
        (view.parent as? DialogWindowProvider)?.window ?: run {
            var c:android.content.Context=context
            while(c is android.content.ContextWrapper && c !is android.app.Activity)c=c.baseContext
            (c as? android.app.Activity)?.window
        }
    }
    DisposableEffect(window,settingsPage){
        val ctrl=window?.let{WindowCompat.getInsetsController(it,it.decorView)}
        val status=window?.statusBarColor
        val nav=window?.navigationBarColor
        val lightStatus=ctrl?.isAppearanceLightStatusBars
        val lightNav=ctrl?.isAppearanceLightNavigationBars
        val contrast=if(android.os.Build.VERSION.SDK_INT>=29)window?.isNavigationBarContrastEnforced else null
        // Full-screen Compose dialogs must not animate a transparent window
        // over the previous Arena screen while their first frame is rendered.
        if(view.parent is DialogWindowProvider){
            window?.setWindowAnimations(0)
            window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            window?.let{WindowCompat.setDecorFitsSystemWindows(it,true)}
            window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0xFF150B3E.toInt()))
        }
        // Settings now replaces the Arena route; its own artwork can safely
        // extend behind system bars without revealing the previous screen.
        window?.statusBarColor=if(view.parent is DialogWindowProvider)0xFF1B1535.toInt() else android.graphics.Color.TRANSPARENT
        window?.navigationBarColor=android.graphics.Color.TRANSPARENT
        if(android.os.Build.VERSION.SDK_INT>=29)window?.isNavigationBarContrastEnforced=false
        ctrl?.isAppearanceLightStatusBars=false
        ctrl?.isAppearanceLightNavigationBars=false
        onDispose{
            if(status!=null)window?.statusBarColor=status
            if(nav!=null)window?.navigationBarColor=nav
            if(lightStatus!=null)ctrl?.isAppearanceLightStatusBars=lightStatus
            if(lightNav!=null)ctrl?.isAppearanceLightNavigationBars=lightNav
            if(android.os.Build.VERSION.SDK_INT>=29 && contrast!=null)window?.isNavigationBarContrastEnforced=contrast
        }
    }
}
// Match-settings-only surfaces. Keep the stadium artwork unchanged.
@Composable
internal fun Wq150SettingPanel(tone:String="purple",content:@Composable ColumnScope.()->Unit){
    val shape=RoundedCornerShape(17.dp)
    Column(Modifier.fillMaxWidth().clip(shape)
        .background(Brush.verticalGradient(listOf(Color(0xCF29204D),Color(0xDD18152F))))
        .border(1.dp,Color(0x72D8BD82),shape).padding(horizontal=10.dp,vertical=8.dp),
        verticalArrangement=Arrangement.spacedBy(5.dp),content=content)
}
@Composable
internal fun Wq159SettingsChoice(label:String,selected:Boolean,modifier:Modifier=Modifier,onClick:()->Unit){
    val shape=RoundedCornerShape(11.dp)
    Box(modifier.heightIn(min=44.dp).clip(shape)
        .background(Brush.verticalGradient(if(selected)listOf(Color(0xFF7240B0),Color(0xFF452475))
            else listOf(Color(0xB8342A56),Color(0xBC231C3D))))
        .border(if(selected)1.5.dp else 1.dp,if(selected)Color(0xFFFFD98B) else Color(0x627F739D),shape)
        .clickable(onClick=onClick).padding(horizontal=4.dp,vertical=5.dp),
        contentAlignment=Alignment.Center){
        Text(label+(if(selected)" ✓" else ""),color=Color(0xFFFFF4E3),fontWeight=FontWeight.SemiBold,
            fontSize=13.sp,textAlign=TextAlign.Center,maxLines=2)
    }
}
@Composable
internal fun Wq150LevelRow(values:List<String>,selected:List<String>,onPick:(String)->Unit){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){
        values.forEach{level->
            val active=level in selected
            val shape=RoundedCornerShape(10.dp)
            Box(Modifier.weight(1f).heightIn(min=44.dp).clip(shape)
                .background(Brush.verticalGradient(if(active)listOf(Color(0xFF7240B0),Color(0xFF452475))
                    else listOf(Color(0xB8342A56),Color(0xBC231C3D))))
                .border(if(active)1.5.dp else 1.dp,if(active)Color(0xFFFFD98B) else Color(0x627F739D),shape)
                .clickable{onPick(level)},contentAlignment=Alignment.Center){
                Text(when(level){"easy"->"Kolay";"medium"->"Orta";"hard"->"Zor";else->level},
                    color=Color(0xFFFFF4E3),fontWeight=FontWeight.Bold,fontSize=13.sp,maxLines=1)
                if(active)Text("✓",Modifier.align(Alignment.TopEnd).padding(end=2.dp),color=Wq122Gold,fontSize=9.sp)
            }
        }
    }
}
@Composable
internal fun Wq150CompactPicker(values:Map<String,String>,selected:String,customRange:IntRange?=null,customTitle:String="Özel değer",customLabel:(Int)->String={it.toString()},onPick:(String)->Unit){
    var open by remember{mutableStateOf(false)}
    var custom by remember{mutableStateOf(false)}
    var input by remember{mutableStateOf("")}
    BoxWithConstraints(Modifier.fillMaxWidth()){
        val pickerWidth=maxWidth
        OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth().heightIn(min=44.dp),
            border=androidx.compose.foundation.BorderStroke(1.dp,Color(0x728F80AC)),
            colors=ButtonDefaults.outlinedButtonColors(containerColor=Color(0x95231C3D)),
            contentPadding=PaddingValues(horizontal=10.dp,vertical=4.dp)){
            Text(values[selected]?:selected.toIntOrNull()?.let{customLabel(it)}?:selected,Modifier.weight(1f),color=Wq122Ink,fontWeight=FontWeight.Bold,fontSize=13.sp)
            Text("⌄",color=Wq122Gold)
        }
        DropdownMenu(expanded=open,onDismissRequest={open=false},modifier=Modifier.width(pickerWidth).heightIn(max=320.dp).background(Color(0xFF251347))){
            values.forEach{(id,label)->DropdownMenuItem(text={Text(label+if(id==selected)" ✓" else "",color=Color.White,fontSize=15.sp,fontWeight=if(id==selected)FontWeight.Bold else FontWeight.Normal)},
                onClick={onPick(id);open=false})}
            if(customRange!=null)DropdownMenuItem(text={Text("Özel…",color=Wq122Gold,fontWeight=FontWeight.Bold)},onClick={open=false;input=selected;custom=true})
        }
    }
    if(custom&&customRange!=null){
        val number=input.toIntOrNull()
        val valid=number!=null&&number in customRange
        AlertDialog(onDismissRequest={custom=false},containerColor=Color(0xFF251347),
            title={Text(customTitle,color=Wq122Gold,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold)},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("İzin verilen aralık: ${customRange.first}–${customRange.last}",color=Color(0xFFE7D9FF))
                OutlinedTextField(input,{input=it.filter(Char::isDigit).take(4)},singleLine=true,modifier=Modifier.fillMaxWidth(),
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),label={Text("Özel değer")},
                    isError=input.isNotEmpty()&&!valid)
            }},
            confirmButton={TextButton(enabled=valid,onClick={number?.let{onPick(it.toString())};custom=false}){Text("Uygula",color=Wq122Gold)}},
            dismissButton={TextButton(onClick={custom=false}){Text("Vazgeç",color=Color.White)}})
    }
}
@Composable
internal fun Wq150MatchSettingsDialog(initial:Wq150Settings,topics:Map<String,String>,wordTypes:Map<String,String>,
    available:Boolean,onDismiss:()->Unit,onApply:(Wq150Settings)->Unit,catalog:Wq150Catalog?=null) {
    var s by remember(initial){mutableStateOf(initial)}
    val timed=s.kind=="coop"&&s.mode=="timed"
    val levels=Wq150ModeCatalog.levels(s.scale)
    val scales=if(catalog==null||catalog.hasYds())mapOf("cefr" to "CEFR","yds" to "YDS")else mapOf("cefr" to "CEFR")
    val valid=available&&s.valid()&&(catalog?.supports(s)!=false)
    val timeLabel:(Int)->String={if(it==0)"Süresiz" else if(timed&&it%60==0)"${it/60} dk" else "$it sn"}
    Wq163SettingsPage(onBack=onDismiss,header={
                    Row(Modifier.fillMaxWidth().heightIn(min=44.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xB8241B41)),
                        verticalAlignment=Alignment.CenterVertically){
                        TextButton(onClick=onDismiss,contentPadding=PaddingValues(horizontal=6.dp)){Text("‹ Geri",color=Wq122Gold)}
                        Text("Maç Ayarları",Modifier.weight(1f),color=Wq122Gold,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Black,fontSize=19.sp)
                    }
                    Wq150SettingPanel{
                        Text("Maç Özeti",color=Wq122Gold,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,fontSize=12.sp)
                        Text(Wq150ModeCatalog.kinds[s.kind].orEmpty()+" · "+s.levels.joinToString(" + ")+" · "+s.count+" "+(if(s.format=="quiz")"soru" else "kelime")+" · "+timeLabel(s.seconds)+" · ${s.hints} ipucu",color=Color(0xFFFFF4E3),fontSize=13.sp)
                        if(!valid)Text(if(!available)"İçerik veya bağlantı hazırlanıyor…" else "Seçimleri kontrol et.",color=Color(0xFFFFD6A1),fontSize=12.sp)
                    }

    },footer={Wq159SettingsFooter("Ayarları Uygula",valid){onApply(s)}}){
                    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(9.dp)){
                    BoxWithConstraints(Modifier.fillMaxWidth()){
                        val wide=maxWidth>=650.dp
                        val basic:@Composable ()->Unit={
                            Wq150SettingPanel{
                                Wq122Section("mode","Oyun biçimi")
                                Wq150ModeCatalog.modes(s.kind).entries.toList().chunked(3).forEach{row->
                                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                        row.forEach{(id,label)->Wq159SettingsChoice(label,id==s.mode,Modifier.weight(1f)){s=s.changeMode(id)}}
                                    }
                                }
                                if(s.kind=="team")Wq150SettingValue("2'ye 2 · içerik",mapOf("crossword" to "Bulmaca","quiz" to "5 Şıklı"),s.format){s=s.changeFormat(it)}
                            }
                            Wq150SettingPanel("blue"){
                                Wq122Section("level","Seviye sistemi")
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                    scales.forEach{(id,label)->Wq159SettingsChoice(label,id==s.scale,Modifier.weight(1f)){s=s.changeScale(id)}}
                                }
                                if(catalog!=null&&!catalog.hasYds())Text("YDS içerikleri henüz aktarılmadı.",color=Color(0xFFFFD6A1),fontSize=12.sp)
                                Wq122Section("level","Seviyeler","Birden fazla seviye seçebilirsin.")
                                Wq150LevelRow(levels,s.levels){s=s.toggleLevel(it)}
                            }
                            Wq150SettingPanel("green"){
                                Wq150Filter("Konu",mapOf("all" to "Tüm konular")+topics,s.topic?:"all"){s=if(it=="all")s.copy(topic=null) else s.copy(topic=it,wordType=null)}
                                if(wordTypes.isNotEmpty()||catalog==null)
                                    Wq150Filter("Kelime türü",mapOf("all" to "Tüm kelime türleri")+wordTypes,s.wordType?:"all"){s=if(it=="all")s.copy(wordType=null) else s.copy(wordType=it,topic=null)}
                                else Text("Kelime türü etiketleri henüz aktarılmadı.",color=Color(0xFFFFD6A1),fontSize=12.sp)
                            }
                        }
                        val rules:@Composable ()->Unit={
                            Wq150SettingPanel("amber"){
                                Wq122Section("count","Maç kuralları")
                                Wq150SettingValue(if(s.format=="quiz")"Soru sayısı" else "Kelime sayısı",Wq150ModeCatalog.sizes(s.format).associate{it.toString() to it.toString()},s.count.toString(),maxOf(if(s.format=="quiz")5 else 10,s.levels.size)..70){s=s.copy(count=it.toInt())}
                                Wq150SettingValue(if(timed)"Ortak maç süresi" else "Soru süresi",
                                    (if(timed)listOf(180,300,600,900)else listOf(0,20,30,45,60)).associate{it.toString() to timeLabel(it)},s.seconds.toString(),if(timed)1..3600 else 0..600,timeLabel){s=s.copy(seconds=it.toInt())}
                                Wq150SettingValue("İpucu hakkı",listOf(0,3,5,8,12,20).associate{it.toString() to it.toString()},s.hints.toString(),0..20){s=s.copy(hints=it.toInt())}
                                if(s.kind=="duel"){
                                    Wq150SettingValue("Tekrar hakkı",(0..3).associate{it.toString() to it.toString()},s.retries.toString(),0..3){s=s.copy(retries=it.toInt())}
                                    Wq150SettingValue("Kelimeyi geçme hakkı",(0..5).associate{it.toString() to it.toString()},s.skipLimit.toString(),0..5){s=s.copy(skipLimit=it.toInt())}
                                }
                                if(s.mode=="limited")Wq150SettingValue("Seri sınırı",listOf(2,3,5).associate{it.toString() to "$it doğru"},s.limit.toString()){s=s.copy(limit=it.toInt())}
                                if(s.kind=="coop"&&s.mode=="perfect")Wq150SettingValue("Ortak yanlış hakkı",listOf(1,2,3,5).associate{it.toString() to it.toString()},s.maxMistakes.toString(),1..20){s=s.copy(maxMistakes=it.toInt())}
                            }
                        }
                        Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(6.dp)){
                            if(wide)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                Column(Modifier.weight(1.08f),verticalArrangement=Arrangement.spacedBy(9.dp)){basic()}
                                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(9.dp)){rules()}
                            }else{basic();rules()}
                            if(catalog!=null&&!catalog.supports(s))Text("Seçilen seviyeler ve filtre için yeterli içerik yok. Soru sayısını azalt veya filtreyi değiştir.",color=Color(0xFFFFD6A1),modifier=Modifier.background(Color(0xE2251347)).padding(6.dp))
                            if(!available)Text("İşlem sürüyor veya yeniden denenmesi gerekiyor.",color=Color(0xFFFFD6A1))
                        }
                    }
                    }

    }
}

/** Settings is an Activity content route, never a second fullscreen window.
 * Scaffold measures header/footer before giving remaining space to the scroll.
 * The backdrop is owned by this route and opaque from its first frame. */
@Composable
internal fun Wq163SettingsPage(onBack:()->Unit,header:@Composable ColumnScope.()->Unit,
    footer:@Composable ()->Unit,content:@Composable ColumnScope.()->Unit){
    BackHandler(onBack=onBack)
    Wq150ArenaBars(settingsPage=true)
    val safe=Wq160SettingsInsets()
    MaterialTheme(colorScheme=Wq122Scheme){
        Box(Modifier.fillMaxSize().background(Color(0xFF150B3E))){
            Wq150SettingsBackdrop(Modifier.matchParentSize())
            Scaffold(modifier=Modifier.fillMaxSize(),containerColor=Color.Transparent,
                contentWindowInsets=WindowInsets(0,0,0,0),
                topBar={
                    Box(Modifier.fillMaxWidth()
                        .windowInsetsPadding(safe.only(WindowInsetsSides.Top+WindowInsetsSides.Horizontal)),
                        contentAlignment=Alignment.Center){
                        Column(Modifier.widthIn(max=1200.dp).fillMaxWidth().padding(horizontal=10.dp,vertical=6.dp),
                            verticalArrangement=Arrangement.spacedBy(6.dp),content=header)
                    }
                },
                bottomBar={
                    Box(Modifier.fillMaxWidth()
                        .windowInsetsPadding(safe.only(WindowInsetsSides.Bottom+WindowInsetsSides.Horizontal))
                        .imePadding().padding(horizontal=10.dp,vertical=8.dp),contentAlignment=Alignment.Center){
                        footer()
                    }
                }
            ){padding->
                Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                    .windowInsetsPadding(safe.only(WindowInsetsSides.Horizontal)),contentAlignment=Alignment.TopCenter){
                    Column(Modifier.widthIn(max=1200.dp).fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal=10.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(9.dp),content=content)
                }
            }
        }
    }
}
/** Dialog windows may report a zero Compose navigation inset on some Samsung builds.
 * Keep the platform navigation area reserved even when the dialog reports zero. */
@Composable
internal fun Wq160SettingsInsets():WindowInsets{
    val view=LocalView.current
    val context=LocalContext.current
    val density=LocalDensity.current
    val resources=context.resources
    val id=resources.getIdentifier("navigation_bar_height","dimen","android")
    val fallback=if(id!=0)resources.getDimensionPixelSize(id) else with(density){48.dp.roundToPx()}
    val root=ViewCompat.getRootWindowInsets(view)
    val navigation=root?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
    val bottom=maxOf(navigation?.bottom?:0,fallback,with(density){24.dp.roundToPx()})
    val status=root?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())
    val statusId=resources.getIdentifier("status_bar_height","dimen","android")
    val statusFallback=if(statusId!=0)resources.getDimensionPixelSize(statusId) else with(density){24.dp.roundToPx()}
    val top=maxOf(status?.top?:0,statusFallback)
    return WindowInsets.safeDrawing.union(WindowInsets(left=navigation?.left?:0,top=top,right=navigation?.right?:0,bottom=bottom))
}
@Composable
internal fun Wq159SettingsFooter(label:String,enabled:Boolean,onClick:()->Unit){
    // Same transparent footer carrier as New Game, independent of the scroll.
    Surface(Modifier.fillMaxWidth(),color=Color.Transparent,shadowElevation=0.dp){
        Box(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=4.dp),contentAlignment=Alignment.Center){
            val shape=RoundedCornerShape(17.dp)
            val fill=if(enabled)listOf(Color(0xFFFFEDB9),Color(0xFFF1C16F),Color(0xFFB77B32)) else listOf(Color(0xE03B3451),Color(0xD52C2544))
            Button(onClick=onClick,enabled=enabled,modifier=Modifier.widthIn(max=440.dp).fillMaxWidth()
                .height(56.dp).shadow(if(enabled)5.dp else 0.dp,shape).clip(shape).background(Brush.verticalGradient(fill))
                .border(1.dp,Color(0xBFFFE0A6),shape),
                shape=shape,colors=ButtonDefaults.buttonColors(
                    containerColor=Color.Transparent,contentColor=Color(0xFF241536),
                    disabledContainerColor=Color.Transparent,disabledContentColor=Color(0xFFD1C6DC)),
                contentPadding=PaddingValues(horizontal=14.dp,vertical=10.dp)){
                Text(label,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Black,fontSize=17.sp,textAlign=TextAlign.Center,maxLines=1,softWrap=false)
            }
        }
    }
}
@Composable
internal fun Wq150StartButton(label:String,enabled:Boolean,onClick:()->Unit){
    Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
        shape=RoundedCornerShape(13.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF087D59),contentColor=Color.White,disabledContainerColor=Color(0xFF354D51),disabledContentColor=Color(0xFFCED8DC)),
        contentPadding=PaddingValues(horizontal=12.dp,vertical=8.dp)){
        Text(label,fontWeight=FontWeight.Black,fontSize=17.sp)
    }
}
@Composable
private fun Wq150SettingValue(title:String,values:Map<String,String>,selected:String,customRange:IntRange?=null,customLabel:(Int)->String={it.toString()},onPick:(String)->Unit){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
        Text(title,Modifier.weight(1f),color=Wq122Ink,fontSize=13.sp,fontWeight=FontWeight.Bold)
        Box(Modifier.weight(1f)){Wq150CompactPicker(values,selected,customRange,title,customLabel,onPick)}
    }
}

@Composable
internal fun Wq150Filter(title:String,values:Map<String,String>,selected:String,onPick:(String)->Unit){
    var open by remember{mutableStateOf(false)}
    var query by remember{mutableStateOf("")}
    Wq122Section("topic",title)
    Surface(onClick={query="";open=true},color=Color(0xA5231C3D),shape=RoundedCornerShape(12.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0x728F80AC)),modifier=Modifier.fillMaxWidth()){
        Row(Modifier.heightIn(min=44.dp).padding(horizontal=10.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){
            Text(values[selected].orEmpty(),Modifier.weight(1f),color=Color.White,fontSize=15.sp)
            Text("⌄",color=Wq122Gold)
        }
    }
    if(open)AlertDialog(onDismissRequest={open=false;query=""},containerColor=Color(0xFF251347),title={Text(title,color=Wq122Gold)},text={
        Column(Modifier.heightIn(max=420.dp)){
            OutlinedTextField(query,{query=it},label={Text("Ara",color=Color.White)},singleLine=true,modifier=Modifier.fillMaxWidth(),colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,unfocusedTextColor=Color.White,focusedBorderColor=Wq122Gold,unfocusedBorderColor=Color(0xFF9675C8),cursorColor=Wq122Gold))
            Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState())){
                val filtered=values.filterValues{it.contains(query,ignoreCase=true)}
                if(filtered.isEmpty())Text("Sonuç bulunamadı.",color=Color.White,modifier=Modifier.padding(12.dp))
                filtered.forEach{(id,name)->
                    TextButton(onClick={onPick(id);open=false;query=""},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
                        Text(name+if(id==selected)" ✓" else "",color=Color.White,fontSize=15.sp,fontWeight=if(id==selected)FontWeight.Bold else FontWeight.Normal,modifier=Modifier.fillMaxWidth())
                    }
                }
            }
        }
    },confirmButton={TextButton(onClick={open=false;query=""}){Text("Kapat",color=Wq122Gold)}})
}
