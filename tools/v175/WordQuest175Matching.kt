package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** A real bilingual pair from the existing on-device, attributed CEFR assets. */
internal data class Wq175MatchPair(
    val level:String, val kind:String, val english:String, val turkish:String
) {
    val id:String get()=level+":"+english.uppercase(Locale.ROOT)
}

internal object Wq175MatchingBank {
    private val tiers=setOf("A1","A2","B1","B2","C1","C2")
    /** No network, no unlicensed generated filler and no change to existing catalog. */
    fun parse(lines:Sequence<String>, level:String):List<Wq175MatchPair> {
        val seenEnglish=HashSet<String>()
        val seenTurkish=HashSet<String>()
        return lines.mapNotNull { line ->
            val cols=line.split('\t')
            if(cols.size!=5 || cols[0] !in tiers || cols[0]!=level)return@mapNotNull null
            val english=cols[3].trim()
            val raw=cols[4].trim()
            // Exclude ambiguous, grammatical or overly detailed glosses instead
            // of displaying a false one-to-one translation in the matching game.
            val turkish=raw.substringBefore(';').substringBefore(',')
                .substringAfter(':').trim()
            if(english.length !in 2..32 || turkish.length !in 2..42 ||
                !english.all { it.isLetter() || it==' ' || it=='\'' || it=='-' } ||
                turkish.any { it=='(' || it==')' || it=='/' } ||
                !seenEnglish.add(english.uppercase(Locale.ROOT)) ||
                !seenTurkish.add(turkish.lowercase(Locale.forLanguageTag("tr"))))
                return@mapNotNull null
            Wq175MatchPair(cols[0],cols[2],english,turkish)
        }.toList()
    }
    fun load(context:Context,level:String):List<Wq175MatchPair> {
        val sources=listOf("vocabulary-extra.tsv","vocabulary-addendum-v175.tsv")
        val lines=sources.asSequence().flatMap { name ->
            runCatching {
                context.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readLines() }
            }.getOrElse { emptyList() }.asSequence()
        }
        return parse(lines,level)
    }
    fun prepare(items:List<Wq175MatchPair>,size:Int):List<Wq175MatchPair> =
        items.distinctBy { it.id }.distinctBy { it.turkish.lowercase(Locale.forLanguageTag("tr")).trim() }.shuffled().take(size)
    /** Personal decks can be smaller than a preset; do not lock them out. */
    fun requiredPairs(mode:String,requested:Int):Int =
        if(mode=="mistakes" || mode=="notebook") 2 else requested
}

/** Independent, profile-scoped progress; never writes into existing puzzles or FSRS records. */
internal class Wq175MatchingProgress(context:Context,profile:String) {
    private val prefs=context.getSharedPreferences(
        "wq175_matching_"+profile.take(100),Context.MODE_PRIVATE)
    val played:Int get()=prefs.getInt("played",0)
    val wins:Int get()=prefs.getInt("wins",0)
    val best:Int get()=prefs.getInt("best",0)
    fun finish(score:Int,complete:Boolean) {
        prefs.edit()
            .putInt("played",played+1)
            .putInt("wins",wins+if(complete)1 else 0)
            .putInt("best",maxOf(best,score))
            .apply()
    }
    fun missed():List<Wq175MatchPair> {
        val array=runCatching{JSONArray(prefs.getString("missed","[]"))}.getOrNull()
            ?:return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            runCatching {
                val obj=array.getJSONObject(index)
                Wq175MatchPair(obj.getString("level"),obj.getString("kind"),
                    obj.getString("english"),obj.getString("turkish"))
            }.getOrNull()
        }.distinctBy { it.id }
    }
    fun mistake(pair:Wq175MatchPair) {
        val entries=(listOf(pair)+missed()).distinctBy { it.id }.take(160)
        val arr=JSONArray()
        entries.forEach { item ->
            arr.put(JSONObject().put("level",item.level).put("kind",item.kind)
                .put("english",item.english).put("turkish",item.turkish))
        }
        prefs.edit().putString("missed",arr.toString()).apply()
    }
    fun learned(pair:Wq175MatchPair) {
        val arr=JSONArray()
        missed().filterNot { it.id==pair.id }.forEach { item ->
            arr.put(JSONObject().put("level",item.level).put("kind",item.kind)
                .put("english",item.english).put("turkish",item.turkish))
        }
        prefs.edit().putString("missed",arr.toString()).apply()
    }
}

