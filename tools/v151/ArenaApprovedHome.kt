package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

/** Arena entrance rebuilt from the two approved reference screenshots. */
@Composable
internal fun ArenaApprovedHome(
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
    val prefs=remember(context) {
        context.getSharedPreferences("wordquest_arena_mascot",Context.MODE_PRIVATE)
    }
    var mascot by remember { mutableStateOf(prefs.getString("kind","robot")?:"robot") }
    var avatarPicker by rememberSaveable { mutableStateOf(false) }
    var solo by rememberSaveable { mutableStateOf(soloInitially) }
    var joinOpen by rememberSaveable { mutableStateOf(false) }
    var friendOpen by rememberSaveable { mutableStateOf(false) }
    var timeMenu by remember { mutableStateOf(false) }
    var sizeMenu by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth()
            .background(ArenaApproved.background)
            .padding(horizontal=0.dp,vertical=12.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)
    ){
        // Compact centered brand in the exact placement of the accepted screen.
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                Text("❧ WordQuest AI",fontSize=21.sp,color=Color(0xFF174D43),
                    fontWeight=FontWeight.ExtraBold,maxLines=1)
                Box(Modifier.clip(RoundedCornerShape(25.dp))
                    .background(Color(0xFFEBE0FF))
                    .padding(horizontal=17.dp,vertical=3.dp)) {
                    Text("Arena",color=ArenaApproved.ink,fontWeight=FontWeight.Bold,fontSize=12.sp)
                }
            }
            Surface(onClick={avatarPicker=true},color=Color.White,
                shadowElevation=4.dp,shape=RoundedCornerShape(28.dp),
                border=BorderStroke(1.dp,ArenaApproved.stroke)) {
                Row(Modifier.padding(horizontal=9.dp,vertical=6.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    ArenaMascotV2(mascot,Modifier.size(26.dp))
                    Text(playerName.ifBlank{"Profil"},color=ArenaApproved.ink,
                        fontWeight=FontWeight.Bold,fontSize=11.sp,
                        maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("⌄",color=ArenaApproved.purple,fontSize=12.sp)
                }
            }
        }
        Text("Arena",Modifier.fillMaxWidth(),color=Color(0xFF145047),
            fontWeight=FontWeight.ExtraBold,fontSize=26.sp)
        // Mascot previews are display-only: actual selection stays in the profile.
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,
            verticalAlignment=Alignment.CenterVertically) {
            listOf("robot" to "Bip","fox" to "Pufi","owl" to "Mimo").forEach{(key,label)->
                Column(horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Box(contentAlignment=Alignment.TopEnd) {
                        ArenaApprovedMascot(key,selected=mascot==key,sizeDp=64)
                        if(mascot==key)
                            Box(Modifier.size(17.dp).background(Color(0xFF62AF33),CircleShape),
                                contentAlignment=Alignment.Center){
                                Text("✓",fontSize=11.sp,color=Color.White)
                            }
                    }
                    Text(label,color=ArenaApproved.ink,fontSize=12.sp,
                        fontWeight=FontWeight.SemiBold)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(152.dp)
                .shadow(8.dp,RoundedCornerShape(27.dp),
                    ambientColor=Color(0x247C5BD3))
                .clip(RoundedCornerShape(27.dp))
                .background(Brush.horizontalGradient(
                    listOf(Color(0xFFF0E7FF),Color(0xFFFAF5FF),Color(0xFFE8DEFF))))
                .border(1.dp,Color.White,RoundedCornerShape(27.dp))
                .padding(start=16.dp),
            verticalAlignment=Alignment.CenterVertically
        ){
            Column(Modifier.weight(1.12f),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Text("İNGİLİZCE ARENASI",color=ArenaApproved.purple,
                    fontSize=13.sp,fontWeight=FontWeight.ExtraBold)
                Text("Her maç yeni bir keşif",color=Color(0xFF134B3C),
                    fontSize=23.sp,lineHeight=25.sp,fontWeight=FontWeight.ExtraBold)
            }
            ArenaApprovedHeroArt(Modifier.weight(.98f).fillMaxHeight())
        }
        ArenaApprovedPanel("Oyun Türü","🎮"){
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                ArenaApprovedOption("Bulmaca",mode!="quiz",Modifier.weight(1f),
                    symbol="✤",description="Kesişen kelimeler"){onMode(if(mode=="quiz")"duel" else mode)}
                ArenaApprovedOption("5 Şıklı",mode=="quiz",Modifier.weight(1f),
                    symbol="☷",description="Kelime bilgisi"){onMode("quiz")}
            }
            if(mode!="quiz") {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),
                    verticalAlignment=Alignment.CenterVertically){
                    Text("Bulmaca modu",color=ArenaApproved.sub,fontSize=11.sp)
                    FilterChip(selected=mode=="duel",onClick={onMode("duel")},
                        label={Text("Düello",fontSize=11.sp)})
                    FilterChip(selected=mode=="team",onClick={onMode("team")},
                        label={Text("Ortak",fontSize=11.sp)})
                }
            }
        }
        ArenaApprovedPanel("İngilizce Seviyesi","🎓"){
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(7.dp)){
                PremiumArenaRules.allowedLevels.forEach { value ->
                    val selected=value==level
                    Box(
                        Modifier.widthIn(min=54.dp).height(43.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if(selected)ArenaApproved.lightGreen
                                else ArenaApproved.softPurple)
                            .border(if(selected)1.7.dp else 0.dp,
                                if(selected)Color(0xFF75BC3A) else Color.Transparent,
                                RoundedCornerShape(14.dp))
                            .clickable{onLevel(value)}
                            .padding(horizontal=10.dp),
                        contentAlignment=Alignment.Center
                    ){
                        Text(value,color=ArenaApproved.ink,fontWeight=
                            if(selected)FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize=14.sp)
                    }
                }
            }
        }
        ArenaApprovedPanel("Konu / Paket","▱"){
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                topics.forEachIndexed { index,value ->
                    val selected=value==topic
                    Row(
                        Modifier.widthIn(min=95.dp,max=160.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if(selected)ArenaApproved.lightGreen
                                else Color(0xFFFDF9FF))
                            .border(if(selected)1.6.dp else 1.dp,
                                if(selected)Color(0xFF73BE41) else ArenaApproved.stroke,
                                RoundedCornerShape(13.dp))
                            .clickable{onTopic(value)}
                            .padding(horizontal=11.dp,vertical=12.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(7.dp)
                    ){
                        Text(when(index) {0->"▦";1->"♣";2->"⌂";else->"◆"},
                            color=if(selected)ArenaApproved.green else ArenaApproved.purple,
                            fontSize=17.sp)
                        Text(value,color=ArenaApproved.ink,fontSize=11.sp,
                            fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium,
                            maxLines=2)
                    }
                }
            }
        }
        ArenaApprovedPanel("Oyun Biçimi","🎮"){
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                ArenaApprovedOption("Arkadaşınla Oyna",!solo,Modifier.weight(1f),
                    symbol="♧",description="Oda oluştur, arkadaşını davet et"){solo=false}
                ArenaApprovedOption("Tek Başına Oyna",solo,Modifier.weight(1f),
                    symbol="♙",description="Hemen oyuna başla"){solo=true}
            }
        }
        ArenaApprovedPanel("Maç Ayarları","⚙"){
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFAF6FF)).padding(9.dp),
                    verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Text("◴  Süre",fontSize=12.sp,color=ArenaApproved.ink,
                        fontWeight=FontWeight.Bold)
                    Box {
                        OutlinedButton(onClick={timeMenu=true},
                            modifier=Modifier.fillMaxWidth(),
                            contentPadding=PaddingValues(horizontal=4.dp,vertical=4.dp)){
                            Text(if(seconds==0)"Süresiz" else "$seconds sn",
                                fontSize=11.sp,maxLines=1)
                            Text(" ⌄",fontSize=12.sp)
                        }
                        DropdownMenu(expanded=timeMenu,onDismissRequest={timeMenu=false}) {
                            allowedTimes.forEach { time ->
                                DropdownMenuItem(text={Text(if(time==0)"Süresiz" else "$time sn")},
                                    onClick={onSeconds(time);timeMenu=false})
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFAF6FF)).padding(9.dp),
                    verticalArrangement=Arrangement.spacedBy(7.dp)){
                    Text("✤  Bulmaca",fontSize=12.sp,color=ArenaApproved.ink,
                        fontWeight=FontWeight.Bold)
                    Box {
                        OutlinedButton(onClick={sizeMenu=true},
                            modifier=Modifier.fillMaxWidth(),
                            contentPadding=PaddingValues(horizontal=4.dp,vertical=4.dp)){
                            Text(if(mode=="quiz")"12 soru" else "$size kelime",
                                fontSize=11.sp,maxLines=1)
                            Text(" ⌄",fontSize=12.sp)
                        }
                        DropdownMenu(expanded=sizeMenu,onDismissRequest={sizeMenu=false}) {
                            sizes.forEach { n ->
                                DropdownMenuItem(text={Text("$n kelime")},
                                    onClick={onSize(n);sizeMenu=false})
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEFFAE6)).padding(9.dp),
                    verticalArrangement=Arrangement.spacedBy(7.dp)){
                    Text("ⓘ  İpucu",fontSize=12.sp,color=ArenaApproved.ink,
                        fontWeight=FontWeight.Bold)
                    Text("Zorlanırsan sınırlı sayıda ipucu kullan.",
                        fontSize=10.sp,color=ArenaApproved.sub,lineHeight=13.sp)
                }
            }
        }
        if(!solo) ArenaRulesCardV3(mode,if(mode=="quiz")12 else size,seconds)
        ArenaApprovedPrimary(
            text=if(solo)"Oyuna Başla" else if(loggedIn)"Odayı Oluştur" else "Giriş Yap",
            enabled=solo || !loggedIn || canCreate,
            onClick=if(solo)onOffline else if(loggedIn)onCreate else onAccount,
            green=true
        )
        if(!solo){
            OutlinedButton(onClick={joinOpen=!joinOpen},
                modifier=Modifier.fillMaxWidth().height(47.dp),
                shape=RoundedCornerShape(16.dp),
                border=BorderStroke(1.dp,ArenaApproved.stroke)){
                Text("Oda Koduyla Katıl",color=ArenaApproved.plum,fontWeight=FontWeight.Bold)
            }
            if(joinOpen) {
                OutlinedTextField(value=roomCode,onValueChange=onRoomCode,
                    label={Text("9 karakterli oda kodu")},singleLine=true,
                    modifier=Modifier.fillMaxWidth())
                ArenaApprovedPrimary("Odaya Katıl",roomCode.length==9,onJoin)
            }
            TextButton(onClick={friendOpen=!friendOpen}) {
                Text(if(friendOpen)"Arkadaş davetlerini gizle" else "Arkadaşlar ve davetler",
                    color=ArenaApproved.purple)
            }
            if(friendOpen)onFriends()
        }
        Text(quotaLabel,color=ArenaApproved.sub,fontSize=11.sp)
    }
    if(avatarPicker){
        AlertDialog(
            onDismissRequest={avatarPicker=false},
            title={Text("Avatarım",fontWeight=FontWeight.Bold,color=ArenaApproved.ink)},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
                    Text("Karakterini seç. Bu ayar cihazında saklanır.",
                        color=ArenaApproved.sub,fontSize=12.sp)
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                        listOf("robot" to "Bip","fox" to "Pufi","owl" to "Mimo")
                            .forEach { (key,label) ->
                            Column(
                                Modifier.weight(1f)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(if(mascot==key)ArenaApproved.softPurple
                                        else ArenaApproved.cream)
                                    .clickable{
                                        mascot=key
                                        prefs.edit().putString("kind",key).apply()
                                    }
                                    .padding(8.dp),
                                horizontalAlignment=Alignment.CenterHorizontally){
                                ArenaMascotV2(key,Modifier.size(56.dp))
                                Text(label,color=ArenaApproved.ink,fontSize=12.sp,
                                    fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                    TextButton(onClick={avatarPicker=false;onAccount()}){
                        Text("Hesap ayarlarım",color=ArenaApproved.purple)
                    }
                }
            },
            confirmButton={TextButton(onClick={avatarPicker=false}){
                Text("Tamam",color=ArenaApproved.plum)}},
            containerColor=Color.White
        )
    }
}
