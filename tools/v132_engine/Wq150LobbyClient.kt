package com.kelimebulmacasi.a1

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.UUID
import kotlin.coroutines.coroutineContext

internal data class Wq150LobbyResponse(val id:String,val view:JSONObject?)
internal data class Wq150LobbyCommand(
    val type:String,val revision:Long?=null,val configRevision:Long?=null,
    val ready:Boolean?=null,val settings:Wq150Settings?=null,
    val id:String=UUID.randomUUID().toString()
){
    companion object {
        fun validId(id:String)=Regex("^[A-Za-z0-9_-]{1,100}$").matches(id)&&
            id !in listOf("constructor","prototype","__proto__")
    }
    fun valid():Boolean {
        if(!validId(id)||revision!=null&&revision !in 0L..9007199254740991L)return false
        if(type!="join"&&revision==null)return false
        return when(type){
            "join","leave","start" -> configRevision==null&&ready==null&&settings==null
            "ready" -> configRevision!=null&&configRevision in 0L..9007199254740991L&&ready!=null&&settings==null
            "settings" -> configRevision==null&&ready==null&&settings?.valid()==true
            else -> false
        }
    }
    fun json():JSONObject {
        require(valid())
        return JSONObject().put("id",id).put("type",type).also{j->
            revision?.let{j.put("revision",it)}
            configRevision?.let{j.put("configRevision",it)}
            ready?.let{j.put("ready",it)}
            settings?.let{j.put("settings",it.json())}
        }
    }
}
internal data class Wq150LobbyState(
    val room:String?=null,val view:JSONObject?=null,val busy:Boolean=false,val error:String?=null,
    val pending:Boolean=false,val left:Boolean=false
)
private sealed interface Wq150LobbyRequest {
    data class Create(val settings:Wq150Settings,val id:String):Wq150LobbyRequest
    data class Action(val room:String,val command:Wq150LobbyCommand):Wq150LobbyRequest
}
/** Keep the exact request identity on transport failure. Lobby revisions reset
 * to zero on game start, so phase transition takes precedence over revision. */
internal class Wq150LobbyController(private val gateway:Wq150Gateway){
    private val lock=Mutex()
    private val mutable=MutableStateFlow(Wq150LobbyState())
    val state=mutable.asStateFlow()
    private var pending:Wq150LobbyRequest?=null
    private fun accept(response:Wq150LobbyResponse){
        val old=mutable.value
        val next=response.view
        val prev=old.view
        val changedRoom=old.room!=response.id
        val started=prev?.optString("phase")=="lobby"&&next?.optString("phase") in listOf("playing","paused","finished")
        val oldGame=prev?.optString("phase") in listOf("playing","paused","finished")
        if(!changedRoom&&oldGame&&next?.optString("phase")=="lobby")return
        if(next==null||changedRoom||prev==null||started||next.optLong("revision")>=prev.optLong("revision"))
            mutable.value=old.copy(room=response.id,view=next,left=next==null,error=null)
    }
    private suspend fun execute(request:Wq150LobbyRequest){
        pending=request;mutable.value=mutable.value.copy(busy=true,pending=true,error=null)
        try{
            val result=when(request){
                is Wq150LobbyRequest.Create -> gateway.createLobby(request.settings,request.id)
                is Wq150LobbyRequest.Action -> gateway.lobbyAction(request.room,request.command)
            }
            accept(result);pending=null
        }catch(e:CancellationException){throw e}
        catch(e:Wq150GatewayError){
            if(e.status in listOf(400,401,403,404,405,413,503))pending=null
            mutable.value=mutable.value.copy(error=when(e.code){
                "lobby-not-ready"->"Tüm oyuncular hazır olmalı."
                "lobby-unavailable"->"Yeni Arena sunucusu henüz etkin değil."
                "not-found"->"Oda bulunamadı veya kapatıldı."
                "unauthenticated"->"Tekrar giriş yap."
                else->"İşlem doğrulanamadı. Oda durumunu yenile."
            })
        }catch(e:Exception){mutable.value=mutable.value.copy(error="Bağlantı kesildi. Aynı işlemi tekrar deneyebilirsin.")}
        finally{mutable.value=mutable.value.copy(busy=false,pending=pending!=null)}
    }
    suspend fun create(settings:Wq150Settings)=lock.withLock{
        if(pending!=null||mutable.value.room!=null&&!mutable.value.left)return@withLock
        require(settings.valid())
        execute(Wq150LobbyRequest.Create(settings,UUID.randomUUID().toString()))
    }
    suspend fun join(room:String)=lock.withLock{
        if(pending!=null||mutable.value.room!=null&&!mutable.value.left)return@withLock
        execute(Wq150LobbyRequest.Action(room,Wq150LobbyCommand("join")))
    }
    suspend fun action(type:String,ready:Boolean?=null,settings:Wq150Settings?=null)=lock.withLock{
        if(pending!=null)return@withLock
        val current=mutable.value
        val view=current.view?:return@withLock
        val room=current.room?:return@withLock
        if(view.optString("phase")!="lobby")return@withLock
        val command=Wq150LobbyCommand(type,view.optLong("revision"),
            if(type=="ready")view.optLong("configRevision") else null,ready,settings)
        require(command.valid())
        execute(Wq150LobbyRequest.Action(room,command))
    }
    suspend fun retry()=lock.withLock{pending?.let{execute(it)}}
    suspend fun refresh()=lock.withLock{
        val current=mutable.value
        val room=current.room?:return@withLock
        if(current.left)return@withLock
        try{accept(gateway.lobby(room))}
        catch(e:CancellationException){throw e}
        catch(e:Exception){mutable.value=mutable.value.copy(error="Oda durumu doğrulanamadı.")}
    }
    suspend fun poll(){
        while(coroutineContext.isActive){
            refresh()
            val s=state.value
            if(s.left||s.view?.optString("phase") in listOf("playing","paused","finished"))break
            delay(if(s.error==null)1500L else 4000L)
        }
    }
}
