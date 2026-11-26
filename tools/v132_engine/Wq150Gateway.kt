package com.kelimebulmacasi.a1

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.net.HttpURLConnection
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.coroutineContext

internal fun wq150ValidRoom(id:String)=Regex("^[A-Za-z0-9_-]{6,64}$").matches(id)&&id !in listOf("constructor","prototype","__proto__")

/** Explicitly configured HTTPS origin. Never discover a gateway from room content. */
internal class Wq150Endpoint(raw:String) {
    private val origin:URI=URI(raw.trim()).also{
        require(it.scheme=="https"&&!it.host.isNullOrBlank()&&it.userInfo==null&&
            it.rawQuery==null&&it.rawFragment==null&&it.port in listOf(-1,443)&&
            (it.rawPath.isNullOrEmpty()||it.rawPath=="/")){"Geçersiz Arena sunucusu"}
    }
    private fun roomUrl(path:String,id:String,commands:Boolean):URI {
        require(wq150ValidRoom(id))
        return origin.resolve("/v150/"+path+"/"+id+(if(commands)"/commands" else ""))
    }
    fun matchUrl(id:String,commands:Boolean=false):URI=roomUrl("matches",id,commands)
    fun lobbyUrl(id:String,commands:Boolean=false):URI=roomUrl("lobbies",id,commands)
    fun createUrl():URI=origin.resolve("/v150/lobbies")
    fun presenceUrl(id:String):URI=roomUrl("presence",id,false)
    fun profileUrl(id:String,version:String?=null):URI {
        val base=roomUrl("profiles",id,false)
        if(version==null)return base
        require(Regex("^[a-f0-9]{64}$").matches(version))
        return URI(base.toString()+"?version="+version)
    }
}

/** Stable request ID for safe retry. Never include a client actor or clock. */
internal data class Wq150Command(
    val revision:Long,val type:String,val word:Int?=null,val index:Int?=null,
    val item:Int?=null,val answer:String?=null,val choice:Int?=null,
    val id:String=UUID.randomUUID().toString(),val symbol:String?=null,val offerId:String?=null
) {
    val social:Boolean get()=type in setOf("reaction","finish-offer","finish-accept","finish-reject","finish-withdraw")
    fun valid():Boolean {
        if(revision<0||revision>9007199254740991L||!Regex("^[A-Za-z0-9_-]{1,100}$").matches(id)||
            id in listOf("constructor","prototype","__proto__"))return false
        if(social){
            if(listOf(word,index,item,answer,choice).any{it!=null})return false
            return when(type){
                "reaction" -> symbol in listOf("👏","👍","😊","😄","😮","🔥","🎉","💪","🤔","😢","❤️","🏆")&&offerId==null
                "finish-offer" -> symbol==null&&offerId==null
                else -> symbol==null&&offerId!=null&&Wq150LobbyCommand.validId(offerId)
            }
        }
        if(symbol!=null||offerId!=null)return false
        if(type !in setOf("submit","answer","pass","skip","choose","hint","eliminate","half","time"))return false
        if(listOf(word,index,item).count{it!=null}!=1||listOfNotNull(word,index,item).any{it !in 0..69})return false
        if(choice!=null&&choice !in 0..4)return false
        if(answer!=null&&(answer.isBlank()||answer.length>80))return false
        return when(type){
            "submit" -> (answer!=null)!=(choice!=null)
            "answer" -> choice!=null&&answer==null
            else -> answer==null&&choice==null
        }
    }
    fun json():JSONObject {
        require(valid())
        return JSONObject().put("id",id).put("revision",revision).put("type",type).also{j->
            word?.let{j.put("word",it)};index?.let{j.put("index",it)}
            item?.let{j.put("item",it)};answer?.let{j.put("answer",it)};choice?.let{j.put("choice",it)}
            symbol?.let{j.put("symbol",it)};offerId?.let{j.put("offerId",it)}
        }
    }
}

internal class Wq150GatewayError(val status:Int,val code:String):IOException("Arena isteği başarısız: "+code)

