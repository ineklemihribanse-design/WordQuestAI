package com.kelimebulmacasi.a1

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Build-provisioned HTTPS origin. Empty by default; no remote room can override it. */
internal object Wq150ArenaFeature {
    fun endpoint(context:android.content.Context):Wq150Endpoint? {
        val configured=runCatching{context.assets.open("wq150_arena_origin.txt").bufferedReader().use{it.readText().trim()}}.getOrNull()
        return configured?.takeIf{it.isNotBlank()}?.let{runCatching{Wq150Endpoint(it)}.getOrNull()}
    }
}
@Composable
internal fun Wq150ArenaEntry(session:AppSession,endpoint:Wq150Endpoint,back:()->Unit,account:()->Unit,
    legacy:()->Unit,onMatch:(String,JSONObject)->Unit){
    val context=LocalContext.current
    val catalog=remember(context){Wq150Catalog.read(context)}
    val auth=session.accounts.socialAuth()
    val uid=session.accounts.uid.orEmpty()
    val gateway=remember(endpoint,auth,uid){auth?.let{Wq150Gateway(endpoint,it)}}
    val controller=remember(gateway){gateway?.let{Wq150LobbyController(it)}}
    val ownAvatar by rememberWq2DSelection(session.profileKey)
    if(controller==null||gateway==null||uid.isBlank()){
        BackHandler(onBack=back)
        MaterialTheme(colorScheme=Wq122Scheme){
            Box(Modifier.fillMaxSize().background(Color(0xFF150B3E))){
                Wq183ArenaBackdrop(Modifier.matchParentSize(),active=false)
                Column(Modifier.align(Alignment.TopCenter).widthIn(max=700.dp).fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(12.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){
                    TextButton(onClick=back,modifier=Modifier.align(Alignment.Start)){Text("‹ Geri",color=Wq122Gold)}
                    Row(Modifier.fillMaxWidth().height(156.dp),verticalAlignment=Alignment.CenterVertically){
                        Wq150ApprovedArt("crest",Modifier.weight(1f).fillMaxHeight())
                        Wq2DPortrait(ownAvatar,Modifier.weight(1f).fillMaxHeight(),victory=false)
                    }
                    Text("Oyun Modları",color=Wq122Gold,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
                    listOf("duel","coop","quiz","team").chunked(2).forEach{row->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            row.forEach{kind->Wq150PremiumModeCard(kind,false,true,Modifier.weight(1f),account)}
                        }
                    }
                    Wq150PremiumPanel{
                        Text("Arkadaşlarınla çevrim içi oynamak için giriş yap.",color=Wq122Ink)
                        Wq122Primary("Giriş yap",true,account)
                        Wq122Choice("Tek Başına Oyna",false,Modifier.fillMaxWidth(),"solo",legacy)
                    }
                }
            }
        }
        return
    }
    val config=LocalConfiguration.current
    val landscape=config.screenWidthDp>=700 && config.screenWidthDp>config.screenHeightDp
    val state by controller.state.collectAsState()
    val scope=rememberCoroutineScope()
    var settings by remember {mutableStateOf(Wq150Settings())}
    var edit by rememberSaveable {mutableStateOf(false)}
    var roomCode by rememberSaveable {mutableStateOf("")}
    var exit by rememberSaveable {mutableStateOf(false)}
    var codeTouched by rememberSaveable {mutableStateOf(false)}
    var panel by rememberSaveable {mutableStateOf("")}
    val snapshot=state.view
    val room=state.room
    val lobby=snapshot?.optString("phase")=="lobby"&&!state.left
    val active= snapshot?.optString("phase") in listOf("playing","paused","finished")
    LaunchedEffect(room,state.left){
        if(room!=null&&!state.left)controller.poll()
    }
    LaunchedEffect(room,active){
        if(room!=null&&active&&snapshot!=null)onMatch(room,snapshot)
    }
    BackHandler {
        if(lobby||active)exit=true else back()
    }
    MaterialTheme(colorScheme=Wq122Scheme){
        Box(Modifier.fillMaxSize().background(Color(0xFF150B3E))){
            Wq183ArenaBackdrop(Modifier.matchParentSize(),active=false)
            Column(Modifier.align(Alignment.TopCenter).widthIn(max=1000.dp).fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal=12.dp,vertical=8.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    TextButton(onClick={if(lobby||active)exit=true else back()}){Text("‹ Geri",color=Wq122Gold)}
                    Spacer(Modifier.weight(1f));Text("WORDQUEST · ARENA",color=Wq122Gold,fontWeight=FontWeight.Bold)
                }
                if(!lobby&&!active){
                    Row(Modifier.fillMaxWidth().height(132.dp),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                            Wq150ApprovedArt("crest",Modifier.height(96.dp).fillMaxWidth())
                            Text("Arkadaşınla Oyna",color=Wq122Gold,fontWeight=FontWeight.Black,fontSize=16.sp)
                        }
                        Wq2DPortrait(ownAvatar,Modifier.weight(1f).fillMaxHeight(),victory=false)
                    }
                    val enabled=!state.busy&&!state.pending
                    Text("Oyun Modları",color=Wq122Gold,fontWeight=FontWeight.Black,fontSize=17.sp)
                    val modeGrid: @Composable ()->Unit = {
                    BoxWithConstraints(Modifier.fillMaxWidth()){
                        val wide=landscape||maxWidth>=600.dp
                        val columns=if(maxWidth>=330.dp)2 else 1
                        val kinds=listOf("duel","coop","quiz","team")
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                            kinds.chunked(columns).forEach{row->
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                    row.forEach{kind->
                                        Wq150PremiumModeCard(kind,wide,enabled,Modifier.weight(1f)){
                                            settings=settings.changeKind(kind);edit=true
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                    val roomPanel: @Composable ()->Unit = {
                    Wq150PremiumPanel {
                        Text("Davet kodu",color=Wq122Gold,fontWeight=FontWeight.Bold)
                        val valid=wq150ValidRoom(roomCode)
                        OutlinedTextField(value=roomCode,onValueChange={roomCode=it.trim().take(64);codeTouched=true},
                            singleLine=true,label={Text("Oda kodu")},modifier=Modifier.fillMaxWidth(),
                            enabled=enabled,isError=codeTouched&&roomCode.isNotEmpty()&&!valid)
                        if(codeTouched&&roomCode.isNotEmpty()&&!valid)
                            Text("Geçerli bir oda kodu gir.",color=Color(0xFFFFB4B4),fontSize=12.sp)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            OutlinedButton(enabled=enabled,modifier=Modifier.weight(1f),onClick={
                                val clipboard=context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                roomCode=clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()?.take(64).orEmpty()
                                codeTouched=true
                            }){Text("Yapıştır")}
                            Button(enabled=valid&&enabled,modifier=Modifier.weight(1f),onClick={
                                scope.launch{controller.join(roomCode)}
                            }){Text("Odaya katıl")}
                        }
                        if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                        if(auth!=null)Wq150Invites(session,auth,uid,null,false,enabled,null){id->
                            controller.join(id);controller.state.value.room==id&&controller.state.value.view!=null&&!controller.state.value.left
                        }
                    }
                    }
                    if(landscape){
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),
                            verticalAlignment=Alignment.Top){
                            Column(Modifier.weight(1.55f)){modeGrid()}
                            Column(Modifier.weight(1f)){roomPanel()}
                        }
                    }else{
                        modeGrid()
                        roomPanel()
                    }
                    Wq122Choice("Tek Başına Oyna",false,Modifier.fillMaxWidth(),"solo"){if(enabled)legacy()}
                    val shortcuts=listOf("ranking" to "Sıralama","friends" to "Arkadaşlarım","history" to "Son Maçlarım","settings" to "Ayarlar","info" to "Bilgi")
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                        shortcuts.forEach{(icon,title)->
                            Surface(onClick={panel=title},color=Color(0xD92C145B),
                                shape=RoundedCornerShape(12.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Wq122Gold),
                                modifier=Modifier.weight(1f)){
                                Column(Modifier.padding(horizontal=2.dp,vertical=6.dp),
                                    horizontalAlignment=Alignment.CenterHorizontally){
                                    Wq150ApprovedArt(icon,Modifier.size(if(landscape)32.dp else 25.dp))
                                    Text(title,color=Wq122Ink,fontSize=if(landscape)12.sp else 10.sp,textAlign=TextAlign.Center)
                                }
                            }
                        }
                    }
                }else if(lobby&&snapshot!=null&&room!=null){
                    val host=snapshot.optString("host")==uid
                    val members=snapshot.optJSONArray("members")
                    val count=members?.length()?:0
                    val capacity=snapshot.optInt("capacity",2)
                    val cfg=snapshot.optJSONObject("settings")?:JSONObject()
                    val kind=cfg.optString("kind")
                    val mode=cfg.optString("mode")
                    Text(Wq150ModeCatalog.kinds[kind].orEmpty(),style=MaterialTheme.typography.headlineSmall,color=Wq122Ink,fontWeight=FontWeight.Black)
                    Text(Wq150ModeCatalog.modes(kind)[mode].orEmpty()+" · "+count+"/"+capacity+" oyuncu",color=Wq122Gold)
                    Wq150PremiumPanel{
                        val levels=cfg.optJSONArray("levels")
                        val levelText=if(levels==null)"" else (0 until levels.length()).joinToString(" · "){
                            when(val v=levels.optString(it)){"easy"->"Kolay";"medium"->"Orta";"hard"->"Zor";else->v}
                        }
                        Text("Seçilen maç ayarları",color=Wq122Gold,fontWeight=FontWeight.Bold)
                        Text(listOf(levelText, cfg.optInt("count").toString()+" "+if(cfg.optString("format")=="quiz")"soru" else "kelime",
                            if(cfg.optInt("seconds")==0)"Süresiz" else cfg.optInt("seconds").toString()+" sn",
                            cfg.optInt("hints").toString()+" ipucu").filter{it.isNotBlank()}.joinToString(" · "),color=Wq122Ink)
                        Text(if(state.error!=null)"Oda bağlantısı doğrulanamadı" else if(state.busy)"Oda işlemi sürüyor" else "Oda durumu alındı",
                            color=if(state.error!=null)Color(0xFFFFB4B4)else Wq122Ink,fontSize=12.sp)
                    }
                    Surface(color=Color(0xDD251347),shape=MaterialTheme.shapes.large,modifier=Modifier.fillMaxWidth()){
                        Column(Modifier.padding(12.dp)){
                            Text("Davet kodu",color=Wq122Gold)
                            Text(room,color=Wq122Ink,fontWeight=FontWeight.Bold)
                            val context=LocalContext.current
                            TextButton(onClick={
                                val manager=context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                manager.setPrimaryClip(android.content.ClipData.newPlainText("Arena oda kodu",room))
                            }){Text("Kopyala",color=Wq122Gold)}
                        }
                    }
                    val profiles=rememberWq150Profiles(room,session,gateway)
                    var myReady=false
                    for(i in 0 until capacity){
                        val player=members?.optJSONObject(i)
                        val id=player?.optString("uid").orEmpty()
                        val ready=player?.optBoolean("ready")==true
                        if(id==uid)myReady=ready
                        Surface(color=Color(0xDD251347),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()){
                            Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                                if(player!=null){
                                    Wq150Portrait(profiles.optJSONObject(id),ownAvatar.takeIf{id==uid},Modifier.size(72.dp))
                                    Spacer(Modifier.width(8.dp))
                                }
                                val name=if(id==uid)session.profileName else if(player==null)"Oyuncu bekleniyor"
                                    else profiles.optJSONObject(id)?.optString("name")?.takeIf{it.isNotBlank()}
                                        ?:session.social.friends.firstOrNull{it.uid==id}?.name?:"Oyuncu "+(i+1)
                                Text(name,color=Wq122Ink,modifier=Modifier.weight(1f),fontWeight=FontWeight.Bold)
                                Text(if(player==null)"—" else if(ready)"Hazır" else "Bekliyor",color=if(ready)Color(0xFF91E8AA)else Wq122Gold)
                            }
                        }
                    }
                    if(auth!=null)Wq150Invites(session,auth,uid,room,host&&count<capacity,!state.busy&&!state.pending,room){false}
                    Wq122Primary(if(myReady)"Hazır değilim" else "Hazırım",!state.busy&&!state.pending,{
                        scope.launch{controller.action("ready",ready=!myReady)}
                    },green=true)
                    if(host){
                        TextButton(enabled=!state.busy&&!state.pending,onClick={
                            settings=Wq150Settings.fromJson(cfg);edit=true
                        }){Text("Maç ayarlarını değiştir",color=Wq122Gold)}
                        Wq122Primary("Maçı başlat",snapshot.optBoolean("canStart")&&!state.busy&&!state.pending,{
                            scope.launch{controller.action("start")}
                        },green=false)
                    }
                    TextButton(onClick={exit=true},enabled=!state.busy&&!state.pending){Text("Odadan ayrıl",color=Wq122Gold)}
                }else if(active){
                    Text("Maç hazırlanıyor…",color=Wq122Ink)
                    // Keep the active room, never replace it with a legacy room.
                }
                if(state.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let{Text(it,color=Color(0xFFFFB4B4))}
                if(state.pending&&!state.busy){
                    Wq122Primary("Aynı işlemi tekrar dene",true,{scope.launch{controller.retry()}},green=false)
                }
                if(state.error!=null&&room!=null&&!state.busy)TextButton(onClick={scope.launch{controller.refresh()}}){Text("Odayı yenile",color=Wq122Gold)}
            }
        }
        if(panel.isNotBlank())AlertDialog(onDismissRequest={panel=""},title={Text(panel,color=Wq122Gold)},
            text={Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
                when(panel){
                    "Ayarlar"->Wq179ArenaAudioSettings()
                    "Bilgi"->Text("Bulmaca Düellosu: rakibinle yarış. Ortak Bulmaca: aynı tahtayı birlikte çöz. 5 Şıklı Düello: bilgini ve hızını göster. Takım Oyunu: 2'ye 2 mücadele et.",color=Wq122Ink)
                    "Arkadaşlarım"->{
                        if(session.social.friends.isEmpty())Text("Henüz arkadaş eklenmedi.",color=Wq122Ink)
                        session.social.friends.forEach{friend->
                            Text(friend.name,color=Wq122Ink,fontWeight=FontWeight.Bold)
                            Text(Wq180FriendActivity.label(friend.lastActiveAt),color=Wq122Ink)
                        }
                        TextButton(onClick={panel="";legacy()}){Text("Arkadaş yönetimi",color=Wq122Gold)}
                    }
                    else->{
                        Wq150Records(context,session.profileKey,panel=="Sıralama")
                        HorizontalDivider()
                        Text("Önceki Arena kayıtları",color=Wq122Gold,fontWeight=FontWeight.Bold)
                        Wq121ArenaRecords(context,session.profileKey,panel=="Sıralama")
                    }
                }
            }},confirmButton={TextButton(onClick={panel=""}){Text("Kapat")}})
        if(edit)Wq150MatchSettingsDialog(settings,catalog?.options("topics").orEmpty(),catalog?.options("wordTypes").orEmpty(),!state.busy&&!state.pending,
            onDismiss={edit=false},onApply={next->
                settings=next;edit=false
                scope.launch{if(lobby)controller.action("settings",settings=next) else controller.create(next)}
            },catalog=catalog)
        if(exit)AlertDialog(onDismissRequest={exit=false},title={Text("Arena'dan ayrıl?")},
            text={Text(if(active)"Aktif maçın durumu korunur. Bu ekrandan ayrılmak maçtan hükmen çıkış değildir." else "Odadan ayrılmak istediğine emin misin?")},
            confirmButton={TextButton(onClick={exit=false;if(lobby)scope.launch{controller.action("leave")} else back()}){Text("Ayrıl")}},
            dismissButton={TextButton(onClick={exit=false}){Text("Vazgeç")}})
    }
}

