package com.kelimebulmacasi.a1

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.runtime.saveable.rememberSaveable
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
/** Compute a measured viewport. All coordinates are in dp, not screen fractions. */
internal data class Wq150Viewport(val cell:Float,val x:Float,val y:Float,val minX:Float,val maxX:Float,val minY:Float,val maxY:Float)
internal fun wq150Viewport(width:Float,height:Float,rows:Int,cols:Int,zoom:Int,focusRow:Float,focusCol:Float):Wq150Viewport{
    require(width>0&&height>0&&rows>0&&cols>0&&zoom in 0..2)
    val fit=minOf(width/cols,height/rows,40f)
    val scale=when(zoom){1->1.55f;2->2.35f;else->1f}
    val cell=fit*scale
    val boardW=cols*cell;val boardH=rows*cell
    val minX=minOf(0f,width-boardW);val maxX=maxOf(0f,(width-boardW)/2f)
    val minY=minOf(0f,height-boardH);val maxY=maxOf(0f,(height-boardH)/2f)
    val x=if(boardW<=width)(width-boardW)/2f else (width/2f-focusCol*cell).coerceIn(minX,0f)
    val y=if(boardH<=height)(height-boardH)/2f else (height/2f-focusRow*cell).coerceIn(minY,0f)
    return Wq150Viewport(cell,x,y,if(boardW<=width)x else minX,maxX,if(boardH<=height)y else minY,maxY)
}
@Composable
private fun Wq150Board(words:List<JSONObject>,selected:Int,fixed:Map<Wq150Cell,String>,
    selectable:Boolean,onSelect:(Int)->Unit,zoom:Int,modifier:Modifier=Modifier){
    val positions=remember(words){words.map(::wordCells)}
    val cells=remember(positions){positions.flatten().distinct()}
    if(cells.isEmpty())return
    val r0=cells.minOf{it.row};val c0=cells.minOf{it.col}
    val rows=cells.maxOf{it.row}-r0+1;val cols=cells.maxOf{it.col}-c0+1
    val active=positions.getOrNull(selected).orEmpty()
    val owners=remember(positions){buildMap<Wq150Cell,List<Int>>{
        positions.forEachIndexed{i,word->word.forEach{p->put(p,get(p).orEmpty()+i)}}
    }}
    BoxWithConstraints(modifier.fillMaxWidth().clipToBounds()){
        val density=LocalDensity.current.density
        val width=maxWidth.value.coerceAtLeast(1f);val height=maxHeight.value.coerceAtLeast(1f)
        val focusRow=if(active.isEmpty())rows/2f else (active.map{it.row-r0}.average().toFloat()+.5f)
        val focusCol=if(active.isEmpty())cols/2f else (active.map{it.col-c0}.average().toFloat()+.5f)
        val viewport=wq150Viewport(width,height,rows,cols,zoom,focusRow,focusCol)
        var x by remember(zoom,selected,width,height,rows,cols){mutableFloatStateOf(viewport.x)}
        var y by remember(zoom,selected,width,height,rows,cols){mutableFloatStateOf(viewport.y)}
        val size=viewport.cell.dp
        val drag=if(zoom==0)Modifier else Modifier.pointerInput(zoom,selected,width,height,rows,cols){
            detectDragGestures{change,amount->
                change.consume()
                x=(x+amount.x/density).coerceIn(viewport.minX,viewport.maxX)
                y=(y+amount.y/density).coerceIn(viewport.minY,viewport.maxY)
            }
        }
        Box(Modifier.fillMaxSize().then(drag)){
            cells.forEach{p->
                val candidates=owners[p].orEmpty()
                val i=candidates.firstOrNull{it==selected}?:candidates.firstOrNull()?:-1
                val label=fixed[p].orEmpty()
                val px=x+(p.col-c0)*viewport.cell;val py=y+(p.row-r0)*viewport.cell
                if(px+viewport.cell>0&&px<width&&py+viewport.cell>0&&py<height){
                    Box(Modifier.offset(px.dp,py.dp).requiredSize(size)
                        .background(if(p in active)Color(0xFFF4D58D)else Color(0xFFFAF3DE))
                        .border(0.7.dp,Color(0xFF645580))
                        .clickable(enabled=selectable&&i>=0){
                            onSelect(candidates.firstOrNull{it!=selected}?:i)
                        },contentAlignment=Alignment.Center){
                        if(label.isNotBlank())Text(label,color=Color(0xFF241638),fontSize=(size.value*.55f).coerceIn(4f,22f).sp,fontWeight=FontWeight.Bold)
                        else if(size.value>=14f&&positions.getOrNull(i)?.firstOrNull()==p)
                            Text((i+1).toString(),Modifier.align(Alignment.TopStart).padding(1.dp),color=Color(0xFF645580),fontSize=(size.value*.28f).coerceIn(6f,10f).sp)
                    }
                }
            }
        }
    }
}

