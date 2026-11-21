package com.kelimebulmacasi.a1

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val Wq180Ink = Color(0xFF35284C)
private val Wq180Purple = Color(0xFF6740A6)
private val Wq180Pale = Color(0xFFF4EDFF)
private val Wq180Muted = Color(0xFF776C88)

/** Activity means last successful self-published Firebase activity, not online presence. */
internal object Wq180FriendActivity {
    fun label(lastAt:Long?,now:Long=System.currentTimeMillis()):String {
        if(lastAt==null || lastAt<=0 || lastAt>now+60000) return "Son etkinlik paylaşılmadı"
        val elapsed=(now-lastAt).coerceAtLeast(0L)
        return when {
            elapsed<120000L -> "Az önce aktifti"
            elapsed<3600000L -> "${elapsed/60000} dk önce aktifti"
            elapsed<86400000L -> "${elapsed/3600000} sa önce aktifti"
            elapsed<604800000L -> "${elapsed/86400000} gün önce aktifti"
            else -> java.text.SimpleDateFormat("dd.MM.yyyy",Locale("tr","TR"))
                .format(java.util.Date(lastAt))+" tarihinde aktifti"
        }
    }
}

@Composable
private fun Wq180FriendFace(avatar:String?,size:androidx.compose.ui.unit.Dp=61.dp) {
    Box(Modifier.size(size).background(Color(0xFFF2E6FF),RoundedCornerShape(17.dp)),
        contentAlignment=Alignment.Center){
        if(Wq2DMascots.valid(avatar)) Wq2DPortrait(avatar!!,Modifier.size(size-5.dp))
        else Text("♧",fontSize=27.sp,color=Wq180Purple)
    }
}

@Composable
private fun Wq180SmallAction(title:String,onClick:()->Unit,outlined:Boolean=false,enabled:Boolean=true){
    if(outlined) OutlinedButton(onClick,enabled=enabled,
        contentPadding=PaddingValues(horizontal=10.dp,vertical=2.dp)){
        Text(title,fontSize=12.sp,maxLines=1)
    }
    else Button(onClick,enabled=enabled,
        colors=ButtonDefaults.buttonColors(containerColor=Wq180Purple),
        contentPadding=PaddingValues(horizontal=10.dp,vertical=2.dp)){
        Text(title,fontSize=12.sp,maxLines=1)
    }
}

@Composable
private fun Wq180FriendTile(
    entry:FriendEntry,
    description:String,
    mainButton:String?=null,
    onMain:(()->Unit)?=null,
    minorButton:String?=null,
    onMinor:(()->Unit)?=null
){
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(19.dp),
        border=BorderStroke(1.dp,Color(0xFFE4DBED)),color=Color.White,
        shadowElevation=2.dp){
        Row(Modifier.fillMaxWidth().padding(11.dp),
            horizontalArrangement=Arrangement.spacedBy(11.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Wq180FriendFace(entry.avatarId)
            Column(Modifier.weight(1f),
                verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(entry.name, fontWeight=FontWeight.Bold,
                    fontSize=16.sp,color=Wq180Ink,maxLines=1,
                    overflow=TextOverflow.Ellipsis)
                Text(description,fontSize=11.sp,color=Wq180Muted,lineHeight=15.sp)
                if(entry.points!=null || entry.completedGames!=null){
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Text("★ ${entry.points ?: 0} puan",fontSize=11.sp,
                            fontWeight=FontWeight.SemiBold,color=Wq180Purple)
                        Text("✓ ${entry.completedGames ?: 0} oyun",
                            fontSize=11.sp,color=Wq180Muted)
                    }
                }
                if(mainButton!=null && onMain!=null || minorButton!=null && onMinor!=null) {
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        if(mainButton!=null && onMain!=null)
                            Wq180SmallAction(mainButton,onMain)
                        if(minorButton!=null && onMinor!=null)
                            Wq180SmallAction(minorButton,onMinor,outlined=true)
                    }
                }
            }
        }
    }
}

