package com.kelimebulmacasi.a1

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LobbyPurple = Color(0xFF8051CC)
private val LobbyInk = Color(0xFF291D4F)
private val LobbyMuted = Color(0xFF787391)
private val LobbyLine = Color(0xFFE5D7FB)
private val LobbyReady = Color(0xFF228B49)

@Composable
private fun LobbyPillV3(label: String) {
    Box(Modifier.clip(RoundedCornerShape(19.dp)).background(Color(0xFFFFF1DF))
        .padding(horizontal=10.dp,vertical=8.dp)){
        Text(label,color=LobbyInk,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
    }
}

@Composable
private fun LobbyPlayerV3(
    name: String, avatar: String, owner: Boolean, present: Boolean,
    ready: Boolean, mine: Boolean, onReady: () -> Unit, modifier: Modifier=Modifier
) {
    Column(modifier.border(1.dp,Color.White,RoundedCornerShape(22.dp))
        .background(if(owner)Color(0xFFEEE5FF) else Color(0xFFFFEFEA),
            RoundedCornerShape(22.dp))
        .padding(12.dp), horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(7.dp)) {
        if(present) ArenaMascotV2(avatar,Modifier.size(70.dp))
        else Box(Modifier.size(70.dp),contentAlignment=Alignment.Center){
            Text("+",color=LobbyMuted,fontSize=37.sp)
        }
        Text(if(present)name.ifBlank{"İsim yükleniyor..."} else "Arkadaş bekleniyor",
            color=LobbyInk,fontWeight=FontWeight.Bold,fontSize=17.sp,maxLines=1,
            overflow=TextOverflow.Ellipsis)
        Text(if(owner)"♛ Oda sahibi" else if(present)"Katılımcı" else "Davet gönder",
            color=LobbyMuted,fontSize=11.sp)
        Surface(
            onClick=onReady,
            enabled=mine && present,
            color=if(ready)Color(0xFFDBF8DC) else Color.White,
            shape=RoundedCornerShape(50.dp),
            border=BorderStroke(1.4.dp,if(ready)LobbyReady else LobbyLine)
        ){
            Box(Modifier.fillMaxWidth().padding(vertical=10.dp),contentAlignment=Alignment.Center) {
                Text(if(!present)"Bekleniyor" else if(ready)"✓ Hazır" else if(mine)"Hazırım" else "Hazır değil",
                    color=if(ready)LobbyReady else LobbyInk,fontSize=13.sp,fontWeight=FontWeight.Bold)
            }
        }
    }
}

/** Shared room UI: settings can be edited after a friend joins but only before starting. */
@Composable
internal fun ArenaRoomLobbyV3(
    match: PremiumOnlineMatch, client: PremiumOnlineRoom, isHost: Boolean,
    myUid: String, hostName: String, guestName: String,
    packs: PremiumArenaContent, profileKey: String,
    onInvites: @Composable () -> Unit
) {
    val context=LocalContext.current
    var editOpen by remember(match.code){mutableStateOf(false)}
    val hostReady=match.hostUid in match.readyBy
    val guestReady=match.guestUid.isNotBlank() && match.guestUid in match.readyBy
    val allReady=match.guestUid.isNotBlank() && hostReady && guestReady
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp))
            .background(Color.White).border(1.dp,LobbyLine,RoundedCornerShape(21.dp))
            .padding(13.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("ODA KODU",color=LobbyPurple,fontSize=11.sp,fontWeight=FontWeight.Bold)
                    Text(match.code,color=LobbyInk,fontWeight=FontWeight.Black,fontSize=23.sp)
                }
                Button(onClick={
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                        ?.setPrimaryClip(ClipData.newPlainText("WordQuest Arena",match.code))
                }, colors=ButtonDefaults.buttonColors(containerColor=LobbyPurple),
                    shape=RoundedCornerShape(13.dp)) {
                    Text("Kopyala",fontWeight=FontWeight.Bold)
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp))
            .background(Color.White).border(1.dp,LobbyLine,RoundedCornerShape(21.dp))
            .padding(14.dp),verticalArrangement=Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("⚙ Yarışma Ayarları",color=LobbyInk,
                        fontWeight=FontWeight.Bold,fontSize=17.sp)
                    Text("Arkadaşın katıldıktan sonra da düzenlenebilir.",
                        color=LobbyMuted,fontSize=11.sp)
                }
                if(isHost) TextButton(onClick={editOpen=true}) {
                    Text("✎ Düzenle",color=LobbyPurple,fontWeight=FontWeight.Bold)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                LobbyPillV3(when(match.mode){"quiz"->"5 Şıklı";"team"->"Ortak";else->"Bulmaca"})
                LobbyPillV3(match.level)
                LobbyPillV3(if(match.seconds==0)"Süresiz" else match.seconds.toString()+" sn")
                LobbyPillV3(if(match.mode=="quiz")"12 soru" else match.size.toString()+" kelime")
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){
            LobbyPlayerV3(hostName,match.hostAvatar,true,true,hostReady,
                myUid==match.hostUid,client::toggleReady,Modifier.weight(1f))
            LobbyPlayerV3(guestName,match.guestAvatar,false,
                match.guestUid.isNotBlank(),guestReady,myUid==match.guestUid,
                client::toggleReady,Modifier.weight(1f))
        }
        if(allReady) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                .background(Color(0xFFE3F9E2)).padding(13.dp),contentAlignment=Alignment.Center) {
                Text("✓ İki oyuncu da hazır. Yarışma başlatılabilir.",
                    color=LobbyReady,fontWeight=FontWeight.Bold,fontSize=12.sp)
            }
        } else {
            Text(if(match.guestUid.isBlank())"Arkadaşının odaya katılması bekleniyor."
                else "Her iki oyuncunun da Hazırım düğmesine basması gerekiyor.",
                color=LobbyMuted,fontSize=12.sp)
        }
        if(isHost) {
            Button(onClick=client::start,enabled=allReady && !client.busy,
                modifier=Modifier.fillMaxWidth().height(53.dp),shape=RoundedCornerShape(17.dp),
                colors=ButtonDefaults.buttonColors(containerColor=LobbyPurple)) {
                Text("▶ Yarışmayı Başlat",fontSize=16.sp,fontWeight=FontWeight.Bold)
            }
        } else {
            Text("Yarışmayı oda sahibi başlatacak.",fontSize=12.sp,color=LobbyMuted)
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color.White).border(1.dp,LobbyLine,RoundedCornerShape(20.dp))
            .padding(13.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Arkadaşlarını Davet Et",color=LobbyInk,fontSize=16.sp,
                fontWeight=FontWeight.Bold)
            Text("Arkadaşını ara ve odaya davet gönder.",color=LobbyMuted,fontSize=12.sp)
            if(isHost) onInvites()
        }
    }
    if(editOpen && isHost) {
        ArenaLobbyEditDialogV3(match,packs,profileKey,
            onDismiss={editOpen=false},
            onSave={mode,level,topic,size,seconds,boardId->
                client.updateLobbySettings(mode,level,topic,size,seconds,boardId)
                editOpen=false
            })
    }
}