/** All actions use the current authenticated server snapshot; no local scoring. */
@Composable
internal fun Wq150ArenaMatch(room:String,initial:JSONObject,uid:String,gateway:Wq150Gateway,session:AppSession,onBack:()->Unit){
    val profiles=rememberWq150Profiles(room,session,gateway)
    val ownAvatar by rememberWq2DSelection(session.profileKey)
    val tablet=androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp>=600
    val context=androidx.compose.ui.platform.LocalContext.current
    val sessionId=remember(room,uid){
        val prefs=context.getSharedPreferences("wq150_match_session",android.content.Context.MODE_PRIVATE)
        val key=uid+":"+room
        prefs.getString(key,null)?:java.util.UUID.randomUUID().toString().also{prefs.edit().putString(key,it).apply()}
    }
    val controller=remember(room,uid,gateway,sessionId){Wq150RemoteMatch(room,gateway,sessionId,initial)}
    val remote by controller.state.collectAsState()
    val scope=rememberCoroutineScope()
    val view=remote.snapshot?:initial
    val game=view.optJSONObject("game")?:JSONObject()
    val settings=view.optJSONObject("settings")?:JSONObject()
    val kind=view.optString("kind");val mode=settings.optString("mode")
    val phase=view.optString("phase");val paused=phase=="paused";val finished=phase=="finished"
    // Save only final server-provided results. A later profile refresh may fill names;
    // the room ID makes the write idempotent, including four-player matches.
    LaunchedEffect(room,finished,view.optJSONObject("summary")?.toString(),profiles){
        if(finished)Wq150History.record(context,session.profileKey,room,view,profiles,uid,session.profileName)
    }
    val quiz=kind=="quiz"||(kind=="team"&&settings.optString("format")=="quiz")
    val team=game.objects("teams").firstOrNull{uid in it.strings("members")}?.optString("id").orEmpty()
    val mine=game.optJSONObject("mine")
    val words=game.objects(if(kind=="team")"items" else "words")
    val active=game.number("active")?.toInt()
    val choose=mode=="strategy"&&active==null
    val free=kind=="coop"&&mode in listOf("free","timed","perfect")
    var selected by remember(room,active){mutableIntStateOf(active?:0)}
    var zoom by rememberSaveable(room){mutableIntStateOf(0)}
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
    val typingAllowed=eligible&&phase=="playing"&&!choose&&!quiz
    val imeVisible=WindowInsets.ime.getBottom(LocalDensity.current)>0
    val focusRequester=remember(room){FocusRequester()}
    val focusManager=LocalFocusManager.current
    var hadFocus by remember(room){mutableStateOf(false)}
    LaunchedEffect(typingAllowed,index){
        if(!typingAllowed){focusManager.clearFocus();hadFocus=false}
        else if(hadFocus)runCatching{focusRequester.requestFocus()}
    }
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
    var now by remember{mutableLongStateOf(view.optLong("serverNow",System.currentTimeMillis()))}
    LaunchedEffect(room){controller.poll()}
    LaunchedEffect(view,phase){
        val serverBase=view.optLong("serverNow",System.currentTimeMillis())
        val localBase=android.os.SystemClock.elapsedRealtime()
        now=serverBase
        while(phase!="finished"){
            now=serverBase+android.os.SystemClock.elapsedRealtime()-localBase
            delay(250L)
        }
    }
    val deadline=if(kind=="quiz")mine.number("deadline") else game.number("deadline")
    val connection=view.optJSONObject("connection")
    val clock=if(paused)connection.number("pausedAt")?:now else now
    val remaining=deadline?.let{((it-clock+999L)/1000L).coerceAtLeast(0L)}
    Wq150Audio(room,phase,if(eligible)remaining else null,deadline,view.optJSONObject("feedback"),session.preferences.correctSoundEnabled)
    val social=view.optJSONObject("social")?:JSONObject()
    val offer=social.optJSONObject("offer")
    val offerId=offer?.optString("id").orEmpty()
    val offerPending=offer?.optString("status")=="pending"&&(offer.number("expiresAt")?:0)>now
    val offeredByMe=offer?.optString("from")==uid
    val mustVote=offerPending&&!offeredByMe&&uid in offer.strings("required")&&uid !in offer.strings("accepted")
    val canSocial=phase=="playing"&&!remote.busy&&remote.pending==null&&remote.error==null
    var emojiMenu by remember(room){mutableStateOf(false)}
    LaunchedEffect(canSocial,imeVisible){if(!canSocial||imeVisible)emojiMenu=false}
    var lastEmoji by remember(room){mutableLongStateOf(0L)}
    var activeReaction by remember(room){mutableStateOf<Pair<String,String>?>(null)}
    val reactionText=activeReaction?.second.orEmpty()
    // Track only the last event per player; the server keeps one event per player.
    val seen=remember(room){mutableMapOf<String,String>()}
    val incoming=social.optJSONObject("reactions")
    val eventKey=incoming?.toString().orEmpty()
    LaunchedEffect(room,eventKey){
        val events=incoming?.keys()?.asSequence()?.mapNotNull{p->incoming.optJSONObject(p)?.let{p to it}}?.toList().orEmpty()
        val latest=events.sortedByDescending{it.second.optLong("at")}.firstOrNull{(p,j)->j.optString("id").isNotBlank()&&seen[p]!=j.optString("id")}
        events.forEach{(p,j)->seen[p]=j.optString("id")}
        if(latest!=null){
            activeReaction=(latest.first+":"+latest.second.optString("id")) to latest.second.optString("symbol")
        }
    }
    // Expiring unrelated reactions must not cancel the currently visible reaction's timer.
    LaunchedEffect(room,activeReaction){
        if(activeReaction!=null){
            delay(1200L)
            activeReaction=null
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
                remaining?.let{
                    val urgent=phase=="playing"&&it in 0L..10L
                    Text("$it sn",color=if(urgent)Color(0xFFFFB4A8)else Wq122Gold,
                        fontSize=if(urgent)24.sp else 15.sp,fontWeight=FontWeight.Black)
                }
                Box{
                    TextButton(enabled=canSocial&&!imeVisible&&now-lastEmoji>=1500L,onClick={emojiMenu=true},contentPadding=PaddingValues(horizontal=6.dp)){Text("☺",color=Wq122Gold,fontSize=22.sp)}
                    DropdownMenu(expanded=emojiMenu&&canSocial&&!imeVisible,onDismissRequest={emojiMenu=false}){
                        listOf("👏","👍","😊","😄","😮","🔥","🎉","💪","🤔","😢","❤️","🏆").chunked(if(tablet)4 else 3).forEach{row->
                            Row(Modifier.padding(horizontal=6.dp)){
                                row.forEach{symbol->TextButton(enabled=canSocial,onClick={emojiMenu=false;lastEmoji=now;sendSocial("reaction",symbol=symbol)},modifier=Modifier.size(48.dp),contentPadding=PaddingValues(0.dp)){Text(symbol,fontSize=22.sp)}}
                            }
                        }
                    }
                }
                TextButton(enabled=canSocial&&!offerPending&&now>=(social.number("cooldownUntil")?:0),onClick={sendSocial("finish-offer")}){Text("Bitir",color=Wq122Gold)}
            }
            val scores=game.optJSONObject("scores")?:game.optJSONObject("stats")
            if(imeVisible&&!quiz&&!finished){
                val ids=connection.objects("players").map{it.optString("id")}
                val groups=if(kind=="team")game.objects("teams").map{it.optString("id")}else ids
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    groups.forEachIndexed{i,id->
                        val score=scores?.optJSONObject(id)?.optInt("score")?:scores?.optInt(id)?:0
                        val turn=game.optString("turn")==id
                        val name=if(kind=="team")"Takım "+(i+1) else if(id==uid)"Sen" else profiles.optJSONObject(id)?.optString("name")?.takeIf{it.isNotBlank()}?:"Rakip"
                        Text(name+" · "+score,color=if(turn)Wq122Gold else Wq122Ink,fontSize=12.sp,fontWeight=if(turn)FontWeight.Bold else FontWeight.Normal,modifier=Modifier.weight(1f),maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                val ids=connection.objects("players").map{it.optString("id")}
                val groups=if(kind=="team")game.objects("teams").map{it.optString("id")}else ids
                groups.forEachIndexed{i,id->
                    val value=scores?.optJSONObject(id)?.optInt("score")?:scores?.optInt(id)?:0
                    val currentTurn=game.optString("turn")==id||mode in listOf("speed","simultaneous")
                    Surface(color=Color(0xFF39245D),shape=MaterialTheme.shapes.medium,modifier=Modifier.weight(1f),
                        border=if(currentTurn&&phase=="playing")androidx.compose.foundation.BorderStroke(2.dp,Color(0xFFFFD56B))else null){
                        Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){
                            val members=if(kind=="team")game.objects("teams").firstOrNull{it.optString("id")==id}.strings("members") else listOf(id)
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                members.forEach{player->
                                    Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                                        Wq150Portrait(profiles.optJSONObject(player),ownAvatar.takeIf{player==uid},
                                            Modifier.fillMaxWidth().height(if(imeVisible)24.dp else if(tablet)110.dp else 64.dp))
                                        Text(profiles.optJSONObject(player)?.optString("name")?.takeIf{it.isNotBlank()}?:if(player==uid)session.profileName else "Oyuncu",
                                            color=Wq122Ink,fontSize=if(tablet)15.sp else 12.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            Text(if(kind=="coop")"Ortak" else value.toString(),color=Wq122Gold,fontWeight=FontWeight.Black)
                        }
                    }
                }
            }
            if(!quiz&&!finished){
                if(!imeVisible)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text(when(zoom){1->"Orta yakınlık";2->"Aktif kelime";else->"Tüm tahta"},color=Wq122Ink,modifier=Modifier.weight(1f),fontSize=12.sp)
                    TextButton(onClick={zoom=(zoom+1)%3},contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)){
                        Text(when(zoom){1->"Yakınlaştır +";2->"Tüm tahta";else->"Yakınlaştır"},color=Wq122Gold)
                    }
                }
                val typed=missing.zip(draft.map{it.toString()}).toMap()
                Wq150Board(words,index,typed+known,enabled&&(choose||free),{selected=it},zoom,Modifier.weight(1f))
            }
            Column(Modifier.fillMaxWidth().then(if(quiz||finished)Modifier.weight(1f)else Modifier.heightIn(max=320.dp))
                .verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(if(imeVisible&&!quiz)2.dp else 6.dp)){
                if(finished){
                    Wq150ResultPanel(view,uid,team,profiles,ownAvatar,session.profileName,onBack)
                }else{
                    Text(if(choose)"Çözeceğin kelimeyi seç" else prompt,color=Wq122Ink,fontWeight=FontWeight.Bold,maxLines=if(imeVisible&&!quiz)2 else Int.MAX_VALUE,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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
                            val direction=if(currentCells.size>=2&&currentCells[0].row==currentCells[1].row)"Yatay" else "Dikey"
                            if(!imeVisible)Text("Kelime "+(index+1)+" · "+direction+" · "+length+" harf",color=Wq122Gold,fontSize=12.sp)
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                OutlinedTextField(draft,{if(typingAllowed)draft=it.filter{c->c.isLetter()}.uppercase(java.util.Locale.ROOT).take(if(currentCells.isEmpty())totalLength else missing.size)},
                                    enabled=typingAllowed,readOnly=!enabled,singleLine=true,
                                    keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Characters,imeAction=ImeAction.Send),
                                    keyboardActions=KeyboardActions(onSend={if(enabled&&answerComplete&&answerText.isNotBlank())send("submit",answer=answerText)}),
                                    label={Text(if(imeVisible)"$length harf" else "Eksik harfler")},modifier=Modifier.weight(1f).focusRequester(focusRequester).onFocusChanged{if(it.isFocused)hadFocus=true})
                                Button(enabled=enabled&&answerComplete&&answerText.isNotBlank(),onClick={send("submit",answer=answerText)},contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp)){Text("Gönder")}
                            }
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                            val padding=PaddingValues(horizontal=4.dp,vertical=4.dp)
                            OutlinedButton(enabled=enabled,onClick={send("pass")},modifier=Modifier.weight(1f),contentPadding=padding){Text("Pas",fontSize=12.sp)}
                            OutlinedButton(enabled=enabled&&hintBudget>0,onClick={send(if(kind=="quiz")"eliminate" else "hint")},modifier=Modifier.weight(1f),contentPadding=padding){
                                Text((if(quiz)"Şık ele" else "Harf aç")+" ($hintBudget)",fontSize=12.sp)
                            }
                            if(kind=="duel")OutlinedButton(enabled=enabled,onClick={send("skip")},modifier=Modifier.weight(1f),contentPadding=padding){Text("Kelimeyi geç",fontSize=12.sp)}
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
        if(paused&&!finished&&!exit){
            val disconnected=connection.objects("players").filter{!it.optBoolean("connected")&&!it.optBoolean("removed")}
            val nextDeadline=disconnected.mapNotNull{it.number("until")}.minOrNull()
            val seconds=nextDeadline?.let{((it-now+999L)/1000L).coerceAtLeast(0L)}
            AlertDialog(onDismissRequest={},title={Text("Oyuncu bağlantısı kesildi")},
                text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("Maç duraklatıldı. Tahta, puanlar ve süre korunuyor.")
                    seconds?.let{Text("Geri dönüş süresi: $it sn",fontWeight=FontWeight.Bold)}
                }},confirmButton={TextButton(onClick={exit=true}){Text("Maçtan ayrıl")}})
        }
        if(exit)AlertDialog(onDismissRequest={if(!remote.busy)exit=false},title={Text("Arena'dan ayrıl?")},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(if(finished)"Arena'ya dönülsün mü?" else "Bilerek ayrılmak geri dönüş hakkını sonlandırır. Rakip hükmen kazanabilir.")
                remote.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            }},
            confirmButton={TextButton(enabled=!remote.busy,onClick={
                scope.launch{if(controller.leave()){exit=false;onBack()}}
            }){Text(if(remote.busy)"Doğrulanıyor…" else "Ayrıl")}},
            dismissButton={TextButton(enabled=!remote.busy,onClick={exit=false}){Text("Devam et")}})
    }
}
@Composable
internal fun Wq150ArenaFlow(session:AppSession,endpoint:Wq150Endpoint,back:()->Unit,account:()->Unit,legacy:()->Unit){
    val auth=session.accounts.socialAuth()
    val uid=session.accounts.uid.orEmpty()
    val context=androidx.compose.ui.platform.LocalContext.current
    val prefs=remember(context){context.getSharedPreferences("wq150_active_room",android.content.Context.MODE_PRIVATE)}
    val gateway=remember(endpoint,auth,uid){auth?.let{Wq150Gateway(endpoint,it)}}
    var room by remember(uid){mutableStateOf(if(uid.isBlank())null else prefs.getString(uid,null)?.takeIf(::wq150ValidRoom))}
    var initial by remember(uid){mutableStateOf<JSONObject?>(null)}
    var resumeError by remember(uid){mutableStateOf<String?>(null)}
    var retry by remember(uid){mutableIntStateOf(0)}
    fun clearRoom(){
        if(uid.isNotBlank())prefs.edit().remove(uid).apply()
        room=null;initial=null;resumeError=null
    }
    val id=room;val snapshot=initial
    LaunchedEffect(uid,id,retry,gateway){
        if(id==null||initial!=null||gateway==null)return@LaunchedEffect
        resumeError=null
        try{
            initial=gateway.view(id)
        }catch(e:kotlinx.coroutines.CancellationException){throw e}
        catch(e:Wq150GatewayError){
            if(e.status==404)clearRoom()
            else resumeError="Aktif maç doğrulanamadı. Bağlantını kontrol et."
        }catch(e:Exception){resumeError="Bağlantı kurulamadı. Maç kaydı korunuyor."}
    }
    if(id!=null&&snapshot!=null&&gateway!=null&&uid.isNotBlank())
        Wq150ArenaMatch(id,snapshot,uid,gateway,session,{clearRoom();back()})
    else if(id!=null&&gateway!=null){
        BackHandler{back()}
        Column(Modifier.fillMaxSize().background(Color(0xFF150B3E)).windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("Aktif maçına dön",color=Wq122Gold,style=MaterialTheme.typography.headlineSmall)
            if(resumeError==null){LinearProgressIndicator(Modifier.fillMaxWidth());Text("Maç durumu alınıyor…",color=Wq122Ink)}
            else{Text(resumeError.orEmpty(),color=Wq122Ink);Button(onClick={retry++}){Text("Tekrar dene")}}
            TextButton(onClick=back){Text("Geri · maç kaydını koru",color=Wq122Gold)}
        }
    }else Wq150ArenaEntry(session,endpoint,back,account,legacy){r,v->
        if(uid.isNotBlank()&&wq150ValidRoom(r)){prefs.edit().putString(uid,r).apply();room=r;initial=v}
    }
}

