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
            val englishKey=english.uppercase(Locale.ROOT)
            val turkishKey=turkish.lowercase(Locale.forLanguageTag("tr"))
            // Reject duplicate pairs without reserving keys until accepted.
            if(english.length !in 2..32 || turkish.length !in 2..42 ||
                !english.all { it.isLetter() || it==' ' || it=='\'' || it=='-' } ||
                turkish.any { it=='(' || it==')' || it=='/' } ||
                englishKey in seenEnglish ||
                turkishKey in seenTurkish)
                return@mapNotNull null
            seenEnglish.add(englishKey)
            seenTurkish.add(turkishKey)
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

/** These are different completion events; never tell a player that time
 * expired when they chose to end a round early. */
internal enum class Wq175MatchEnd { Completed, Timeout, Stopped }

internal fun wq175MatchResultTitle(reason:Wq175MatchEnd):String = when(reason) {
    Wq175MatchEnd.Completed -> "Harika! Bütün kartlar eşleşti!"
    Wq175MatchEnd.Timeout -> "Süre doldu!"
    Wq175MatchEnd.Stopped -> "Oyun sonlandırıldı"
}

/** Keep a word in revision if the learner missed it earlier this same round. */
internal fun wq175MayClearReviewedMistake(
    mode:String,pair:Wq175MatchPair,wrongPairs:List<Wq175MatchPair>
):Boolean = mode=="mistakes" && wrongPairs.none { it.id==pair.id }

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
internal fun Wq175MatchingDialog(session:AppSession,ai:AiSession,onDismiss:()->Unit,onImport:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val store=remember(context,session.profileKey){
        Wq175MatchingProgress(context,session.profileKey)
    }
    var mode by rememberSaveable { mutableStateOf("classic") }
    var importOpen by remember { mutableStateOf(false) }
    var folderRevision by remember { mutableIntStateOf(0) }
    var chosenFolderId by rememberSaveable { mutableStateOf("") }
    val foldersStore=remember(context,session.profileKey) {
        Wq186MatchingFolders(context,session.profileKey)
    }
    val savedFolders=remember(folderRevision,session.profileKey){foldersStore.all()}
    val selectedFolder=savedFolders.firstOrNull { it.id==chosenFolderId }
    var wordSource by rememberSaveable { mutableStateOf("Hazır Paketten Seç") }
    var levels by rememberSaveable { mutableStateOf(listOf("A1")) }
    var number by rememberSaveable { mutableIntStateOf(10) }
    var quickSeconds by rememberSaveable { mutableIntStateOf(90) }
    var englishFirst by rememberSaveable { mutableStateOf(false) }
    var kind by rememberSaveable { mutableStateOf("Tümü") }
    var busy by remember { mutableStateOf(false) }
    var issue by remember { mutableStateOf("") }
    var deck by remember { mutableStateOf<List<Wq175MatchPair>>(emptyList()) }
    var rightDeck by remember { mutableStateOf<List<IndexedValue<Wq175MatchPair>>>(emptyList()) }
    var leftChoice by remember { mutableIntStateOf(-1) }
    var matched by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var wrong by remember { mutableIntStateOf(0) }
    var wrongPairs by remember { mutableStateOf<List<Wq175MatchPair>>(emptyList()) }
    var feedback by remember { mutableStateOf("") }
    var remaining by remember { mutableIntStateOf(quickSeconds) }
    var finished by remember { mutableStateOf(false) }
    var endReason by remember { mutableStateOf(Wq175MatchEnd.Stopped) }
    var played by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    val meaningStore = remember(context,session.profileKey) {
        Wq175MatchingProgress(context,session.profileKey+":english-definitions")
    }
    val progressStore = if(mode=="definitions") meaningStore else store
    val englishDefinitionGame = mode=="definitions"

    fun finishGame(reason:Wq175MatchEnd) {
        if(finished)return
        finished=true
        endReason=reason
        val score=matched.size*10-(wrong*2)
        progressStore.finish(score.coerceAtLeast(0),reason==Wq175MatchEnd.Completed)
        revision++
    }

    LaunchedEffect(played,finished,mode) {
        if(played && !finished && mode=="timed") {
            while(remaining>0 && !finished) {
                delay(1000)
                if(remaining>0)remaining--
            }
            if(!finished && remaining<=0)finishGame(Wq175MatchEnd.Timeout)
        }
    }

    fun beginGame() {
        if(busy)return
        busy=true
        issue=""
        scope.launch {
            val items=try {
                when {
                    mode=="mistakes" -> store.missed()
                    mode=="definitions" && wordSource=="Klasörlerim" ->
                        selectedFolder?.takeIf { it.englishDefinitions }?.words.orEmpty()
                    mode=="definitions" -> Wq186MeaningBank.load(levels)
                    wordSource=="Klasörlerim" ->
                        selectedFolder?.takeIf { !it.englishDefinitions }?.words.orEmpty()
                    mode=="notebook" || wordSource=="Kendi Kelimelerinle" ->
                        session.searchLearning("",0,5000).map {
                            Wq175MatchPair("Defterim","Kişisel",it.answer,it.clue)
                        }
                    else -> withContext(Dispatchers.IO) {
                        levels.flatMap { Wq175MatchingBank.load(context,it) }
                    }
                }
            }catch(_:Exception){emptyList()}
            val personalDeck = mode=="notebook" || (mode!="mistakes" && wordSource=="Kendi Kelimelerinle") || wordSource=="Klasörlerim"
            val filtered=items.filter { mode=="mistakes" || mode=="definitions" || personalDeck || kind=="Tümü" || it.kind==kind }
            val available=Wq175MatchingBank.prepare(filtered,number)
            busy=false
            val minimum = if(mode=="mistakes") 2 else number
            if(available.size<minimum) {
                issue=if(mode=="mistakes" || personalDeck)
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
            wrongPairs=emptyList()
            feedback=""
            remaining=quickSeconds
            finished=false
            played=true
        }
    }

    if(importOpen) Wq186MatchingImporter(
        ai=ai,profile=session.profileKey,englishDefinitions=englishDefinitionGame,
        onDone={ id ->
            folderRevision++
            chosenFolderId=id
            wordSource="Klasörlerim"
            importOpen=false
        },onDismiss={importOpen=false}
    )
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
                if(!played) Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(27.dp))
                    .background(Brush.horizontalGradient(listOf(
                        Color(0xFF146E79),Color(0xFF21A8A2),Color(0xFFEC91B8))))
                    .padding(19.dp)) {
                    Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text("KELİME KARTLARI",fontSize=11.sp,
                            color=Color(0xFFCCFFF6),fontWeight=FontWeight.Black)
                        Text("Bul, eşleştir, öğren!",
                            fontSize=27.sp,lineHeight=30.sp,
                            color=Color.White,fontWeight=FontWeight.ExtraBold)
                        Text("Türkçe–İngilizce ve English–English eşleştir.",
                            fontSize=12.sp,color=Color.White)
                        Text("Oynanan: ${progressStore.played} · Kazanılan: ${progressStore.wins} · En iyi: ${progressStore.best}",
                            fontSize=11.sp,color=Color(0xFFFFF4FB))
                    }
                }
                if(!played) {
                    Text("Eşleştirme için kelime kaynağın",
                        fontWeight=FontWeight.Bold,color=MatchInk,fontSize=15.sp)
                    Wq186OptionTiles(
                        listOf("Hazır Paketten Seç","Klasörlerim","Kendi Kelimelerinle"),
                        setOf(wordSource),!busy
                    ) { chosen ->
                        wordSource=chosen
                        if(chosen=="Kendi Kelimelerinle")mode="notebook"
                        else if(mode=="notebook")mode="classic"
                        issue=""
                    }
                    if(wordSource=="Klasörlerim") {
                        Text("Klasörünü seç",color=MatchInk,fontWeight=FontWeight.Bold)
                        val compatible=savedFolders.filter{
                            it.englishDefinitions == englishDefinitionGame
                        }
                        if(compatible.isEmpty()) Text(
                            "Bu eşleştirme türü için henüz klasör yok. Kelimeleri içe aktar.",
                            fontSize=12.sp,color=MatchPink
                        )
                        else Wq186OptionTiles(
                            compatible.map{it.name},
                            compatible.firstOrNull{it.id==chosenFolderId}
                                ?.let{setOf(it.name)} ?: emptySet(),!busy
                        ) { name ->
                            chosenFolderId=compatible.first{it.name==name}.id
                        }
                    }
                    OutlinedButton(
                        onClick={importOpen=true},
                        modifier=Modifier.fillMaxWidth()
                    ) { Text("＋ Kelime / PDF / Görsel / Kamera / Yapay zekâ") }
                    Text("Aktarılan kelimeler eşleştirme klasöründe saklanır; " +
                        "Kelime Hafızam'a otomatik eklenmez.",
                        fontSize=11.sp,color=Color(0xFF58747A))
                    Text("Oyununu seç",fontWeight=FontWeight.ExtraBold,
                        color=MatchInk,fontSize=19.sp)
                    val modes=listOf(
                        "classic" to "Klasik Eşleştirme",
                        "timed" to "Hızlı Eşleştirme",
                        "definitions" to "English Word · English Meaning",
                        "mistakes" to "Yanlışlarımdan Eşleştirme",
                        "notebook" to "Kendi Defterimden"
                    )
                    modes.forEach { (value,label) ->
                        val modeArt = when(value) {
                            "classic" -> "cards"
                            "timed" -> "timer"
                            "definitions" -> "synonym"
                            "mistakes" -> "review"
                            else -> "notebook"
                        }
                        val detail = when(value) {
                            "classic" -> "Dilediğin hızda eşleştir"
                            "timed" -> "Süreyi sen belirle"
                            "definitions" -> "İngilizce kelimeyi İngilizce anlamıyla eşleştir"
                            "mistakes" -> "Önceki yanlışlarını tekrar et"
                            else -> "Kendi kaydettiğin kelimeler"
                        }
                        Surface(onClick={mode=value;issue="";if(value=="definitions")englishFirst=true},
                            shape=RoundedCornerShape(20.dp),
                            shadowElevation=if(mode==value)5.dp else 2.dp,
                            color=if(mode==value)Color(0xFFDBFAF1)
                                else Color(0xFFF7FBFF),
                            border=BorderStroke(if(mode==value)2.dp else 1.dp,
                                if(mode==value)MatchTeal else Color(0xFFDCE5E8))) {
                            Row(Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement=Arrangement.spacedBy(12.dp),
                                verticalAlignment=Alignment.CenterVertically) {
                                if(value=="definitions") Wq186EnglishMeaningIcon(Modifier.size(57.dp))
                                else Wq175GlossIcon(modeArt,Modifier.size(57.dp))
                                Column(Modifier.weight(1f),
                                    verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                    Text(label,color=MatchInk,fontSize=15.sp,
                                        fontWeight=FontWeight.ExtraBold)
                                    Text(detail,color=Color(0xFF58747A),
                                        fontSize=11.sp,lineHeight=15.sp)
                                }
                                if(mode==value) Text("✓",color=MatchTeal,
                                    fontWeight=FontWeight.Black,fontSize=21.sp)
                            }
                        }
                    }
                    if((mode=="classic" || mode=="timed" || mode=="definitions")
                        && wordSource=="Hazır Paketten Seç") {
                        Text("Seviye · çoklu seçim",color=MatchInk,fontWeight=FontWeight.Bold)
                        Wq186OptionTiles(listOf("A1","A2","B1","B2","C1","C2"),
                            levels.toSet(),!busy) { selected ->
                            levels=if(selected in levels) {
                                if(levels.size==1)levels else levels.filterNot { it==selected }
                            } else levels+selected
                            issue=""
                        }
                        if(mode!="definitions") {
                            Text("Kelime türü",color=MatchInk,fontWeight=FontWeight.Bold)
                            Wq186OptionTiles(listOf("Tümü","İsimler","Fiiller","Sıfatlar","Zarflar"),
                                setOf(kind),!busy) { kind=it;issue="" }
                        }
                    }
                    Text("Çift sayısı",color=MatchInk,fontWeight=FontWeight.Bold)
                    Wq186OptionTiles(listOf("6","10","15","20","30"),
                        setOf(number.toString()),!busy) { selected ->
                        number=selected.toInt();issue=""
                    }
                    Text("Tek oyunda $number çift / ${number*2} kart",
                        fontSize=12.sp,color=Color(0xFF58747A))
                    if(mode=="timed") {
                        Text("Hızlı oyun süresi",color=MatchInk,fontWeight=FontWeight.Bold)
                        Wq186OptionTiles(listOf("60 sn","90 sn","120 sn","180 sn","300 sn"),
                            setOf("$quickSeconds sn"),!busy) {
                            quickSeconds=it.substringBefore(" ").toInt()
                        }
                    }
                    if(!englishDefinitionGame) Row(verticalAlignment=Alignment.CenterVertically) {
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
                    Surface(
                        modifier=Modifier.fillMaxWidth(),
                        shape=RoundedCornerShape(24.dp),
                        color=Color(0xFFE8F6F5),
                        border=BorderStroke(1.dp,Color(0xFFB6DBDC))
                    ) {
                        Row(Modifier.padding(16.dp),
                            verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Wq175GlossIcon(
                                if(endReason==Wq175MatchEnd.Completed)
                                    "success" else "review",
                                Modifier.size(79.dp))
                            Column(Modifier.weight(1f),
                                verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                Text(wq175MatchResultTitle(endReason),
                                    fontSize=21.sp,color=MatchInk,
                                    fontWeight=FontWeight.Black)
                                Text("Doğru: ${matched.size}/${deck.size} · Yanlış: $wrong · Puan: "+
                                    (matched.size*10-wrong*2).coerceAtLeast(0),
                                    color=MatchInk,fontSize=13.sp)
                            }
                        }
                    }
                    if(wrongPairs.isEmpty()) {
                        Text("Bu turda yanlış eşleştirme yapmadın.",
                            color=Color(0xFF65787D),fontSize=12.sp)
                    } else {
                        Text("Tekrar çalışacağın kelimeler:",
                            color=MatchInk,fontWeight=FontWeight.Bold,fontSize=14.sp)
                        wrongPairs.forEach { word ->
                            Surface(shape=RoundedCornerShape(12.dp),
                                color=Color(0xFFFFF1E6),
                                modifier=Modifier.fillMaxWidth()) {
                                Text(word.english+"  —  "+word.turkish,
                                    Modifier.padding(11.dp),color=MatchInk,
                                    fontSize=13.sp,fontWeight=FontWeight.Medium)
                            }
                        }
                        Text("Bu kelimeler Yanlışlarımdan Eşleştirme'ye kaydedildi.",
                            color=Color(0xFF65787D),fontSize=12.sp)
                    }
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
                    Text(if(englishDefinitionGame)"Önce İngilizce kelimeyi, sonra İngilizce tanımı seç."
                        else if(englishFirst)"Önce İngilizce, sonra Türkçe kartını seç."
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
                                    modifier=Modifier.fillMaxWidth().heightIn(min=54.dp)){
                                    Box(Modifier.padding(9.dp),contentAlignment=Alignment.Center) {
                                        Text(if(!active)"✓" else if(englishFirst || englishDefinitionGame)pair.english else pair.turkish,
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
                                            if(wq175MayClearReviewedMistake(mode,pair,wrongPairs)) {
                                                progressStore.learned(pair)
                                            }
                                            feedback="Doğru! ${pair.english} = ${pair.turkish}"
                                            if(next.size==deck.size)finishGame(Wq175MatchEnd.Completed)
                                        }else {
                                            wrong++
                                            val mistaken=deck[left]
                                            wrongPairs=(wrongPairs+mistaken).distinctBy { it.id }
                                            progressStore.mistake(mistaken)
                                            feedback="Tekrar dene. Bu çift eşleşmiyor."
                                        }
                                        leftChoice=-1
                                    } else if(leftChoice<0)feedback="Önce soldan bir kart seç."
                                },enabled=active,
                                    shape=RoundedCornerShape(16.dp),
                                    color=if(active)Color.White else Color(0xFFD2F4E3),
                                    border=BorderStroke(1.dp,Color(0xFFDCE5EA)),
                                    modifier=Modifier.fillMaxWidth().heightIn(min=54.dp)){
                                    Box(Modifier.padding(9.dp),contentAlignment=Alignment.Center) {
                                        Text(if(!active)"✓" else if(englishFirst || englishDefinitionGame)pair.turkish else pair.english,
                                            color=MatchInk,fontSize=13.sp,
                                            fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                    if(feedback.isNotBlank())Text(feedback,fontSize=12.sp,
                        color=if(feedback.startsWith("Doğru"))MatchTeal else MatchPink)
                    OutlinedButton(onClick={finishGame(Wq175MatchEnd.Stopped)},
                        modifier=Modifier.fillMaxWidth()){
                        Text("Oyunu bitir",color=MatchInk)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