@Composable
internal fun Wq180FriendsPanel(
    hub:FriendsHub,roomCode:String?=null,
    onJoin:((String,String)->Unit)?=null,premium:Boolean=false
){
    if(!hub.enabled)return
    var lookup by remember{mutableStateOf("")}
    val canInvite=roomCode?.let(RoomInvite::isValid)==true
    // The same contrast-safe cards are used both on the light account page and
    // inside the Arena invitation surface (previous white-on-white labels fixed).
    Column(Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(Color(0xFFFFFAFF),Color(0xFFF3ECFA))),
            RoundedCornerShape(21.dp)).padding(13.dp),
        verticalArrangement=Arrangement.spacedBy(11.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){
                Text(if(canInvite)"Arkadaşını Odaya Davet Et" else "Arkadaşların",
                    color=Wq180Ink,fontSize=19.sp,fontWeight=FontWeight.ExtraBold)
                Text("${hub.friends.size} arkadaş · Maskot, ilerleme ve son etkinlik",
                    color=Wq180Muted,fontSize=11.sp)
            }
            Text("♧",fontSize=26.sp,color=Wq180Purple)
        }
        Surface(color=Color.White,shape=RoundedCornerShape(15.dp),
            modifier=Modifier.fillMaxWidth(),
            border=BorderStroke(1.dp,Color(0xFFDED2EB))){
            Row(Modifier.fillMaxWidth().padding(7.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(6.dp)){
                OutlinedTextField(lookup,{lookup=it.take(ArenaNickname.maximumLength)},
                    modifier=Modifier.weight(1f),singleLine=true,
                    label={Text("Kullanıcı adıyla ara",fontSize=12.sp)},
                    placeholder={Text("Arkadaşının adını yaz",fontSize=12.sp)})
                Wq180SmallAction("Ara",{hub.searchName(lookup)},
                    enabled=ArenaNickname.isValid(lookup) && hub.nickname.isNotBlank())
            }
        }
        if(hub.friends.isEmpty() && hub.requests.isEmpty() && hub.found.isEmpty())
            Text("Henüz arkadaşın görünmüyor. Kullanıcı adıyla ara ve istek gönder.",
                fontSize=12.sp,color=Wq180Muted)
        if(hub.found.isNotEmpty()){
            Text("ARAMA SONUÇLARI",color=Wq180Purple,fontWeight=FontWeight.Bold,fontSize=11.sp)
            hub.found.forEach{p->
                Wq180FriendTile(p,"Oyuncu profili",
                    mainButton=if(hub.friends.any{it.uid==p.uid})"Arkadaşın" else "İstek gönder",
                    onMain={if(hub.friends.none{it.uid==p.uid})hub.requestFriend(p.uid)})
            }
        }
        if(hub.requests.isNotEmpty()){
            Text("GELEN ARKADAŞLIK İSTEKLERİ",fontSize=11.sp,
                fontWeight=FontWeight.Bold,color=Wq180Purple)
            hub.requests.forEach{p->
                Wq180FriendTile(p,"Arkadaşlık isteği gönderdi",
                    mainButton="Kabul et",onMain={hub.acceptFriend(p.uid)},
                    minorButton="Reddet",onMinor={hub.rejectFriend(p.uid)})
            }
        }
        if(hub.friends.isNotEmpty()){
            Text(if(canInvite)"DAVET EDİLEBİLEN ARKADAŞLAR" else "ARKADAŞ KARTLARI",
                fontSize=11.sp,fontWeight=FontWeight.Bold,color=Wq180Purple)
            hub.friends.forEach{p->
                Wq180FriendTile(p,Wq180FriendActivity.label(p.lastActiveAt),
                    mainButton=if(canInvite)"Odaya çağır" else null,
                    onMain=if(canInvite){ {hub.inviteFriend(
                        p.uid,roomCode!!,if(premium)"premium" else "legacy")} }else null,
                    minorButton=if(canInvite)null else "Arkadaşı çıkar",
                    onMinor=if(canInvite)null else { {hub.removeFriend(p.uid)} })
            }
        }
        if(hub.invites.isNotEmpty()){
            Text("GELEN ARENA DAVETLERİ",fontSize=11.sp,
                color=Wq180Purple,fontWeight=FontWeight.Bold)
            hub.invites.forEach{inv->
                val sender=hub.friends.firstOrNull{it.uid==inv.uid}
                    ?: FriendEntry(inv.uid,inv.name)
                Wq180FriendTile(sender,"Seni Arena odasına davet ediyor",
                    mainButton=if(onJoin!=null)"Odaya katıl" else null,
                    onMain=if(onJoin!=null){{onJoin(inv.uid,inv.code)}}else null,
                    minorButton="Sil",onMinor={hub.dismissInvite(inv.uid)})
            }
        }
        if(hub.status.isNotBlank())
            Text(hub.status,color=Color(0xFF65527E),fontSize=12.sp)
        if(hub.friends.any{it.points==null || it.lastActiveAt==null})
            Text("Arkadaş ilerlemesi, maskotu ve son etkinliği yalnızca paylaşıldığında görünür.",
                color=Wq180Muted,fontSize=10.sp)
    }
}
