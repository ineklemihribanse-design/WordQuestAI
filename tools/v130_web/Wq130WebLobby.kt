package com.kelimebulmacasi.a1

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceError
import android.webkit.RenderProcessGoneDetail
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** Signed HTML/CSS/JS for an isolated WebView, not downloaded Android executable code. */
internal object Wq130WebBundle {
    const val PATH="web/arena_lobby.html"
    const val MAX_BYTES=96*1024
    const val ORIGIN="https://wq-local.invalid/"
    private const val CSP="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'"
    fun validate(bytes:ByteArray):String {
        require(bytes.size in 200..MAX_BYTES)
        val text=String(bytes,Charsets.UTF_8)
        require(text.startsWith("<!DOCTYPE html>")&&text.contains(CSP)&&text.contains("window.wqSetState"))
        return text
    }
    fun load(c:Context):String?=runCatching {
        WqContentUpdates.open(c,PATH).use { input ->
            val out=ByteArrayOutputStream()
            val buf=ByteArray(4096)
            while(true){val n=input.read(buf);if(n<0)break;require(out.size()+n<=MAX_BYTES);out.write(buf,0,n)}
            validate(out.toByteArray())
        }
    }.getOrNull()
    fun action(uri:Uri,gesture:Boolean,mainFrame:Boolean):String? {
        if(!gesture||!mainFrame||uri.scheme!="https"||uri.host!="wq-local.invalid"||
            uri.port!=-1||uri.userInfo!=null||uri.query!=null||uri.fragment!=null)return null
        return when(uri.encodedPath) {
            "/action/ready"->"ready";"/action/start"->"start";"/action/invite"->"invite"
            "/action/edit"->"edit";"/action/info"->"info";"/action/copy"->"copy";"/action/share"->"share"
            else->null
        }
    }
}

@Composable
private fun webPortrait130(avatar:String,mine:Boolean,present:Boolean):String {
    val c=LocalContext.current
    val remote=if(!mine&&present)Wq183ArenaShare.remoteImage(avatar) else null
    val result by produceState("",avatar,mine,present,remote) {
        value=if(!present)"" else withContext(Dispatchers.Default) {
            runCatching {
                val source=if(mine&&Wq183MascotStore.isCustomId(avatar)) Wq183MascotStore.picture(c,avatar,false)
                    else remote?:Wq2DMascots.picture(c,if(Wq183MascotStore.isCustomId(avatar))"piko" else avatar,false)
                if(source==null)return@runCatching ""
                val trimmed=Wq128RoomHd.trim(source)
                val scale=minOf(1f,768f/maxOf(trimmed.width,trimmed.height))
                val bitmap=Bitmap.createScaledBitmap(trimmed,(trimmed.width*scale).toInt().coerceAtLeast(1),
                    (trimmed.height*scale).toInt().coerceAtLeast(1),true)
                try {
                    for(q in listOf(88,72,55)){
                        val out=ByteArrayOutputStream()
                        if(bitmap.compress(Bitmap.CompressFormat.WEBP,q,out)&&out.size()<=280_000)
                            return@runCatching "data:image/webp;base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP)
                    }
                    ""
                } finally {if(bitmap!==trimmed)bitmap.recycle()}
            }.getOrDefault("")
        }
    }
    return result
}

