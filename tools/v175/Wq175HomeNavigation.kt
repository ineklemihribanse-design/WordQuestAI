package com.kelimebulmacasi.a1

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Live reminders based only on the current profile's real due-word count. */
@Composable
internal fun Wq175HomeAlerts(due:Int,onMemory:()->Unit,onDaily:()->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick={expanded=true}) {
            Box(Modifier.size(29.dp)) {
                Image(painter=painterResource(R.drawable.wq175_home_alert_bell),
                    contentDescription="Bildirimler",modifier=Modifier.size(27.dp))
                if(due>0) Box(Modifier.align(Alignment.TopEnd).size(9.dp)
                    .background(Color(0xFFE46A56),CircleShape))
            }
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            DropdownMenuItem(
                text={Text(if(due>0)"$due kelime tekrar bekliyor"
                    else "Tekrar bekleyen kelimen yok")},
                onClick={expanded=false;onMemory()})
            DropdownMenuItem(text={Text("Günlük çalışmanı aç")},
                onClick={expanded=false;onDaily()})
        }
    }
}

/** Fixed bottom navigation on the Home screen, with real destination callbacks. */
@Composable
internal fun Wq175HomeBottomNav(
    session:AppSession,onNewGame:()->Unit,onArena:()->Unit,
    onDaily:()->Unit,onAccount:()->Unit,
    modifier:Modifier=Modifier
) {
    val labelColor=Color(0xFF254552)
    val chosen=Wq2DMascots.selected(LocalContext.current,session.profileKey)
    NavigationBar(modifier=modifier.fillMaxWidth(),
        containerColor=Color(0xFFFFFBF3),tonalElevation=6.dp) {
        NavigationBarItem(selected=true,enabled=false,onClick={},
            icon={Wq175ThemeIcon("ana_sayfa",Modifier.size(29.dp))},
            label={Text("Ana",fontSize=10.sp,fontWeight=FontWeight.Bold,color=labelColor)})
        NavigationBarItem(selected=false,onClick=onNewGame,
            icon={Wq175ThemeIcon("yeni_oyun",Modifier.size(29.dp))},
            label={Text("Yeni Oyun",fontSize=10.sp,color=labelColor)})
        NavigationBarItem(selected=false,onClick=onArena,
            icon={Wq175ThemeIcon("arena",Modifier.size(29.dp))},
            label={Text("Arena",fontSize=10.sp,color=labelColor)})
        NavigationBarItem(selected=false,onClick=onDaily,
            icon={Wq175ThemeIcon("gunluk_plan",Modifier.size(29.dp))},
            label={Text("Günlük",fontSize=10.sp,color=labelColor)})
        NavigationBarItem(selected=false,onClick=onAccount,
            icon={Wq2DPortrait(chosen,Modifier.size(29.dp))},
            label={Text("Profil",fontSize=10.sp,color=labelColor)})
    }
}
