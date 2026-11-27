package com.kelimebulmacasi.a1

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal data class Wq150Invite(val sender:String,val room:String,val sentAt:Long)
internal fun wq150FreshInvite(sentAt:Long,now:Long)=sentAt in (now-600_000)..(now+10_000)

@Composable
internal fun Wq150Invites(session:AppSession,auth:FirebaseAuth,uid:String,room:String?,canInvite:Boolean,
    enabled:Boolean,joinedRoom:String?,onJoin:suspend (String)->Boolean){
    val scope=rememberCoroutineScope()
    val db=remember(auth,uid){runCatching{FirebaseDatabase.getInstance(auth.app,"https://wordquest-ai-default-rtdb.firebaseio.com").reference.child("arenaV150Invites")}.getOrNull()}
    var invites by remember(uid){mutableStateOf(emptyList<Wq150Invite>())}
    var message by remember(uid){mutableStateOf("")}
    var now by remember(uid){mutableLongStateOf(System.currentTimeMillis())}
    var writing by remember(uid){mutableStateOf(false)}
    var lookup by remember(uid){mutableStateOf("")}
    var open by remember(uid){mutableStateOf(false)}
    LaunchedEffect(uid){while(true){now=System.currentTimeMillis();delay(10_000)}}
    DisposableEffect(db,uid){
        val inbox=db?.child(uid)
        val listener=object:ValueEventListener{
            override fun onDataChange(snapshot:DataSnapshot){
                message=""
                invites=snapshot.children.mapNotNull{child->
                    val sender=child.key?:return@mapNotNull null
                    val id=child.child("room").getValue(String::class.java)?:return@mapNotNull null
                    val sent=child.child("sentAt").getValue(Long::class.java)?:return@mapNotNull null
                    if(!Regex("^[a-f0-9]{20}$").matches(id))null else Wq150Invite(sender,id,sent)
                }.sortedByDescending{it.sentAt}
            }
            override fun onCancelled(error:DatabaseError){
                invites=emptyList()
                message=when(error.code){
                    DatabaseError.PERMISSION_DENIED->"Arena davet izni yok. Sunucudaki davet güvenlik kuralları doğrulanmalı."
                    DatabaseError.DISCONNECTED,DatabaseError.NETWORK_ERROR->"Arena davet bağlantısı kesildi."
                    else->"Arena davetleri okunamadı (hata ${error.code})."
                }
            }
        }
        if(inbox==null)message="Arena davet bağlantısı yapılandırılamadı."
        inbox?.addValueEventListener(listener)
        onDispose{inbox?.removeEventListener(listener)}
    }
    val friends=session.social.friends
    val visible=invites.filter{wq150FreshInvite(it.sentAt,now)&&friends.any{f->f.uid==it.sender}}
    LaunchedEffect(joinedRoom,visible){
        if(joinedRoom!=null)visible.filter{it.room==joinedRoom}.forEach{db?.child(uid)?.child(it.sender)?.removeValue()}
    }
    if(room==null&&visible.isNotEmpty()){
        Text("Gelen Arena davetleri",color=Wq122Gold,fontWeight=FontWeight.Bold)
        visible.forEach{invite->
            Surface(color=Color(0xDD251347),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()){
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                    Text(friends.firstOrNull{it.uid==invite.sender}?.name?:"Arkadaşın",color=Wq122Ink,fontWeight=FontWeight.Bold)
                    Text("Seni Arena maçına davet ediyor.",color=Wq122Ink)
                    Row{
                        Button(enabled=enabled&&!writing,onClick={
                            scope.launch{writing=true;try{if(onJoin(invite.room)){db?.child(uid)?.child(invite.sender)?.removeValue();message=""}}finally{writing=false}}
                        }){Text("Kabul et")}
                        TextButton(enabled=!writing,onClick={db?.child(uid)?.child(invite.sender)?.removeValue()?.addOnFailureListener{message="Davet silinemedi."}}){Text("Reddet",color=Wq122Gold)}
                    }
                }
            }
        }
    }
    if(canInvite&&room!=null&&Regex("^[a-f0-9]{20}$").matches(room)){
        OutlinedButton(onClick={open=!open},enabled=enabled,modifier=Modifier.fillMaxWidth()){Text(if(open)"Arkadaş listesini kapat" else "Arkadaşını davet et")}
        if(open){
            OutlinedTextField(lookup,{lookup=it.take(24)},label={Text("Kullanıcı adıyla ara")},singleLine=true,modifier=Modifier.fillMaxWidth())
            if(friends.isEmpty())Text("Önce mevcut arkadaşlar ekranından arkadaş ekle.",color=Wq122Ink)
            friends.filter{it.name.contains(lookup,ignoreCase=true)}.forEach{friend->
                Surface(color=Color(0xDD251347),shape=MaterialTheme.shapes.medium,modifier=Modifier.fillMaxWidth()){
                    Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(friend.name,color=Wq122Ink,fontWeight=FontWeight.Bold)
                            Text(Wq180FriendActivity.label(friend.lastActiveAt),color=Wq122Ink,style=MaterialTheme.typography.bodySmall)
                        }
                        TextButton(enabled=enabled&&!writing&&db!=null,onClick={
                            val ref=db?.child(friend.uid)?.child(uid)?:return@TextButton
                            writing=true;message=""
                            ref.setValue(mapOf("room" to room,"sentAt" to ServerValue.TIMESTAMP))
                                .addOnCompleteListener{task->writing=false;message=if(task.isSuccessful)"Davet gönderildi." else "Davet gönderilemedi. Arkadaşlık ve bağlantıyı kontrol et."}
                        }){Text("Davet et",color=Wq122Gold)}
                    }
                }
            }
        }
    }
    if(message.isNotBlank())Text(message,color=Wq122Ink)
}
