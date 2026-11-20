package com.kelimebulmacasi.a1

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ArenaGold175=Color(0xFFFFD36B)
private val ArenaDeep175=Color(0xFF281057)
private val ArenaRose175=Color(0xFFFFB4E8)

/** Original scalable stadium art. Real selected 2D character is composited on top. */
@Composable
private fun Wq175StadiumBackdrop(modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val w=size.width
        val h=size.height
        drawRect(Brush.verticalGradient(listOf(
            Color(0xFF21104B),Color(0xFF52248D),Color(0xFF9C55C5),
            Color(0xFFE4B4ED)),endY=h))
        // Colored illuminated arena seating rings, not a flat purple rectangle.
        for(ring in 0..4) {
            val y=h*(.25f+ring*.115f)
            val alpha=0.09f+ring*.035f
            drawOval(Color.White.copy(alpha=alpha),
                topLeft=Offset(-w*.17f,y),size=Size(w*1.34f,h*.32f),
                style=Stroke((3+ring*1.5f).dp.toPx()))
        }
        // Two spotlights with soft beams and a gold backlit platform.
        val left=Path().apply {
            moveTo(0f,0f);lineTo(w*.54f,h*.84f)
            lineTo(w*.32f,h*.84f);close()
        }
        val right=Path().apply {
            moveTo(w,0f);lineTo(w*.47f,h*.85f)
            lineTo(w*.66f,h*.85f);close()
        }
        drawPath(left,Color(0x2BFFF2B8))
        drawPath(right,Color(0x25E6DDFF))
        drawOval(Brush.radialGradient(
            colors=listOf(Color(0xDFFFDE86),Color(0x40EAAEFF),Color.Transparent),
            center=Offset(w*.72f,h*.77f),radius=w*.35f),
            topLeft=Offset(w*.27f,h*.45f),size=Size(w*.75f,h*.50f))
        for(i in 0..15) {
            val x=w*(i+.5f)/16f
            drawCircle(
                if(i%3==0)ArenaGold175.copy(alpha=.70f)
                else Color.White.copy(alpha=.24f),
                radius=(if(i%3==0)2.8f else 1.6f).dp.toPx(),
                center=Offset(x,h*(.22f+(i%4)*.055f))
            )
        }
        // Roped-off royal podium behind the mascot.
        drawRoundRect(Brush.verticalGradient(listOf(
            Color(0xFFF4C8FF),Color(0xFF8B3CB3))),
            topLeft=Offset(w*.48f,h*.85f),size=Size(w*.5f,h*.13f),
            cornerRadius=CornerRadius(17.dp.toPx()))
        drawRoundRect(ArenaGold175,topLeft=Offset(w*.46f,h*.83f),
            size=Size(w*.54f,4.dp.toPx()),
            cornerRadius=CornerRadius(3.dp.toPx()))
        // Hanging tournament banners.
        listOf(.04f,.89f).forEach { scale ->
            val x=w*scale
            drawRoundRect(Color(0xFF8E3BCA),
                topLeft=Offset(x,h*.16f),size=Size(w*.075f,h*.24f),
                cornerRadius=CornerRadius(8.dp.toPx()))
            drawLine(ArenaGold175,Offset(x,h*.16f),
                Offset(x+w*.075f,h*.16f),3.dp.toPx())
        }
    }
}

@Composable
private fun Wq175ArenaModeSymbol(kind:String) {
    Canvas(Modifier.size(61.dp)) {
        val u=size.minDimension/100f
        when(kind) {
            "duel" -> {
                drawLine(Color(0xFFF8D68A),Offset(22*u,82*u),Offset(78*u,17*u),12*u)
                drawLine(Color(0xFFDAB7FF),Offset(78*u,82*u),Offset(22*u,17*u),12*u)
                drawLine(Color.White,Offset(22*u,15*u),Offset(78*u,69*u),5*u)
                drawLine(Color.White,Offset(78*u,15*u),Offset(22*u,69*u),5*u)
                drawCircle(ArenaGold175,9*u,Offset(50*u,50*u))
            }
            "team" -> {
                drawRoundRect(Color(0xFFBA93FA),Offset(11*u,14*u),
                    Size(55*u,60*u),CornerRadius(13*u))
                drawRoundRect(ArenaGold175,Offset(42*u,37*u),
                    Size(48*u,50*u),CornerRadius(12*u))
                drawCircle(Color(0xFFBA93FA),10*u,Offset(67*u,37*u))
            }
            else -> {
                val lightning=Path().apply {
                    moveTo(55*u,4*u);lineTo(20*u,55*u);lineTo(48*u,55*u)
                    lineTo(37*u,97*u);lineTo(84*u,38*u);lineTo(53*u,38*u)
                    close()
                }
                drawPath(lightning,Brush.verticalGradient(
                    listOf(Color(0xFFFFF5B5),ArenaGold175,Color(0xFFEA9C34))))
                drawPath(lightning,Color(0xFFFFF4D9),style=Stroke(2*u))
            }
        }
    }
}