/** Uses the current Firebase identity; tokens and answers are never logged. */
internal class Wq150Gateway(private val endpoint:Wq150Endpoint,private val auth:FirebaseAuth) {
    private suspend fun token():String {
        val user=auth.currentUser?:throw Wq150GatewayError(401,"unauthenticated")
        val uid=user.uid
        return suspendCancellableCoroutine { c->
            user.getIdToken(false).addOnCompleteListener{task->
                if(c.isActive){
                    val value=if(task.isSuccessful)task.result?.token else null
                    if(value.isNullOrBlank()||auth.currentUser?.uid!=uid)
                        c.resumeWithException(Wq150GatewayError(401,"unauthenticated"))
                    else c.resume(value)
                }
            }
        }
    }
    suspend fun profiles(room:String,version:String?):JSONObject=request(endpoint.profileUrl(room,version),null)
    suspend fun publishPortrait(room:String,avatar:String,preview:String?,victory:String?=null){
        val body=JSONObject().put("avatar",avatar).put("preview",preview?:JSONObject.NULL)
        if(victory!=null)body.put("victory",victory)
        request(endpoint.profileUrl(room),body)
    }
    suspend fun view(room:String):JSONObject=matchView(request(endpoint.matchUrl(room),null))
    suspend fun presence(room:String,session:String,leave:Boolean=false):JSONObject {
        require(Regex("^[A-Za-z0-9_-]{16,100}$").matches(session))
        return matchView(request(endpoint.presenceUrl(room),JSONObject()
            .put("type",if(leave)"leave" else "heartbeat").put("session",session)))
    }
    suspend fun send(room:String,command:Wq150Command):JSONObject=
        matchView(request(endpoint.matchUrl(room,true),command.json()))
    suspend fun createLobby(settings:Wq150Settings,id:String):Wq150LobbyResponse {
        require(Wq150LobbyCommand.validId(id))
        return lobbyResponse(request(endpoint.createUrl(),JSONObject().put("id",id).put("settings",settings.json())))
    }
    suspend fun lobby(room:String):Wq150LobbyResponse=lobbyResponse(request(endpoint.lobbyUrl(room),null),room)
    suspend fun lobbyAction(room:String,command:Wq150LobbyCommand):Wq150LobbyResponse=
        lobbyResponse(request(endpoint.lobbyUrl(room,true),command.json()),room,command.type=="leave")
    private fun matchView(response:JSONObject):JSONObject {
        val view=response.optJSONObject("view")?:throw IOException("Eksik Arena durumu")
        if(view.optInt("schema")!=150||view.optLong("revision",-1)<0||
            view.optString("phase") !in setOf("playing","paused","finished"))
            throw IOException("Geçersiz Arena durumu")
        return view
    }
    private fun lobbyResponse(response:JSONObject,expected:String?=null,allowEmpty:Boolean=false):Wq150LobbyResponse {
        val id=response.optString("id")
        if(!Regex("^[A-Za-z0-9_-]{6,64}$").matches(id)||expected!=null&&expected!=id)
            throw IOException("Geçersiz oda kodu")
        val view=response.optJSONObject("view")
        if(view==null&&!allowEmpty)throw IOException("Eksik oda durumu")
        if(view!=null){
            if(view.optString("phase")=="lobby"){
                if(view.optInt("schema")!=150||view.optLong("revision",-1)<0||
                    view.optLong("configRevision",-1)<0||view.optJSONArray("members")==null||
                    view.optInt("capacity") !in listOf(2,4))throw IOException("Geçersiz oda durumu")
            }else matchView(response)
        }
        return Wq150LobbyResponse(id,view)
    }
    private suspend fun request(uri:URI,json:JSONObject?):JSONObject {
        val url=uri.toURL()
        val bearer=token()
        val body=json?.toString()?.toByteArray(Charsets.UTF_8)
        return withContext(Dispatchers.IO){
            val conn=url.openConnection() as HttpURLConnection
            try{
                conn.instanceFollowRedirects=false
                conn.connectTimeout=8000;conn.readTimeout=12000
                conn.useCaches=false
                conn.requestMethod=if(body==null)"GET" else "POST"
                conn.setRequestProperty("Authorization","Bearer "+bearer)
                conn.setRequestProperty("Accept","application/json")
                if(body!=null){
                    conn.doOutput=true
                    conn.setRequestProperty("Content-Type","application/json; charset=utf-8")
                    conn.setFixedLengthStreamingMode(body.size)
                    conn.outputStream.use{it.write(body)}
                }
                val status=conn.responseCode
                val stream=if(status in 200..299)conn.inputStream else conn.errorStream
                val text=stream?.use{input->
                    val out=ByteArrayOutputStream();val buffer=ByteArray(8192)
                    while(true){
                        val n=input.read(buffer);if(n<0)break
                        val max=if(uri.path.startsWith("/v150/profiles/"))1_600_000 else 1_048_576
                        if(out.size()+n>max)throw IOException("Arena yanıtı çok büyük")
                        out.write(buffer,0,n)
                    }
                    out.toString("UTF-8")
                }?:"{}"
                val response=runCatching{JSONObject(text)}.getOrElse{JSONObject()}
                if(status !in 200..299||!response.optBoolean("ok"))
                    throw Wq150GatewayError(status,response.optString("code","refresh-required").take(80))
                response
            }finally{conn.disconnect()}
        }
    }
}
internal data class Wq150RemoteState(
    val snapshot:JSONObject?=null,val busy:Boolean=false,val error:String?=null,
    val pending:Wq150Command?=null
)
/** Room-scoped controller. Keep last valid snapshot during transient failures.
 * Call poll() only inside a lifecycle-owned coroutine; cancellation stops polling.
 * The old Arena is never used as fallback for a partially completed new match.
 */
