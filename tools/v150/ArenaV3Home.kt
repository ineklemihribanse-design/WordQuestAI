package com.kelimebulmacasi.a1

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val V3Green = Color(0xFF348A53)
private val V3LightGreen = Color(0xFFEAF8E2)
private val V3Lavender = Color(0xFFF2EAFF)
private val V3Navy = Color(0xFF232446)
private val V3Border = Color(0xFFE4D8F4)
private val V3Purple = Color(0xFF8352CE)

@Composable
private fun ArenaV3Section(title: String, symbol: String, body: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().border(1.dp,V3Border, RoundedCornerShape(22.dp))
            .background(Color.White,RoundedCornerShape(22.dp)).padding(14.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            Text(symbol,fontSize=22.sp,color=V3Green)
            Text(title,color=V3Navy,fontSize=17.sp,fontWeight=FontWeight.Bold)
        }
        body()
    }
}

@Composable
private fun ArenaV3Choice(
    text: String, selected: Boolean, modifier: Modifier=Modifier,
    symbol: String?=null, description: String?=null, onClick: () -> Unit
) {
    Column(
        modifier.border(if(selected) 1.7.dp else 1.dp,
            if(selected) Color(0xFF65AD3D) else V3Border,RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp))
            .background(if(selected)V3LightGreen else Color(0xFFFAF7FF))
            .clickable(onClick=onClick).padding(horizontal=9.dp,vertical=12.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(4.dp)
    ) {
        if(symbol!=null) Text(symbol,color=if(selected)V3Green else V3Purple,fontSize=23.sp)
        Text(text,color=V3Navy,fontSize=12.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium,
            maxLines=2,overflow=TextOverflow.Ellipsis)
        if(description!=null) Text(description,color=Color(0xFF72758E),fontSize=10.sp,maxLines=2,
            overflow=TextOverflow.Ellipsis)
    }
}

/**
 * Matches the approved light cream/lilac Arena concept. Avatar selection stays
 * in the profile, never a second crowded list on the game launch screen.
 */
