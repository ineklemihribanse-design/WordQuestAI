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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.json.JSONObject

private object Wq150CatalogCache {
    @Volatile var cached:Wq150Catalog?=null
        private set
    @Synchronized fun load(context:android.content.Context):Wq150Catalog?{
        cached?.let{return it}
        return Wq150Catalog.read(context).also{cached=it}
    }
}
/** Build-provisioned HTTPS origin. Empty by default; no remote room can override it. */
internal object Wq150ArenaFeature {
    fun endpoint(context:android.content.Context):Wq150Endpoint? {
        val configured=runCatching{context.assets.open("wq150_arena_origin.txt").bufferedReader().use{it.readText().trim()}}.getOrNull()
        return configured?.takeIf{it.isNotBlank()}?.let{runCatching{Wq150Endpoint(it)}.getOrNull()}
    }
}
@Composable
internal fun Wq150ArenaEntry(session:AppSession,endpoint:Wq150Endpoint,back:()->Unit,account:()->Unit,
    legacy:()->Unit,soloSelected:Boolean,onTab:(Boolean)->Unit,onSolo:(WqArenaSoloLaunch)->Unit,onMatch:(String,JSONObject)->Unit){
    val context=LocalContext.current
    val appContext=context.applicationContext
    var catalogState by remember(appContext){mutableStateOf<Pair<Boolean,Wq150Catalog?>>((Wq150CatalogCache.cached!=null) to Wq150CatalogCache.cached)}
    LaunchedEffect(appContext){
        val loadedCatalog=withContext(Dispatchers.IO){Wq150CatalogCache.load(appContext)}
        catalogState=true to loadedCatalog
    }
    val catalog=catalogState.second
    val auth=session.accounts.socialAuth()
    val uid=session.accounts.uid.orEmpty()
    val gateway=remember(endpoint,auth,uid){auth?.let{Wq150Gateway(endpoint,it)}}
    val controller=remember(gateway){gateway?.let{Wq150LobbyController(it)}}
    val ownAvatar by rememberWq2DSelection(session.profileKey)
    var soloMode by rememberSaveable{mutableStateOf<String?>(null)}
    var guestPanel by rememberSaveable{mutableStateOf("")}
    if(controller==null||gateway==null||uid.isBlank()){
        BackHandler(onBack=back)
        MaterialTheme(colorScheme=Wq122Scheme){
            Wq150ArenaHome(session,ownAvatar,soloSelected,onTab,true,back,
                onMode={if(soloSelected)soloMode=it else account()},
                onPanel={if(it=="Hızlı Oda Katılımı"||it=="Arkadaşlarım")account() else guestPanel=it},
                status=null,onRetry=null)
            if(guestPanel.isNotBlank())AlertDialog(onDismissRequest={guestPanel=""},containerColor=Color(0xFF251347),title={Text(guestPanel,color=Wq122Gold)},
                text={Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState())){
                    when(guestPanel){
                        "Ayarlar"->Wq179ArenaAudioSettings()
                        "Bilgi"->Text("Tek kişilik oyunlar internet gerektirmez. Arkadaşlarınla oynamak için giriş yap.")
                        else->{
                            Wq150Records(context,session.profileKey,guestPanel=="Sıralama")
                            Wq121ArenaRecords(context,session.profileKey,guestPanel=="Sıralama")
                        }
                    }
                }},confirmButton={TextButton(onClick={guestPanel=""}){Text("Kapat")}})
            soloMode?.let{Wq150SoloSettings(session,it,{soloMode=null},onSolo)}
        }
        return
    }
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
            if(lobby&&!active)Wq160LobbyBackdrop(Modifier.matchParentSize())
            else if(active)Wq183ArenaBackdrop(Modifier.matchParentSize(),active=false)
            if(!lobby&&!active){
                val enabled=!state.busy&&!state.pending
                Wq150ArenaHome(session,ownAvatar,soloSelected,onTab,enabled,back,
                    onMode={kind->if(soloSelected)soloMode=kind else{settings=settings.changeKind(kind);edit=true}},
                    onPanel={panel=it},
                    status=state.error?:if(state.busy)"Oda işlemi sürüyor…" else if(!soloSelected&&!catalogState.first)"İçerik hazırlanıyor…" else null,
                    onRetry=if(state.pending&&!state.busy)({scope.launch{controller.retry()};Unit})else null)
            }else Column(Modifier.align(Alignment.TopCenter).widthIn(max=1000.dp).fillMaxSize()
                .windowInsetsPadding(Wq160SettingsInsets()).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal=12.dp,vertical=8.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)){
                if(lobby&&snapshot!=null&&room!=null){
                    val host=snapshot.optString("host")==uid
                    val members=snapshot.optJSONArray("members")
                    val count=members?.length()?:0
                    val capacity=snapshot.optInt("capacity",2)
                    val cfg=snapshot.optJSONObject("settings")?:JSONObject()
                    val kind=cfg.optString("kind")
                    val mode=cfg.optString("mode")
                    val enabled=!state.busy&&!state.pending
                    val profiles=rememberWq150Profiles(room,session,gateway)
                    val config=LocalConfiguration.current
                    val wide=config.screenWidthDp>=650
                    val cardHeight=(config.screenHeightDp*(if(wide).52f else .48f)).coerceIn(if(wide)210f else 280f,if(wide)430f else 460f).dp
                    var info by remember(room){mutableStateOf(false)}
                    var inviteOpen by remember(room){mutableStateOf(false)}
                    val consent by rememberWq183ShareConsent(session.profileKey)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        TextButton(onClick={exit=true},contentPadding=PaddingValues(horizontal=4.dp)){Text("‹",color=Wq122Gold,fontSize=25.sp)}
                        Wq150ApprovedArt("friends",Modifier.size(34.dp))
                        Column(Modifier.weight(1f)){
                            Text("Oda Lobisi",color=Color.White,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Black,fontSize=if(wide)28.sp else 22.sp)
                            Text("WordQuest AI · Arena · $count/$capacity",color=Wq122Gold,fontSize=11.sp)
                        }
                        Wq161LobbyAction("Arkadaşlar",{panel="Arkadaşlarım"},Modifier.widthIn(min=78.dp))
                        Wq161LobbyAction("i",{info=true},Modifier.width(44.dp))
                    }
                    Wq161LobbyPlate{
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                            Column(Modifier.weight(1f)){
                                Text("Oda kodu",color=Color(0xFFE8D6FC),fontSize=12.sp)
                                Text(room,color=Wq122Gold,fontWeight=FontWeight.Bold,fontSize=if(wide)19.sp else 13.sp)
                            }
                            Wq161LobbyAction("Kopyala",{
                                val manager=context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                manager.setPrimaryClip(android.content.ClipData.newPlainText("Arena oda kodu",room))
                            })
                            Wq161LobbyAction("Paylaş",{
                                val share=android.content.Intent(android.content.Intent.ACTION_SEND).apply{
                                    type="text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT,"WordQuest AI Arena · Oda kodu: $room")
                                }
                                context.startActivity(android.content.Intent.createChooser(share,"Arena daveti"))
                            })
                        }
                    }
                    Wq161LobbyPlate{
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            Wq150ApprovedArt("settings",Modifier.size(28.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Maç Ayarları",Modifier.weight(1f),color=Color.White,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,fontSize=17.sp)
                            if(host)TextButton(enabled=enabled,onClick={settings=Wq150Settings.fromJson(cfg);edit=true}){Text("Düzenle",color=Wq122Gold)}
                        }
                        val levels=cfg.optJSONArray("levels")
                        val levelText=if(levels==null)"" else (0 until levels.length()).joinToString(" · "){
                            when(val v=levels.optString(it)){"easy"->"Kolay";"medium"->"Orta";"hard"->"Zor";else->v}
                        }
                        val chips=listOf(Wq150ModeCatalog.kinds[kind].orEmpty(),Wq150ModeCatalog.modes(kind)[mode].orEmpty(),levelText,
                            if(cfg.optInt("seconds")==0)"Süresiz" else cfg.optInt("seconds").toString()+" sn",
                            cfg.optInt("count").toString()+" "+if(cfg.optString("format")=="quiz")"soru" else "kelime",
                            cfg.optInt("hints").toString()+" ipucu")
                        chips.chunked(if(wide)6 else 3).forEach{line->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                line.forEach{label->Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xAA27183F))
                                    .border(1.dp,Color(0x667E62A5),RoundedCornerShape(10.dp)).padding(vertical=7.dp,horizontal=3.dp),contentAlignment=Alignment.Center){
                                    Text(label,color=Color.White,fontSize=12.sp,textAlign=TextAlign.Center,maxLines=2)
                                }}
                            }
                        }
                    }
                    // Render the same authoritative member list, not a legacy room.
                    val columns=if(wide&&capacity>2)4 else 2
                    (0 until capacity).toList().chunked(columns).forEach{slots->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            slots.forEach{i->
                                val player=members?.optJSONObject(i)
                                val id=player?.optString("uid").orEmpty()
                                val ready=player?.optBoolean("ready")==true
                                val mine=id==uid
                                val owner=id==snapshot.optString("host")
                                val name=if(mine)session.profileName else if(player==null)"Arkadaş bekleniyor"
                                    else profiles.optJSONObject(id)?.optString("name")?.takeIf{it.isNotBlank()}
                                        ?:session.social.friends.firstOrNull{it.uid==id}?.name?:"Oyuncu "+(i+1)
                                Wq161LobbyPlayer(name,profiles.optJSONObject(id),ownAvatar.takeIf{mine},owner,player!=null,mine,ready,enabled,
                                    cardHeight,Modifier.weight(1f),canInvite=host&&count<capacity&&enabled,onInvite={inviteOpen=true}){
                                    scope.launch{controller.action("ready",ready=!ready)}
                                }
                            }
                        }
                    }
                    Wq161LobbyPlate{
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){
                                Text("Maskotumu rakibime göster",color=Color.White,fontWeight=FontWeight.Bold,fontSize=13.sp)
                                Text(if(consent)"Paylaşım açık" else "Paylaşım kapalı",color=Color(0xFFE7D9FF),fontSize=12.sp)
                            }
                            Switch(checked=consent,onCheckedChange={Wq183ArenaShare.setConsent(context,session.profileKey,it)})
                        }
                    }
                    if(auth!=null)Wq150Invites(session,auth,uid,room,host&&count<capacity,enabled,room,inviteOpen,{inviteOpen=false}){false}
                    if(host)Wq159SettingsFooter("Maçı başlat",snapshot.optBoolean("canStart")&&enabled){
                        scope.launch{controller.action("start")}
                    }
                    TextButton(onClick={exit=true},enabled=enabled){Text("Odadan ayrıl",color=Wq122Gold)}
                    if(info)AlertDialog(onDismissRequest={info=false},containerColor=Color(0xFF251347),title={Text("Oda Lobisi",color=Wq122Gold)},
                        text={Text("Arkadaşını davet et. Her oyuncu kendi kartından hazır olduğunu bildirir. Herkes hazır olduğunda oda sahibi maçı başlatabilir.",color=Color.White)},
                        confirmButton={TextButton(onClick={info=false}){Text("Kapat",color=Wq122Gold)}})
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
        if(panel=="Arkadaşlarım")Wq157ArenaFriends(session.social){panel=""}
        if(panel.isNotBlank()&&panel!="Arkadaşlarım")AlertDialog(onDismissRequest={panel=""},containerColor=Color(0xFF251347),title={Text(panel,color=Wq122Gold)},
            text={Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
                when(panel){
                    "Hızlı Oda Katılımı"->{
                        val enabled=!state.busy&&!state.pending
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

                        state.error?.let{Text(it,color=Color(0xFFFFB4B4))}
                    }
                    "Ayarlar"->Wq179ArenaAudioSettings()
                    "Bilgi"->Text("Bulmaca Düellosu: rakibinle yarış. Ortak Bulmaca: aynı tahtayı birlikte çöz. 5 Şıklı Düello: bilgini ve hızını göster. Takım Oyunu: 2'ye 2 mücadele et.",color=Wq122Ink)
                    else->{
                        Wq150Records(context,session.profileKey,panel=="Sıralama")
                        HorizontalDivider()
                        Text("Önceki Arena kayıtları",color=Wq122Gold,fontWeight=FontWeight.Bold)
                        Wq121ArenaRecords(context,session.profileKey,panel=="Sıralama")
                    }
                }
            }},confirmButton={TextButton(onClick={panel=""}){Text("Kapat")}})
        LaunchedEffect(lobby,active){if(lobby||active)panel=""}
        soloMode?.let{Wq150SoloSettings(session,it,{soloMode=null},onSolo)}
        if(edit)Wq150MatchSettingsDialog(settings,catalog?.options("topics").orEmpty(),catalog?.options("wordTypes").orEmpty(),!state.busy&&!state.pending&&catalogState.first,
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
/** Retain one complete viewport across navigation, independent of the per-image LRU. */
internal class Wq150RetainedFrame<T>{
    private data class Snapshot<T>(val keys:Set<String>,val images:Map<String,T?>)
    @Volatile private var snapshot:Snapshot<T>?=null
    fun frame(keys:Set<String>):Map<String,T?>?=snapshot?.takeIf{it.keys==keys}?.images
    fun image(name:String):T?=snapshot?.images?.get(name)
    fun publish(keys:Set<String>,images:Map<String,T?>):Map<String,T?>{
        require(images.keys==keys)
        val stable=images.toMap()
        snapshot=Snapshot(keys.toSet(),stable)
        return stable
    }
}
private object Wq150ArtCache{
    private val retained=Wq150RetainedFrame<ImageBitmap>()
    fun frame(keys:Set<String>)=retained.frame(keys)
    @Synchronized fun prepare(context:android.content.Context,keys:Set<String>):Map<String,ImageBitmap?>{
        frame(keys)?.let{return it}
        return retained.publish(keys,keys.associateWith{load(context,it)})
    }
    private val cache=object:android.util.LruCache<String,ImageBitmap>((Runtime.getRuntime().maxMemory()/8L).coerceIn(24L*1024*1024,64L*1024*1024).toInt()){
        override fun sizeOf(key:String,value:ImageBitmap):Int=(value.width.toLong()*value.height*4L)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
    fun get(name:String)=retained.image(name)?:cache.get(name)
    fun put(name:String,art:ImageBitmap){cache.put(name,art)}
    // Called only on Dispatchers.IO. Serialize misses to avoid decoding the same
    // WebP in parallel when several Arena cards use a common decorative plate.
    @Synchronized fun load(context:android.content.Context,name:String):ImageBitmap?{
        get(name)?.let{return it}
        return runCatching{
            val asset=if('/' in name)name else "arena_v120/$name.webp"
            val background=name.substringAfterLast('/').substringBeforeLast('.') in listOf("phone","tablet")
            val limit=if(background)maxOf(context.resources.displayMetrics.widthPixels,context.resources.displayMetrics.heightPixels,1600) else if(name.contains("plate")||name.contains("_card_"))1024 else 512
            val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}
            context.assets.open(asset).use{android.graphics.BitmapFactory.decodeStream(it,null,bounds)}
            var sample=1
            while(maxOf(bounds.outWidth,bounds.outHeight)/sample>=limit*2)sample*=2
            val options=android.graphics.BitmapFactory.Options().apply{inSampleSize=sample}
            context.assets.open(asset).use{android.graphics.BitmapFactory.decodeStream(it,null,options)?.asImageBitmap()}
        }.getOrNull()?.also{put(name,it)}
    }
}
// A prepared home frame owns strong references even if the shared LRU evicts an entry.
private val LocalWq150HomeArt=staticCompositionLocalOf<Map<String,ImageBitmap?>?>{null}

@Composable
private fun Wq150ApprovedArt(name:String,modifier:Modifier,scale:ContentScale=ContentScale.Fit){
    val prepared=LocalWq150HomeArt.current
    if(prepared!=null&&prepared.containsKey(name)){
        prepared[name]?.let{Image(it,null,modifier,contentScale=scale)}
        return
    }
    val context=LocalContext.current.applicationContext
    // Key the state by asset name: a rotated screen must never show the previous plate.
    var art by remember(context,name){mutableStateOf(Wq150ArtCache.get(name))}
    LaunchedEffect(context,name){
        val cached=Wq150ArtCache.get(name)
        if(cached!=null){
            art=cached
        }else{
            val decoded=withContext(Dispatchers.IO){Wq150ArtCache.load(context,name)}
            art=decoded
        }
    }
    art?.let{Image(it,null,modifier,contentScale=scale)}
}
/** Share the same cache with both full-screen settings routes. */
@Composable
internal fun Wq150SettingsBackdrop(modifier:Modifier){
    val c=LocalConfiguration.current
    val tablet=c.screenWidthDp>c.screenHeightDp&&c.screenWidthDp>=600
    Wq150ApprovedArt(if(tablet)"arena_v160/settings/tablet.webp" else "arena_v160/settings/phone.webp",
        modifier,ContentScale.Fit)
}
@Composable
private fun Wq160LobbyBackdrop(modifier:Modifier){
    val c=LocalConfiguration.current
    val tablet=c.screenWidthDp>c.screenHeightDp&&c.screenWidthDp>=600
    Wq150ApprovedArt(if(tablet)"arena_v160/lobby/tablet.webp" else "arena_v160/lobby/phone.webp",
        modifier,ContentScale.Fit)
}
@Composable
private fun Wq150PremiumPanel(content:@Composable ColumnScope.()->Unit){
    Surface(color=Color(0xE6251347),shape=RoundedCornerShape(18.dp),
        border=androidx.compose.foundation.BorderStroke(1.dp,Wq122Gold),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
    }
}

/** One shared visual hub. Only the selected tab's cards exist in composition. */
internal object Wq150HomeModes {
    val online=listOf("duel","coop","quiz","team")
    val solo=listOf("classic","timed","test","obstacle")
    fun visible(local:Boolean)=if(local)solo else online
    fun localEngine(kind:String)=if(kind=="test")"quiz" else "duel"
    fun localTimes(kind:String)=when(kind){"obstacle"->listOf(20);"timed"->listOf(20,30,45);"classic"->listOf(0);else->listOf(0,20,30,45)}
    fun localHints(kind:String)=if(kind=="obstacle")listOf(0) else listOf(0,3,5,8,12,20)
}
/** Measured in dp, independent of tab selection. If a short display cannot fit
 * accessible cards, allow a bounded scroll instead of overlapping the footer. */
internal object Wq150HomeLayout {
    data class Metrics(val height:Float,val tabTop:Float,val tabHeight:Float,val headingHeight:Float,
        val footerHeight:Float,val gap:Float,val cardArea:Float){
        val cardTop get()=tabTop+tabHeight+headingHeight+2*gap
        val footerTop get()=height-footerHeight-2*gap
        fun cardHeight(wide:Boolean)=if(wide)cardArea else (cardArea-3*gap)/4
    }
    fun measure(available:Float,wide:Boolean):Metrics{
        var h=available.coerceAtLeast(if(wide)460f else 640f)
        repeat(100){
            val gap=if(wide)8f else 5f
            val tab=(h*.065f).coerceIn(40f,54f)
            val heading=if(wide)30f else 42f
            val footer=(h*.075f).coerceIn(46f,66f)
            val remaining=h-footer-3*gap-tab-heading-2*gap
            // On short landscape tablets reserve the actual card and footer space
            // first. Reduce the decorative hero instead of growing past the viewport.
            // Keep the approved phone proportions and its existing scroll fallback.
            val top=if(wide)minOf(h*.62f,(remaining-89f).coerceAtLeast(0f)) else h*.425f
            val area=remaining-top
            val m=Metrics(h,top,tab,heading,footer,gap,area)
            if(m.cardHeight(wide)>=if(wide)88f else 56f)return m
            h+=8f
        }
        error("Arena viewport is invalid")
    }
}
/** Coordinates measured on the actual shipped art. The crop transform used
 * here is exactly the one used by the backdrop image, not a screen guess. */
internal object Wq150Podium {
    fun anchor(w:Float,h:Float,wide:Boolean,newArt:Boolean):Pair<Float,Float> =
        Wq121ArenaLayout.project(w,h,wide,
            if(wide||newArt).50f else .73f,
            if(newArt){if(wide).505f else .345f}else{if(wide).505f else .385f})
}
@Composable
private fun rememberWq150NewArt():Boolean{
    val context=LocalContext.current
    return remember(context.applicationContext){
        runCatching{context.assets.open("arena_v150_premium/manifest.json").use{true}}.getOrDefault(false)
    }
}
private data class Wq150CardSpec(val title:String,val detail:String,val icon:String,val plate:String,val top:Long,val bottom:Long)
private fun wq150Card(kind:String)=when(kind){
    "duel"->Wq150CardSpec("Bulmaca Düellosu","Rakibinle kelime yarışına gir.","duel","duel",0xFF7921BC,0xFF38116C)
    "coop"->Wq150CardSpec("Ortak Bulmaca","Birlikte çöz, birlikte kazan.","team","team",0xFF0078B9,0xFF064274)
    "quiz"->Wq150CardSpec("5 Şıklı Düello","Bilgini ve hızını göster.","quiz","quiz",0xFFBD2A13,0xFF7B1B08)
    "team"->Wq150CardSpec("Takım Oyunu","Takımınla birlikte mücadele et.","friends","",0xFF137D48,0xFF06402F)
    "classic"->Wq150CardSpec("Klasik Bulmaca","Kendi hızında, keyifle oyna.","wq175_icons/puzzle.webp","duel",0xFF7921BC,0xFF38116C)
    "timed"->Wq150CardSpec("Zamana Karşı","Süreli kelime yarışında puan topla.","wq175_icons/timer.webp","team",0xFF0078B9,0xFF064274)
    "test"->Wq150CardSpec("5 Şıklı Test","Bilgini ölç, seviyeni gör.","quiz","quiz",0xFFBD2A13,0xFF7B1B08)
    "obstacle"->Wq150CardSpec("Engelli Parkur","20 saniyede, ipucusuz çöz.","arena_v122/rules.webp","",0xFF137D48,0xFF06402F)
    else->error("Unknown Arena card")
}
@Composable
private fun Wq150HomeTab(label:String,local:Boolean,selected:Boolean,enabled:Boolean,modifier:Modifier,onClick:()->Unit){
    val shape=RoundedCornerShape(17.dp)
    Box(modifier.clip(shape).background(Brush.verticalGradient(if(local)
        listOf(Color(0xFF0093DD),Color(0xFF084080)) else listOf(Color(0xFF9723CF),Color(0xFF3D126F))))
        .border(if(selected)2.dp else 1.dp,if(selected)Wq122Gold else Color(0xFF9A70D7),shape)
        .clickable(enabled=enabled,onClick=onClick),contentAlignment=Alignment.Center){
        Wq150ApprovedArt(if(local)"phone_card_team" else "phone_card_duel",Modifier.matchParentSize(),ContentScale.FillBounds)
        // The selected outline is drawn above the plate so the state stays visible.
        if(selected)Box(Modifier.matchParentSize().border(2.dp,Wq122Gold,shape))
        Row(Modifier.padding(horizontal=6.dp,vertical=3.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.Center){
            Wq150ApprovedArt(if(local)"arena_v122/solo.webp" else "friends",Modifier.size(28.dp))
            Spacer(Modifier.width(4.dp))
            Text(label,Modifier.weight(1f,fill=false),color=Color.White,fontWeight=FontWeight.Bold,fontSize=13.sp,
                maxLines=2,textAlign=TextAlign.Center)
            if(selected)Text(" ✓",color=Wq122Gold,fontWeight=FontWeight.Black)
        }
    }
}
@Composable
private fun Wq150HomeCard(kind:String,wide:Boolean,enabled:Boolean,modifier:Modifier,onClick:()->Unit){
    val spec=wq150Card(kind);val shape=RoundedCornerShape(16.dp)
    val newArt=rememberWq150NewArt()
    val plate=Wq150HomeArtPlan.plate(kind,wide,newArt)
    val icon=Wq150HomeArtPlan.icon(kind,newArt)
    Box(modifier.clip(shape).background(Brush.verticalGradient(listOf(Color(spec.top),Color(spec.bottom))))
        .border(1.5.dp,Wq122Gold,shape).clickable(enabled=enabled,onClick=onClick)){
        if(plate!=null)Wq150ApprovedArt(plate,Modifier.matchParentSize(),ContentScale.FillBounds)
        Row(Modifier.fillMaxSize().padding(horizontal=if(wide)6.dp else 9.dp,vertical=4.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
            Wq150ApprovedArt(icon,Modifier.fillMaxWidth(if(wide).34f else .27f).fillMaxHeight())
            Column(Modifier.weight(1f)){
                Text(spec.title,color=Color.White,fontFamily=FontFamily.Serif,fontWeight=FontWeight.ExtraBold,fontSize=if(wide)15.sp else 16.sp,
                    maxLines=2,overflow=TextOverflow.Ellipsis,lineHeight=18.sp)
                Text(spec.detail,color=Color(0xFFF6E9FF),fontSize=if(wide)11.sp else 11.sp,
                    maxLines=3,overflow=TextOverflow.Ellipsis,lineHeight=13.sp)
            }
            Text("›",color=Wq122Gold,fontSize=20.sp,fontWeight=FontWeight.Bold)
        }
    }
}
/** The exact same manifest drives rendering and preloading for both tabs. */
internal object Wq150HomeArtPlan {
    fun icon(kind:String,newArt:Boolean)=if(newArt&&kind=="team")"arena_v150_premium/team_crest.webp" else wq150Card(kind).icon
    fun plate(kind:String,wide:Boolean,newArt:Boolean):String?{
        val spec=wq150Card(kind)
        val plate=when(kind){"classic"->"duel";"timed"->"coop";"test"->"quiz";"obstacle"->"team";else->kind}
        return if(newArt)"arena_v150_premium/${plate}_plate.webp"
        else if(spec.plate.isNotEmpty())(if(wide)"tablet" else "phone")+"_card_"+spec.plate else null
    }
    fun keys(background:String,settingsBackground:String,wide:Boolean,newArt:Boolean):Set<String> = buildSet{
        addAll(listOf(background,"crest","book","back","phone_card_team","phone_card_duel",
            "arena_v122/solo.webp","friends","modes","ranking","history","settings","info"))
        (Wq150HomeModes.online+Wq150HomeModes.solo).forEach{kind->
            add(icon(kind,newArt))
            plate(kind,wide,newArt)?.let{add(it)}
        }
        add(settingsBackground)
        if(settingsBackground.startsWith("arena_v160/settings/"))add(settingsBackground.replace("/settings/","/lobby/"))
    }
}

/** Start the same bounded local preparation from the root home composition. */
@Composable
internal fun Wq150WarmArenaArt(session:AppSession){
    val context=LocalContext.current.applicationContext
    val config=LocalConfiguration.current
    val newArt=rememberWq150NewArt()
    val wide=Wq121ArenaLayout.wide(config.screenWidthDp.toFloat(),config.screenHeightDp.toFloat(),config.smallestScreenWidthDp)
    val device=if(wide)"tablet" else "phone"
    val background=if(newArt)"arena_v150_premium/$device.webp" else device
    val settingsTablet=config.screenWidthDp>config.screenHeightDp&&config.screenWidthDp>=600
    val settingsKey=if(settingsTablet)"arena_v160/settings/tablet.webp" else "arena_v160/settings/phone.webp"
    val keys=remember(background,settingsKey,wide,newArt){
        Wq150HomeArtPlan.keys(background,settingsKey,wide,newArt)
    }
    val avatar by rememberWq2DSelection(session.profileKey)
    val portraitKey=Wq157OwnPortrait.key(context,session.profileKey,avatar)
    LaunchedEffect(context,keys,portraitKey){
        withContext(Dispatchers.IO){
            Wq150ArtCache.prepare(context,keys)
            Wq157OwnPortrait.load(context,portraitKey,avatar)
        }
    }
}

@Composable
private fun Wq150ArenaHome(session:AppSession,avatar:String,local:Boolean,onTab:(Boolean)->Unit,enabled:Boolean,
    back:()->Unit,onMode:(String)->Unit,onPanel:(String)->Unit,status:String?,onRetry:(()->Unit)?){
    Wq150ArenaBars()
    val newArt=rememberWq150NewArt()
    val config=LocalConfiguration.current
    BoxWithConstraints(Modifier.fillMaxSize()){
        val fullW=maxWidth
        val safe=WindowInsets.safeDrawing.asPaddingValues()
        val direction=androidx.compose.ui.platform.LocalLayoutDirection.current
        val left=safe.calculateLeftPadding(direction)
        val right=safe.calculateRightPadding(direction)
        val top=safe.calculateTopPadding()
        val bottom=safe.calculateBottomPadding()
        val w=(fullW-left-right).coerceAtLeast(1.dp)
        val available=(maxHeight-top-bottom).coerceAtLeast(1.dp)
        val wide=Wq121ArenaLayout.wide(w.value,available.value,config.smallestScreenWidthDp)
        val metrics=Wq150HomeLayout.measure(available.value,wide)
        val h=metrics.height.dp
        val fullH=h+top+bottom
        val context=LocalContext.current.applicationContext
        val background=if(wide)"tablet" else "phone"
        val backgroundKey=if(newArt)"arena_v150_premium/$background.webp" else background
        val settingsTablet=config.screenWidthDp>config.screenHeightDp&&config.screenWidthDp>=600
        val settingsKey=if(settingsTablet)"arena_v160/settings/tablet.webp" else "arena_v160/settings/phone.webp"
        val keys=remember(backgroundKey,settingsKey,wide,newArt){
            Wq150HomeArtPlan.keys(backgroundKey,settingsKey,wide,newArt)
        }
        val portraitKey=Wq157OwnPortrait.key(context,session.profileKey,avatar)
        var prepared by remember(context,keys){mutableStateOf(Wq150ArtCache.frame(keys))}
        var portrait by remember(context,portraitKey){mutableStateOf(Wq157OwnPortrait.get(portraitKey))}
        LaunchedEffect(context,keys,portraitKey){
            val result=withContext(Dispatchers.IO){
                Wq150ArtCache.prepare(context,keys) to Wq157OwnPortrait.load(context,portraitKey,avatar)
            }
            portrait=result.second
            prepared=result.first
        }
        val frame=prepared
        val face=portrait
        if(frame==null||face==null){
            Box(Modifier.fillMaxSize().background(Color(0xFF150B3E)),contentAlignment=Alignment.Center){
                CircularProgressIndicator(color=Wq122Gold)
            }
        }else CompositionLocalProvider(LocalWq150HomeArt provides frame){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
            Box(Modifier.fillMaxWidth().height(fullH)){
                // Never use the v183 floating-trophy lobby art here: its podium is at another height.
                // The old approved v120 art is an exact-coordinate fallback until the new ZIP is staged.
                Wq150ApprovedArt(backgroundKey,
                    Modifier.matchParentSize(),ContentScale.Crop)
                val pos=Wq150Podium.anchor(fullW.value,fullH.value,wide,newArt)
                Box(Modifier.fillMaxSize().padding(safe)){
                val artFeet=pos.second.dp-top
                // Only compact landscape layouts may move the decorative portrait;
                // the tab and accessible controls always win over decorative overlap.
                val feet=if(wide)minOf(artFeet,(metrics.tabTop-metrics.gap).dp) else artFeet
                val avatarW=w*(if(wide).34f else .54f)
                val avatarH=(feet-h*.075f).coerceAtLeast(40.dp)
                val crestW=w*(if(wide).26f else if(newArt).34f else .48f)
                val crestH=(h*(if(wide).32f else if(newArt).17f else .21f))
                val headerH=(h*.065f).coerceIn(40.dp,60.dp)
                Box(Modifier.fillMaxWidth().height(headerH).padding(horizontal=8.dp)){
                    Surface(Modifier.align(Alignment.Center),color=Color(0xB51B1046),shape=RoundedCornerShape(18.dp)){
                        Row(Modifier.padding(horizontal=10.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
                            Wq150ApprovedArt("book",Modifier.size(if(wide)32.dp else 24.dp))
                            Text("WordQuest AI",color=Wq122Gold,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Black,fontSize=if(wide)24.sp else 17.sp,maxLines=1)
                        }
                    }
                    Surface(Modifier.align(Alignment.CenterStart),color=Color(0xC91B1046),shape=RoundedCornerShape(18.dp)){
                        TextButton(onClick=back,contentPadding=PaddingValues(horizontal=6.dp)){
                            Wq150ApprovedArt("back",Modifier.size(22.dp))
                            Text("Geri",color=Wq122Gold,fontSize=12.sp)
                        }
                    }
                }
                Wq150ApprovedArt("crest",Modifier.offset(x=w*.035f,y=headerH).width(crestW).height(crestH))
                Column(Modifier.offset(x=if(wide)w*.68f else w*.025f,y=if(wide)h*.16f else headerH+crestH)
                    .width(if(wide)w*.28f else if(newArt)w*.34f else w*.46f)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xD21A103D)).padding(6.dp),
                    horizontalAlignment=Alignment.CenterHorizontally){
                    Text("Arena’ya Hoş Geldin",color=Wq122Gold,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Black,
                        fontSize=if(wide)20.sp else 13.sp,textAlign=TextAlign.Center,maxLines=2)
                    Text("Oyun modunu seç ve kelime arenasında mücadeleye başla!",color=Color.White,
                        fontSize=if(wide)14.sp else 10.sp,lineHeight=if(wide)17.sp else 12.sp,textAlign=TextAlign.Center)
                    if(wide&&!local)TextButton(enabled=enabled,onClick={onPanel("Hızlı Oda Katılımı")}){Text("Hızlı Oda Katılımı",color=Wq122Gold,fontWeight=FontWeight.Bold,fontSize=13.sp)}
                }
                val portraitModifier=Modifier.offset(x=pos.first.dp-left-avatarW/2,y=feet-avatarH)
                    .width(avatarW).height(avatarH)
                if(face.image!=null)Image(face.image,Wq2DMascots.label(avatar),portraitModifier,
                    alignment=Alignment.BottomCenter,contentScale=ContentScale.Fit,
                    filterQuality=androidx.compose.ui.graphics.FilterQuality.High)
                else Wq2DPortrait(avatar,portraitModifier,victory=false,imageAlignment=Alignment.BottomCenter)
                val tabTop=metrics.tabTop.dp
                val tabH=metrics.tabHeight.dp
                val footerH=metrics.footerHeight.dp
                val headingH=metrics.headingHeight.dp
                val gap=metrics.gap.dp
                val cardArea=metrics.cardArea.dp
                val kinds=Wq150HomeModes.visible(local)
                Column(Modifier.offset(y=tabTop).fillMaxWidth().padding(horizontal=if(wide)12.dp else 8.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(gap)){
                    Row(Modifier.fillMaxWidth(if(wide).56f else 1f).height(tabH),horizontalArrangement=Arrangement.spacedBy(gap)){
                        Wq150HomeTab("Arkadaşınla Oyna",false,!local,enabled,Modifier.weight(1f).fillMaxHeight()){onTab(false)}
                        Wq150HomeTab("Tek Başına Oyna",true,local,enabled,Modifier.weight(1f).fillMaxHeight()){onTab(true)}
                    }
                    Row(Modifier.height(headingH).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.Center){
                        Row(Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xD92C145B)).padding(horizontal=8.dp,vertical=4.dp),
                            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                            Wq150ApprovedArt("modes",Modifier.size(20.dp))
                            Text(if(local)"Tek Kişilik Oyun Modları" else if(wide)"Çevrim İçi Oyun Modları" else "Çevrim İçi Modlar",
                                color=Wq122Gold,fontWeight=FontWeight.Black,fontSize=if(wide)16.sp else 12.sp,maxLines=1)
                        }
                        if(!wide&&!local)TextButton(enabled=enabled,onClick={onPanel("Hızlı Oda Katılımı")},
                            modifier=Modifier.padding(start=4.dp),contentPadding=PaddingValues(horizontal=5.dp)){
                            Text("Hızlı Oda Katılımı ›",color=Wq122Gold,fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)
                        }
                    }
                    if(wide)Row(Modifier.fillMaxWidth().height(cardArea),horizontalArrangement=Arrangement.spacedBy(gap)){
                        kinds.forEach{kind->Wq150HomeCard(kind,true,enabled,Modifier.weight(1f).fillMaxHeight()){onMode(kind)}}
                    }else{
                        val cardH=metrics.cardHeight(false).dp
                        kinds.forEach{kind->Wq150HomeCard(kind,false,enabled,Modifier.fillMaxWidth().height(cardH)){onMode(kind)}}
                    }
                }
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal=8.dp,vertical=gap).height(footerH),
                    horizontalArrangement=Arrangement.spacedBy(gap)){
                    listOf("ranking" to "Sıralama","friends" to "Arkadaşlarım","history" to "Son Maçlarım","settings" to "Ayarlar","info" to "Bilgi").forEach{(icon,label)->
                        Surface(onClick={onPanel(label)},modifier=Modifier.weight(1f).fillMaxHeight(),color=Color(0xE62C145B),
                            shape=RoundedCornerShape(11.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Wq122Gold)){
                            Column(Modifier.padding(3.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                                Wq150ApprovedArt(icon,Modifier.size(if(wide)27.dp else 22.dp))
                                Text(label,color=Color.White,fontSize=if(wide)12.sp else 9.sp,textAlign=TextAlign.Center,maxLines=2)
                            }
                        }
                    }
                }
                if(status!=null||onRetry!=null)Surface(Modifier.align(Alignment.TopCenter).padding(top=headerH),color=Color(0xF0251347),
                    shape=RoundedCornerShape(12.dp)){
                    Row(Modifier.padding(5.dp),verticalAlignment=Alignment.CenterVertically){
                        Text(status.orEmpty(),color=Wq122Ink,fontSize=12.sp,modifier=Modifier.widthIn(max=w*.65f))
                        if(onRetry!=null)TextButton(onClick=onRetry){Text("Tekrar dene")}
                    }
                }
            }
        }
    }
    }
    }
}
@Composable
private fun Wq150SoloSettings(session:AppSession,kind:String,onDismiss:()->Unit,onStart:(WqArenaSoloLaunch)->Unit){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val saved=remember(session.profileKey){Wq186ArenaPresetStore.read(context,session.profileKey,"A1")}
    val engine=Wq150HomeModes.localEngine(kind)
    var levels by remember(kind){mutableStateOf(saved.levels)}
    var count by remember(kind){mutableIntStateOf(if(engine=="quiz")10 else 20)}
    var seconds by remember(kind){mutableIntStateOf(Wq150HomeModes.localTimes(kind).first())}
    var hints by remember(kind){mutableIntStateOf(saved.hints.takeIf{it in Wq150HomeModes.localHints(kind)}?:0)}
    var topic by remember(kind){mutableStateOf(PremiumArenaRules.allTopics)}
    var topics by remember(kind){mutableStateOf(listOf(PremiumArenaRules.allTopics))}
    var busy by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf("")}
    LaunchedEffect(levels,engine){
        val result=withContext(Dispatchers.IO){
            runCatching{if(engine=="quiz")listOf(PremiumArenaRules.allTopics)else{
                val content=PremiumArenaContent(context.applicationContext)
                (listOf(PremiumArenaRules.allTopics)+levels.flatMap{content.topics(it)}).distinct()
            }}.getOrDefault(listOf(PremiumArenaRules.allTopics))
        }
        topics=result
        if(topic !in topics)topic=PremiumArenaRules.allTopics
    }
    Dialog(onDismissRequest={if(!busy)onDismiss()},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=true)){
        Wq150ArenaBars()
        Box(Modifier.fillMaxSize().background(Color(0xFF150B3E))){
            Wq150SettingsBackdrop(Modifier.matchParentSize())
            Column(Modifier.align(Alignment.TopCenter).widthIn(max=1200.dp).fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(horizontal=8.dp,vertical=10.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    TextButton(enabled=!busy,onClick=onDismiss){Text("‹ Geri",color=Wq122Gold)}
                    Text("Maç Ayarları",color=Wq122Gold,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Black,fontSize=21.sp)
                }
                Wq150SettingPanel("green"){
                    Text("Maç Özeti",color=Wq122Gold,fontWeight=FontWeight.Bold,fontSize=13.sp)
                    Text(levels.joinToString(" + ")+" · "+count+" "+(if(engine=="quiz")"soru" else "kelime")+" · "+(if(seconds==0)"Süresiz" else "$seconds sn")+" · $hints ipucu",color=Wq122Ink)
                    if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){
                val basic:@Composable ()->Unit={
                    Wq150SettingPanel("blue"){
                        Text(wq150Card(kind).title+" · Tek Başına",color=Wq122Gold,fontWeight=FontWeight.Bold)
                        Wq122Section("level","CEFR seviyeleri","Birden fazla seviye seçebilirsin.")
                        Wq150LevelRow(Wq150ModeCatalog.cefr,levels){level->
                            if(!busy)levels=if(level in levels){if(levels.size>1)levels-level else levels}else Wq150ModeCatalog.cefr.filter{it in levels+level}
                        }
                        if(engine!="quiz"){
                            if(!busy)Wq150Filter("Konu",topics.associateWith{it},topic){topic=it}
                            else Text("Konu: $topic",color=Color.White)
                        }
                    }
                }
                val rules:@Composable ()->Unit={
                    Wq150SettingPanel("amber"){
                        Wq122Section("count",if(engine=="quiz")"Soru sayısı" else "Kelime sayısı")
                        Wq150SoloNumbers(if(engine=="quiz")listOf(5,10,12,15,20)else PremiumArenaRules.allowedSizes,count,{it.toString()}){if(!busy)count=it}
                        Wq122Section("time","Soru süresi")
                        Wq150SoloNumbers(Wq150HomeModes.localTimes(kind),seconds,{if(it==0)"Süresiz" else "$it sn"}){if(!busy)seconds=it}
                        Wq122Section("hint","İpucu hakkı")
                        Wq150SoloNumbers(Wq150HomeModes.localHints(kind),hints,{it.toString()}){if(!busy)hints=it}
                        if(kind=="obstacle")Text("Her kelime için 20 saniye; harf açma hakkı yok.",color=Wq122Ink)
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth()){
                    if(maxWidth>=650.dp)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Column(Modifier.weight(1f)){basic()}
                        Column(Modifier.weight(1f)){rules()}
                    }else Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(6.dp)){basic();rules()}
                }
                if(error.isNotBlank())Text(error,color=Color(0xFFFFB4B4),modifier=Modifier.fillMaxWidth().background(Color(0xFF251347)).padding(8.dp))
                }
                    Wq159SettingsFooter(if(busy)"Oyun hazırlanıyor…" else "Oyunu Başlat",!busy&&count>=levels.size){
                        if(!busy){
                            busy=true;error=""
                            val id=java.util.UUID.randomUUID().toString()
                            val preset=Wq186MatchPreset(levels,hints,0)
                            val chosenCount=count;val chosenSeconds=seconds;val chosenTopic=topic
                            scope.launch{
                                try{
                                    val round=Wq132WebEngine.generate(context,engine,preset.levels,chosenTopic,chosenCount,chosenSeconds,preset.hints,preset.retries,id.hashCode())
                                    require(if(engine=="quiz")round.questions.isNotEmpty() else round.board!=null)
                                    onStart(WqArenaSoloLaunch(id,engine,preset.levels.first(),chosenTopic,chosenCount,chosenSeconds,preset,round))
                                }catch(e:kotlinx.coroutines.CancellationException){throw e}
                                catch(e:Exception){error="Oyun hazırlanamadı. Seviye, konu veya sayıyı değiştir: "+(e.message?:"").take(150)}
                                finally{busy=false}
                            }
                        }
                    }


            }
        }
    }
}
@Composable
private fun Wq150SoloNumbers(values:List<Int>,selected:Int,label:(Int)->String,onPick:(Int)->Unit){
    Wq150CompactPicker(values.associate{it.toString() to label(it)},selected.toString()){onPick(it.toInt())}
}

