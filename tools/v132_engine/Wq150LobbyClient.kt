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
// Explicit rejections must not leave a request permanently blocking the lobby.
internal fun wq150LobbyFinalError(status:Int,code:String):Boolean =
    status in listOf(400,401,403,404,405,413,429) ||
    code in setOf("room-quota","lobby-not-ready","lobby-expired","invalid-settings","invalid-command","invalid-ready","not-found","match-already-started","lobby-unavailable","not-configured")

internal fun wq150LobbyErrorText(status:Int,code:String):String = when(code){
    "room-quota"->"Günlük 10 yeni oda sınırına ulaşıldı. Arkadaşının odasına katılabilirsin."
    "lobby-not-ready"->"Tüm oyuncular hazır olmalı."
    "lobby-unavailable","not-configured"->"Arena sunucusu şu anda kullanılamıyor."
    "not-found","lobby-expired"->"Oda bulunamadı veya süresi doldu."
    "unauthenticated"->"Oturum doğrulanamadı. Tekrar giriş yap."
    "invalid-settings"->"Oda ayarları sunucu tarafından kabul edilmedi. Ayarları kontrol et."
    "invalid-command","invalid-ready"->"Oda işlemi sunucu tarafından kabul edilmedi."
    "match-already-started"->"Maç başladı. Oda durumu yenileniyor."
    "refresh-required","stale-revision","stale-settings"->"Oda durumu değişti. Yenileniyor."
    else->when(status){
        429->"Sunucu istek sınırına ulaşıldı. Bir süre sonra tekrar dene."
        503,502,504,500->"Arena sunucusuna şu anda ulaşılamıyor."
        else->"Oda işlemi doğrulanamadı (HTTP $status)."
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
    private suspend fun execute(request:Wq150LobbyRequest,allowReadyRecovery:Boolean=true){
        pending=request;mutable.value=mutable.value.copy(busy=true,pending=true,error=null)
        try{
            val result=when(request){
                is Wq150LobbyRequest.Create -> gateway.createLobby(request.settings,request.id)
                is Wq150LobbyRequest.Action -> gateway.lobbyAction(request.room,request.command)
            }
            accept(result);pending=null
        }catch(e:CancellationException){throw e}
        catch(e:Wq150GatewayError){
            val action=request as? Wq150LobbyRequest.Action
            val conflict=e.status==409 && action!=null
            if(wq150LobbyFinalError(e.status,e.code)||conflict)pending=null
            if(conflict){
                // A concurrent join/ready may advance the room revision. Fetch authoritative
                // state, then reissue only the same readiness intent and unchanged settings.
                try{
                    val failedAction=requireNotNull(action)
                    val latest=gateway.lobby(failedAction.room)
                    accept(latest)
                    val view=latest.view
                    val command=failedAction.command
                    val sameConfig=view!=null&&view.optString("phase")=="lobby"&&
                        view.optLong("configRevision",-1)==command.configRevision
                    if(allowReadyRecovery&&command.type=="ready"&&sameConfig){
                        val nextRevision=view!!.optLong("revision",-1)
                        if(nextRevision>=0&&nextRevision!=command.revision){
                            execute(Wq150LobbyRequest.Action(failedAction.room,command.copy(revision=nextRevision,id=UUID.randomUUID().toString())),false)
                            return
                        }
                    }
                    mutable.value=mutable.value.copy(error=if(command.type=="ready"&&!sameConfig)
                        "Maç ayarları değişti. Ayarları kontrol edip yeniden Hazırım'a bas."
                        else "Oda yenilendi. İşlemi yeniden deneyebilirsin.")
                }catch(cancel:CancellationException){throw cancel}
                catch(refreshError:Exception){
                    if(refreshError is Wq150GatewayError&&refreshError.status==404)
                        mutable.value=mutable.value.copy(room=null,view=null,left=true,error="Oda bulunamadı veya süresi doldu.")
                    else mutable.value=mutable.value.copy(error=wq150LobbyErrorText(e.status,e.code))
                }
            }else mutable.value=mutable.value.copy(error=wq150LobbyErrorText(e.status,e.code))
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
        catch(e:Wq150GatewayError){
            if(e.status==404){pending=null;mutable.value=mutable.value.copy(room=null,view=null,left=true,pending=false,error=wq150LobbyErrorText(e.status,e.code))}
            else mutable.value=mutable.value.copy(error=wq150LobbyErrorText(e.status,e.code))
        }
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