@Composable
internal fun ArenaHomeV3(
    playerName: String, loggedIn: Boolean, canCreate: Boolean,
    quotaLabel: String, mode: String, onMode: (String) -> Unit,
    level: String, onLevel: (String) -> Unit,
    topic: String, topics: List<String>, onTopic: (String) -> Unit,
    sizes: List<Int>, size: Int, onSize: (Int) -> Unit,
    seconds: Int, allowedTimes: List<Int>, onSeconds: (Int) -> Unit,
    roomCode: String, onRoomCode: (String) -> Unit,
    onCreate: () -> Unit, onJoin: () -> Unit, onAccount: () -> Unit,
    onOffline: () -> Unit, onFriends: @Composable () -> Unit,
    soloInitially: Boolean = false
) {
    val context=LocalContext.current
    val prefs=remember(context){context.getSharedPreferences("wordquest_arena_mascot",Context.MODE_PRIVATE)}
    var mascot by remember {mutableStateOf(prefs.getString("kind","robot")?:"robot")}
    var avatarPicker by rememberSaveable {mutableStateOf(false)}
    var solo by rememberSaveable { mutableStateOf(soloInitially) }
    var joinExpanded by rememberSaveable {mutableStateOf(false)}
    var allTopics by rememberSaveable {mutableStateOf(false)}
    var friendsExpanded by rememberSaveable {mutableStateOf(false)}
    val purple = V3Purple
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(13.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween) {
            Column {
                Text("WordQuest AI",color=Color(0xFF173F38),fontWeight=FontWeight.Black,fontSize=20.sp)
                Text("ARENA",color=purple,fontWeight=FontWeight.Bold,fontSize=12.sp)
            }
            Surface(onClick={avatarPicker=true},color=Color.White,shape=RoundedCornerShape(22.dp),
                border=BorderStroke(1.dp,V3Border)) {
                Row(Modifier.padding(horizontal=12.dp, vertical=6.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    ArenaMascotV2(mascot,Modifier.size(29.dp))
                    Text(playerName.ifBlank{"Profil"},color=V3Navy,fontSize=13.sp,fontWeight=FontWeight.Bold)
                    Text("⌄",color=purple)
                }
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFEDE5FF)).padding(17.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("İNGİLİZCE ARENASI",fontSize=12.sp,color=purple,fontWeight=FontWeight.Bold)
                Text("Her maç yeni bir keşif",fontSize=19.sp,color=Color(0xFF173F38),
                    fontWeight=FontWeight.ExtraBold)
            }
            ArenaMascotV2("robot",Modifier.size(67.dp))
        }
        ArenaV3Section("Oyun Türü","✧") {
            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                ArenaV3Choice("Bulmaca",mode!="quiz",Modifier.weight(1f),"▦","Kesişen kelimeler") {
                    if(mode=="quiz")onMode("duel")
                }
                ArenaV3Choice("5 Şıklı",mode=="quiz",Modifier.weight(1f),"☷","Kelime bilgisi") {
                    onMode("quiz")
                }
            }
        }
        ArenaV3Section("İngilizce Seviyesi","✧") {
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                PremiumArenaRules.allowedLevels.forEach { v ->
                    ArenaV3Choice(v,v==level,Modifier.widthIn(min=65.dp),onClick={onLevel(v)})
                }
            }
        }
        ArenaV3Section("Konu / Paket","▱") {
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                (if(allTopics)topics else topics.take(4)).forEachIndexed { index,v ->
                    ArenaV3Choice(v,v==topic,Modifier.widthIn(min=95.dp,max=150.dp),
                        symbol=listOf("▦","♧","⌂","♧")[index%4],onClick={onTopic(v)})
                }
            }
            if(topics.size>4) TextButton(onClick={allTopics=!allTopics}) {
                Text(if(allTopics)"Daha az göster" else "Tüm konuları göster",color=purple)
            }
        }
        ArenaV3Section("Oyun Biçimi","♧") {
            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                ArenaV3Choice("Arkadaşınla Oyna",!solo,Modifier.weight(1f),"♧","Oda kur ve davet et") {
                    solo=false
                }
                ArenaV3Choice("Tek Başına Oyna",solo,Modifier.weight(1f),"♙","Hemen bulmacaya başla") {
                    solo=true
                }
            }
        }
        ArenaV3Section("Maç Ayarları","⚙") {
            Text("Kelime sayısı",fontSize=12.sp,color=V3Navy,fontWeight=FontWeight.Bold)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                sizes.forEach { n ->
                    ArenaV3Choice(n.toString(),n==size,Modifier.weight(1f),onClick={onSize(n)})
                }
            }
            Text("Cevap süresi",fontSize=12.sp,color=V3Navy,fontWeight=FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                allowedTimes.forEach { t ->
                    ArenaV3Choice(if(t==0)"Süresiz" else "${t} sn",t==seconds,
                        Modifier.width(82.dp),onClick={onSeconds(t)})
                }
            }
            Text("İpucu hakkı kelime sayısına göre belirlenir.",color=Color(0xFF7C7796),
                fontSize=11.sp)
        }
        Button(onClick={if(solo)onOffline else if(loggedIn)onCreate else onAccount},
            modifier=Modifier.fillMaxWidth().height(54.dp),
            enabled=solo || !loggedIn || canCreate,shape=RoundedCornerShape(19.dp),
            colors=ButtonDefaults.buttonColors(containerColor=V3Green)) {
            Text(if(solo)"▶ Tek Başına Başla" else if(loggedIn)"▶ Odayı Oluştur" else "Giriş Yap",
                fontWeight=FontWeight.Bold,fontSize=16.sp)
        }
        if(!solo) {
            OutlinedButton(onClick={joinExpanded=!joinExpanded},
                modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)) {
                Text("Oda koduyla katıl",color=purple,fontWeight=FontWeight.Bold)
            }
            if(joinExpanded) {
                OutlinedTextField(value=roomCode,onValueChange=onRoomCode,
                    modifier=Modifier.fillMaxWidth(),label={Text("9 karakterli oda kodu")},
                    singleLine=true)
                Button(onClick=onJoin,enabled=roomCode.length==9,
                    modifier=Modifier.fillMaxWidth()) {Text("Odaya Katıl")}
            }
            TextButton(onClick={friendsExpanded=!friendsExpanded}) {
                Text("Arkadaşlar ve davetler",color=purple)
            }
            if(friendsExpanded)onFriends()
        }
        Text(quotaLabel,color=Color(0xFF878197),fontSize=11.sp)
    }

    if(avatarPicker) AlertDialog(
        onDismissRequest={avatarPicker=false},
        title={Text("Avatarım",color=V3Navy,fontWeight=FontWeight.Bold)},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(11.dp)) {
                Text("Karakterini buradan değiştirebilirsin.",color=Color(0xFF72758E))
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    listOf("robot" to "Bip","fox" to "Pufi","owl" to "Mimo").forEach { (key,label) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if(mascot==key)V3Lavender else Color(0xFFFAF7FF))
                            .clickable {
                                mascot=key
                                prefs.edit().putString("kind",key).apply()
                            }.padding(9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            ArenaMascotV2(key,Modifier.size(55.dp))
                            Text(label,color=V3Navy,fontSize=12.sp,fontWeight=FontWeight.Bold)
                        }
                    }
                }
                TextButton(onClick={avatarPicker=false;onAccount()}) {
                    Text("Hesabımı yönet",color=V3Purple)
                }
            }
        },
        confirmButton={TextButton(onClick={avatarPicker=false}){Text("Tamam")}},
        containerColor=Color.White
    )
}
