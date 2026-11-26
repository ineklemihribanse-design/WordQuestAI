package com.kelimebulmacasi.a1

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private fun JSONObject?.ints(key:String):List<Int>{
    val a=this?.optJSONArray(key)?:return emptyList()
    return (0 until a.length()).map{a.optInt(it,-1)}.filter{it>=0}
}
private fun JSONObject?.strings(key:String):List<String>{
    val a=this?.optJSONArray(key)?:return emptyList()
    return (0 until a.length()).map{a.optString(it)}
}
private fun JSONObject?.objects(key:String):List<JSONObject>{
    val a=this?.optJSONArray(key)?:return emptyList()
    return (0 until a.length()).mapNotNull{a.optJSONObject(it)}
}
private fun JSONObject?.number(key:String):Long? =
    if(this==null||!has(key)||isNull(key))null else optLong(key)
private data class Wq150Cell(val row:Int,val col:Int)
private fun cell(s:String):Wq150Cell?{
    val parts=s.split(",");if(parts.size!=2)return null
    val r=parts[0].toIntOrNull()?:return null;val c=parts[1].toIntOrNull()?:return null
    return Wq150Cell(r,c).takeIf{r in 0..999&&c in 0..999}
}
private fun wordCells(w:JSONObject)=w.strings("cells").mapNotNull(::cell)
private fun letters(game:JSONObject,kind:String,words:List<JSONObject>):Map<Wq150Cell,String>{
    val fixed=mutableMapOf<Wq150Cell,String>()
    val resolved=game.optJSONObject("resolved")
    val hints=game.optJSONObject("myHints")
    val known=game.optJSONObject("known")
    known?.keys()?.forEach{k->cell(k)?.let{fixed[it]=known.optString(k)}}
    words.forEachIndexed{i,w->
        val cells=wordCells(w)
        val answer=resolved?.optJSONObject(i.toString())?.optString("answer").orEmpty()
        if(answer.length==cells.size)cells.forEachIndexed{j,c->fixed[c]=answer[j].toString()}
        if(kind=="duel"){
            val h=hints?.optJSONObject(i.toString())
            h?.keys()?.forEach{k->k.toIntOrNull()?.let{j->cells.getOrNull(j)?.let{fixed[it]=h.optString(k)}}}
        }else if(kind=="team"){
            val a=hints?.optJSONArray(i.toString())
            if(a!=null)for(j in 0 until a.length()){
                val h=a.optJSONObject(j)?:continue
                cells.getOrNull(h.optInt("position",-1))?.let{fixed[it]=h.optString("letter")}
            }
        }
    }
    return fixed
}
@Composable
private fun Wq150Board(words:List<JSONObject>,selected:Int,fixed:Map<Wq150Cell,String>,
    selectable:Boolean,onSelect:(Int)->Unit,modifier:Modifier=Modifier){
    val positions=remember(words){words.map(::wordCells)}
    val cells=remember(positions){positions.flatten().distinct()}
    if(cells.isEmpty())return
    val r0=cells.minOf{it.row};val c0=cells.minOf{it.col}
    val rows=cells.maxOf{it.row}-r0+1;val cols=cells.maxOf{it.col}-c0+1
    val active=positions.getOrNull(selected).orEmpty()
    BoxWithConstraints(modifier.fillMaxWidth(),contentAlignment=Alignment.Center){
        val size=minOf(maxWidth.value/cols,maxHeight.value/rows,40f).coerceAtLeast(1f).dp
        Box(Modifier.size(size*cols,size*rows)){
            cells.forEach{p->
                val i=positions.indexOfFirst{p in it}
                val label=fixed[p].orEmpty()
                Box(Modifier.offset(size*(p.col-c0),size*(p.row-r0)).size(size)
                    .background(if(p in active)Color(0xFFF4D58D)else Color(0xFFFAF3DE))
                    .border(0.7.dp,Color(0xFF645580))
                    .clickable(enabled=selectable&&i>=0){onSelect(i)},contentAlignment=Alignment.Center){
                    if(label.isNotBlank())Text(label,color=Color(0xFF241638),fontSize=(size.value*.55f).coerceIn(5f,22f).sp,fontWeight=FontWeight.Bold)
                    else if(size.value>=14f&&positions.getOrNull(i)?.firstOrNull()==p)
                        Text((i+1).toString(),Modifier.align(Alignment.TopStart).padding(1.dp),color=Color(0xFF645580),fontSize=(size.value*.28f).coerceIn(6f,10f).sp)
                }
            }
        }
    }
}