@Composable
private fun Wq150ResultPanel(view:JSONObject,uid:String,team:String,profiles:JSONObject,ownAvatar:String,ownName:String,onBack:()->Unit){
    val kind=view.optString("kind")
    val result=view.optJSONObject("result")
    val game=view.optJSONObject("game")?:JSONObject()
    val reason=result?.optString("kind").orEmpty()
    val winner=result?.optString("winner").orEmpty().takeUnless{it=="null"}.orEmpty()
    val won=winner.isNotBlank()&&(winner==uid||winner==team)
    val complete=kind=="coop"&&result?.optBoolean("completed")==true
    val title=when{
        reason=="mutual-finish"->"Karşılıklı anlaşmayla bitti"
        reason=="no-contest"->"Maç sonuçlanamadı"
        reason=="incomplete"->"Ortak bulmaca tamamlanmadı"
        complete->"Birlikte başardınız!"
        reason=="forfeit"->if(won)"Hükmen kazandın!" else "Hükmen sonuçlandı"
        won->"Kazandın!"
        winner.isNotBlank()->"Maç sona erdi"
        kind=="coop"->"Ortak bulmaca sona erdi"
        else->"Berabere"
    }
    val ids=view.optJSONObject("connection").objects("players").map{it.optString("id")}
    val winners=if(kind=="team")game.objects("teams").firstOrNull{it.optString("id")==winner}.strings("members") else listOf(winner).filter{it.isNotBlank()}
    val hero=if(complete)uid else winners.firstOrNull{it==uid}?:winners.firstOrNull()
    val summary=view.optJSONObject("summary")
    val playerStats=summary?.optJSONObject("players")?:if(kind=="team")game.optJSONObject("stats")?.optJSONObject("players") else game.optJSONObject("stats")
    val teamStats=summary?.optJSONObject("teams")?:game.optJSONObject("stats")?.optJSONObject("teams")
    fun name(id:String)=profiles.optJSONObject(id)?.optString("name")?.takeIf{it.isNotBlank()}?:if(id==uid)ownName else "Oyuncu "+(ids.indexOf(id)+1)
    var entered by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){entered=true}
    val scale by animateFloatAsState(if(entered)1f else .72f,animationSpec=spring(dampingRatio=.55f,stiffness=180f),label="arena-victory")
    Surface(color=Color(0xFF39245D),shape=MaterialTheme.shapes.large,modifier=Modifier.fillMaxWidth(),
        border=androidx.compose.foundation.BorderStroke(1.dp,Wq122Gold)){
        Column(Modifier.padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text("MAÇ SONUCU",color=Wq122Gold,fontSize=12.sp,fontWeight=FontWeight.Bold)
            if(hero!=null){
                Wq150Portrait(profiles.optJSONObject(hero),ownAvatar.takeIf{hero==uid},
                    Modifier.fillMaxWidth().height(180.dp).graphicsLayer{scaleX=scale;scaleY=scale},victory=true)
                Text(name(hero),color=Wq122Gold,fontWeight=FontWeight.Bold)
            }
            Text(title,color=Wq122Gold,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            val done=summary?.optInt("solved",-1)?:-1
            val total=summary?.optInt("total",0)?:0
            if(done>=0&&total>0){
                Text((if(kind=="quiz")"Tamamlanan soru" else "Çözülen kelime")+": "+done+"/"+total,color=Wq122Ink)
                LinearProgressIndicator(progress={ (done.toFloat()/total).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth(),color=Wq122Gold)
            }
        }
    }
    if(kind=="team")game.objects("teams").forEachIndexed{i,t->
        val id=t.optString("id")
        Wq150StatCard("Takım "+(i+1),teamStats?.optJSONObject(id),id==winner)
    }
    ids.forEach{id->Wq150StatCard(name(id),playerStats?.optJSONObject(id),id in winners)}
    Button(onClick=onBack,modifier=Modifier.fillMaxWidth()){Text("Arena'ya dön")}
}
@Composable
private fun Wq150StatCard(name:String,stats:JSONObject?,winner:Boolean){
    Surface(color=Color(0xFF39245D),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth(),
        border=if(winner)androidx.compose.foundation.BorderStroke(1.dp,Wq122Gold)else null){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text(name,color=Wq122Ink,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                if(stats?.has("score")==true)Text(stats.optInt("score").toString()+" puan",color=Wq122Gold,fontWeight=FontWeight.Black)
            }
            if(stats==null)Text("İstatistik henüz alınamadı.",color=Wq122Ink,fontSize=12.sp)
            else listOf("correct" to "Doğru","wrong" to "Yanlış","pass" to "Pas","timeout" to "Süre doldu","skip" to "Geçilen","hints" to "İpucu","spent" to "Takım ipucu")
                .filter{stats.has(it.first)}.chunked(3).forEach{row->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                        row.forEach{(key,label)->
                            Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                                Text(stats.optInt(key).toString(),color=if(key=="correct")Color(0xFF91E8AA)else if(key=="wrong")Color(0xFFFFB4B4)else Wq122Gold,fontWeight=FontWeight.Bold)
                                Text(label,color=Wq122Ink,fontSize=12.sp)
                            }
                        }
                        repeat(3-row.size){Spacer(Modifier.weight(1f))}
                    }
                }
        }
    }
}