/** The approved v120 icon/card assets are decoded off the UI thread.
 * Only the decorative empty plate is stretched; the 3D icon keeps its ratio. */
private object Wq150ArtCache{
    private val cache=object:android.util.LruCache<String,ImageBitmap>(24*1024*1024){
        override fun sizeOf(key:String,value:ImageBitmap):Int=(value.width.toLong()*value.height*4L)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
    fun get(name:String)=cache.get(name)
    fun put(name:String,art:ImageBitmap){cache.put(name,art)}
}
@Composable
private fun Wq150ApprovedArt(name:String,modifier:Modifier,scale:ContentScale=ContentScale.Fit){
    val context=LocalContext.current.applicationContext
    // Key the state by asset name: a rotated screen must never show the previous plate.
    var art by remember(context,name){mutableStateOf(Wq150ArtCache.get(name))}
    LaunchedEffect(context,name){
        val cached=Wq150ArtCache.get(name)
        if(cached!=null){
            art=cached
        }else{
            val decoded=withContext(Dispatchers.IO){
                runCatching{context.assets.open("arena_v120/$name.webp").use{
                    android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap()
                }}.getOrNull()?.also{Wq150ArtCache.put(name,it)}
            }
            art=decoded
        }
    }
    art?.let{Image(it,null,modifier,contentScale=scale)}
}
@Composable
private fun Wq150PremiumModeCard(kind:String,wide:Boolean,enabled:Boolean,modifier:Modifier,onClick:()->Unit){
    val shape=RoundedCornerShape(16.dp)
    val art=if(kind=="coop")"team" else if(kind=="team")"friends" else kind
    val colors=when(kind){
        "coop"->listOf(Color(0xFF254A8E),Color(0xFF102653))
        "quiz"->listOf(Color(0xFF8B2948),Color(0xFF47152E))
        "team"->listOf(Color(0xFF176B55),Color(0xFF0C352D))
        else->listOf(Color(0xFF683C9E),Color(0xFF301450))
    }
    val description=when(kind){
        "coop"->"Birlikte çöz, birlikte kazan."
        "quiz"->"Bilgini ve hızını göster."
        "team"->"Takımınla birlikte mücadele et."
        else->"Rakibinle kelime yarışına gir."
    }
    Box(modifier.heightIn(min=if(wide)112.dp else 112.dp).clip(shape)
        .background(Brush.verticalGradient(colors)).border(1.dp,Wq122Gold,shape)
        .clickable(enabled=enabled,onClick=onClick)){
        if(kind!="team")Wq150ApprovedArt((if(wide)"tablet" else "phone")+"_card_"+art,
            Modifier.matchParentSize(),ContentScale.FillBounds)
        Column(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=8.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)){
            Wq150ApprovedArt(art,Modifier.size(if(wide)58.dp else 44.dp))
            Text(Wq150ModeCatalog.kinds.getValue(kind),color=Color.White,fontWeight=FontWeight.ExtraBold,
                fontSize=if(wide)17.sp else 14.sp,textAlign=TextAlign.Center)
            Text(description,color=Color(0xFFF6E9FF),fontSize=if(wide)13.sp else 11.sp,textAlign=TextAlign.Center)
        }
    }
}
@Composable
private fun Wq150PremiumPanel(content:@Composable ColumnScope.()->Unit){
    Surface(color=Color(0xE6251347),shape=RoundedCornerShape(18.dp),
        border=androidx.compose.foundation.BorderStroke(1.dp,Wq122Gold),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
    }
}
