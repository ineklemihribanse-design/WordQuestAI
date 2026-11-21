package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shown ONLY after a YDS practice or exam has finished. */
@Composable
internal fun Wq175YdsExamReview(
    questionIds:List<String>, submitted:Map<String,Int>
) {
    val all=remember(questionIds) {
        questionIds.mapNotNull { id -> YdsContent.questions.firstOrNull { it.id==id } }
    }
    val incorrect=all.filter { q -> submitted[q.id] != q.answer }
    var expanded by rememberSaveable(questionIds.size,submitted.size) { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp),
        modifier=Modifier.fillMaxWidth()) {
        Text("Sınav sonrası inceleme",fontSize=19.sp,
            fontWeight=FontWeight.ExtraBold,color=Color(0xFF193660))
        if(incorrect.isEmpty()) {
            Text("Tüm sorular doğru yanıtlandı!",color=Color(0xFF167D62))
        } else {
            Text(incorrect.size.toString()+" yanlış veya boş soru · Açıklamaları incele",
                fontSize=12.sp,color=Color(0xFF60758D))
            val visible=if(expanded)incorrect else incorrect.take(8)
            visible.forEachIndexed { index,q ->
                val chosen=submitted[q.id]?.let { q.options.getOrNull(it) }
                Surface(shape=RoundedCornerShape(17.dp),
                    color=Color(0xFFF6F8FF),
                    border=BorderStroke(1.dp,Color(0xFFD9E3F1)),
                    modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(q.topic.label+" · "+q.difficulty.label,
                            color=Color(0xFF4970AE),fontSize=11.sp,fontWeight=FontWeight.Bold)
                        Text(q.question,color=Color(0xFF213B60),fontWeight=FontWeight.Bold,
                            fontSize=14.sp)
                        Text("Senin yanıtın: "+(chosen?:"Yanıtlanmadı"),
                            color=Color(0xFFA14E57),fontSize=12.sp)
                        Text("Doğru: "+q.options[q.answer],
                            color=Color(0xFF197454),fontWeight=FontWeight.Bold,fontSize=12.sp)
                        Text(q.explanation,color=Color(0xFF415675),fontSize=12.sp,
                            lineHeight=18.sp)
                    }
                }
            }
            if(incorrect.size>8) {
                OutlinedButton(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth()) {
                    Text(if(expanded)"Daha az göster" else "Tüm ${incorrect.size} soruyu göster")
                }
            }
        }
    }
}