@Composable
internal fun Wq130WebLobby(match:PremiumOnlineMatch,client:PremiumOnlineRoom,isHost:Boolean,myUid:String,
    hostName:String,guestName:String,onAction:(String)->Unit,onFallback:()->Unit) {
    val context=LocalContext.current
    val config=LocalConfiguration.current
    val html=remember(context.applicationContext){Wq130WebBundle.load(context)}
    var failed by remember(match.code){mutableStateOf(false)}
    var loaded by remember(match.code){mutableStateOf(false)}
    val guestPresent=match.guestUid.isNotBlank()
    val hostReady=match.hostUid in match.readyBy
    val guestReady=guestPresent&&match.guestUid in match.readyBy
    val hostImage=webPortrait130(match.hostAvatar,myUid==match.hostUid,true)
    val guestImage=webPortrait130(match.guestAvatar,myUid==match.guestUid,guestPresent)
    val model=JSONObject().apply {
        put("code",match.code);put("mode",when(match.mode){"team"->"Ortak";"quiz"->"5 Şıklı";else->"Düello"})
        put("level",match.level);put("time",if(match.seconds==0)"Süresiz" else "${match.seconds} sn")
        put("count","${match.size} ${if(match.mode=="quiz")"soru" else "kelime"}")
        put("hostName",hostName);put("guestName",if(guestPresent)guestName else "Arkadaş bekleniyor")
        put("hostImage",hostImage);put("guestImage",guestImage);put("isHost",isHost)
        put("hostReady",hostReady);put("guestReady",guestReady);put("guestPresent",guestPresent);put("busy",client.busy)
        put("status",if(hostReady&&guestReady)"İki oyuncu da hazır." else if(!guestPresent)"Arkadaşının katılması bekleniyor." else "İki oyuncu da hazır olmalı.")
        put("error",client.status.take(300))
    }.toString()
    val currentModel by rememberUpdatedState(model)
    val action by rememberUpdatedState(onAction)
    val canAct by rememberUpdatedState(match.phase=="lobby"&&!client.busy&&myUid.isNotBlank()&&
        (myUid==match.hostUid||myUid==match.guestUid))
    val host by rememberUpdatedState(isHost)
    val allReady by rememberUpdatedState(hostReady&&guestReady)
    if(html==null||failed) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Text("Web lobi açılamadı. Yerel lobi kullanılabilir.",color=Color.White)
            Button(onClick=onFallback){Text("Yerel Lobiye Dön")}
        }
        return
    }
    val view=remember(match.code,html) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            settings.apply {
                javaScriptEnabled=true
                domStorageEnabled=false
                allowFileAccess=false
                allowContentAccess=false
                blockNetworkLoads=true
                javaScriptCanOpenWindowsAutomatically=false
                setSupportMultipleWindows(false)
                mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            webViewClient=object:WebViewClient(){
                override fun shouldOverrideUrlLoading(v:WebView,request:WebResourceRequest):Boolean {
                    val verb=Wq130WebBundle.action(request.url,request.hasGesture(),request.isForMainFrame)
                    if(verb!=null&&canAct) {
                        when(verb) {
                            "ready"->action(verb)
                            "start"->if(host&&allReady)action(verb)
                            "invite","edit"->if(host)action(verb)
                            "copy","share","info"->action(verb)
                        }
                    }
                    return true
                }
                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(v:WebView,url:String):Boolean=true
                override fun shouldInterceptRequest(v:WebView,request:WebResourceRequest):WebResourceResponse? {
                    if(request.url.scheme=="data")return null
                    return WebResourceResponse("text/plain","UTF-8",403,"Blocked",emptyMap(),ByteArray(0).inputStream())
                }
                override fun onPageFinished(v:WebView,url:String) {
                    loaded=true
                    v.evaluateJavascript("window.wqSetState("+JSONObject.quote(currentModel)+")",null)
                }
                override fun onReceivedError(v:WebView,request:WebResourceRequest,error:WebResourceError){if(request.isForMainFrame)failed=true}
                override fun onRenderProcessGone(v:WebView,detail:RenderProcessGoneDetail):Boolean{failed=true;return true}
            }
            loadDataWithBaseURL(Wq130WebBundle.ORIGIN,html,"text/html","UTF-8",null)
        }
    }
    DisposableEffect(view){onDispose{view.stopLoading();view.destroy()}}
    LaunchedEffect(view,model,loaded) {
        if(loaded)view.evaluateJavascript("window.wqSetState("+JSONObject.quote(model)+")",null)
    }
    AndroidView(factory={view},modifier=Modifier.fillMaxWidth().height((config.screenHeightDp-105).coerceAtLeast(360).dp))
}