internal class Wq150RemoteMatch(private val room:String,private val gateway:Wq150Gateway,
    private val sessionId:String,initial:JSONObject?=null){
    private var departed=false
    private val lock=Mutex()
    private val mutable=MutableStateFlow(Wq150RemoteState(snapshot=initial))
    val state:StateFlow<Wq150RemoteState> = mutable.asStateFlow()
    private fun accept(view:JSONObject) {
        val before=mutable.value.snapshot
        if(before==null||view.optLong("revision")>=before.optLong("revision")){
            mutable.value=mutable.value.copy(snapshot=view,error=null)
        }
    }
    suspend fun refresh()=lock.withLock{
        if(departed)return@withLock
        try{accept(gateway.presence(room,sessionId))}
        catch(e:CancellationException){throw e}
        catch(e:Exception){mutable.value=mutable.value.copy(error="Bağlantı doğrulanamadı.")}
    }
    suspend fun submit(command:Wq150Command)=lock.withLock{
        val current=mutable.value
        if(current.pending!=null&&current.pending.id!=command.id)return@withLock
        if(current.pending==null&&(current.snapshot?.optString("phase")!="playing"||
            (!command.social&&current.snapshot?.optLong("revision")!=command.revision)))return@withLock
        require(command.valid())
        mutable.value=current.copy(busy=true,pending=command,error=null)
        try{
            accept(gateway.send(room,command))
            mutable.value=mutable.value.copy(pending=null)
        }catch(e:CancellationException){throw e}
        catch(e:Wq150GatewayError){
            // An explicit command rejection is final; transport ambiguity retains the ID.
            val rejected=e.code=="command-rejected"||e.status in listOf(400,401,403,404,405,413)
            mutable.value=mutable.value.copy(pending=if(rejected)null else command,error=if(rejected)"İşlem kabul edilmedi. Oyun durumu yenileniyor." else "İşlem doğrulanamadı. Durumu yenile.")
        }catch(e:Exception){
            mutable.value=mutable.value.copy(error="Bağlantı kesildi. Aynı işlem tekrar denenebilir.")
        }finally{mutable.value=mutable.value.copy(busy=false)}
    }
    suspend fun retryPending(){state.value.pending?.let{submit(it)}}
    suspend fun leave():Boolean=lock.withLock{
        if(departed||mutable.value.snapshot?.optString("phase")=="finished")return@withLock true
        mutable.value=mutable.value.copy(busy=true,error=null)
        try{
            val view=gateway.presence(room,sessionId,leave=true)
            accept(view)
            departed=true
            mutable.value=mutable.value.copy(pending=null)
            true
        }catch(e:CancellationException){throw e}
        catch(e:Exception){
            mutable.value=mutable.value.copy(error="Ayrılma doğrulanamadı. Bağlantıyı kontrol edip tekrar dene.")
            false
        }finally{mutable.value=mutable.value.copy(busy=false)}
    }
    suspend fun poll(){
        while(coroutineContext.isActive){
            refresh()
            if(departed||state.value.snapshot?.optString("phase")=="finished")break
            delay(if(state.value.error==null)1500L else 4000L)
        }
    }
}
