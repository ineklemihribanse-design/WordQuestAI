package com.kelimebulmacasi.a1

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun ArenaApprovedPlayerCard(
    name:String,avatar:String,host:Boolean,present:Boolean,ready:Boolean,mine:Boolean,
    busy:Boolean,onReady:()->Unit,modifier:Modifier=Modifier
){
    val selectedGradient=if(host) Brush.verticalGradient(
        listOf(Color(0xFFF2E7FF),Color(0xFFEFE6FC)))
        else Brush.verticalGradient(listOf(Color(0xFFFFF2ED),Color(0xFFFFE9E0)))
    Column(
        modifier.heightIn(min=215.dp)
            .shadow(5.dp,RoundedCornerShape(25.dp),ambientColor=Color(0x208A69A9))
            .clip(RoundedCornerShape(25.dp))
            .background(selectedGradient)
            .border(2.dp,Color.White,RoundedCornerShape(25.dp))
            .padding(horizontal=11.dp,vertical=15.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(7.dp)
    ){
        if(present) {
            ArenaMascotV2(avatar,Modifier.size(83.dp))
        } else {
            Box(Modifier.size(83.dp).background(Color.White.copy(alpha=.55f),CircleShape),
                contentAlignment=Alignment.Center) {
                Text("+",fontSize=43.sp,color=ArenaApproved.purple)
            }
        }
        Text(if(present)name.ifBlank{"Oyuncu bağlanıyor"} else "Arkadaş bekleniyor",
            color=ArenaApproved.ink,fontSize=18.sp,fontWeight=FontWeight.ExtraBold,
            maxLines=1,overflow=TextOverflow.Ellipsis)
        Text(if(host)"👑  Oda sahibi" else if(present)"♧  Katılımcı" else "Davet gönder",
            color=ArenaApproved.sub,fontSize=12.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(45.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(if(ready)Brush.horizontalGradient(
                    listOf(Color(0xFFDFFFD3),Color(0xFFCFF4C4)))
                    else Brush.horizontalGradient(listOf(Color.White,Color(0xFFFAF5FF))))
                .border(if(ready)1.6.dp else 1.dp,
                    if(ready)Color(0xFF76D66E) else ArenaApproved.stroke,
                    RoundedCornerShape(26.dp))
                .clickable(enabled=mine && present && !busy,onClick=onReady),
            contentAlignment=Alignment.Center
        ) {
            Text(when {
                !present -> "Bekleniyor"
                ready -> "✓  Hazır"
                mine -> "Hazırım"
                else -> "Hazır değil"
            },fontWeight=FontWeight.ExtraBold,fontSize=15.sp,
                color=if(ready)Color(0xFF0E762B) else ArenaApproved.ink)
        }
    }
}

@Composable
private fun ArenaApprovedRoomChip(icon:String,label:String){
    Row(Modifier.clip(RoundedCornerShape(15.dp))
        .background(Color(0xFFFFF2E3))
        .padding(horizontal=12.dp,vertical=11.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(icon,color=ArenaApproved.purple,fontSize=17.sp)
        Text(label,color=ArenaApproved.ink,fontWeight=FontWeight.SemiBold,
            fontSize=13.sp,maxLines=1)
    }
}

/** Approved room reference: big character cards, gradient start, game settings, invite. */
@Composable
internal fun ArenaApprovedRoomLobby(
    match: PremiumOnlineMatch,client: PremiumOnlineRoom,isHost:Boolean,
    myUid:String,hostName:String,guestName:String,
    packs: PremiumArenaContent,profileKey:String,
    onInvites: @Composable ()->Unit
){
    val context=LocalContext.current
    var editOpen by remember(match.code){mutableStateOf(false)}
    val hostReady=match.hostUid in match.readyBy
    val guestPresent=match.guestUid.isNotBlank()
    val guestReady=guestPresent && match.guestUid in match.readyBy
    val allReady=guestPresent && hostReady && guestReady

    Column(
        Modifier.fillMaxWidth().background(ArenaApproved.background)
            .padding(vertical=10.dp),
        verticalArrangement=Arrangement.spacedBy(15.dp)
    ){
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
            Text("👑",fontSize=26.sp,color=ArenaApproved.gold)
            Text("WordQuest",fontSize=29.sp,color=ArenaApproved.ink,
                fontWeight=FontWeight.Black)
            Box(Modifier.clip(RoundedCornerShape(25.dp))
                .background(Brush.horizontalGradient(
                    listOf(Color(0xFFE9D8FF),Color(0xFFD7BFFF))))
                .padding(horizontal=22.dp,vertical=4.dp)){
                Text("AI Arena",color=ArenaApproved.plum,
                    fontWeight=FontWeight.ExtraBold,fontSize=12.sp)
            }
        }
        Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
            Text("Oda Lobisi",color=ArenaApproved.ink,
                fontWeight=FontWeight.ExtraBold,fontSize=29.sp)
            Text("Arkadaşınla yarışmaya hazırsın!",color=ArenaApproved.sub,
                fontSize=17.sp)
        }

        Row(
            Modifier.fillMaxWidth().heightIn(min=83.dp)
                .shadow(6.dp,RoundedCornerShape(23.dp),
                    ambientColor=Color(0x287C60C4))
                .clip(RoundedCornerShape(23.dp))
                .background(Brush.horizontalGradient(
                    listOf(Color(0xFFF7EEFF),Color(0xFFFDF9FF))))
                .border(1.5.dp,ArenaApproved.stroke,RoundedCornerShape(23.dp))
                .padding(horizontal=13.dp,vertical=11.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(9.dp)
        ){
            Text("♧",color=ArenaApproved.purple,fontSize=25.sp)
            Column(Modifier.weight(.75f)){
                Text("Oda Kodu",color=ArenaApproved.sub,
                    fontSize=12.sp,fontWeight=FontWeight.Bold)
                Text(match.code,color=ArenaApproved.ink,
                    fontSize=18.sp,fontWeight=FontWeight.Black,
                    maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            Box(Modifier.clip(RoundedCornerShape(16.dp))
                .background(ArenaApproved.purpleGradient)
                .clickable{
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                        ?.setPrimaryClip(ClipData.newPlainText("WordQuest Arena",match.code))
                }.padding(horizontal=13.dp,vertical=13.dp),
                contentAlignment=Alignment.Center){
                Text("▣ Kopyala",color=Color.White,
                    fontSize=13.sp,fontWeight=FontWeight.ExtraBold)
            }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Text("⚙",color=ArenaApproved.purple,fontSize=25.sp)
            Spacer(Modifier.width(8.dp))
            Text("Yarışma Ayarları",color=ArenaApproved.ink,fontSize=18.sp,
                fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f))
            if(isHost) TextButton(onClick={editOpen=true}){
                Text("✎ Düzenle",color=ArenaApproved.purple,fontWeight=FontWeight.Bold)
            }
        }
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(23.dp))
                .background(Color.White)
                .border(1.dp,ArenaApproved.stroke,RoundedCornerShape(23.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal=12.dp,vertical=11.dp),
            horizontalArrangement=Arrangement.spacedBy(7.dp)
        ){
            ArenaApprovedRoomChip("✤",when(match.mode){
                "quiz"->"5 Şıklı";"team"->"Ortak";else->"Bulmaca"})
            ArenaApprovedRoomChip("▥",match.level)
            ArenaApprovedRoomChip("◴",
                if(match.seconds==0)"Süresiz" else match.seconds.toString()+" sn")
            ArenaApprovedRoomChip("▱",
                if(match.mode=="quiz")"12 soru" else match.size.toString()+" kelime")
        }
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){
            ArenaApprovedPlayerCard(hostName,match.hostAvatar,host=true,present=true,
                ready=hostReady,mine=myUid==match.hostUid,busy=client.busy,
                onReady=client::toggleReady,modifier=Modifier.weight(1f))
            ArenaApprovedPlayerCard(guestName,match.guestAvatar,host=false,
                present=guestPresent,ready=guestReady,mine=myUid==match.guestUid,
                busy=client.busy,onReady=client::toggleReady,
                modifier=Modifier.weight(1f))
        }

        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(23.dp))
                .background(if(allReady)Brush.horizontalGradient(
                    listOf(Color(0xFFE3FADB),Color(0xFFDAF7CE)))
                    else Brush.horizontalGradient(
                        listOf(Color(0xFFF6F2FC),Color(0xFFF0EBFA))))
                .padding(16.dp),
            contentAlignment=Alignment.Center
        ){
            Text(when {
                allReady -> "✓  İki oyuncu da hazır. Yarışma başlatılabilir."
                !guestPresent -> "Arkadaşının odaya katılması bekleniyor."
                else -> "İki oyuncunun da Hazırım düğmesine basması gerekiyor."
            },color=if(allReady)Color(0xFF0B792B) else ArenaApproved.sub,
                fontWeight=FontWeight.Bold,fontSize=13.sp)
        }

        if(isHost){
            ArenaApprovedPrimary("Yarışmayı Başlat",
                enabled=allReady && !client.busy,onClick=client::start)
        }else {
            Text("Oda sahibi iki oyuncu hazır olduğunda yarışmayı başlatabilir.",
                color=ArenaApproved.sub,fontSize=12.sp)
        }
        if(client.status.isNotBlank()){
            Text(client.status,fontSize=12.sp,color=if(
                client.status.contains("hata",true)||
                client.status.contains("kaydedilemedi",true)||
                client.status.contains("başlatılamadı",true)||
                client.status.contains("izin",true))
                Color(0xFFB4344C) else ArenaApproved.sub)
        }

        ArenaApprovedPanel("Arkadaşlarını Davet Et","♧"){
            Text("Arkadaşını ara ve odaya davet gönder.",
                color=ArenaApproved.sub,fontSize=12.sp)
            if(isHost)onInvites()
        }
        ArenaRulesCardV3(match.mode,match.size,match.seconds)
    }
    if(editOpen && isHost){
        ArenaLobbyEditDialogV3(
            match,packs,profileKey,onDismiss={editOpen=false},
            onSave={mode,level,topic,size,seconds,boardId->
                client.updateLobbySettings(mode,level,topic,size,seconds,boardId)
                editOpen=false
            }
        )
    }
}
