package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun ArenaModeTileV2(
    title: String, subtitle: String, symbol: String, selected: Boolean,
    accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Column(modifier.clip(RoundedCornerShape(17.dp))
        .border(if(selected) 2.dp else 1.dp, if(selected)accent else Color(0xFFE2E1EF), RoundedCornerShape(17.dp))
        .background(if(selected)accent.copy(alpha=.13f) else Color.White)
        .clickable(onClick=onClick).padding(10.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(symbol,fontSize=24.sp,color=accent)
        Text(title,color=ArenaV2Colors.ink,fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=1)
        Text(subtitle,color=ArenaV2Colors.muted,fontSize=10.sp,maxLines=1)
    }
}

@Composable
private fun ArenaChipV2(label: String) {
    Box(Modifier.clip(RoundedCornerShape(28.dp)).background(Color(0xFFF0EEFB))
        .padding(horizontal=9.dp,vertical=5.dp)) {
        Text(label,color=ArenaV2Colors.violet,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
    }
}

/** Modern one-screen launcher. Existing login, room creation, quotas and pack selection stay wired. */
@Composable
internal fun ArenaHomeV2(
    playerName: String, loggedIn: Boolean, canCreate: Boolean,
    quotaLabel: String, mode: String, onMode: (String) -> Unit,
    level: String, onLevel: (String) -> Unit,
    topic: String, topics: List<String>, onTopic: (String) -> Unit,
    sizes: List<Int>, size: Int, onSize: (Int) -> Unit,
    seconds: Int, allowedTimes: List<Int>, onSeconds: (Int) -> Unit,
    roomCode: String, onRoomCode: (String) -> Unit,
    onCreate: () -> Unit, onJoin: () -> Unit, onAccount: () -> Unit,
    onOffline: () -> Unit, onFriends: @Composable () -> Unit
) {
    val context=LocalContext.current
    val prefs=remember(context){context.getSharedPreferences("wordquest_arena_mascot",Context.MODE_PRIVATE)}
    var mascot by remember { mutableStateOf(prefs.getString("kind","robot") ?: "robot") }
    var avatarPicker by rememberSaveable {mutableStateOf(false)}
    var advanced by rememberSaveable {mutableStateOf(false)}
    var joinOpen by rememberSaveable {mutableStateOf(false)}
    var friendsOpen by rememberSaveable {mutableStateOf(false)}
    val modeName=when(mode){"team"->"Ortak Bulmaca";"quiz"->"5 Şıklı Düello";else->"Bulmaca Düellosu"}

    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White)
            .clickable {avatarPicker=true}.padding(14.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(54.dp).background(Color(0xFFEEE9FF),CircleShape).padding(5.dp)) {
                ArenaMascotV2(mascot,Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f)) {
                Text("OYUNCU PROFİLİ",color=ArenaV2Colors.muted,fontSize=10.sp)
                Text(playerName.ifBlank{"Kelime Kaşifi"},color=ArenaV2Colors.ink,
                    fontSize=18.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text("Avatar ve hesap",color=ArenaV2Colors.violet,fontSize=11.sp)
            }
            Text("›",color=ArenaV2Colors.violet,fontSize=25.sp)
        }
        Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text("Maceranı seç!",fontSize=23.sp,fontWeight=FontWeight.Bold,color=ArenaV2Colors.ink)
            Text("Bir maç seç, arkadaşını davet et.",fontSize=13.sp,color=ArenaV2Colors.muted)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            ArenaModeTileV2("Düello","Rekabet","⚔",mode=="duel",ArenaV2Colors.violet,Modifier.weight(1f)){onMode("duel")}
            ArenaModeTileV2("Ortak","Takım","✦",mode=="team",ArenaV2Colors.mint,Modifier.weight(1f)){onMode("team")}
            ArenaModeTileV2("5 Şıklı","Hız","⚡",mode=="quiz",ArenaV2Colors.coral,Modifier.weight(1f)){onMode("quiz")}
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
            .clickable {advanced=!advanced}.padding(13.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(modeName,color=ArenaV2Colors.ink,fontWeight=FontWeight.Bold,fontSize=14.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    ArenaChipV2(level)
                    ArenaChipV2(if(mode=="quiz")"12 soru" else size.toString()+" kelime")
                    ArenaChipV2(if(mode=="team"||seconds==0)"Süresiz" else seconds.toString()+" sn")
                }
            }
            Text(if(advanced)"⌃" else "⌄",color=ArenaV2Colors.violet,fontSize=20.sp)
        }
        if(advanced) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(14.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Seviye",color=ArenaV2Colors.ink,fontWeight=FontWeight.Bold)
                PremiumArenaRules.allowedLevels.chunked(3).forEach { group ->
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        group.forEach { l ->
                            OutlinedButton(onClick={onLevel(l)},modifier=Modifier.weight(1f),
                                contentPadding=PaddingValues(horizontal=5.dp,vertical=7.dp)) {
                                Text(l,fontSize=12.sp,color=if(l==level)ArenaV2Colors.violet else ArenaV2Colors.muted)
                            }
                        }
                    }
                }
                Text("Konu",color=ArenaV2Colors.ink,fontWeight=FontWeight.Bold)
                topics.take(12).chunked(2).forEach { group ->
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        group.forEach { t ->
                            OutlinedButton(onClick={onTopic(t)},modifier=Modifier.weight(1f),
                                contentPadding=PaddingValues(horizontal=6.dp,vertical=7.dp)) {
                                Text(t,fontSize=11.sp,
                                    color=if(t==topic)ArenaV2Colors.violet else ArenaV2Colors.muted,
                                    maxLines=1,overflow=TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if(mode!="quiz") {
                    Text("Kelime sayısı",color=ArenaV2Colors.ink,fontWeight=FontWeight.Bold)
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        sizes.forEach { n ->
                            OutlinedButton(onClick={onSize(n)},modifier=Modifier.weight(1f),
                                contentPadding=PaddingValues(horizontal=3.dp,vertical=6.dp)) {
                                Text(n.toString(),fontSize=12.sp,
                                    color=if(n==size)ArenaV2Colors.violet else ArenaV2Colors.muted)
                            }
                        }
                    }
                }
                if(mode!="team") {
                    Text("Cevap süresi",color=ArenaV2Colors.ink,fontWeight=FontWeight.Bold)
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        allowedTimes.forEach { n ->
                            OutlinedButton(onClick={onSeconds(n)},modifier=Modifier.weight(1f),
                                contentPadding=PaddingValues(horizontal=3.dp,vertical=5.dp)) {
                                Text(if(n==0)"∞" else n.toString(),fontSize=11.sp,
                                    color=if(seconds==n)ArenaV2Colors.violet else ArenaV2Colors.muted)
                            }
                        }
                    }
                }
            }
        }
        Button(onClick=if(loggedIn)onCreate else onAccount,
            enabled=!loggedIn||canCreate,modifier=Modifier.fillMaxWidth().height(52.dp),
            shape=RoundedCornerShape(15.dp),
            colors=ButtonDefaults.buttonColors(containerColor=ArenaV2Colors.violet)) {
            Text(if(loggedIn)"Oyun oluştur" else "Hesaba giriş yap",
                fontSize=16.sp,fontWeight=FontWeight.Bold)
        }
        OutlinedButton(onClick={joinOpen=!joinOpen},modifier=Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(15.dp),
            colors=ButtonDefaults.outlinedButtonColors(contentColor=ArenaV2Colors.violet)) {
            Text("Oda koduyla katıl",fontSize=14.sp,fontWeight=FontWeight.SemiBold)
        }
        if(joinOpen) {
            OutlinedTextField(value=roomCode,onValueChange=onRoomCode,
                modifier=Modifier.fillMaxWidth(),label={Text("9 karakterli oda kodu")},
                singleLine=true)
            Button(onClick=onJoin,enabled=roomCode.length==9,modifier=Modifier.fillMaxWidth(),
                colors=ButtonDefaults.buttonColors(containerColor=ArenaV2Colors.mint)) {
                Text("Odaya katıl")
            }
        }
        Text(quotaLabel,fontSize=11.sp,color=ArenaV2Colors.muted)
        TextButton(onClick={friendsOpen=!friendsOpen}) {
            Text("Arkadaş davetleri " + if(friendsOpen)"⌃" else "⌄",
                color=ArenaV2Colors.violet)
        }
        if(friendsOpen) onFriends()
        TextButton(onClick=onOffline){Text("Tek başıma bulmaca çöz",color=ArenaV2Colors.muted)}
    }
    if(avatarPicker) {
        AlertDialog(onDismissRequest={avatarPicker=false},
            title={Text("Avatarım",color=ArenaV2Colors.ink)},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("Karakterini seç. Seçimin bu cihazda saklanır.",color=ArenaV2Colors.muted)
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        listOf("robot","fox","owl").forEach { item ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                                .background(if(mascot==item)Color(0xFFE9E3FF) else Color.White)
                                .clickable { mascot=item;prefs.edit().putString("kind",item).apply() }
                                .padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                ArenaMascotV2(item,Modifier.size(54.dp))
                                Text(when(item){"robot"->"Bip";"fox"->"Pufi";else->"Mimo"},
                                    fontSize=12.sp,color=ArenaV2Colors.ink)
                            }
                        }
                    }
                    TextButton(onClick={avatarPicker=false;onAccount()}) {Text("Takma adımı düzenle")}
                }
            },
            confirmButton={TextButton(onClick={avatarPicker=false}){Text("Tamam")}},
            containerColor=Color.White)
    }
}