// One local portrait per profile/selection/revision, never the remote room bitmap.
// Keep only a small, bounded number of decoded portraits.
private object Wq157OwnPortrait {
    data class Frame(val image:ImageBitmap?)
    private val cache=android.util.LruCache<String,Frame>(2)
    fun key(context:android.content.Context,profile:String,avatar:String):String{
        val rev=context.getSharedPreferences("wq_2d_selected_173",android.content.Context.MODE_PRIVATE).getInt("art_revision",0)
        return "$profile:$avatar:$rev"
    }
    fun get(key:String)=cache.get(key)
    @Synchronized fun load(context:android.content.Context,key:String,avatar:String):Frame{
        get(key)?.let{return it}
        val bmp=runCatching{
            if(Wq183MascotStore.isCustomId(avatar))Wq183MascotStore.picture(context,avatar,false)
            else Wq2DMascots.picture(context,avatar,false)?.let{Wq184BitmapTrim.crop(it)}
        }.getOrNull()
        return Frame(bmp?.asImageBitmap()).also{cache.put(key,it)}
    }
}
private val Wq157Panel=Color(0xFF251347)
private val Wq157Tile=Color(0xFF34205B)
private val Wq157Muted=Color(0xFFD6C7EC)
private object Wq157FriendImages{
    private val cache=object:android.util.LruCache<String,ImageBitmap>(12*1024*1024){
        override fun sizeOf(key:String,value:ImageBitmap)=(value.width.toLong()*value.height*4L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
    fun key(entry:FriendEntry):String{
        val digest=java.security.MessageDigest.getInstance("SHA-256").digest(entry.avatarPreview.orEmpty().toByteArray(Charsets.UTF_8))
            .joinToString(""){"%02x".format(it)}
        return entry.uid+":"+entry.avatarId+":"+digest
    }
    fun get(key:String)=cache.get(key)
    @Synchronized fun load(key:String,entry:FriendEntry):ImageBitmap?{
        get(key)?.let{return it}
        return Wq185FriendMascot.decode(entry.avatarPreview)?.asImageBitmap()?.also{cache.put(key,it)}
    }
}
@Composable
private fun Wq157FriendFace(entry:FriendEntry,modifier:Modifier,hd:ImageBitmap?=null){
    val key=remember(entry.uid,entry.avatarId,entry.avatarPreview){Wq157FriendImages.key(entry)}
    var image by remember(key){mutableStateOf(Wq157FriendImages.get(key))}
    LaunchedEffect(key){
        if(image==null)image=withContext(Dispatchers.IO){Wq157FriendImages.load(key,entry)}
    }
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF1B1238)),
        contentAlignment=Alignment.Center){
        val art=hd?:image
        if(art!=null)Image(art,null,Modifier.fillMaxSize().padding(3.dp),contentScale=ContentScale.Fit,
            filterQuality=androidx.compose.ui.graphics.FilterQuality.High)
        else if(Wq2DMascots.valid(entry.avatarId)&&!Wq183MascotStore.isCustomId(entry.avatarId.orEmpty()))
            Wq2DPortrait(entry.avatarId!!,Modifier.fillMaxSize().padding(3.dp))
        else Text(entry.name.take(1).uppercase(),color=Wq122Gold,fontSize=24.sp,fontWeight=FontWeight.Bold)
    }
}
@Composable
private fun Wq157ArenaFriends(hub:FriendsHub,onDismiss:()->Unit){
    var lookup by remember{mutableStateOf("")}
    var selected by remember{mutableStateOf<String?>(null)}
    var remove by remember{mutableStateOf<String?>(null)}
    val entry=hub.friends.firstOrNull{it.uid==selected}
    val removing=hub.friends.firstOrNull{it.uid==remove}
    val cfg=LocalConfiguration.current
    val tablet=cfg.screenWidthDp>=600
    val maxHeight=(cfg.screenHeightDp*0.76f).coerceIn(220f,620f).dp
    Dialog(onDismissRequest={if(removing!=null)remove=null else if(selected!=null)selected=null else onDismiss()},
        properties=DialogProperties(usePlatformDefaultWidth=false)){
        Surface(Modifier.padding(horizontal=18.dp,vertical=12.dp).widthIn(max=if(tablet)620.dp else 420.dp)
            .fillMaxWidth().heightIn(max=maxHeight),
            color=Wq157Panel,shape=RoundedCornerShape(24.dp),
            border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF8862B2))){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Wq150ApprovedArt("friends",Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if(entry!=null)"Arkadaş profili" else "Arkadaşların",Modifier.weight(1f),
                        color=Wq122Gold,fontWeight=FontWeight.Black,fontSize=20.sp)
                    TextButton(onClick={if(selected!=null)selected=null else onDismiss()}){Text(if(selected!=null)"Geri" else "Kapat",color=Wq122Gold)}
                }
                if(entry!=null){
                    Column(Modifier.weight(1f,fill=false).fillMaxWidth().verticalScroll(rememberScrollState()),
                        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
                        val hd=Wq158RememberFriendHd(hub,entry)
                        Wq157FriendFace(entry,Modifier.width(if(tablet)220.dp else 180.dp).height(if(tablet)250.dp else 210.dp),hd)
                        Text(entry.name,color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Black)
                        Text(Wq180FriendActivity.label(entry.lastActiveAt),color=Wq157Muted,fontSize=13.sp)
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            listOf("Toplam puan" to entry.points,"Tamamlanan oyun" to entry.completedGames).forEach{(label,value)->
                                Surface(Modifier.weight(1f),shape=RoundedCornerShape(12.dp),color=Wq157Tile,
                                    border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF8862B2))){
                                    Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){
                                        Text(label,color=Wq157Muted,fontSize=12.sp)
                                        Text(value?.toString()?:"—",color=Wq122Gold,fontSize=22.sp,fontWeight=FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Text("Yalnızca paylaşılan profil bilgileri gösterilir.",color=Wq157Muted,fontSize=11.sp)
                    }
                }else{
                    Text("${hub.friends.size} arkadaş",color=Wq157Muted,fontSize=13.sp)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        OutlinedTextField(lookup,{lookup=it.take(ArenaNickname.maximumLength)},Modifier.weight(1f),singleLine=true,
                            label={Text("Kullanıcı adıyla ara")})
                        TextButton(enabled=ArenaNickname.isValid(lookup)&&hub.nickname.isNotBlank(),onClick={hub.searchName(lookup)}){Text("Ara")}
                    }
                    Column(Modifier.weight(1f,fill=false).fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement=Arrangement.spacedBy(8.dp)){
                        if(hub.found.isNotEmpty())Text("Arama sonuçları",color=Wq122Gold,fontWeight=FontWeight.Bold)
                        hub.found.forEach{p->Wq157FriendRow(p,"Oyuncu profili","İstek gönder",{hub.requestFriend(p.uid)},null,null,
                            enabled=hub.friends.none{it.uid==p.uid})}
                        if(hub.requests.isNotEmpty())Text("Gelen istekler",color=Wq122Gold,fontWeight=FontWeight.Bold)
                        hub.requests.forEach{p->Wq157FriendRow(p,"Arkadaşlık isteği","Kabul et",{hub.acceptFriend(p.uid)},"Reddet",{hub.rejectFriend(p.uid)})}
                        if(hub.friends.isNotEmpty())Text("Arkadaş kartları",color=Wq122Gold,fontWeight=FontWeight.Bold)
                        hub.friends.forEach{p->Wq157FriendRow(p,Wq180FriendActivity.label(p.lastActiveAt),"Profili incele",{selected=p.uid},"Arkadaşı çıkar",{remove=p.uid})}
                        if(hub.invites.isNotEmpty())Text("Gelen Arena davetleri",color=Wq122Gold,fontWeight=FontWeight.Bold)
                        hub.invites.forEach{inv->
                            val sender=hub.friends.firstOrNull{it.uid==inv.uid}?:FriendEntry(inv.uid,inv.name)
                            Wq157FriendRow(sender,"Arena daveti",null,null,"Sil",{hub.dismissInvite(inv.uid)})
                        }
                        if(hub.friends.isEmpty()&&hub.found.isEmpty()&&hub.requests.isEmpty())Text("Henüz arkadaşın yok. Kullanıcı adıyla ara.",color=Wq157Muted)
                        if(hub.status.isNotBlank())Text(hub.status,color=Wq157Muted,fontSize=12.sp)
                    }
                }
            }
        }
    }
    if(removing!=null)AlertDialog(onDismissRequest={remove=null},containerColor=Wq157Panel,
        title={Text("Arkadaşı çıkar?",color=Wq122Gold)},
        text={Text("${removing.name} arkadaş listenden çıkarılsın mı?",color=Color.White)},
        confirmButton={TextButton(onClick={hub.removeFriend(removing.uid);remove=null}){Text("Çıkar",color=Color(0xFFFFB4B4))}},
        dismissButton={TextButton(onClick={remove=null}){Text("Vazgeç",color=Wq122Gold)}})
}
@Composable
private fun Wq157FriendRow(entry:FriendEntry,description:String,primary:String?,onPrimary:(()->Unit)?,secondary:String?,onSecondary:(()->Unit)?,enabled:Boolean=true){
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),color=Wq157Tile,
        border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF8862B2))){
        Row(Modifier.padding(8.dp),horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=Alignment.CenterVertically){
            Wq157FriendFace(entry,Modifier.width(64.dp).height(82.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                Text(entry.name,color=Color.White,fontWeight=FontWeight.Bold,fontSize=16.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(description,color=Wq157Muted,fontSize=12.sp)
                val stats=listOfNotNull(entry.points?.let{"$it puan"},entry.completedGames?.let{"$it oyun"})
                if(stats.isNotEmpty())Text(stats.joinToString(" · "),color=Wq122Gold,fontSize=12.sp)
                // Stack on narrow phones; never cut off either action.
                BoxWithConstraints(Modifier.fillMaxWidth()){
                    val actions:@Composable ()->Unit={
                        if(primary!=null&&onPrimary!=null)TextButton(enabled=enabled,onClick=onPrimary,contentPadding=PaddingValues(horizontal=4.dp)){Text(primary,color=Wq122Gold,fontSize=12.sp)}
                        if(secondary!=null&&onSecondary!=null)TextButton(onClick=onSecondary,contentPadding=PaddingValues(horizontal=4.dp)){Text(secondary,color=Color(0xFFFFB4B4),fontSize=12.sp)}
                    }
                    if(maxWidth>=245.dp)Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){actions()}
                    else Column{actions()}
                }
            }
        }
    }
}

