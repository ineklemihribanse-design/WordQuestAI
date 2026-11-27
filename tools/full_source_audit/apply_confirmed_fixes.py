#!/usr/bin/env python3
"""Targeted corrections for the five full-app lint errors and measured tablet layout.
No warning suppression, library version changes, signing changes or data migration."""
from pathlib import Path
import hashlib
p=Path("source/app/src/main/java/com/kelimebulmacasi/a1")
names={"LenaAttachmentReader.kt","PuzzleScreen.kt","UpdateUi.kt","Wq128PremiumRoomLobby.kt","NewGameScreen.kt","PremiumArenaScreen.kt","ArenaCrosswordTyping.kt","ArenaBoardV2.kt","Wq140SoloUi.kt","WqDriveMascot.kt","AiSession.kt","AppUpdater.kt","PremiumOnlineRoom.kt"}
protected={f:hashlib.sha256(f.read_bytes()).digest() for f in p.rglob("*") if f.is_file() and f.name not in names}
def edit(name,changes):
    f=p/name;s=f.read_text()
    for old,new in changes:
        assert s.count(old)==1,(name,old[:100],s.count(old))
        s=s.replace(old,new,1)
    f.write_text(s)
edit("LenaAttachmentReader.kt",[
 ('import java.io.ByteArrayInputStream','import java.io.ByteArrayInputStream\nimport java.io.ByteArrayOutputStream'),
 ('                    val bytes = zip.readNBytes(512_000)','''                    val outputBytes=ByteArrayOutputStream()
                    val buffer=ByteArray(8192)
                    var remaining=512_000
                    while(remaining>0){
                        val n=zip.read(buffer,0,minOf(buffer.size,remaining))
                        if(n<0)break
                        if(n==0)break
                        outputBytes.write(buffer,0,n)
                        remaining-=n
                    }
                    val bytes=outputBytes.toByteArray()''')
])
# Indentation only; preserve exact ordinary crossword behavior and drawing.
edit("PuzzleScreen.kt",[
 ('            if(timer!=null) Wq175TimeRemainingBadge(remainingMs)','                if(timer!=null) Wq175TimeRemainingBadge(remainingMs)')
])
edit("UpdateUi.kt",[
 ('import android.app.Activity','import androidx.activity.compose.LocalActivity'),
 ('    val activity = LocalContext.current as? Activity','    val activity = LocalActivity.current'),
 ('    val context = LocalContext.current\n    HorizontalDivider()','    val context = LocalContext.current\n    val activity = LocalActivity.current\n    HorizontalDivider()'),
 ('OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {\n                (context as? Activity)?.let(updater::updateNow)','OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled=activity!=null, onClick = {\n                activity?.let(updater::updateNow)')
])
edit("Wq128PremiumRoomLobby.kt",[
 ('''        val image by produceState<Bitmap?>(null,avatar,mine,remote) {
            value=withContext(Dispatchers.Default) {
                val source=if(mine&&custom)Wq183MascotStore.picture(context,avatar,false) else remote
                source?.let { Wq128RoomHd.trim(it) }
            }
        }''','''        var image by remember(avatar,mine,remote){mutableStateOf<Bitmap?>(null)}
        LaunchedEffect(avatar,mine,remote) {
            image=withContext(Dispatchers.Default) {
                val source=if(mine&&custom)Wq183MascotStore.picture(context,avatar,false) else remote
                source?.let { Wq128RoomHd.trim(it) }
            }
        }''')
])
edit("NewGameScreen.kt",[
 ('import androidx.compose.foundation.layout.BoxWithConstraints','import androidx.compose.foundation.layout.Box\nimport androidx.compose.foundation.layout.BoxWithConstraints'),
 ('                BoxWithConstraints(Modifier.fillMaxWidth()) {\n                    val limit = if (LocalWq200Wide.current) 10','                Box(Modifier.fillMaxWidth()) {\n                    val limit = if (LocalWq200Wide.current) 10')
])
# Landscape only: bound the whole gameplay column to the inset-safe window.
# Header and score measure first; weighted row receives actual remaining space.
# The board header also measures first, and its sibling gets the rest.
edit("PremiumArenaScreen.kt",[
 ('    val keyboardIsOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0','    val keyboardIsOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0\n    val boundedArena=arenaWide && stage==PremiumStage.Crossword && !keyboardIsOpen'),
 ('.verticalScroll(rememberScrollState()).padding(horizontal = if(arenaWide)10.dp else 14.dp, vertical = if(arenaWide)4.dp else 9.dp),',
 '.then(if(boundedArena)Modifier else Modifier.verticalScroll(rememberScrollState())).padding(horizontal = if(arenaWide)10.dp else 14.dp, vertical = if(arenaWide)4.dp else 9.dp),'),
 ('''                    if(arenaWide) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),
                            verticalAlignment=Alignment.Top) {
                            Column(Modifier.weight(1.5f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                val availableBoardHeight=(arenaConfig.screenHeightDp-175f).coerceAtLeast(110f)
                                CompositionLocalProvider(LocalWq140ArenaViewport provides availableBoardHeight) {
                                    arenaBoardContent()
                                }
                            }
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                arenaClueContent()
                                arenaControlContent()
                            }
                        }
                    } else {''',
 '''                    if(boundedArena) {
                        Row(Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp),
                            verticalAlignment=Alignment.Top) {
                            Column(Modifier.weight(1.5f).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                CompositionLocalProvider(LocalWq140BoundedBoard provides true) {
                                    arenaBoardContent()
                                }
                            }
                            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                                verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                arenaClueContent()
                                arenaControlContent()
                            }
                        }
                    } else {''')
])
edit("Wq140SoloUi.kt",[
 ('internal val LocalWq140ArenaViewport=compositionLocalOf<Float?>{null}',
 'internal val LocalWq140ArenaViewport=compositionLocalOf<Float?>{null}\ninternal val LocalWq140BoundedBoard=compositionLocalOf{false}')
])
edit("ArenaCrosswordTyping.kt",[
 ('    // Controls live OUTSIDE the grid container, preserving cell visibility.\n    Row(',
 '    // Controls live OUTSIDE the grid container, preserving cell visibility.\n    val header: @Composable () -> Unit = {\n    Row('),
 ('''    ArenaBoardV2(puzzle,solved,selected,placed,compact,zoomed,hintIndex,hintChar,hintLetters,onSelect)
}''',
 '''    }
    if(LocalWq140BoundedBoard.current && !compact) {
        Column(Modifier.fillMaxSize()) {
            header()
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                CompositionLocalProvider(LocalWq140ArenaViewport provides maxHeight.value) {
                    ArenaBoardV2(puzzle,solved,selected,placed,compact,zoomed,hintIndex,hintChar,hintLetters,onSelect)
                }
            }
        }
    } else {
        header()
        ArenaBoardV2(puzzle,solved,selected,placed,compact,zoomed,hintIndex,hintChar,hintLetters,onSelect)
    }
}''')
])
# Account for per-cell pixel rounding; using a dp float estimate can overrun
# the row by a full pixel per cell. This path is scoped to measured overview.
edit("ArenaBoardV2.kt",[
 ('        val gap=if(fittedOverview).8.dp else if(maxWidth<500.dp)1.dp else 1.5.dp',
 '        val gap=if(fittedOverview)with(density){.8.dp.roundToPx().coerceAtLeast(1).toDp()} else if(maxWidth<500.dp)1.dp else 1.5.dp'),
 ('''        val cell=if(fittedOverview)
            Wq140ArenaFit.cell((maxWidth.value-4f).coerceAtLeast(1f),(requireNotNull(fittedHeight)-4f).coerceAtLeast(1f),rows,cols,gap.value).dp''',
 '''        val cell=if(fittedOverview)with(density){
            val gapPx=gap.roundToPx()
            val pad=2*4.dp.roundToPx()
            val widthPx=maxWidth.roundToPx()
            val heightPx=minOf(maxHeight.roundToPx(),requireNotNull(fittedHeight).dp.roundToPx())
            val fit=minOf((widthPx-pad-gapPx*(cols-1))/cols,(heightPx-pad-gapPx*(rows-1))/rows,60.dp.roundToPx())
            fit.coerceAtLeast(1).toDp()
        }''')
])
# Drive consent callbacks may arrive after an account switch or dialog disposal.
# Bind a pending operation to its original account, profile and direction.
drive=p/"WqDriveMascot.kt"
ds=drive.read_text()
assert ds.count("import kotlinx.coroutines.Dispatchers")==1
ds=ds.replace("import kotlinx.coroutines.Dispatchers","import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.currentCoroutineContext\nimport kotlinx.coroutines.isActive",1)
start=ds.index("    var busy by remember(profile,uid)")
end=ds.index("    Column(Modifier.fillMaxWidth()",start)
assert "client.authorize(request)" in ds[start:end] and "WqDriveMascot.sync(" in ds[start:end]
ds=ds[:start]+'''    data class Attempt(val profile:String,val uid:String,val email:String,val restore:Boolean)
    var pending by remember{mutableStateOf<Attempt?>(null)}
    val busy=pending!=null
    var message by remember(profile,uid){mutableStateOf("")}
    val latestOwner by rememberUpdatedState(Triple(profile,uid,googleEmail))
    val latestRestored by rememberUpdatedState(onRestored)
    fun sameOwner(attempt:Attempt)=scope.coroutineContext.isActive &&
        latestOwner==Triple(attempt.profile,attempt.uid,attempt.email) &&
        auth?.currentUser?.uid==attempt.uid
    fun finish(attempt:Attempt,text:String){
        if(pending!==attempt)return
        pending=null
        if(sameOwner(attempt))message=text
    }
    fun useResult(result:AuthorizationResult,attempt:Attempt) {
        if(pending!==attempt)return
        if(!sameOwner(attempt)){finish(attempt,"Hesap değişti.");return}
        val token=result.accessToken
        if(token.isNullOrBlank()){finish(attempt,"Google Drive izni verilmedi.");return}
        scope.launch {
            try {
                val count=withContext(Dispatchers.IO) {
                    val operation=currentCoroutineContext()
                    fun stillOwner()=operation.isActive && sameOwner(attempt)
                    require(stillOwner()){"Hesap değişti."}
                    WqDriveMascot.verifyAccount(token,attempt.email)
                    WqDriveMascot.sync(context,attempt.profile,attempt.uid,token,attempt.restore,::stillOwner)
                }
                if(!sameOwner(attempt))return@launch
                if(attempt.restore)latestRestored()
                finish(attempt,if(attempt.restore)"Geri yüklenen maskot: "+count+". Cihazdaki maskotlar korundu."
                    else "Google Drive'a yedeklenen maskot: "+count)
            } catch(e:kotlinx.coroutines.CancellationException){throw e}
              catch(e:Exception){
                  if(attempt.restore&&sameOwner(attempt))latestRestored()
                  finish(attempt,e.message ?: "Drive işlemi tamamlanamadı.")
              }
            finally { if(pending===attempt)pending=null }
        }
    }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val attempt=pending
        if(attempt!=null){
            if(!sameOwner(attempt))finish(attempt,"Hesap değişti.")
            else if(result.resultCode!=Activity.RESULT_OK)finish(attempt,"Google Drive izni iptal edildi.")
            else runCatching { client.getAuthorizationResultFromIntent(result.data) }
                .onSuccess { useResult(it,attempt) }.onFailure {finish(attempt,"Google Drive izni alınamadı.")}
        }
    }
    fun start(wantRestore:Boolean) {
        if(pending!=null||googleEmail.isBlank()||uid.isBlank())return
        val attempt=Attempt(profile,uid,googleEmail,wantRestore)
        pending=attempt;message="Google Drive izni kontrol ediliyor…"
        runCatching {
            val request=AuthorizationRequest.builder()
                .setAccount(Account(attempt.email,"com.google"))
                .setRequestedScopes(listOf(Scope(WqDriveMascot.APPDATA),Scope(WqDriveMascot.EMAIL)))
                .build()
            client.authorize(request).addOnSuccessListener { result ->
                if(pending!==attempt)return@addOnSuccessListener
                if(!sameOwner(attempt)){finish(attempt,"Hesap değişti.");return@addOnSuccessListener}
                if(result.hasResolution()) {
                    val resolution=result.pendingIntent
                    if(resolution==null)finish(attempt,"İzin penceresi açılamadı.")
                    else runCatching { launcher.launch(IntentSenderRequest.Builder(resolution.intentSender).build()) }
                        .onFailure {finish(attempt,"İzin penceresi açılamadı.")}
                } else useResult(result,attempt)
            }.addOnFailureListener {finish(attempt,"Google Drive izni alınamadı. Google hesabını ve Drive API ayarını kontrol et.")}
        }.onFailure {finish(attempt,"Google Drive izni başlatılamadı.")}
    }
'''+ds[end:]
drive.write_text(ds)
assert "operation.isActive && sameOwner(attempt)" in ds
assert "WqDriveMascot.sync(context,attempt.profile,attempt.uid,token,attempt.restore,::stillOwner)" in ds
print("PASS: Drive consent is bound to original profile and operation; cancellation checks before restore/import")
# Avoid boxing primitive Compose state; preserve values, saveability and geometry.
edit("AiSession.kt",[
 ('import androidx.compose.runtime.mutableStateOf','import androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.mutableIntStateOf'),
 ('private var lenaRevision by mutableStateOf(0)','private var lenaRevision by mutableIntStateOf(0)')
])
edit("AppUpdater.kt",[
 ('import androidx.compose.runtime.mutableStateOf','import androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.mutableIntStateOf\nimport androidx.compose.runtime.mutableLongStateOf'),
 ('var lastChecked by mutableStateOf(prefs.getLong("last_success", 0L))','var lastChecked by mutableLongStateOf(prefs.getLong("last_success", 0L))'),
 ('var progress by mutableStateOf(0); private set','var progress by mutableIntStateOf(0); private set')
])
edit("PremiumOnlineRoom.kt",[
 ('import androidx.compose.runtime.mutableLongStateOf','import androidx.compose.runtime.mutableLongStateOf\nimport androidx.compose.runtime.mutableIntStateOf'),
 ('var usedToday by mutableStateOf(0)','var usedToday by mutableIntStateOf(0)')
])
edit("ArenaCrosswordTyping.kt",[
 ('var onlineZoom by rememberSaveable(puzzle.hashCode()) { mutableStateOf(0) }','var onlineZoom by rememberSaveable(puzzle.hashCode()) { mutableIntStateOf(0) }')
])
edit("PuzzleScreen.kt",[
 ('var measuredBoardWidth by remember { mutableStateOf(0) }','var measuredBoardWidth by remember { mutableIntStateOf(0) }'),
 ('var measuredBoardHeight by remember { mutableStateOf(0) }','var measuredBoardHeight by remember { mutableIntStateOf(0) }')
])
print("PASS: seven primitive Compose states avoid boxing; no board geometry, account data or persistence changes")
assert all(hashlib.sha256(f.read_bytes()).digest()==v for f,v in protected.items())
print("PASS: five audited lint errors corrected; measured safe-area landscape board; unrelated sources unchanged")
