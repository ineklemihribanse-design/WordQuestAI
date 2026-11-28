package com.kelimebulmacasi.a1

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
internal fun wq150Viewport(width:Float,height:Float,rows:Int,cols:Int,zoom:Int,focusRow:Float,focusCol:Float,fitHeight:Float=height,widthFraction:Float=0f):Wq150Viewport{
    require(width>0&&height>0&&rows>0&&cols>0&&zoom in 0..2)
    val fullFit=minOf(width/cols,fitHeight.coerceAtLeast(height)/rows,56f)
    val fit=if(widthFraction>0f)(width*widthFraction.coerceIn(0f,1f)/cols).coerceAtMost(72f) else fullFit
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
    selectable:Boolean,onSelect:(Int)->Unit,zoom:Int,imeVisible:Boolean,wideDefault:Boolean,modifier:Modifier=Modifier){
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
        // Preserve the measured non-IME scale; the smaller visible area becomes a pannable viewport.
        var fullHeight by remember(width){mutableFloatStateOf(height)}
        SideEffect{if(!imeVisible&&fullHeight!=height)fullHeight=height}
        val viewport=wq150Viewport(width,height,rows,cols,if(wideDefault&&zoom==1)0 else zoom,focusRow,focusCol,
            if(imeVisible)fullHeight else height,widthFraction=if(wideDefault&&zoom!=1).92f else 0f)
        var x by remember(zoom,selected,width,height,rows,cols,viewport.cell){mutableFloatStateOf(viewport.x)}
        var y by remember(zoom,selected,width,height,rows,cols,viewport.cell){mutableFloatStateOf(viewport.y)}
        val size=viewport.cell.dp
        val drag=if(viewport.minX==viewport.maxX&&viewport.minY==viewport.maxY)Modifier else Modifier.pointerInput(zoom,selected,width,height,rows,cols,viewport.cell){
            detectDragGestures{change,amount->
                change.consume()
                x=(x+amount.x/density).coerceIn(viewport.minX,viewport.maxX)
                y=(y+amount.y/density).coerceIn(viewport.minY,viewport.maxY)
            }
        }
        Box(Modifier.fillMaxSize().then(drag)){
            Canvas(Modifier.matchParentSize()){
                val step=viewport.cell*density
                val left=x*density;val top=y*density
                val w=cols*step;val h=rows*step
                drawRect(Color(0xA8E5DCEF),topLeft=Offset(left,top),size=Size(w,h))
                for(c in 0..cols)drawLine(Color.White.copy(alpha=.30f),Offset(left+c*step,top),Offset(left+c*step,top+h),strokeWidth=1f)
                for(r in 0..rows)drawLine(Color.White.copy(alpha=.30f),Offset(left,top+r*step),Offset(left+w,top+r*step),strokeWidth=1f)
            }
            cells.forEach{p->
                val candidates=owners[p].orEmpty()
                val i=candidates.firstOrNull{it==selected}?:candidates.firstOrNull()?:-1
                val label=fixed[p].orEmpty()
                val px=x+(p.col-c0)*viewport.cell;val py=y+(p.row-r0)*viewport.cell
                if(px+viewport.cell>0&&px<width&&py+viewport.cell>0&&py<height){
                    Box(Modifier.offset{IntOffset((px*density).roundToInt(),(py*density).roundToInt())}.size(size)
                        .background(if(p in active)Color(0xFFE0D2FF)else Color(0xF5FFFDF6),RoundedCornerShape(3.dp))
                        .border(if(p in active)1.5.dp else 0.65.dp,if(p in active)Color(0xFF7851C2)else Color(0xFF9B8DAE),RoundedCornerShape(3.dp))
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

internal fun wq150SkipRemaining(limit:Int,used:Int,round:Int):Int=if(round==1)(limit-used).coerceAtLeast(0) else 0

internal fun wq150ArenaPlayerName(vararg choices:String?):String=choices.firstOrNull{!it.isNullOrBlank()&&!it.trim().equals("Oyuncu",true)}?.trim()?:"Rakip"

internal fun wq150FullWordDraft(input:String,length:Int):String =
    input.filter{it.isLetter()}.uppercase(java.util.Locale.ROOT).take(length.coerceAtLeast(0))

internal fun wq150AutomaticAnswer(known:List<String?>,draft:String,total:Int):String?{
    if(total<=0)return null
    if(draft.length==total)return draft
    if(known.size!=total)return null
    val missing=known.count{it==null}
    if(missing>=total||draft.length!=missing)return null
    return wq150CompleteAnswer(known,draft,true)
}

internal fun wq150PreviewMissing(known:List<String?>,draft:String,total:Int):Boolean{
    if(known.size!=total||draft.length>=total)return false
    val missing=known.count{it==null}
    if(missing>=total||draft.length>missing)return false
    if(draft.length==missing)return true
    val fixedPrefix=known.take(draft.length).mapIndexedNotNull{i,c->c?.let{i to it}}
    return fixedPrefix.isEmpty()||fixedPrefix.any{(i,c)->draft[i].toString()!=c}
}

internal fun wq150CompleteAnswer(known:List<String?>,draft:String,missingOnly:Boolean):String{
    if(!missingOnly||known.isEmpty())return draft
    var at=0
    return known.joinToString(""){it?:draft.getOrNull(at++)?.toString().orEmpty()}
}

@Composable
private fun Wq150CenterClock(remaining:Long?,phase:String,tablet:Boolean,compact:Boolean){
    val urgent=phase=="playing"&&remaining!=null&&remaining in 0L..10L
    val accent=if(urgent)Color(0xFFFF907D)else Color(0xFF52DDEB)
    val diameter=if(compact)46.dp else if(tablet)100.dp else 65.dp
    Surface(modifier=Modifier.size(diameter),shape=CircleShape,color=Color(0xF2222C68),
        border=androidx.compose.foundation.BorderStroke(if(urgent)3.dp else 2.dp,accent)){
        Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
            val display=remaining?.let{if(it>=60L)"%d:%02d".format(java.util.Locale.ROOT,it/60L,it%60L)else it.toString()}?:"∞"
            Text(display,color=if(urgent)accent else Color.White,
                fontSize=if(compact){if(urgent)20.sp else 15.sp}else if(tablet){if(urgent)40.sp else 30.sp}else{if(urgent)27.sp else 20.sp},
                fontWeight=FontWeight.Black,maxLines=1)
            if(!compact)Text(if(remaining==null)"Süresiz" else if(remaining<60L)"saniye" else "Süre",color=accent,fontSize=if(tablet)12.sp else 10.sp,maxLines=1)
        }
    }
}

/** All actions use the current authenticated server snapshot; no local scoring. */
@Composable
internal fun Wq150ArenaMatch(room:String,initial:JSONObject,uid:String,gateway:Wq150Gateway,session:AppSession,onBack:()->Unit,onTeacher:(String)->Unit){
    val profiles=rememberWq150Profiles(room,session,gateway)
    val ownAvatar by rememberWq2DSelection(session.profileKey)
    val config=androidx.compose.ui.platform.LocalConfiguration.current
    val tablet=config.smallestScreenWidthDp>=600
    val split=tablet&&config.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
    val reviewPrefs=remember(room,uid){context.getSharedPreferences("wq150_review_"+session.profileKey,android.content.Context.MODE_PRIVATE)}
    var localReview by remember(room,uid){mutableStateOf(runCatching{JSONObject(reviewPrefs.getString(room+":"+uid,"{}")?:"{}")}.getOrElse{JSONObject()})}
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
    val totalLength=entry?.optInt("length",entry.optInt("letterCount"))?.takeIf{it>0}?:currentCells.size
    val skipLeft=wq150SkipRemaining(settings.optInt("skipLimit",0),game.optJSONObject("stats")?.optJSONObject(uid)?.optInt("skip",0)?:0,game.optInt("round",1))
    val hintBudget=when(kind){
        "quiz" -> mine?.optInt("hintsLeft")?:0
        "coop" -> (game.optInt("hints")-game.optInt("spent")).coerceAtLeast(0)
        else -> game.optInt("hintsLeft")
    }
    var draft by remember(room,index){mutableStateOf("")}
    // One field accepts a complete word or only its unknown letters.
    val missing=currentCells.filterNot{it in known}
    val missingCount=missing.size
    val knownLetters=currentCells.map{known[it]}
    val answerText=wq150AutomaticAnswer(knownLetters,draft,totalLength)
    val answerComplete=answerText!=null
    val previewCells=if(wq150PreviewMissing(knownLetters,draft,totalLength))missing else currentCells
    var inspected by remember(room){mutableStateOf<List<String>>(emptyList())}
    var actionFeedback by remember(room){mutableStateOf("")}
    var exit by remember(room){mutableStateOf(false)}
    var disconnectExit by remember(room){mutableStateOf(false)}
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
    LaunchedEffect(canSocial){if(!canSocial)emojiMenu=false}
    var lastEmoji by remember(room){mutableLongStateOf(0L)}
    var activeReaction by remember(room){mutableStateOf<Pair<String,String>?>(null)}
    val reactionText=activeReaction?.second.orEmpty()
    val reactionTravel=remember(room){Animatable(0f)}
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
            reactionTravel.snapTo(0f)
            reactionTravel.animateTo(1f,tween(1000))
            delay(200L)
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
    val feedback=view.optJSONObject("feedback")
    var previousWrong by remember(room){mutableIntStateOf(feedback?.optInt("wrong")?:0)}
    LaunchedEffect(feedback?.optInt("wrong")){
        val wrong=feedback?.optInt("wrong")?:0
        if(wrong>previousWrong){draft="";actionFeedback="Yanlış cevap"}
        previousWrong=wrong
    }
    LaunchedEffect(actionFeedback){if(actionFeedback.isNotBlank()){delay(1100);actionFeedback=""}}
    fun send(type:String,answer:String?=null,choice:Int?=null){
        if(!enabled)return
        val command=Wq150Command(view.optLong("revision"),type,
            word=if(kind in listOf("duel","coop"))index else null,
            index=if(kind=="quiz")index else null,item=if(kind=="team")index else null,
            answer=answer,choice=choice)
        if(command.valid())scope.launch{
            val before=view.optJSONObject("feedback")
            val reviewClue=prompt
            controller.submit(command)
            val confirmed=controller.state.value
            if(confirmed.pending==null&&confirmed.error==null&&
                (confirmed.snapshot?.optLong("revision")?:-1L)>command.revision){
                val after=confirmed.snapshot?.optJSONObject("feedback")
                val wrong=(after?.optInt("wrong")?:0)>(before?.optInt("wrong")?:0)
                val correct=(after?.optInt("correct")?:0)>(before?.optInt("correct")?:0)
                if(reviewClue.isNotBlank()&&(wrong||correct||type in listOf("pass","skip"))){
                    val copy=JSONObject(localReview.toString())
                    val old=copy.optJSONObject(index.toString())
                    val status=when{
                        wrong||old?.optString("kind")=="wrong"->"wrong"
                        correct->"correct"
                        old?.optString("kind")=="correct"->"correct"
                        else->"blank"
                    }
                    copy.put(index.toString(),JSONObject().put("index",index).put("kind",status).put("clue",reviewClue))
                    localReview=copy
                    reviewPrefs.edit().putString(room+":"+uid,copy.toString()).apply()
                }
                if(type in listOf("submit","pass","skip"))draft=""
                if(type=="pass")actionFeedback="Pas geçildi"
                if(type=="skip")actionFeedback="Sonraki kelime"
            }
        }
    }
    val playerIds=connection.objects("players").map{it.optString("id")}
    val directoryNames=rememberWq150ArenaNames(session,playerIds)
    fun playerName(id:String):String=wq150ArenaPlayerName(
        if(id==uid)session.social.nickname.ifBlank{session.profileName} else null,
        directoryNames[id],
        profiles.optJSONObject(id)?.optString("name"),
        session.social.friends.firstOrNull{it.uid==id}?.name,
        if(id==uid)"Sen" else "Rakip "+(playerIds.indexOf(id)+1).coerceAtLeast(1))
    val emojiButton:@Composable ()->Unit={
        Box{
                    TextButton(enabled=canSocial&&now-lastEmoji>=1500L,onClick={emojiMenu=true},modifier=Modifier.size(if(imeVisible)32.dp else 36.dp),contentPadding=PaddingValues(0.dp)){Text("☺",color=Wq122Gold,fontSize=if(imeVisible)18.sp else 22.sp)}
                    DropdownMenu(expanded=emojiMenu&&canSocial,onDismissRequest={emojiMenu=false}){
                        listOf("👏","👍","😊","😄","😮","🔥","🎉","💪","🤔","😢","❤️","🏆").chunked(if(tablet)4 else 3).forEach{row->
                            Row(Modifier.padding(horizontal=6.dp)){
                                row.forEach{symbol->TextButton(enabled=canSocial,onClick={emojiMenu=false;lastEmoji=now;sendSocial("reaction",symbol=symbol)},modifier=Modifier.size(48.dp),contentPadding=PaddingValues(0.dp)){Text(symbol,fontSize=22.sp)}}
                            }
                        }
                    }
                }
    }
    BackHandler{if(imeVisible)focusManager.clearFocus() else {disconnectExit=paused;exit=true}}
    MaterialTheme(colorScheme=Wq122Scheme){
        Box(Modifier.fillMaxSize().clipToBounds().background(Color(0xFF150B3E))){
        Wq183ArenaBackdrop(Modifier.matchParentSize(),active=true)
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding().padding(horizontal=if(tablet)10.dp else 8.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(3.dp)){
            Row(Modifier.fillMaxWidth().heightIn(max=44.dp),verticalAlignment=Alignment.CenterVertically){
                TextButton(onClick={disconnectExit=paused;exit=true}){Text("‹ Geri",color=Wq122Gold)}
                Text(Wq150ModeCatalog.kinds[kind].orEmpty(),color=Wq122Gold,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
                TextButton(enabled=canSocial&&!offerPending&&now>=(social.number("cooldownUntil")?:0),onClick={sendSocial("finish-offer")}){Text("Bitir",color=Wq122Gold)}
            }
            if(!finished){
            val scores=game.optJSONObject("scores")?:game.optJSONObject("stats")
            val ids=playerIds
            val groups=(if(kind=="team")game.objects("teams").map{it.optString("id")}else ids)
                .sortedBy{if(it==(if(kind=="team")team else uid))0 else 1}
            val compact=imeVisible&&!quiz&&!finished
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(if(compact)4.dp else 8.dp)){
                if(groups.isEmpty()){
                    Spacer(Modifier.weight(1f))
                    Wq150CenterClock(remaining,phase,tablet,compact)
                    Spacer(Modifier.weight(1f))
                }
                groups.forEachIndexed{i,id->
                    if(i==1)Wq150CenterClock(remaining,phase,tablet,compact)
                    val value=scores?.optJSONObject(id)?.optInt("score")?:scores?.optInt(id)?:0
                    val currentTurn=game.optString("turn")==id||mode in listOf("speed","simultaneous")
                    val members=if(kind=="team")game.objects("teams").firstOrNull{it.optString("id")==id}.strings("members") else listOf(id)
                    val name=if(kind=="team")members.joinToString(" / "){playerName(it)} else playerName(id)
                    val myCard=uid in members
                    Surface(color=if(currentTurn&&phase=="playing")Color(0xE65A368C)else Color(0xD93D2569),
                        shape=RoundedCornerShape(16.dp),modifier=Modifier.weight(1f).clickable{inspected=members},
                        border=androidx.compose.foundation.BorderStroke(if(currentTurn&&phase=="playing")2.dp else 1.dp,
                            if(currentTurn&&phase=="playing")Color(0xFFFFD56B)else Color(0xFF9D84D6))){
                        if(compact){
                            Row(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=2.dp),verticalAlignment=Alignment.CenterVertically){
                                if(split)Wq150Portrait(profiles.optJSONObject(members.firstOrNull().orEmpty()),ownAvatar.takeIf{members.firstOrNull()==uid},Modifier.size(36.dp))
                                Text("$name · $value",Modifier.weight(1f),color=if(currentTurn)Wq122Gold else Color.White,
                                    fontWeight=FontWeight.Bold,fontSize=12.sp,textAlign=TextAlign.Center,maxLines=1,overflow=TextOverflow.Ellipsis)
                                if(myCard)emojiButton()
                            }
                        }else{
                            Row(Modifier.fillMaxWidth().padding(if(tablet)8.dp else 4.dp),verticalAlignment=Alignment.CenterVertically){
                                Row(Modifier.weight(1.15f),horizontalArrangement=Arrangement.spacedBy(2.dp)){
                                    members.forEach{player->
                                        Wq150Portrait(profiles.optJSONObject(player),ownAvatar.takeIf{player==uid},
                                            Modifier.weight(1f).height(if(split)76.dp else if(tablet)100.dp else 76.dp))
                                    }
                                }
                                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                                    Text(name,color=Color.White,fontSize=if(tablet)17.sp else 12.sp,fontWeight=FontWeight.SemiBold,
                                        textAlign=TextAlign.Center,maxLines=1,overflow=TextOverflow.Ellipsis)
                                    Text(if(kind=="coop")"Ortak" else value.toString(),color=Wq122Gold,fontSize=if(tablet)24.sp else 20.sp,fontWeight=FontWeight.Black)
                                    if(myCard)emojiButton()
                                }
                            }
                        }
                    }
                    if(groups.size==1)Wq150CenterClock(remaining,phase,tablet,compact)
                }
            }
            }
            val boardContent:@Composable (Modifier)->Unit={area->
              Column(area.clipToBounds(),verticalArrangement=Arrangement.spacedBy(2.dp)){
                if(!imeVisible||split)Row(Modifier.fillMaxWidth().height(32.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(when(zoom){1->"Tüm tahta";2->"Aktif kelime";else->"Geniş tahta"},color=Wq122Ink,modifier=Modifier.weight(1f),fontSize=12.sp)
                    TextButton(onClick={zoom=(zoom+1)%3},contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)){
                        Text(when(zoom){1->"Yakınlaştır";2->"Geniş tahta";else->"Tüm tahta"},color=Wq122Gold)
                    }
                }
                val typed=previewCells.zip(draft.map{it.toString()}).toMap()
                Wq150Board(words,index,typed+known,enabled&&(choose||free),{selected=it},zoom,imeVisible,true,Modifier.weight(1f))
              }
            }
            val answerContent:@Composable (Modifier)->Unit={area->
              Column(area
                .verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(if(imeVisible&&!quiz)2.dp else 6.dp)){
                if(finished){
                    Wq150ResultPanel(view,uid,team,profiles,ownAvatar,::playerName,localReview,onBack,onTeacher)
                }else{
                    Text(if(choose)"Çözeceğin kelimeyi seç" else prompt,modifier=Modifier.fillMaxWidth(),color=Wq122Ink,fontWeight=FontWeight.Bold,fontSize=if(quiz||tablet)16.sp else 14.sp,maxLines=if(quiz)Int.MAX_VALUE else 3,overflow=TextOverflow.Ellipsis)
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
                            Text("Kelime "+(index+1)+" · "+direction+" · "+length+" harf",color=Wq122Gold,fontSize=12.sp,maxLines=1)
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                OutlinedTextField(draft,{if(typingAllowed&&!remote.busy)draft=wq150FullWordDraft(it,totalLength)},
                                    enabled=typingAllowed,readOnly=!typingAllowed,singleLine=true,
                                    keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Characters,imeAction=ImeAction.Send),
                                    keyboardActions=KeyboardActions(onSend={if(enabled&&answerComplete&&!answerText.isNullOrBlank())send("submit",answer=answerText)}),
                                    label={Text("Kelimeyi yaz",maxLines=1,fontSize=12.sp)},colors=OutlinedTextFieldDefaults.colors(focusedContainerColor=Color(0xDD24183E),unfocusedContainerColor=Color(0xDD24183E),focusedTextColor=Color.White,unfocusedTextColor=Color.White),modifier=Modifier.weight(1f).focusRequester(focusRequester).onFocusChanged{if(it.isFocused)hadFocus=true})
                                Button(enabled=enabled&&answerComplete&&!answerText.isNullOrBlank(),onClick={send("submit",answer=answerText)},contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp)){Text("Gönder")}
                            }
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                            val padding=PaddingValues(horizontal=4.dp,vertical=4.dp)
                            OutlinedButton(enabled=enabled,onClick={send("pass")},modifier=Modifier.weight(1f),contentPadding=padding){Text("Pas geç",fontSize=12.sp,maxLines=1)}
                            OutlinedButton(enabled=enabled&&hintBudget>0,onClick={send(if(kind=="quiz")"eliminate" else "hint")},modifier=Modifier.weight(1f),contentPadding=padding){
                                Text((if(quiz)"Şık ele" else "Harf aç")+" ($hintBudget)",fontSize=12.sp)
                            }
                            if(kind=="duel"&&skipLeft>0)OutlinedButton(enabled=enabled,onClick={send("skip")},modifier=Modifier.weight(1f),contentPadding=padding){Text("Kelimeyi geç ($skipLeft)",fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}
                        }
                        if(kind=="quiz")Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            OutlinedButton(enabled=enabled&&hintBudget>0&&"half" !in mine.strings("used"),onClick={send("half")}){Text("50:50")}
                            OutlinedButton(enabled=enabled&&hintBudget>0&&deadline!=null&&"time" !in mine.strings("used"),onClick={send("time")}){Text("+10 sn")}
                        }
                    }
                    if(!eligible&&!paused)Text("Rakibin sırası",color=Wq122Ink,fontSize=12.sp,maxLines=1)
                }
                if(offerPending&&offeredByMe)TextButton(enabled=canSocial,onClick={sendSocial("finish-withdraw",id=offerId)}){Text("Bitirme teklifini geri çek",color=Wq122Gold)}
                remote.error?.let{Text(it,color=Color(0xFFFFB4B4))}
                if(remote.pending!=null&&!remote.busy)TextButton(onClick={scope.launch{controller.retryPending()}}){Text("İşlemi tekrar dene")}
            }
            }
            if(quiz||finished)answerContent(Modifier.weight(1f).fillMaxWidth())
            else if(split){
                Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
                    boardContent(Modifier.weight(1.9f).fillMaxHeight())
                    Surface(Modifier.weight(1f).fillMaxHeight(),color=Color(0xBC251747),shape=RoundedCornerShape(14.dp)){
                        answerContent(Modifier.fillMaxSize().padding(8.dp))
                    }
                }
            }else{
                boardContent(Modifier.weight(1f).fillMaxWidth())
                answerContent(Modifier.fillMaxWidth().heightIn(max=if(imeVisible)164.dp else 240.dp))
            }
        }
        if(reactionText.isNotEmpty()){
            BoxWithConstraints(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top=if(tablet)102.dp else 78.dp)){
                val sender=activeReaction?.first?.substringBefore(':').orEmpty()
                val fromLeft=sender==uid||(kind=="team"&&game.objects("teams").firstOrNull{it.optString("id")==team}.strings("members").contains(sender))
                val travel=if(fromLeft)reactionTravel.value else 1f-reactionTravel.value
                val x=(maxWidth-48.dp)*(.15f+.70f*travel)
                Text(reactionText,Modifier.offset(x=x,y=(-18f*kotlin.math.sin(reactionTravel.value*Math.PI).toFloat()).dp).size(48.dp),fontSize=32.sp,textAlign=TextAlign.Center)
            }
        }
        if(remote.busy)LinearProgressIndicator(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(2.dp))
        if(actionFeedback.isNotBlank())Surface(Modifier.align(Alignment.TopCenter).padding(top=48.dp),color=Color(0xEE251347),shape=RoundedCornerShape(12.dp)){
            Text(actionFeedback,Modifier.padding(horizontal=12.dp,vertical=6.dp),color=Wq122Gold,fontSize=13.sp)
        }
        if(noticeText.isNotBlank())Surface(modifier=Modifier.align(Alignment.TopCenter).padding(top=100.dp),color=Color(0xEE251347),shape=MaterialTheme.shapes.medium){
            Text(noticeText,Modifier.padding(12.dp),color=Wq122Ink)
        }
        }
        if(inspected.isNotEmpty())AlertDialog(onDismissRequest={inspected=emptyList()},containerColor=Color(0xFF251747),
            title={Text("Oyuncu profili",color=Wq122Gold)},
            text={Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
                inspected.forEach{id->
                    Wq150Portrait(profiles.optJSONObject(id),ownAvatar.takeIf{id==uid},Modifier.fillMaxWidth().height(160.dp))
                    Text(playerName(id),color=Wq122Gold,fontWeight=FontWeight.Bold,fontSize=18.sp)
                    val stats=if(kind=="team")game.optJSONObject("stats")?.optJSONObject("players")?.optJSONObject(id) else game.optJSONObject("stats")?.optJSONObject(id)
                    Wq150StatCard(playerName(id),stats,false)
                }
            }},confirmButton={TextButton(onClick={inspected=emptyList()}){Text("Kapat",color=Wq122Gold)}})
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
                }},confirmButton={TextButton(onClick={disconnectExit=true;exit=true}){Text("Maçtan ayrıl")}})
        }
        if(exit)AlertDialog(onDismissRequest={if(!remote.busy)exit=false},title={Text("Arena'dan ayrıl?")},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(if(finished)"Arena'ya dönülsün mü?" else if(disconnectExit)"Bağlantı kesintisi nedeniyle maçı sonuçsuz bitirmek ister misin? Hükmen ayrılma isteği gönderilmeyecek." else "Bilerek ayrılmak geri dönüş hakkını sonlandırır. Rakip hükmen kazanabilir.")
                remote.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            }},
            confirmButton={TextButton(enabled=!remote.busy,onClick={
                scope.launch{if(controller.leave(neutral=disconnectExit)){exit=false;onBack()}}
            }){Text(if(remote.busy)"Doğrulanıyor…" else "Ayrıl")}},
            dismissButton={TextButton(enabled=!remote.busy,onClick={exit=false}){Text("Devam et")}})
    }
}
@Composable
internal fun Wq150ArenaFlow(session:AppSession,endpoint:Wq150Endpoint,back:()->Unit,account:()->Unit,legacy:()->Unit,
    soloSelected:Boolean,onTab:(Boolean)->Unit,onSolo:(WqArenaSoloLaunch)->Unit,onTeacher:(String)->Unit){
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
        Wq150ArenaMatch(id,snapshot,uid,gateway,session,{clearRoom();back()},{prompt->clearRoom();onTeacher(prompt)})
    else if(id!=null&&gateway!=null){
        BackHandler{back()}
        Column(Modifier.fillMaxSize().background(Color(0xFF150B3E)).windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("Aktif maçına dön",color=Wq122Gold,style=MaterialTheme.typography.headlineSmall)
            if(resumeError==null){LinearProgressIndicator(Modifier.fillMaxWidth());Text("Maç durumu alınıyor…",color=Wq122Ink)}
            else{Text(resumeError.orEmpty(),color=Wq122Ink);Button(onClick={retry++}){Text("Tekrar dene")}}
            TextButton(onClick=back){Text("Geri · maç kaydını koru",color=Wq122Gold)}
        }
    }else Wq150ArenaEntry(session,endpoint,back,account,legacy,soloSelected,onTab,onSolo){r,v->
        if(uid.isNotBlank()&&wq150ValidRoom(r)){prefs.edit().putString(uid,r).apply();room=r;initial=v}
    }
}