@Composable
private fun Wq161LobbyPlate(content:@Composable ColumnScope.()->Unit){
    val shape=RoundedCornerShape(19.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(Color(0xE453327F),Color(0xF025173E))))
        .border(1.dp,Color(0xB4BE95E8),shape).padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp),content=content)
}
@Composable
private fun Wq161LobbyAction(label:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,ready:Boolean=false){
    val shape=RoundedCornerShape(14.dp)
    val fill=when{ready->listOf(Color(0xFF286949),Color(0xFF164130));!enabled->listOf(Color(0xFF514465),Color(0xFF302641));else->listOf(Color(0xFF8650C7),Color(0xFF492578))}
    Box(modifier.heightIn(min=44.dp).clip(shape).background(Brush.verticalGradient(fill))
        .border(1.dp,if(ready)Color(0xFF9CE8B3)else Color(0xB4D4A7FF),shape)
        .clickable(enabled=enabled,onClick=onClick).padding(horizontal=8.dp,vertical=6.dp),contentAlignment=Alignment.Center){
        Text(label,color=Color.White,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,fontSize=13.sp,textAlign=TextAlign.Center,maxLines=2)
    }
}
@Composable
private fun Wq161LobbyPlayer(name:String,profile:JSONObject?,ownAvatar:String?,host:Boolean,present:Boolean,mine:Boolean,ready:Boolean,enabled:Boolean,
    height:Dp,modifier:Modifier,canInvite:Boolean,onInvite:()->Unit,onReady:()->Unit){
    val shape=RoundedCornerShape(21.dp)
    Column(modifier.height(height).clip(shape)
        .background(Brush.verticalGradient(listOf(Color(0xE25C328B),Color(0xF0251644))))
        .border(if(host||ready)1.7.dp else 1.dp,if(host)Wq122Gold else if(ready)Color(0xFF9CE8B3) else Color(0xFFC29BE8),shape)
        .padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)){
        Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){
            Canvas(Modifier.fillMaxWidth().height(26.dp).align(Alignment.BottomCenter)){
                val w=size.width*.84f;val h=size.height*.65f
                drawOval(if(host)Color(0xFFFFD98B)else Color(0xFFD6A3FF),topLeft=Offset((size.width-w)/2,(size.height-h)/2),size=Size(w,h),style=Stroke(1.5.dp.toPx()))
            }
            if(present)Wq150Portrait(profile,ownAvatar,Modifier.fillMaxSize().padding(top=12.dp,bottom=8.dp))
            else Column(horizontalAlignment=Alignment.CenterHorizontally){
                if(canInvite)Box(Modifier.size(76.dp).clip(RoundedCornerShape(24.dp))
                    .background(Color(0xB3492777)).border(1.dp,Wq122Gold,RoundedCornerShape(24.dp))
                    .clickable(onClick=onInvite),contentAlignment=Alignment.Center){
                    Text("+",color=Wq122Gold,fontSize=44.sp,fontWeight=FontWeight.Light)
                }else Wq150ApprovedArt("friends",Modifier.size(58.dp))
                Text(if(canInvite)"Arkadaş davet et" else "Oyuncu bekleniyor",color=Color(0xFFE7D9FF),fontSize=12.sp,textAlign=TextAlign.Center)
            }
            if(host)Text("Oda sahibi",Modifier.align(Alignment.TopStart).clip(RoundedCornerShape(9.dp)).background(Color(0xE52A154B)).padding(5.dp),
                color=Wq122Gold,fontSize=11.sp,fontWeight=FontWeight.Bold)
        }
        Text(name,color=Color.White,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,fontSize=17.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        Wq161LobbyAction(if(!present&&canInvite)"Davet et +" else if(!present)"Bekleniyor" else if(ready)"✓ Hazır" else if(mine)"Hazırım" else "Hazır değil",if(!present)onInvite else onReady,
            Modifier.fillMaxWidth(),enabled=if(present)mine&&enabled else canInvite,ready=ready)
    }
}
