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
        modifier=Modifier.fillMaxWidth().heightIn(min=145.dp),
        shape=RoundedCornerShape(22.dp),
        color=pale,border=BorderStroke(1.dp,Color(0xFFE2DDE1)),
        shadowElevation=4.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=12.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            if(kind==PracticeKind.Dictation) {
                Wq175ThemeIcon("dinle_ve_yaz",Modifier.size(72.dp))
            } else if(kind==PracticeKind.Grammar) {
                Wq175ThemeIcon("dilbilgisi",Modifier.size(72.dp))
            } else {
                Wq175GlossIcon(Wq175IconNames.practice(kind),Modifier.size(72.dp))
            }
            Column(Modifier.weight(1f),
                verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(kind.title,color=Color(0xFF1E384D),
                    fontSize=15.sp,lineHeight=18.sp,
                    fontWeight=FontWeight.ExtraBold,
                    maxLines=3,overflow=TextOverflow.Ellipsis)
                Text(kind.detail,color=Color(0xFF5E7181),
                    fontSize=11.sp,lineHeight=14.sp,
                    maxLines=4,overflow=TextOverflow.Ellipsis)
            }
            Text("›",color=Color(0xFF355674),fontSize=23.sp,
                fontWeight=FontWeight.Bold)
        }
    }
}


/**
 * The two real Daily Plan extras used to fall back to plain text MenuItems.
 * Keep exactly the same caller-provided callbacks and legacy symbol argument;
 * only replace their visible presentation with illustrated, tappable Android
 * cards. Neither Lena nor the existing lesson/game state is modified.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun Wq175PremiumPracticeSpecialTile(
    title:String,detail:String,legacySymbol:String,onClick:()->Unit
) {
    val teacher=title.contains("AI",ignoreCase=true)
    val shape=RoundedCornerShape(22.dp)
    val tint=if(teacher)Color(0xFFE8E8FF) else Color(0xFFFFF2D8)
    val ink=Color(0xFF213B47)
    Surface(
        onClick=onClick,
        modifier=Modifier.fillMaxWidth().heightIn(min=116.dp),
        shape=shape,color=tint,
        border=BorderStroke(1.dp,if(teacher)Color(0xFFCBC8F3) else Color(0xFFF0D8A6)),
        shadowElevation=4.dp
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=12.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(11.dp)) {
            Wq175GlossIcon(
                if(teacher)"ai_teacher" else "mistakes",
                Modifier.size(78.dp))
            Column(Modifier.weight(1f),
                verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(title,color=ink,fontWeight=FontWeight.ExtraBold,
                    fontSize=17.sp,lineHeight=20.sp,
                    maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(detail,color=Color(0xFF536575),
                    fontSize=12.sp,lineHeight=16.sp,
                    maxLines=3,overflow=TextOverflow.Ellipsis)
            }
            Text("›",color=if(teacher)Color(0xFF5C3EA6)
                else Color(0xFFAC6A27),
                fontSize=27.sp,fontWeight=FontWeight.Bold)
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
