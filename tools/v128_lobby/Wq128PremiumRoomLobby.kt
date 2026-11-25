package com.kelimebulmacasi.a1

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val LobbyGold128=Color(0xFFFFDA78)
private val LobbyLav128=Color(0xFFE7D9FF)
private val LobbyGreen128=Color(0xFFB5F6AE)
private val LobbyWhite128=Color(0xFFFFF8FF)

private fun Modifier.lobbyPlate128(gold:Boolean=false,radius:Int=18):Modifier {
    val shape=RoundedCornerShape(radius.dp)
    return clip(shape).background(Brush.verticalGradient(listOf(Color(0xEE49317C),Color(0xF021123D))))
        .border(if(gold)1.7.dp else 1.dp,if(gold)LobbyGold128 else Color(0xFFAC78E3),shape)
}

@Composable
private fun LobbyAction128(text:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,gold:Boolean=false,ready:Boolean=false) {
    val shape=RoundedCornerShape(15.dp)
    val colors=when {
        !enabled -> listOf(Color(0xFF54466B),Color(0xFF33284B))
        ready -> listOf(Color(0xFF267A42),Color(0xFF15512C))
        gold -> listOf(Color(0xFFFFE6A2),Color(0xFFFFB729))
        else -> listOf(Color(0xFF8C38CB),Color(0xFF452185))
    }
    Box(modifier.heightIn(min=44.dp).clip(shape).background(Brush.verticalGradient(colors))
        .border(1.dp,if(ready)LobbyGreen128 else if(gold&&enabled)LobbyGold128 else Color(0xFFAC83E5),shape)
        .clickable(enabled=enabled,onClick=onClick),contentAlignment=Alignment.Center) {
        Text(text,Modifier.padding(horizontal=7.dp,vertical=5.dp),color=if(gold&&enabled)Color(0xFF321746) else if(ready)LobbyGreen128 else LobbyWhite128,
            fontSize=13.sp,fontWeight=FontWeight.Bold,maxLines=2,textAlign=TextAlign.Center)
    }
}

@Composable
private fun LobbyPortrait128(avatar:String,mine:Boolean,modifier:Modifier) {
    val context=LocalContext.current
    val remote=if(!mine)Wq183ArenaShare.remoteImage(avatar) else null
    val custom=Wq183MascotStore.isCustomId(avatar)
    key(avatar,mine) {
        val image by produceState<Bitmap?>(null,avatar,mine,remote) {
            value=withContext(Dispatchers.Default) {
                val source=if(mine&&custom)Wq183MascotStore.picture(context,avatar,false) else remote
                source?.let { Wq128RoomHd.trim(it) }
            }
        }
        val art=image
        if(art!=null) Image(art.asImageBitmap(),"Oyuncu maskotu",modifier,contentScale=ContentScale.Fit)
        else ArenaMascotV2(avatar,modifier)
    }
}

