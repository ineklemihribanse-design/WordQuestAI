package com.kelimebulmacasi.a1

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Read-only friend information: only the mutual-friend overview is used. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Wq186FriendProfileDetail(entry: FriendEntry, onDismiss: () -> Unit) {
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    val height = if(tablet) 320.dp else 270.dp
    val width = if(tablet) 245.dp else 196.dp
    val avatar = remember(entry.avatarPreview) {
        Wq185FriendMascot.decode(entry.avatarPreview)
    }

    ModalBottomSheet(onDismissRequest = onDismiss,
        containerColor=Color(0xFFFFFBFF),
        shape=RoundedCornerShape(topStart=27.dp,topEnd=27.dp)) {
        Column(Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal=20.dp, vertical=13.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Arkadaş profili",
                fontSize=21.sp,fontWeight=FontWeight.Bold,
                color=Color(0xFF392855))
            Box(Modifier.width(width).height(height)
                    .background(Color(0xFFF0E9FA),RoundedCornerShape(20.dp)),
                contentAlignment=Alignment.BottomCenter) {
                if(avatar!=null) {
                    Image(bitmap=avatar.asImageBitmap(),
                        contentDescription="Arkadaşının paylaştığı maskot",
                        modifier=Modifier.fillMaxSize(),
                        contentScale=ContentScale.Fit)
                } else if(Wq2DMascots.valid(entry.avatarId) &&
                          !Wq183MascotStore.isCustomId(entry.avatarId)) {
                    Wq2DPortrait(entry.avatarId,Modifier.fillMaxSize())
                } else {
                    Text("Maskot paylaşılmadı",fontSize=13.sp,
                        color=Color(0xFF67567D))
                }
            }
            Text(entry.name, fontSize=24.sp,
                fontWeight=FontWeight.ExtraBold,
                color=Color(0xFF392855),maxLines=2,
                textAlign=TextAlign.Center)
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Surface(color=Color(0xFFF1E7FA),
                    shape=RoundedCornerShape(13.dp),
                    modifier=Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp),
                        horizontalAlignment=Alignment.CenterHorizontally) {
                        Text("★ Puan",fontSize=12.sp,color=Color(0xFF6B5790))
                        Text(entry.points?.toString() ?: "Paylaşılmadı",
                            fontSize=17.sp,fontWeight=FontWeight.Bold,
                            color=Color(0xFF432869))
                    }
                }
                Surface(color=Color(0xFFF1E7FA),
                    shape=RoundedCornerShape(13.dp),
                    modifier=Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp),
                        horizontalAlignment=Alignment.CenterHorizontally) {
                        Text("✓ Oyun",fontSize=12.sp,color=Color(0xFF6B5790))
                        Text(entry.completedGames?.toString() ?: "Paylaşılmadı",
                            fontSize=17.sp,fontWeight=FontWeight.Bold,
                            color=Color(0xFF432869))
                    }
                }
            }
            Text(Wq180FriendActivity.label(entry.lastActiveAt),
                fontSize=12.sp,color=Color(0xFF74647E))
            Text("Yalnızca arkadaşının paylaşmayı seçtiği bilgiler gösterilir. " +
                 "Kişisel ZIP dosyası paylaşılmaz.",
                 color=Color(0xFF796985),fontSize=11.sp,
                 textAlign=TextAlign.Center)
            Button(onClick=onDismiss, modifier=Modifier.fillMaxWidth()) {
                Text("Kapat")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