@Composable
private fun Wq150ResultPanel(view:JSONObject,uid:String,team:String,profiles:JSONObject,ownAvatar:String,name:(String)->String,
    localReview:JSONObject,onBack:()->Unit,onTeacher:(String)->Unit){
    val kind=view.optString("kind")
    val result=view.optJSONObject("result")
    val game=view.optJSONObject("game")?:JSONObject()
    val reason=result?.optString("kind").orEmpty()
    val winner=result?.optString("winner").orEmpty().takeUnless{it=="null"}.orEmpty()
    val ids=view.optJSONObject("connection").objects("players").map{it.optString("id")}.sortedBy{if(it==uid)0 else 1}
    val winners=if(kind=="team")game.objects("teams").firstOrNull{it.optString("id")==winner}.strings("members") else listOf(winner).filter{it.isNotBlank()}
    val complete=kind=="coop"&&result?.optBoolean("completed")==true
    val title=when{
        reason=="mutual-finish"->"Karşılıklı anlaşmayla bitti"
        reason=="no-contest"->"Maç sonuçsuz bitti"
        complete->"Birlikte başardınız!"
        reason=="incomplete"->"Ortak bulmaca tamamlanmadı"
        winner.isNotBlank()->winners.joinToString(" & "){name(it)}+if(reason=="forfeit")" hükmen kazandı!" else " kazandı!"
        kind=="coop"->"Ortak bulmaca sona erdi"
        else->"Berabere"
    }
    val summary=view.optJSONObject("summary")
    val stats=summary?.optJSONObject("players")
    val blankCounts=summary?.optJSONObject("blankCounts")
    val fullReview=summary?.optJSONArray("review")
    val review=if(fullReview!=null)(0 until fullReview.length()).mapNotNull{fullReview.optJSONObject(it)}
        else localReview.keys().asSequence().mapNotNull{localReview.optJSONObject(it)}.filter{it.optString("kind") in listOf("wrong","blank")}.sortedBy{it.optInt("index")}.map{row->
            val copy=JSONObject(row.toString())
            val answer=game.optJSONObject("resolved")?.optJSONObject(row.optInt("index").toString())?.optString("answer").orEmpty()
            if(answer.isNotBlank())copy.put("answer",answer)
            copy
        }.toList()
    val wrong=review.filter{it.optString("kind")=="wrong"}
    val blank=review.filter{it.optString("kind")=="blank"}
    var entered by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){entered=true}
    val scale by animateFloatAsState(if(entered)1f else .82f,animationSpec=spring(dampingRatio=.68f,stiffness=180f),label="arena-victory")
    val wide=androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp>=600
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(title,color=Wq122Gold,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,textAlign=TextAlign.Center)
        Box(Modifier.fillMaxWidth().height(if(wide)210.dp else 185.dp)){
            Canvas(Modifier.fillMaxWidth(.86f).height(35.dp).align(Alignment.BottomCenter)){
                drawOval(Color(0xFF7A4A17))
                drawOval(Color(0xFFFFD572),topLeft=Offset(0f,0f),size=Size(size.width,size.height*.68f))
                drawOval(Color(0xFF6D38AD),topLeft=Offset(size.width*.035f,size.height*.055f),size=Size(size.width*.93f,size.height*.55f))
            }
            Row(Modifier.fillMaxSize().padding(horizontal=if(wide)64.dp else 12.dp).padding(bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.Bottom){
                ids.forEach{id->
                    val victory=id in winners
                    Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                        Wq150Portrait(profiles.optJSONObject(id),ownAvatar.takeIf{id==uid},
                            Modifier.fillMaxWidth().weight(1f).graphicsLayer{scaleX=if(victory)scale else .86f;scaleY=if(victory)scale else .86f},victory=victory)
                        Text(name(id),color=if(victory)Wq122Gold else Wq122Ink,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                }
            }
        }
        val done=summary?.optInt("solved",-1)?:-1
        val total=summary?.optInt("total",0)?:0
        if(done>=0&&total>0)Text((if(kind=="quiz")"Tamamlanan soru: " else "Çözülen kelime: ")+done+"/"+total,color=Wq122Ink)
    }
    ids.forEach{id->
        var expanded by remember(id){mutableStateOf(false)}
        val player=stats?.optJSONObject(id)
        Surface(color=Color(0xD939245D),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth(),
            border=androidx.compose.foundation.BorderStroke(1.dp,if(id in winners)Wq122Gold else Color(0xFF8C72B8))){
            Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Row(Modifier.fillMaxWidth().clickable{expanded=!expanded},verticalAlignment=Alignment.CenterVertically){
                    Wq150Portrait(profiles.optJSONObject(id),ownAvatar.takeIf{id==uid},Modifier.size(46.dp))
                    Text(name(id),Modifier.weight(1f).padding(horizontal=8.dp),color=Wq122Ink,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    if(player?.has("score")==true)Text(player.optInt("score").toString()+" puan",color=Wq122Gold,fontWeight=FontWeight.Black)
                    Text(if(expanded)"  ▴" else "  ▾",color=Wq122Gold,fontSize=22.sp)
                }
                androidx.compose.animation.AnimatedVisibility(expanded){
                    Column{
                        if(blankCounts?.has(id)==true)Text("Boş kelime: "+blankCounts.optInt(id),color=Wq122Ink)
                        Wq150StatCard(name(id),player,id in winners,showHeader=false)
                    }
                }
            }
        }
    }
    Surface(color=Color(0xDD251747),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth(),
        border=androidx.compose.foundation.BorderStroke(1.dp,Wq122Gold)){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("Maç özeti",color=Wq122Gold,fontWeight=FontWeight.Black,fontSize=20.sp)
            if(fullReview==null)Text("Bu cihazda doğrulanabilen cevaplar. Sunucu ayrıntılı maç kaydı sağlamıyorsa liste eksik olabilir.",color=Wq122Ink,fontSize=12.sp)
            listOf("Yanlış kelimeler" to wrong,"Boş kelimeler" to blank).forEach{(label,rows)->
                Text("$label · "+rows.size,color=if(label.startsWith("Yanlış"))Color(0xFFFFB4B4)else Wq122Gold,fontWeight=FontWeight.Bold)
                if(rows.isEmpty())Text(if(fullReview!=null)"Yok" else "Kayıt bulunamadı",color=Wq122Ink,fontSize=12.sp)
                rows.forEach{row->
                    val clue=row.optString("clue");val answer=row.optString("answer").takeUnless{it=="null"}.orEmpty()
                    Text(if(answer.isBlank())clue else "$answer — $clue",color=Wq122Ink,fontSize=14.sp)
                }
            }
        }
    }
    Surface(color=Color(0xE83B245E),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth(),
        border=androidx.compose.foundation.BorderStroke(1.dp,Wq122Gold)){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                Wq150ApprovedArt("book",Modifier.size(48.dp))
                Column(Modifier.weight(1f)){
                    Text("Öğretmeninle çalış",color=Wq122Gold,fontSize=19.sp,fontWeight=FontWeight.Black)
                    Text("Yanlış ve boş kelimelerini birlikte tekrar et.",color=Wq122Ink,fontSize=12.sp)
                    Text("${wrong.size} yanlış · ${blank.size} boş",color=Wq122Gold,fontSize=12.sp)
                }
            }
            Button(enabled=wrong.isNotEmpty()||blank.isNotEmpty(),onClick={onTeacher(wq150CombinedStudyPrompt(wrong,blank))},modifier=Modifier.fillMaxWidth()){
                Text("Öğretmenimle çalış",fontWeight=FontWeight.Bold)
            }
        }
    }
    Button(onClick=onBack,modifier=Modifier.fillMaxWidth()){Text("Arena'ya dön")}
}
internal fun wq150CombinedStudyPrompt(wrong:List<JSONObject>,blank:List<JSONObject>):String{
    fun section(rows:List<JSONObject>):String=if(rows.isEmpty())"Yok" else rows.joinToString("\n"){row->
        val answer=row.optString("answer").takeUnless{it=="null"}.orEmpty()
        row.optString("clue")+if(answer.isNotBlank())" — doğru cevap: "+answer else " — doğru cevap maç kaydında yok"
    }
    return "Arena maçımda yanlış yaptığım ve boş bıraktığım kelimeleri birlikte çalışmak istiyorum. Türkçe açıklama, İngilizce örnek cümle ve kısa alıştırmalarla öğret. Cevabı kayıtta olmayan maddelerde kesin cevap uydurma.\n\nYanlış kelimeler:\n"+section(wrong)+"\n\nBoş kelimeler:\n"+section(blank)
}

@Composable
private fun Wq150StatCard(name:String,stats:JSONObject?,winner:Boolean,showHeader:Boolean=true){
    Surface(color=Color(0xFF39245D),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth(),
        border=if(winner)androidx.compose.foundation.BorderStroke(1.dp,Wq122Gold)else null){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            if(showHeader)Row(verticalAlignment=Alignment.CenterVertically){
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
        if(room.isBlank()||view.optString("phase")!="finished"||view.optJSONObject("result")?.optString("kind")=="no-contest")return null
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