@Composable
private fun LobbyPlayer128(name:String,avatar:String,host:Boolean,present:Boolean,mine:Boolean,ready:Boolean,busy:Boolean,height:Dp,onReady:()->Unit,onInvite:()->Unit,canInvite:Boolean,modifier:Modifier) {
    Column(modifier.height(height).lobbyPlate128(gold=host||ready,radius=20).padding(7.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().height(24.dp).align(Alignment.BottomCenter)) {
                val w=size.width*.82f
                val h=size.height*.56f
                val p=Offset((size.width-w)/2f,(size.height-h)/2f)
                drawOval(Brush.radialGradient(listOf(Color(0x88D98DFF),Color.Transparent),center=Offset(size.width/2f,size.height/2f),radius=size.width*.5f),topLeft=p,size=Size(w,h))
                drawOval(if(host)LobbyGold128 else Color(0xFFDC9AFF),topLeft=p,size=Size(w,h),style=androidx.compose.ui.graphics.drawscope.Stroke(width=1.8.dp.toPx()))
            }
            if(present) LobbyPortrait128(avatar,mine,Modifier.fillMaxSize().padding(bottom=9.dp))
            else Box(Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp)).clickable(enabled=canInvite,onClick=onInvite),contentAlignment=Alignment.Center) {
                Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Wq122Art("friends",Modifier.size(54.dp))
                    Text("+",color=LobbyGold128,fontSize=32.sp,fontWeight=FontWeight.Black)
                }
            }
            if(host) Text("Oda sahibi",Modifier.align(Alignment.TopStart).clip(RoundedCornerShape(10.dp))
                .background(Color(0xE52A154B)).padding(horizontal=6.dp,vertical=3.dp),
                color=LobbyGold128,fontSize=10.sp,fontWeight=FontWeight.Bold)
        }
        Text(if(present)name.ifBlank{"Bağlanıyor…"} else "Arkadaş bekleniyor",color=LobbyWhite128,fontSize=15.sp,
            fontWeight=FontWeight.ExtraBold,maxLines=1,overflow=TextOverflow.Ellipsis,textAlign=TextAlign.Center)
        if(present) LobbyAction128(
            text=if(ready)"✓ Hazır" else if(mine)"Hazırım" else "Hazır değil",
            onClick=onReady,modifier=Modifier.fillMaxWidth(),enabled=mine&&!busy,ready=ready)
        else LobbyAction128("Arkadaş Davet Et",onInvite,Modifier.fillMaxWidth(),enabled=canInvite,gold=true)
    }
}

@Composable
private fun LobbyChip128(label:String,modifier:Modifier) {
    Box(modifier.heightIn(min=34.dp).lobbyPlate128(radius=11).padding(horizontal=3.dp,vertical=6.dp),contentAlignment=Alignment.Center) {
        Text(label,color=LobbyWhite128,fontWeight=FontWeight.SemiBold,fontSize=12.sp,maxLines=2,textAlign=TextAlign.Center)
    }
}

