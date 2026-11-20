package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Five real Daily Plan games, with their existing callbacks and original 2.5D WebP icons. */
private fun Wq175PracticeCardTint(kind:PracticeKind):Color = when(kind) {
    PracticeKind.Dictation -> Color(0xFFFFE9DF)
    PracticeKind.Sentence -> Color(0xFFE8F5FF)
    PracticeKind.Grammar -> Color(0xFFF0E8FB)
    PracticeKind.Story -> Color(0xFFFFF1DF)
    PracticeKind.Speak -> Color(0xFFE0F5F1)
}

@Composable
private fun Wq175PremiumPracticeCard(kind:PracticeKind,onClick:()->Unit) {
    val pale=Wq175PracticeCardTint(kind)
    Surface(onClick=onClick,
        modifier=Modifier.fillMaxWidth().heightIn(min=166.dp),
        shape=RoundedCornerShape(22.dp),
        color=pale,border=BorderStroke(1.dp,Color(0xFFD9DEE2)),
        shadowElevation=3.dp) {
        Column(Modifier.padding(13.dp),
            verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top,
                horizontalArrangement=Arrangement.SpaceBetween) {
                Wq175GlossIcon(Wq175IconNames.practice(kind),Modifier.size(70.dp))
                Text("›",color=Color(0xFF355674),fontSize=25.sp,
                    fontWeight=FontWeight.Bold)
            }
            Text(kind.title,color=Color(0xFF1E384D),
                fontSize=15.sp,lineHeight=18.sp,
                fontWeight=FontWeight.ExtraBold,
                maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(kind.detail,color=Color(0xFF5E7181),
                fontSize=11.sp,lineHeight=14.sp,
                maxLines=2,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun Wq175PremiumPracticeGrid(onSelect:(PracticeKind)->Unit) {
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Bugünkü oyunların",
            fontSize=20.sp,fontWeight=FontWeight.ExtraBold,
            color=Color(0xFF1F4154))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val oneColumn=maxWidth < 345.dp || LocalDensity.current.fontScale>1.35f
            if(oneColumn) {
                Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    PracticeKind.entries.forEach { kind ->
                        Wq175PracticeTile(kind){onSelect(kind)}
                    }
                }
            } else {
                Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    PracticeKind.entries.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            pair.forEach { kind ->
                                Box(Modifier.weight(1f)) {
                                    Wq175PremiumPracticeCard(kind){onSelect(kind)}
                                }
                            }
                            if(pair.size==1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