private val MatchInk=Color(0xFF173F50)
private val MatchTeal=Color(0xFF1F9C94)
private val MatchPink=Color(0xFFDF739D)

@Composable
internal fun Wq175MatchingEntry(onClick:()->Unit) {
    Surface(onClick=onClick,shape=RoundedCornerShape(22.dp),
        color=Color(0xFFF0FFFA),
        border=BorderStroke(1.dp,Color(0xFFA5E7D8)),
        modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(13.dp)) {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(17.dp))
                .background(Brush.linearGradient(listOf(
                    Color(0xFFB9E9F5),Color(0xFFFFCDE2)))),
                contentAlignment=Alignment.Center) {
                Text("A ⇄ B",fontSize=16.sp,fontWeight=FontWeight.Black,
                    color=MatchInk)
            }
            Column(Modifier.weight(1f)) {
                Text("Eşleştirmelerim",fontSize=18.sp,
                    fontWeight=FontWeight.ExtraBold,color=MatchInk)
                Text("Türkçe ↔ İngilizce · Kart oyunları · İlerleme",
                    fontSize=11.sp,color=Color(0xFF547781))
            }
            Text("›",fontSize=28.sp,color=MatchTeal)
        }
    }
}

@Composable
internal fun Wq175MatchingDialog(session:AppSession,onDismiss:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val store=remember(context,session.profileKey){
        Wq175MatchingProgress(context,session.profileKey)
    }
    var mode by rememberSaveable { mutableStateOf("classic") }
    var level by rememberSaveable { mutableStateOf("A1") }
    var number by rememberSaveable { mutableIntStateOf(6) }
    var englishFirst by rememberSaveable { mutableStateOf(false) }
    var kind by rememberSaveable { mutableStateOf("Tümü") }
    var busy by remember { mutableStateOf(false) }
    var issue by remember { mutableStateOf("") }
    var deck by remember { mutableStateOf<List<Wq175MatchPair>>(emptyList()) }
    var rightDeck by remember { mutableStateOf<List<IndexedValue<Wq175MatchPair>>>(emptyList()) }
    var leftChoice by remember { mutableIntStateOf(-1) }
    var matched by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var wrong by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    var remaining by remember { mutableIntStateOf(90) }
    var finished by remember { mutableStateOf(false) }
    var played by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }

    fun finishGame(completed:Boolean) {
        if(finished)return
        finished=true
        val score=matched.size*10-(wrong*2)
        store.finish(score.coerceAtLeast(0),completed)
        revision++
    }

    LaunchedEffect(played,finished,mode) {
        if(played && !finished && mode=="timed") {
            while(remaining>0 && !finished) {
                delay(1000)
                if(remaining>0)remaining--
            }
            if(!finished && remaining<=0)finishGame(false)
        }
    }

    fun beginGame() {
        if(busy)return
        busy=true
        issue=""
        scope.launch {
            val items=try {
                when(mode) {
                    "mistakes" -> store.missed()
                    "notebook" -> session.searchLearning("",0,5000).map {
                        Wq175MatchPair("Defterim","Kişisel",it.answer,it.clue)
                    }
                    else -> withContext(Dispatchers.IO) {
                        Wq175MatchingBank.load(context,level)
                    }
                }
            }catch(_:Exception){emptyList()}
            val filtered=items.filter { mode=="mistakes" || mode=="notebook" || kind=="Tümü" || it.kind==kind }
            val available=Wq175MatchingBank.prepare(filtered,number)
            busy=false
            if(available.size<Wq175MatchingBank.requiredPairs(mode,number)) {
                issue=if(mode=="mistakes" || mode=="notebook")
                    "Bu koleksiyonda en az 2 farklı eşleştirme çifti gerekiyor. " +
                    "Koleksiyonuna kelime ekle veya yeni yanlışlarını çalış."
                else "Bu seçenekte "+filtered.size+
                    " uygun çift var. Farklı seviye, tür veya daha az kart seç."
                return@launch
            }
            deck=available
            rightDeck=available.withIndex().shuffled()
            leftChoice=-1
            matched=emptySet()
            wrong=0
            feedback=""
            remaining=90
            finished=false
            played=true
        }
    }

    Dialog(onDismissRequest=onDismiss,
        properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(modifier=Modifier.fillMaxSize(),color=Color(0xFFF7FBFD)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal=17.dp,vertical=12.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    TextButton(onClick=if(played){ { played=false;finished=false } } else onDismiss) {
                        Text("‹  Geri",color=MatchInk,fontWeight=FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("EŞLEŞTİRMELERİM",fontSize=11.sp,
                        color=MatchTeal,fontWeight=FontWeight.Black)
                }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(27.dp))
                    .background(Brush.horizontalGradient(listOf(
                        Color(0xFF146E79),Color(0xFF21A8A2),Color(0xFFEC91B8))))
                    .padding(19.dp)) {
                    Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text("KELİME KARTLARI",fontSize=11.sp,
                            color=Color(0xFFCCFFF6),fontWeight=FontWeight.Black)
                        Text("Bul, eşleştir, öğren!",
                            fontSize=27.sp,lineHeight=30.sp,
                            color=Color.White,fontWeight=FontWeight.ExtraBold)
                        Text("Türkçe ve İngilizce kartları birleştirerek kelime hafızanı güçlendir.",
                            fontSize=12.sp,color=Color.White)
                        Text("Oynanan: ${store.played} · Kazanılan: ${store.wins} · En iyi: ${store.best}",
                            fontSize=11.sp,color=Color(0xFFFFF4FB))
                    }
                }
                if(!played) {
                    Text("Oyununu seç",fontWeight=FontWeight.ExtraBold,
                        color=MatchInk,fontSize=19.sp)
                    val modes=listOf(
                        "classic" to "Klasik Eşleştirme",
                        "timed" to "Hızlı Eşleştirme · 90 sn",
                        "mistakes" to "Yanlışlarımdan Eşleştirme",
                        "notebook" to "Kendi Defterimden"
                    )
                    modes.forEach { (value,label) ->
                        Surface(onClick={mode=value;issue=""},
                            shape=RoundedCornerShape(15.dp),
                            color=if(mode==value)Color(0xFFDBFAF1) else Color.White,
                            border=BorderStroke(if(mode==value)2.dp else 1.dp,
                                if(mode==value)MatchTeal else Color(0xFFDCE5E8))) {
                            Text(label,Modifier.fillMaxWidth().padding(13.dp),
                                color=MatchInk,fontWeight=FontWeight.SemiBold)
                        }
                    }
                    if(mode=="classic" || mode=="timed") {
                        Text("Seviye",color=MatchInk,fontWeight=FontWeight.Bold)
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement=Arrangement.spacedBy(7.dp)){
                            listOf("A1","A2","B1","B2","C1","C2").forEach { candidate ->
                                FilterChip(selected=level==candidate,onClick={level=candidate},
                                    label={Text(candidate)})
                            }
                        }
                        Text("Kelime türü",color=MatchInk,fontWeight=FontWeight.Bold)
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement=Arrangement.spacedBy(7.dp)){
                            listOf("Tümü","İsimler","Fiiller","Sıfatlar","Zarflar").forEach { candidate ->
                                FilterChip(selected=kind==candidate,onClick={kind=candidate},
                                    label={Text(candidate,fontSize=11.sp)})
                            }
                        }
                    }
                    Text("Kart sayısı",color=MatchInk,fontWeight=FontWeight.Bold)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(6,8,10,12).forEach { amount ->
                            FilterChip(selected=number==amount,onClick={number=amount},
                                label={Text(amount.toString())})
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text("İngilizceden Türkçeye",Modifier.weight(1f),color=MatchInk)
                        Switch(checked=englishFirst,onCheckedChange={englishFirst=it})
                    }
                    if(issue.isNotBlank()) Text(issue,color=Color(0xFFB84056),fontSize=12.sp)
                    Button(onClick={beginGame()},enabled=!busy,
                        modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=MatchTeal)) {
                        Text(if(busy)"Kartlar hazırlanıyor…" else "Eşleştirmeye Başla",
                            fontWeight=FontWeight.ExtraBold)
                    }
                } else if(finished) {
                    Text(if(matched.size==deck.size)"Tebrikler, hepsi eşleşti!"
                        else "Süre tamamlandı!",
                        fontSize=23.sp,color=MatchInk,fontWeight=FontWeight.Black)
                    Text("Doğru: ${matched.size}/${deck.size} · Yanlış: $wrong · Puan: "+
                        (matched.size*10-wrong*2).coerceAtLeast(0),
                        color=MatchInk)
                    Text("Yanlış yaptığın kelimeler tekrar modunda saklandı.",
                        color=Color(0xFF65787D),fontSize=12.sp)
                    Button(onClick={played=false;finished=false},
                        modifier=Modifier.fillMaxWidth(),
                        colors=ButtonDefaults.buttonColors(containerColor=MatchTeal)){
                        Text("Yeni eşleştirme")
                    }
                } else {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("Eşleşen: ${matched.size}/${deck.size}",
                            color=MatchInk,fontWeight=FontWeight.Bold)
                        Text(if(mode=="timed")"Kalan: $remaining sn" else "Yanlış: $wrong",
                            color=MatchPink,fontWeight=FontWeight.ExtraBold)
                    }
                    LinearProgressIndicator(progress={matched.size.toFloat()/deck.size.coerceAtLeast(1)},
                        modifier=Modifier.fillMaxWidth().height(7.dp),
                        color=MatchTeal,trackColor=Color(0xFFE0EAE9))
                    Text(if(englishFirst)"Önce İngilizce, sonra Türkçe kartını seç."
                        else "Önce Türkçe, sonra İngilizce kartını seç.",
                        color=MatchInk,fontSize=12.sp)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
                            deck.forEachIndexed { index,pair ->
                                val chosen=leftChoice==index
                                val active=index !in matched
                                Surface(onClick={ if(active)leftChoice=index },
                                    enabled=active,shape=RoundedCornerShape(16.dp),
                                    color=when {
                                        !active -> Color(0xFFD2F4E3)
                                        chosen -> Color(0xFFFFE0ED)
                                        else -> Color.White
                                    },
                                    border=BorderStroke(if(chosen)2.dp else 1.dp,
                                        if(chosen)MatchPink else Color(0xFFDCE5EA)),
                                    modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){
                                    Box(Modifier.padding(9.dp),contentAlignment=Alignment.Center) {
                                        Text(if(!active)"✓" else if(englishFirst)pair.english else pair.turkish,
                                            fontSize=13.sp,color=MatchInk,fontWeight=FontWeight.Bold,
                                            textAlign=TextAlign.Center)
                                    }
                                }
                            }
                        }
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
                            rightDeck.forEach { indexed ->
                                val index=indexed.index
                                val pair=indexed.value
                                val active=index !in matched
                                Surface(onClick={
                                    if(active && leftChoice>=0) {
                                        val left=leftChoice
                                        if(left==index) {
                                            val next=matched+index
                                            matched=next
                                            if(mode=="mistakes")store.learned(pair)
                                            feedback="Doğru! ${pair.english} = ${pair.turkish}"
                                            if(next.size==deck.size)finishGame(true)
                                        }else {
                                            wrong++
                                            store.mistake(deck[left])
                                            feedback="Tekrar dene. Bu çift eşleşmiyor."
                                        }
                                        leftChoice=-1
                                    } else if(leftChoice<0)feedback="Önce soldan bir kart seç."
                                },enabled=active,
                                    shape=RoundedCornerShape(16.dp),
                                    color=if(active)Color.White else Color(0xFFD2F4E3),
                                    border=BorderStroke(1.dp,Color(0xFFDCE5EA)),
                                    modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){
                                    Box(Modifier.padding(9.dp),contentAlignment=Alignment.Center) {
                                        Text(if(!active)"✓" else if(englishFirst)pair.turkish else pair.english,
                                            color=MatchInk,fontSize=13.sp,
                                            fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                    if(feedback.isNotBlank())Text(feedback,fontSize=12.sp,
                        color=if(feedback.startsWith("Doğru"))MatchTeal else MatchPink)
                    OutlinedButton(onClick={finishGame(false)},
                        modifier=Modifier.fillMaxWidth()){
                        Text("Oyunu bitir",color=MatchInk)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
