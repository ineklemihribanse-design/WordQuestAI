package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WqFriendInk = Color(0xFF392855)
private val WqFriendForest = Color(0xFF145848)
private val WqFriendPurple = Color(0xFF7354A5)
private val WqFriendStroke = Color(0xFFE0D4EF)

/**
 * A read-only, responsive friend sheet. Every value comes from the friend's
 * shared profile: no fabricated last-seen status, games or online controls.
 * Uploaded personal ZIP content is never transferred in this view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Wq186FriendProfileDetail(entry: FriendEntry, onDismiss: () -> Unit) {
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    val avatarWidth = if (tablet) 220.dp else 174.dp
    val avatarHeight = if (tablet) 260.dp else 208.dp
    val preview = remember(entry.avatarPreview) {
        Wq185FriendMascot.decode(entry.avatarPreview)
    }
    val builtin = entry.avatarId?.takeIf {
        Wq2DMascots.valid(it) && !Wq183MascotStore.isCustomId(it)
    }
    val lastActivity = Wq180FriendActivity.label(entry.lastActiveAt)
    var detailsOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest=onDismiss,
        containerColor=Color(0xFFFFFBFF),
        shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp)
    ) {
        Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max=660.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal=18.dp,vertical=8.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(13.dp)
            ) {
                Row(Modifier.fillMaxWidth(),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    Wq175GlossIcon("account",Modifier.size(39.dp))
                    Text("Arkadaş profili",Modifier.weight(1f),
                        fontSize=22.sp,fontWeight=FontWeight.ExtraBold,
                        color=WqFriendInk)
                    TextButton(onClick=onDismiss) {
                        Text("Kapat",color=WqFriendForest,fontWeight=FontWeight.Bold)
                    }
                }
                Box(
                    Modifier.fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(
                                Color(0xFFF7F0FF),Color(0xFFF0ECFC),Color(0xFFE8F6F1)
                            )),RoundedCornerShape(27.dp))
                        .border(1.dp,WqFriendStroke,RoundedCornerShape(27.dp))
                        .padding(horizontal=15.dp,vertical=15.dp),
                    contentAlignment=Alignment.Center
                ) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.width(avatarWidth).height(avatarHeight)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFE8DDF7))
                            .border(2.dp,Color.White,RoundedCornerShape(24.dp)),
                            contentAlignment=Alignment.BottomCenter) {
                            if(preview != null) {
                                Image(preview.asImageBitmap(),
                                    contentDescription="Arkadaşının paylaştığı maskot",
                                    modifier=Modifier.fillMaxSize(),
                                    contentScale=ContentScale.Fit)
                            } else if(builtin != null) {
                                Wq2DPortrait(builtin,Modifier.fillMaxSize())
                            } else {
                                Column(horizontalAlignment=Alignment.CenterHorizontally,
                                    verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                    Wq175GlossIcon("account",Modifier.size(64.dp))
                                    Text("Maskot paylaşılmadı",fontSize=12.sp,
                                        color=WqFriendPurple)
                                }
                            }
                        }
                        Text(entry.name,maxLines=2,overflow=TextOverflow.Ellipsis,
                            textAlign=TextAlign.Center,
                            fontSize=25.sp,fontWeight=FontWeight.ExtraBold,
                            color=WqFriendInk)
                        Surface(color=Color(0xFFE4F3EC),
                            shape=RoundedCornerShape(50.dp)) {
                            Text(lastActivity,
                                Modifier.padding(horizontal=14.dp,vertical=7.dp),
                                color=WqFriendForest,fontSize=12.sp,
                                fontWeight=FontWeight.Medium)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(11.dp)) {
                    Wq186FriendStat(
                        modifier=Modifier.weight(1f),
                        icon="trophy",label="Puan",
                        value=entry.points?.toString() ?: "—")
                    Wq186FriendStat(
                        modifier=Modifier.weight(1f),
                        icon="puzzle",label="Oyun",
                        value=entry.completedGames?.toString() ?: "—")
                }
                Surface(modifier=Modifier.fillMaxWidth(),
                    shape=RoundedCornerShape(19.dp),
                    color=Color(0xFFF8F4FC),
                    border=BorderStroke(1.dp,WqFriendStroke)) {
                    Column(Modifier.padding(15.dp),
                        verticalArrangement=Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(),
                            verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Wq175GlossIcon("analytics",Modifier.size(30.dp))
                            Text("Kullanım bilgisi",Modifier.weight(1f),
                                fontWeight=FontWeight.Bold,fontSize=16.sp,
                                color=WqFriendInk)
                            TextButton(onClick={ detailsOpen=!detailsOpen }) {
                                Text(if(detailsOpen)"Bilgiyi gizle" else "ⓘ Bilgi",
                                    color=WqFriendPurple,fontSize=12.sp)
                            }
                        }
                        HorizontalDivider(color=WqFriendStroke)
                        Row(Modifier.fillMaxWidth(),
                            verticalAlignment=Alignment.CenterVertically) {
                            Text("Son etkinlik",Modifier.weight(1f),
                                color=Color(0xFF706480),fontSize=13.sp)
                            Text(lastActivity,color=WqFriendForest,
                                fontWeight=FontWeight.SemiBold,fontSize=12.sp)
                        }
                        if(detailsOpen) {
                            Text("Yalnızca arkadaşının paylaşmayı seçtiği " +
                                "istatistikler gösterilir. Paylaşılmayan değerler " +
                                "— olarak işaretlenir. Kişisel maskot ZIP dosyası " +
                                "ve özel hesap verileri bu ekranda açılmaz.",
                                color=Color(0xFF746783),fontSize=12.sp,
                                lineHeight=17.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun Wq186FriendStat(
    modifier:Modifier,icon:String,label:String,value:String
) {
    Surface(modifier=modifier,
        shape=RoundedCornerShape(19.dp),
        color=Color(0xFFF0E7FA),
        border=BorderStroke(1.dp,WqFriendStroke)) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement=Arrangement.spacedBy(9.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Wq175GlossIcon(icon,Modifier.size(44.dp))
            Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(label,color=WqFriendPurple,fontSize=12.sp,
                    fontWeight=FontWeight.SemiBold)
                Text(value,color=WqFriendInk,fontSize=21.sp,
                    fontWeight=FontWeight.ExtraBold)
            }
        }
    }
}