@Composable
internal fun Wq128PremiumRoomLobby(
    match:PremiumOnlineMatch,client:PremiumOnlineRoom,isHost:Boolean,myUid:String,
    hostName:String,guestName:String,packs:PremiumArenaContent,profileKey:String,
    onInvites:@Composable ()->Unit
) {
    val context=LocalContext.current
    val config=LocalConfiguration.current
    val wide=config.screenWidthDp>=650
    val short=config.screenHeightDp<560
    val cardHeight=(config.screenHeightDp*(if(wide).49f else .47f)).coerceIn(if(short)180f else 242f,if(wide)490f else 405f).dp
    var editOpen by remember(match.code){mutableStateOf(false)}
    var inviteOpen by remember(match.code){mutableStateOf(false)}
    var infoOpen by remember(match.code){mutableStateOf(false)}
    val hostReady=match.hostUid in match.readyBy
    val guestPresent=match.guestUid.isNotBlank()
    val guestReady=guestPresent&&match.guestUid in match.readyBy
    val allReady=guestPresent&&hostReady&&guestReady
    val mode=when(match.mode){"team"->"Ortak";"quiz"->"5 Şıklı";else->"Düello"}
    Column(Modifier.fillMaxWidth().padding(horizontal=if(wide)12.dp else 2.dp),verticalArrangement=Arrangement.spacedBy(if(short)5.dp else 8.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Wq122Art("friends",Modifier.size(34.dp))
            Column(Modifier.weight(1f)) {
                Text("Oda Lobisi",color=LobbyWhite128,fontSize=if(wide)26.sp else 22.sp,fontWeight=FontWeight.Black)
                Text("WordQuest AI · Arena",color=LobbyGold128,fontSize=11.sp,fontWeight=FontWeight.Bold)
            }
            if(isHost) LobbyAction128("Arkadaşlar", {inviteOpen=true})
            Box(Modifier.size(44.dp).lobbyPlate128(radius=14).clickable{infoOpen=true},contentAlignment=Alignment.Center) {
                Wq122Art("info",Modifier.size(27.dp))
            }
        }
        Row(Modifier.fillMaxWidth().lobbyPlate128().padding(horizontal=9.dp,vertical=4.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Oda kodu",color=LobbyLav128,fontSize=10.sp)
                Text(match.code,color=LobbyGold128,fontSize=19.sp,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            LobbyAction128("Kopyala",{
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                    ?.setPrimaryClip(ClipData.newPlainText("WordQuest Arena",match.code))
            })
            LobbyAction128("Paylaş",{
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type="text/plain";putExtra(Intent.EXTRA_TEXT,"WordQuest AI Arena daveti! Oda kodu: ${match.code}")
                },"Arkadaşını davet et"))
            })
        }
        Column(Modifier.fillMaxWidth().lobbyPlate128().padding(7.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Wq122Art("settings",Modifier.size(25.dp))
                Text("Maç Ayarları",Modifier.weight(1f).padding(start=5.dp),color=LobbyWhite128,fontSize=14.sp,fontWeight=FontWeight.Bold)
                if(isHost) TextButton(onClick={editOpen=true},modifier=Modifier.heightIn(min=44.dp)) {
                    Text("Düzenle",color=LobbyGold128,fontWeight=FontWeight.Bold,fontSize=12.sp)
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                LobbyChip128(mode,Modifier.weight(1f))
                LobbyChip128(match.level,Modifier.weight(.7f))
                LobbyChip128(if(match.seconds==0)"Süresiz" else "${match.seconds} sn",Modifier.weight(1f))
                LobbyChip128("${match.size} ${if(match.mode=="quiz")"soru" else "kelime"}",Modifier.weight(1.1f))
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(if(wide)12.dp else 7.dp)) {
            LobbyPlayer128(hostName,match.hostAvatar,true,true,myUid==match.hostUid,hostReady,client.busy,cardHeight,
                client::toggleReady,{inviteOpen=true},false,Modifier.weight(1f))
            LobbyPlayer128(guestName,match.guestAvatar,false,guestPresent,myUid==match.guestUid,guestReady,client.busy,cardHeight,
                client::toggleReady,{inviteOpen=true},isHost&&!guestPresent,Modifier.weight(1f))
        }
        Text(when {
            allReady -> "✓ İki oyuncu da hazır."
            !guestPresent -> "Arkadaşının odaya katılması bekleniyor."
            else -> "İki oyuncu da hazır olmalı."
        },Modifier.fillMaxWidth().lobbyPlate128(radius=12).padding(8.dp),
            color=if(allReady)LobbyGreen128 else LobbyLav128,fontWeight=FontWeight.Bold,fontSize=12.sp,textAlign=TextAlign.Center)
        if(isHost) Wq122Primary("Yarışmayı Başlat",allReady&&!client.busy,client::start)
        else Text("Oda sahibi yarışmayı başlatacak.",Modifier.fillMaxWidth(),color=LobbyWhite128,textAlign=TextAlign.Center,fontSize=12.sp)
        if(client.status.isNotBlank() && listOf("hata","kaydedilemedi","başlatılamadı","izin","reddedildi").any{client.status.contains(it,true)})
            Text(client.status,Modifier.fillMaxWidth().lobbyPlate128().padding(8.dp),color=Color(0xFFFFBCBE),fontSize=12.sp)
    }
    if(editOpen&&isHost) ArenaLobbyEditDialogV3(match,packs,profileKey,onDismiss={editOpen=false},
        onSave={mode,level,topic,size,seconds,boardId->client.updateLobbySettings(mode,level,topic,size,seconds,boardId);editOpen=false})
    if(inviteOpen&&isHost) AlertDialog(onDismissRequest={inviteOpen=false},title={Text("Arkadaş Davet Et")},
        text={Column(Modifier.heightIn(max=(config.screenHeightDp*.60f).dp).verticalScroll(rememberScrollState())){onInvites()}},
        confirmButton={TextButton(onClick={inviteOpen=false}){Text("Kapat")}})
    if(infoOpen) AlertDialog(onDismissRequest={infoOpen=false},title={Text("Maç kuralları")},
        text={Column(Modifier.heightIn(max=360.dp).verticalScroll(rememberScrollState())){ArenaRulesCardV3(match.mode,match.size,match.seconds)}},
        confirmButton={TextButton(onClick={infoOpen=false}){Text("Kapat")}})
}