/** Count deltas rather than replaying a sound on every poll. */
internal fun wq150Feedback(previous:Pair<Int,Int>?,current:Pair<Int,Int>?):Int{
    if(previous==null||current==null)return 0
    return when{
        current.first>previous.first->1
        current.second>previous.second->-1
        else->0
    }
}
@Composable
private fun Wq150Audio(room:String,phase:String,remaining:Long?,deadline:Long?,feedback:JSONObject?,effects:Boolean){
    val context=androidx.compose.ui.platform.LocalContext.current
    val ambient=Wq179ArenaSoundPrefs.ambient(context)
    val clockEnabled=Wq179ArenaSoundPrefs.clock(context)
    val pack=Wq150SoundPacks.selected(context)
    val latestRemaining by rememberUpdatedState(remaining)
    val player=remember(room){Wq150SoundPlayer(context)}
    DisposableEffect(player){onDispose{player.close()}}
    LaunchedEffect(phase,effects,pack){if(phase=="paused"||!effects||pack=="silent")player.stop()}
    val current=feedback?.let{it.optInt("correct") to it.optInt("wrong")}
    var previous by remember(room){mutableStateOf(current)}
    var lastTick by remember(room){mutableStateOf<Pair<Long,Long>?>(null)}
    LaunchedEffect(current,phase,effects){
        val cue=wq150Feedback(previous,current)
        if(current!=null)previous=current
        if(effects&&phase!="paused"&&cue!=0)player.play(pack,if(cue>0)"correct" else "wrong")
    }
    LaunchedEffect(remaining,deadline,phase,effects,clockEnabled){
        if(phase=="playing"&&remaining!=null&&deadline!=null&&remaining in 1L..10L){
            val key=deadline to remaining
            if(lastTick!=key){lastTick=key;if(effects&&clockEnabled)player.play(pack,if(remaining<=3)"urgent" else "tick")}
        }
    }
    LaunchedEffect(phase,effects,ambient,pack){
        while(phase=="playing"&&effects&&ambient&&pack!="silent"){
            delay(12000L)
            val seconds=latestRemaining
            if(seconds==null||seconds>10)player.play(pack,"ambient")
        }
    }
}