@Composable
private fun Wq175ModeCard(
    title:String,subtitle:String,kind:String,
    selected:Boolean,enabled:Boolean,onClick:()->Unit
) {
    val shape=RoundedCornerShape(23.dp)
    val gradient=if(selected)
        Brush.horizontalGradient(listOf(Color(0xFF6128B5),
            Color(0xFF873BD0),Color(0xFFB75BD3)))
    else Brush.horizontalGradient(listOf(
        Color(0xFFFFFAFF),Color(0xFFFAF0FF)))
    Row(Modifier.fillMaxWidth().heightIn(min=98.dp)
        .shadow(if(selected)10.dp else 3.dp,shape)
        .clip(shape).background(gradient)
        .border(if(selected)2.5.dp else 1.dp,
            if(selected)ArenaGold175 else Color(0xFFDECBF0),shape)
        .clickable(enabled=enabled,onClick=onClick)
        .padding(horizontal=15.dp,vertical=12.dp),
        horizontalArrangement=Arrangement.spacedBy(13.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(67.dp).clip(RoundedCornerShape(18.dp))
            .background(if(selected)Color(0x559D65CA) else Color(0xFFEAD9FC)),
            contentAlignment=Alignment.Center){
            Wq175ArenaModeSymbol(kind)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Text(title,fontSize=18.sp,fontWeight=FontWeight.ExtraBold,
                color=if(selected)Color.White else ArenaDeep175)
            Text(if(enabled)subtitle else "Arkadaşınla Oyna modunda kullanılabilir",
                fontSize=12.sp,color=if(selected)Color(0xFFF9E6FF)
                else Color(0xFF665478))
        }
        if(selected) Text("✓",color=ArenaGold175,fontSize=23.sp,
            fontWeight=FontWeight.Black)
    }
}

/** Replaces only the v174 entry illustration. Existing match callbacks/settings stay intact. */
@Composable
internal fun Wq175ArenaHeroAndModes(
    selectedAvatar:String,mode:String,solo:Boolean,onMode:(String)->Unit
) {
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min=243.dp)
            .clip(RoundedCornerShape(29.dp))) {
            Wq175StadiumBackdrop(Modifier.matchParentSize())
            Row(Modifier.fillMaxWidth().padding(start=18.dp,end=9.dp,
                top=18.dp,bottom=17.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f),
                    verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    Text("WORDQUEST · TURNOVA",
                        color=ArenaGold175,fontSize=10.sp,
                        fontWeight=FontWeight.Black,letterSpacing=.8.sp)
                    Text("ARENA",color=Color.White,fontSize=30.sp,
                        fontWeight=FontWeight.Black)
                    Text("Sahne senin!\nRakibini seç,\nzaferi kazan.",
                        color=Color(0xFFFFEEFF),fontSize=15.sp,
                        lineHeight=20.sp,fontWeight=FontWeight.SemiBold)
                    Surface(color=Color(0xC34B217F),
                        shape=RoundedCornerShape(15.dp),
                        border=BorderStroke(1.dp,ArenaGold175)) {
                        Text("★ "+Wq2DMascots.label(selectedAvatar),
                            color=Color.White,fontSize=12.sp,
                            fontWeight=FontWeight.ExtraBold,
                            modifier=Modifier.padding(horizontal=11.dp,vertical=7.dp))
                    }
                }
                Box(Modifier.width(168.dp).height(207.dp),
                    contentAlignment=Alignment.BottomCenter) {
                    Wq2DPortrait(selectedAvatar,
                        Modifier.fillMaxWidth().height(192.dp),
                        victory=(mode=="duel"))
                }
            }
        }
        Text("Maceranı seç",fontSize=19.sp,
            fontWeight=FontWeight.ExtraBold,color=ArenaDeep175)
        Wq175ModeCard("Bulmaca Düellosu",
            "İki oyuncu, tek zafer.", "duel",
            selected=mode=="duel",enabled=true) {onMode("duel")}
        Wq175ModeCard("Ortak Bulmaca",
            "Aynı tahtayı birlikte çöz.",
            "team",selected=mode=="team",enabled=!solo) {onMode("team")}
        Wq175ModeCard("5 Şıklı Düello",
            "Hızlı cevap ver, puanı topla.",
            "quiz",selected=mode=="quiz",enabled=true) {onMode("quiz")}
    }
}