@Composable
private fun ArenaLobbyEditDialogV3(
    match: PremiumOnlineMatch,packs: PremiumArenaContent,profileKey: String,
    onDismiss:()->Unit,
    onSave:(String,String,String,Int,Int,String)->Unit
){
    var mode by remember(match.code){mutableStateOf(match.mode)}
    var level by remember(match.code){mutableStateOf(match.level)}
    var topic by remember(match.code){mutableStateOf(match.topic)}
    var size by remember(match.code){mutableIntStateOf(match.size)}
    var seconds by remember(match.code){mutableIntStateOf(match.seconds)}
    val available=packs.availableSizes(level,topic)
    val topics=packs.topics(level,mode!="quiz")
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Yarışma Ayarları",color=LobbyInk,fontWeight=FontWeight.Bold)},
        text={
            Column(Modifier.heightIn(max=430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Ayarları değiştirince herkes yeniden hazır olmalı.",
                    color=LobbyMuted,fontSize=12.sp)
                Text("Oyun türü",fontWeight=FontWeight.Bold,color=LobbyInk)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    listOf("duel" to "Düello","team" to "Ortak","quiz" to "5 Şıklı").forEach{(value,label)->
                        FilterChip(selected=mode==value,onClick={mode=value},label={Text(label)})
                    }
                }
                Text("İngilizce seviyesi",fontWeight=FontWeight.Bold,color=LobbyInk)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    PremiumArenaRules.allowedLevels.forEach{value->
                        FilterChip(selected=level==value,onClick={
                            level=value;topic=PremiumArenaRules.allTopics
                            size=packs.availableSizes(value).firstOrNull()?:20
                        },label={Text(value)})
                    }
                }
                Text("Konu",fontWeight=FontWeight.Bold,color=LobbyInk)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    topics.forEach{value->
                        FilterChip(selected=topic==value,onClick={
                            topic=value;size=packs.availableSizes(level,value).firstOrNull()?:20
                        },label={Text(value,maxLines=1)})
                    }
                }
                if(mode!="quiz"){
                    Text("Kelime sayısı",fontWeight=FontWeight.Bold,color=LobbyInk)
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                        available.forEach{count->
                            FilterChip(selected=size==count,onClick={size=count},
                                label={Text(count.toString())})
                        }
                    }
                }
                Text("Süre",fontWeight=FontWeight.Bold,color=LobbyInk)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    PremiumArenaRules.allowedTimes.forEach{time->
                        FilterChip(selected=seconds==time,onClick={seconds=time},
                            label={Text(if(time==0)"∞" else "$time sn")})
                    }
                }
            }
        },
        confirmButton={
            TextButton(onClick={
                val targetSize=if(mode=="quiz")12 else size
                val boardId=if(mode=="quiz")"" else
                    packs.nextPuzzle(level,targetSize,topic,profileKey)?.id.orEmpty()
                if(mode=="quiz"||boardId.isNotBlank())
                    onSave(mode,level,topic,targetSize,seconds,boardId)
            }){Text("Kaydet",color=LobbyPurple,fontWeight=FontWeight.Bold)}
        },
        dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}},
        containerColor=Color.White
    )
}