/** All actions use the current authenticated server snapshot; no local scoring. */
@Composable
internal fun Wq150ArenaMatch(room:String,initial:JSONObject,uid:String,gateway:Wq150Gateway,onBack:()->Unit){
    val controller=remember(room,uid,gateway){Wq150RemoteMatch(room,gateway,initial)}
    val remote by controller.state.collectAsState()
    val scope=rememberCoroutineScope()
    val view=remote.snapshot?:initial
    val game=view.optJSONObject("game")?:JSONObject()
    val settings=view.optJSONObject("settings")?:JSONObject()
    val kind=view.optString("kind");val mode=settings.optString("mode")
    val phase=view.optString("phase");val paused=phase=="paused";val finished=phase=="finished"
    val quiz=kind=="quiz"||(kind=="team"&&settings.optString("format")=="quiz")
    val team=game.objects("teams").firstOrNull{uid in it.strings("members")}?.optString("id").orEmpty()
    val mine=game.optJSONObject("mine")
    val words=game.objects(if(kind=="team")"items" else "words")
    val active=game.number("active")?.toInt()
    val choose=mode=="strategy"&&active==null
    val free=kind=="coop"&&mode in listOf("free","timed","perfect")
    var selected by remember(room,active){mutableIntStateOf(active?:0)}
    val index=if(kind=="quiz")game.optInt("index") else if(free||choose)selected else active?:selected
    val entry=words.getOrNull(index)
    val attempted=game.strings("attempted")
    val resolved=game.optJSONObject(if(kind=="coop")"solved" else "resolved")
    val eligible=when(kind){
        "duel" -> (mode=="speed"||game.optString("turn")==uid)&&uid !in attempted
        "quiz" -> (mode in listOf("speed","simultaneous")||game.optString("turn")==uid)&&mine?.optString("status")=="open"
        "coop" -> (mode!="alternating"||game.optString("turn")==uid)&&(mode!="double"||uid !in game.strings("confirmed")&&uid !in game.strings("passes"))
        "team" -> (mode=="speed"||game.optString("turn")==team)&&game.optJSONObject("activePlayers")?.optString(team)==uid&&team !in attempted
        else -> false
    }
    val enabled=eligible&&phase=="playing"&&!remote.busy&&remote.pending==null&&remote.error==null
    val question=if(kind=="quiz")game.optJSONObject("question") else entry
    val prompt=question?.optString(if(kind=="quiz")"prompt" else "clue").orEmpty()
    val known=remember(game,kind,words){letters(game,kind,words)}
    val currentCells=entry?.let(::wordCells).orEmpty()
    val missing=currentCells.filterNot{it in known}
    val totalLength=entry?.optInt("length",entry.optInt("letterCount"))?:0
    val hintBudget=when(kind){
        "quiz" -> mine?.optInt("hintsLeft")?:0
        "coop" -> (game.optInt("hints")-game.optInt("spent")).coerceAtLeast(0)
        else -> game.optInt("hintsLeft")
    }
    var draft by remember(room,index){mutableStateOf("")}
    // Retain entered letters for cells that remain unfilled as hints arrive.
    var previousMissing by remember(room,index){mutableStateOf(missing)}
    LaunchedEffect(missing){
        if(previousMissing!=missing){
            val typed=previousMissing.zip(draft.map{it.toString()}).toMap()
            draft=missing.mapNotNull{typed[it]}.joinToString("")
            previousMissing=missing
        }
    }
    val answerText=if(currentCells.isEmpty())draft else {
        var at=0
        currentCells.joinToString(""){c->known[c]?:draft.getOrNull(at++)?.toString().orEmpty()}
    }
    val answerComplete=if(currentCells.isEmpty())draft.length==totalLength else draft.length==missing.size
    var exit by remember(room){mutableStateOf(false)}
    var now by remember{mutableLongStateOf(System.currentTimeMillis())}
    LaunchedEffect(room){controller.poll()}
    LaunchedEffect(phase){while(phase!="finished"){now=System.currentTimeMillis();delay(250L)}}
    val deadline=if(kind=="quiz")mine.number("deadline") else game.number("deadline")
    val connection=view.optJSONObject("connection")
    val clock=if(paused)connection.number("pausedAt")?:now else now
    val remaining=deadline?.let{((it-clock+999L)/1000L).coerceAtLeast(0L)}
    val social=view.optJSONObject("social")?:JSONObject()
    val offer=social.optJSONObject("offer")
    val offerId=offer?.optString("id").orEmpty()
    val offerPending=offer?.optString("status")=="pending"&&(offer.number("expiresAt")?:0)>now
    val offeredByMe=offer?.optString("from")==uid
    val mustVote=offerPending&&!offeredByMe&&uid in offer.strings("required")&&uid !in offer.strings("accepted")
    val canSocial=phase=="playing"&&!remote.busy&&remote.pending==null&&remote.error==null
    var emojiMenu by remember(room){mutableStateOf(false)}
    var lastEmoji by remember(room){mutableLongStateOf(0L)}
    var reactionText by remember(room){mutableStateOf("")}
    val seen=remember(room){mutableSetOf<String>()}
    val incoming=social.optJSONObject("reactions")
    val eventKey=incoming?.toString().orEmpty()
    LaunchedEffect(eventKey){
        val events=incoming?.keys()?.asSequence()?.mapNotNull{p->incoming.optJSONObject(p)?.let{p to it}}?.toList().orEmpty()
        val latest=events.sortedByDescending{it.second.optLong("at")}.firstOrNull{(p,j)->p+":"+j.optString("id") !in seen}
        events.forEach{(p,j)->seen.add(p+":"+j.optString("id"))}
        if(latest!=null){
            reactionText=latest.second.optString("symbol")
            delay(1200L);reactionText=""
        }
    }
    var noticeText by remember(room){mutableStateOf("")}
    val notice=social.optJSONObject("notice")
    LaunchedEffect(notice?.optString("id"),notice?.optString("kind")){
        noticeText=when(notice?.optString("kind")){
            "rejected"->"Maç bitirme teklifi reddedildi."
            "withdrawn"->"Maç bitirme teklifi geri çekildi."
            "expired"->"Maç bitirme teklifinin süresi doldu."
            "accepted"->"Maç bitirme teklifi kabul edildi."
            else->""
        }
        if(noticeText.isNotEmpty()){delay(3500L);noticeText=""}
    }
    fun sendSocial(type:String,symbol:String?=null,id:String?=null){
        if(!canSocial)return
        val command=Wq150Command(view.optLong("revision"),type,symbol=symbol,offerId=id)
        if(command.valid())scope.launch{controller.submit(command)}
    }
    fun send(type:String,answer:String?=null,choice:Int?=null){
        if(!enabled)return
        val command=Wq150Command(view.optLong("revision"),type,
            word=if(kind in listOf("duel","coop"))index else null,
            index=if(kind=="quiz")index else null,item=if(kind=="team")index else null,
            answer=answer,choice=choice)
        if(command.valid())scope.launch{controller.submit(command)}
    }
    BackHandler{exit=true}
    MaterialTheme(colorScheme=Wq122Scheme){
        Box(Modifier.fillMaxSize().background(Color(0xFF150B3E))){
        Wq183ArenaBackdrop(Modifier.matchParentSize(),active=true)
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding().padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                TextButton(onClick={exit=true}){Text("‹ Geri",color=Wq122Gold)}
                Text(Wq150ModeCatalog.kinds[kind].orEmpty(),color=Wq122Gold,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                remaining?.let{Text("$it sn",color=Wq122Gold,fontWeight=FontWeight.Black)}
                Box{
                    TextButton(enabled=canSocial&&now-lastEmoji>=1500L,onClick={emojiMenu=true}){Text("☺",color=Wq122Gold,fontSize=22.sp)}
                    DropdownMenu(expanded=emojiMenu,onDismissRequest={emojiMenu=false}){
                        listOf("👏","👍","😊","😄","😮","🔥","🎉","💪","🤔","😢","❤️","🏆").chunked(4).forEach{row->
                            Row(Modifier.padding(horizontal=6.dp)){
                                row.forEach{symbol->TextButton(onClick={emojiMenu=false;lastEmoji=now;sendSocial("reaction",symbol=symbol)}){Text(symbol,fontSize=22.sp)}}
                            }
                        }
                    }
                }
                TextButton(enabled=canSocial&&!offerPending&&now>=(social.number("cooldownUntil")?:0),onClick={sendSocial("finish-offer")}){Text("Bitir",color=Wq122Gold)}
            }
            val scores=game.optJSONObject("scores")?:game.optJSONObject("stats")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                val ids=connection.objects("players").map{it.optString("id")}
                val groups=if(kind=="team")game.objects("teams").map{it.optString("id")}else ids
                groups.forEachIndexed{i,id->
                    val value=scores?.optJSONObject(id)?.optInt("score")?:scores?.optInt(id)?:0
                    val currentTurn=game.optString("turn")==id||mode in listOf("speed","simultaneous")
                    Surface(color=Color(0xFF39245D),shape=MaterialTheme.shapes.medium,modifier=Modifier.weight(1f),
                        border=if(currentTurn&&phase=="playing")androidx.compose.foundation.BorderStroke(2.dp,Color(0xFFFFD56B))else null){
                        Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                            Text(if(id==uid)"Sen" else if(kind=="team")"Takım "+(i+1) else "Oyuncu "+(i+1),color=Wq122Ink)
                            Text(if(kind=="coop")"Ortak" else value.toString(),color=Wq122Gold,fontWeight=FontWeight.Black)
                        }
                    }
                }
            }
            if(!quiz&&!finished){
                Wq150Board(words,index,known,enabled&&(choose||free),{selected=it},Modifier.weight(1f))
            }
            Column(Modifier.fillMaxWidth().then(if(quiz||finished)Modifier.weight(1f)else Modifier.heightIn(max=320.dp))
                .verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
                if(finished){
                    val result=view.optJSONObject("result")
                    Text("Maç tamamlandı",color=Wq122Gold,style=MaterialTheme.typography.headlineSmall)
                    val winner=result?.optString("winner").orEmpty()
                    Text(when{
                        result?.optString("kind")=="incomplete"->"Ortak bulmaca tamamlanmadı."
                        result?.optString("kind")=="forfeit"->if(winner==uid||winner==team)"Hükmen kazandın." else "Hükmen sonuçlandı."
                        winner.isNotBlank()&&winner!="null"->if(winner==uid||winner==team)"Kazandın!" else "Maç sona erdi."
                        kind=="coop"->"Ortak ilerleme: "+game.optJSONObject("solved")?.length()
                        else->"Maç sonucu"
                    },color=Wq122Ink)
                    Button(onClick=onBack){Text("Arena'ya dön")}
                }else{
                    Text(if(choose)"Çözeceğin kelimeyi seç" else prompt,color=Wq122Ink,fontWeight=FontWeight.Bold)
                    if(choose||free){
                        val queue=if(kind=="coop")words.indices.filter{resolved?.has(it.toString())!=true} else game.ints("queue")
                        if(index !in queue&&queue.isNotEmpty())LaunchedEffect(queue){selected=queue.first()}
                        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                            OutlinedButton(enabled=enabled&&queue.isNotEmpty(),onClick={selected=queue[(queue.indexOf(index)-1+queue.size)%queue.size]}){Text("Önceki")}
                            Text("Kelime "+(index+1),color=Wq122Gold,modifier=Modifier.align(Alignment.CenterVertically))
                            OutlinedButton(enabled=enabled&&queue.isNotEmpty(),onClick={selected=queue[(queue.indexOf(index)+1)%queue.size]}){Text("Sonraki")}
                        }
                        if(choose)Button(enabled=enabled&&index in queue,onClick={send("choose")}){Text("Kelimeyi seç")}
                    }
                    if(!choose){
                        if(quiz){
                            val removed=if(kind=="quiz")mine.ints("removed") else game.optJSONObject("myHints")?.let{it.ints(index.toString())}.orEmpty()
                            question.strings("options").forEachIndexed{i,label->
                                OutlinedButton(enabled=enabled&&i !in removed,onClick={send(if(kind=="quiz")"answer" else "submit",choice=i)},modifier=Modifier.fillMaxWidth()){
                                    Text(if(i in removed)"Elendi" else label)
                                }
                            }
                        }else{
                            val length=entry?.optInt("length",entry.optInt("letterCount"))?:0
                            Text("Kelime "+(index+1)+" · "+length+" harf",color=Wq122Gold)
                            Text(answerText.ifBlank{"—"},color=Wq122Gold,fontWeight=FontWeight.Bold)
                            OutlinedTextField(draft,{draft=it.filter{c->c.isLetter()}.uppercase(java.util.Locale.ROOT).take(if(currentCells.isEmpty())totalLength else missing.size)},enabled=enabled&&(!answerComplete||draft.isNotEmpty()),singleLine=true,
                                label={Text("Eksik harfleri yaz")},modifier=Modifier.fillMaxWidth())
                            Button(enabled=enabled&&answerComplete&&answerText.isNotBlank(),onClick={send("submit",answer=answerText)},modifier=Modifier.fillMaxWidth()){Text("Gönder")}
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                            OutlinedButton(enabled=enabled,onClick={send("pass")}){Text("Pas")}
                            OutlinedButton(enabled=enabled&&hintBudget>0,onClick={send(if(kind=="quiz")"eliminate" else "hint")}){
                                Text((if(quiz)"Şık ele" else "Harf aç")+" ($hintBudget)")
                            }
                            if(kind=="duel")OutlinedButton(enabled=enabled,onClick={send("skip")}){Text("Kelimeyi geç")}
                        }
                        if(kind=="quiz")Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            OutlinedButton(enabled=enabled&&hintBudget>0&&"half" !in mine.strings("used"),onClick={send("half")}){Text("50:50")}
                            OutlinedButton(enabled=enabled&&hintBudget>0&&deadline!=null&&"time" !in mine.strings("used"),onClick={send("time")}){Text("+10 sn")}
                        }
                    }
                    if(!eligible&&!paused)Text("Rakibin sırası veya cevap bekleniyor.",color=Wq122Ink)
                }
                if(offerPending&&offeredByMe)TextButton(enabled=canSocial,onClick={sendSocial("finish-withdraw",id=offerId)}){Text("Bitirme teklifini geri çek",color=Wq122Gold)}
                if(remote.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                remote.error?.let{Text(it,color=Color(0xFFFFB4B4))}
                if(remote.pending!=null&&!remote.busy)TextButton(onClick={scope.launch{controller.retryPending()}}){Text("İşlemi tekrar dene")}
            }
        }
        androidx.compose.animation.AnimatedVisibility(visible=reactionText.isNotEmpty(),modifier=Modifier.align(Alignment.CenterEnd).padding(16.dp)){
            Surface(color=Color(0xDD251347),shape=MaterialTheme.shapes.large){Text(reactionText,Modifier.padding(10.dp),fontSize=36.sp)}
        }
        if(noticeText.isNotBlank())Surface(modifier=Modifier.align(Alignment.TopCenter).padding(top=100.dp),color=Color(0xEE251347),shape=MaterialTheme.shapes.medium){
            Text(noticeText,Modifier.padding(12.dp),color=Wq122Ink)
        }
        }
        if(mustVote&&!paused&&!finished)AlertDialog(onDismissRequest={},title={Text("Maç bitirme teklifi")},
            text={Text("Rakibin maçı karşılıklı anlaşmayla bitirmek istiyor. Kalan süre: "+(((offer.number("expiresAt")?:now)-now+999L)/1000L).coerceAtLeast(0L)+" sn")},
            confirmButton={TextButton(enabled=canSocial,onClick={sendSocial("finish-accept",id=offerId)}){Text("Kabul et")}},
            dismissButton={TextButton(enabled=canSocial,onClick={sendSocial("finish-reject",id=offerId)}){Text("Reddet")}})
        if(paused&&!finished)AlertDialog(onDismissRequest={},title={Text("Oyuncu bağlantısı kesildi")},
            text={Text("Maç duraklatıldı. Tahta, puanlar ve süre korunuyor.")},confirmButton={})
        if(exit)AlertDialog(onDismissRequest={exit=false},title={Text("Arena'dan ayrıl?")},
            text={Text("Bu ekranı kapatmak sunucuda hükmen ayrılma işlemi değildir.")},
            confirmButton={TextButton(onClick=onBack){Text("Çık")}},dismissButton={TextButton(onClick={exit=false}){Text("Devam et")}})
    }
}
@Composable
internal fun Wq150ArenaFlow(session:AppSession,endpoint:Wq150Endpoint,back:()->Unit,account:()->Unit,legacy:()->Unit){
    var room by remember{mutableStateOf<String?>(null)}
    var initial by remember{mutableStateOf<JSONObject?>(null)}
    val auth=session.accounts.socialAuth()
    val uid=session.accounts.uid.orEmpty()
    val gateway=remember(endpoint,auth,uid){auth?.let{Wq150Gateway(endpoint,it)}}
    val id=room;val snapshot=initial
    if(id!=null&&snapshot!=null&&gateway!=null&&uid.isNotBlank())
        Wq150ArenaMatch(id,snapshot,uid,gateway,back)
    else Wq150ArenaEntry(session,endpoint,back,account,legacy){r,v->room=r;initial=v}
}
