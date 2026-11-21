package com.kelimebulmacasi.a1

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Dedicated catalogue AFTER selecting a CEFR/YDS package. No mock catalogue data. */
@Composable
internal fun Wq175PackageCatalog(
    session:AppSession,
    mode:GameMode,
    level:String,
    onGroup:(String)->Unit,
    onBack:()->Unit
) {
    val entries=session.games.filter { it.mode==mode && it.level==level }
    val groups=entries.groupBy(Game::category).toSortedMap()
    val finished=entries.count { session.records[it.id]?.completed==true }
    ScreenPage("Paket Kataloğu", "${mode.title} · $level", onBack) {
        Box(Modifier.fillMaxWidth().height(152.dp).clip(RoundedCornerShape(24.dp))) {
            Wq175SceneBackdrop("hazir_paketler", Modifier.matchParentSize())
            Column(Modifier.align(Alignment.CenterStart).padding(17.dp),
                verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text("$level · Hazır Paket", color=Color.White,
                    fontSize=22.sp,fontWeight=FontWeight.ExtraBold)
                Text("${entries.size} bulmaca · $finished tamamlandı",
                    color=Color(0xFFFFEEC6),fontSize=13.sp)
            }
        }
        Text("Konunu seç",fontSize=19.sp,color=Color(0xFF293B49),
            fontWeight=FontWeight.ExtraBold)
        if(groups.isEmpty()) {
            Text("Bu seviyede henüz hazır bulmaca bulunmuyor.",
                color=Color(0xFF65727A))
        }
        groups.forEach { (topic,games) ->
            val done=games.count { session.records[it.id]?.completed==true }
            MenuItem(topic,
                "${games.size} bulmaca · $done tamamlandı",
                if(mode==GameMode.Yds) "YDS" else "◇"
            ) { onGroup(topic) }
        }
    }
}
