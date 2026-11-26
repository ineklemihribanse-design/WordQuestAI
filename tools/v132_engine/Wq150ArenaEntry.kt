package com.kelimebulmacasi.a1

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
    val auth=session.accounts.socialAuth()
    val uid=session.accounts.uid.orEmpty()
    val controller=remember(endpoint,auth,uid){auth?.let{Wq150LobbyController(Wq150Gateway(endpoint,it))}}
    if(controller==null||uid.isBlank()){
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp)){
            Text("Arena · yeni oyun sistemi",style=MaterialTheme.typography.titleLarge)
            Text("Arkadaşlarınla oynamak için giriş yap.")
            Button(onClick=account){Text("Giriş yap")}
            TextButton(onClick=legacy){Text("Mevcut Arena / tek başına")}
            TextButton(onClick=back){Text("Geri")}
        }
        return
    }
    val state by controller.state.collectAsState()
    val scope=rememberCoroutineScope()
    var settings by remember {mutableStateOf(Wq150Settings())}
    var edit by rememberSaveable {mutableStateOf(false)}
    var roomCode by rememberSaveable {mutableStateOf("")}
    var exit by rememberSaveable {mutableStateOf(false)}
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
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    TextButton(onClick={if(lobby||active)exit=true else back()}){Text("‹ Geri",color=Wq122Gold)}
                    Spacer(Modifier.weight(1f));Text("WORDQUEST · ARENA",color=Wq122Gold,fontWeight=FontWeight.Bold)
                }
                if(!lobby&&!active){
                    Text("Arkadaşınla oyna",style=MaterialTheme.typography.headlineSmall,color=Wq122Ink,fontWeight=FontWeight.Black)
                    Wq150ModeCatalog.kinds.forEach{(kind,label)->
                        Wq122Primary(label,!state.busy&&!state.pending,{
                            settings=settings.changeKind(kind);edit=true
                        },green=false)
                    }
                    OutlinedTextField(value=roomCode,onValueChange={roomCode=it.trim().take(64)},singleLine=true,
                        label={Text("Davet kodu")},modifier=Modifier.fillMaxWidth(),enabled=!state.busy&&!state.pending)
                    Wq122Primary("Odaya katıl",roomCode.matches(Regex("^[A-Za-z0-9_-]{6,64}$"))&&!state.busy&&!state.pending,{
                        scope.launch{controller.join(roomCode)}
                    },green=false)
                    TextButton(onClick=legacy,enabled=!state.busy&&!state.pending){Text("Mevcut Arena / tek başına",color=Wq122Gold)}
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
                    var myReady=false
                    for(i in 0 until capacity){
                        val player=members?.optJSONObject(i)
                        val id=player?.optString("uid").orEmpty()
                        val ready=player?.optBoolean("ready")==true
                        if(id==uid)myReady=ready
                        Surface(color=Color(0xDD251347),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()){
                            Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                                Text(if(id==uid)"Sen" else if(player==null)"Oyuncu bekleniyor" else "Oyuncu "+(i+1),color=Wq122Ink,
                                    modifier=Modifier.weight(1f),fontWeight=FontWeight.Bold)
                                Text(if(player==null)"—" else if(ready)"Hazır" else "Bekliyor",color=if(ready)Color(0xFF91E8AA)else Wq122Gold)
                            }
                        }
                    }
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
        if(edit)Wq150MatchSettingsDialog(settings,emptyMap(),emptyMap(),!state.busy&&!state.pending,
            onDismiss={edit=false},onApply={next->
                settings=next;edit=false
                scope.launch{if(lobby)controller.action("settings",settings=next) else controller.create(next)}
            })
        if(exit)AlertDialog(onDismissRequest={exit=false},title={Text("Arena'dan ayrıl?")},
            text={Text(if(active)"Aktif maçın durumu korunur. Bu ekrandan ayrılmak maçtan hükmen çıkış değildir." else "Odadan ayrılmak istediğine emin misin?")},
            confirmButton={TextButton(onClick={exit=false;if(lobby)scope.launch{controller.action("leave")} else back()}){Text("Ayrıl")}},
            dismissButton={TextButton(onClick={exit=false}){Text("Vazgeç")}})
    }
}