/** Separate v150 local archive; never reinterpret a four-player result as a v121 duel. */
internal object Wq150History{
    private fun prefs(context:android.content.Context)=context.getSharedPreferences("wq_arena_finished_v150",android.content.Context.MODE_PRIVATE)
    fun read(context:android.content.Context,profile:String):List<JSONObject>{
        val a=runCatching{JSONArray(prefs(context).getString(profile,"[]")?:"[]")}.getOrElse{JSONArray()}
        return (0 until a.length()).mapNotNull{a.optJSONObject(it)}
    }
    fun entry(room:String,view:JSONObject,profiles:JSONObject,uid:String,ownName:String):JSONObject?{
        if(room.isBlank()||view.optString("phase")!="finished")return null
        val summary=view.optJSONObject("summary")?:return null
        val stats=summary.optJSONObject("players")?:return null
        val ids=view.optJSONObject("connection").objects("players").map{it.optString("id")}.filter{it.isNotBlank()}.distinct()
        if(uid !in ids||ids.size !in 2..4)return null
        val players=JSONArray()
        ids.forEach{id->
            val name=if(id==uid)ownName else profiles.optJSONObject(id)?.optString("name")?.takeIf{it.isNotBlank()}?:"Oyuncu"
            val player=JSONObject().put("id",id).put("name",name)
            stats.optJSONObject(id)?.let{s->
                if(s.has("score")&&!s.isNull("score"))player.put("score",s.optInt("score"))
            }
            players.put(player)
        }
        return JSONObject().put("room",room).put("kind",view.optString("kind")).put("players",players)
            .put("at",view.optLong("serverNow",System.currentTimeMillis()))
    }
    fun record(context:android.content.Context,profile:String,room:String,view:JSONObject,profiles:JSONObject,uid:String,ownName:String){
        val entry=entry(room,view,profiles,uid,ownName)?:return
        val old=read(context,profile)
        val previous=old.firstOrNull{it.optString("room")==room}
        if(previous!=null){
            entry.put("at",previous.optLong("at"))
            // Do not replace a previously fetched name with a temporary placeholder.
            val known=previous.objects("players").associate{it.optString("id") to it.optString("name")}
            entry.objects("players").forEach{p->
                if(p.optString("name")=="Oyuncu")known[p.optString("id")]?.let{p.put("name",it)}
            }
            if(entry.toString()==previous.toString())return
        }
        val updated=JSONArray()
        (listOf(entry)+old.filterNot{it.optString("room")==room}).sortedByDescending{it.optLong("at")}.take(100).forEach{updated.put(it)}
        prefs(context).edit().putString(profile,updated.toString()).apply()
    }
}
@Composable
internal fun Wq150Records(context:android.content.Context,profile:String,ranking:Boolean){
    val records=remember(context,profile){Wq150History.read(context,profile)}
    Text(if(ranking)"Yeni Arena · bu cihazda kayıtlı puanlar (genel sıralama değil)" else "Yeni Arena · tamamlanan maçlar",color=Wq122Gold,fontWeight=FontWeight.Bold)
    if(records.isEmpty())Text("Henüz yeni Arena maç kaydı yok.",color=Wq122Ink)
    else if(ranking){
        val totals=mutableMapOf<String,Pair<String,Int>>()
        records.asReversed().forEach{r->r.objects("players").filter{it.has("score")}.forEach{p->
            val id=p.optString("id")
            totals[id]=p.optString("name") to ((totals[id]?.second?:0)+p.optInt("score"))
        }}
        if(totals.isEmpty())Text("Puanı olan maç kaydı yok.",color=Wq122Ink)
        totals.values.sortedByDescending{it.second}.forEachIndexed{index,p->Text("${index+1}. ${p.first} — ${p.second} puan",color=Wq122Ink)}
    }else records.forEach{r->
        Text(Wq150ModeCatalog.kinds[r.optString("kind")].orEmpty(),color=Wq122Gold,fontWeight=FontWeight.Bold)
        r.objects("players").forEach{p->Text(p.optString("name")+if(p.has("score"))" · "+p.optInt("score")+" puan" else "",color=Wq122Ink)}
        Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(r.optLong("at"))),color=Wq122Ink,fontSize=12.sp)
        HorizontalDivider()
    }
}
