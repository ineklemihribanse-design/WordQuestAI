package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
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

/** User's clue action is ABOVE the crossword and confirms the revealed letter. */
@Composable
internal fun Wq181ArenaHintBar(
    left:Int,alreadyUsed:Boolean,canUse:Boolean,revealed:String?,
    onHint:()->Unit
){
    Surface(color=Color(0xFFFFF7E7),shape=RoundedCornerShape(17.dp),
        border=BorderStroke(1.dp,Color(0xFFF3D29A)),
        modifier=Modifier.fillMaxWidth()){
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=9.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Column(Modifier.weight(1f)) {
                Text(if(alreadyUsed)"HARF İPUCU AÇILDI" else "HARF İPUCU",
                    color=Color(0xFF684A35),fontSize=11.sp,
                    fontWeight=FontWeight.ExtraBold)
                Text(when {
                    alreadyUsed -> revealed ?: "Seçili kelimenin bütün harfleri zaten açık."
                    left<=0 -> "İpucu hakkın kalmadı."
                    !canUse -> "İpucu için sıranın gelmesini bekle."
                    else -> "Rastgele bir harfi aç · $left hakkın var"
                },color=Color(0xFF46344D),fontSize=12.sp,
                    fontWeight=FontWeight.Medium)
            }
            if(!alreadyUsed) {
                Button(onClick=onHint,enabled=canUse&&left>0,
                    colors=ButtonDefaults.buttonColors(
                        containerColor=Color(0xFF6B3B9D),
                        contentColor=Color.White),
                    contentPadding=PaddingValues(horizontal=14.dp,vertical=8.dp),
                    shape=RoundedCornerShape(12.dp)){
                    Text("İPUCU ($left)",fontWeight=FontWeight.Bold,
                        fontSize=12.sp)
                }
            } else {
                Text("✓",color=Color(0xFF517D43),
                    fontWeight=FontWeight.Black,fontSize=22.sp)
            }
        }
    }
}
