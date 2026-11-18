package com.kelimebulmacasi.a1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object ArenaV2Colors {
    val ink = Color(0xFF293253)
    val muted = Color(0xFF68748F)
    val violet = Color(0xFF725DD3)
    val mint = Color(0xFF248E80)
    val coral = Color(0xFFD57564)
    val ivory = Color(0xFFF8F6FE)
    val gold = Color(0xFFCD9D3E)
}

/** Small original vector mascots, not network images or a 3D engine. */
@Composable
internal fun ArenaMascotV2(kind: String, modifier: Modifier = Modifier) {
    val robot = kind == "robot"
    val owl = kind == "owl"
    Canvas(modifier) {
        val u = size.minDimension / 100f
        if (robot) {
            drawRoundRect(Color(0xFFB7AEF6), Offset(19*u, 9*u), Size(62*u, 79*u), CornerRadius(22*u))
            drawRoundRect(Color(0xFFF5F4FF), Offset(13*u, 21*u), Size(74*u, 60*u), CornerRadius(22*u))
            drawRoundRect(Color(0xFF30385B), Offset(23*u, 31*u), Size(54*u, 39*u), CornerRadius(15*u))
            drawCircle(Color(0xFF7AE0D3), 7*u, Offset(41*u, 49*u))
            drawCircle(Color(0xFF7AE0D3), 7*u, Offset(59*u, 49*u))
            drawCircle(Color.White, 2.5f*u, Offset(43*u, 46*u))
            drawCircle(Color.White, 2.5f*u, Offset(61*u, 46*u))
            drawArc(Color(0xFF83E2D9), 12f, 156f, false,
                Offset(42*u, 54*u), Size(16*u, 9*u), style=Stroke(2.5f*u))
            drawLine(Color(0xFF8275D8), Offset(50*u, 12*u), Offset(50*u, 22*u), 4*u)
            drawCircle(Color(0xFFFFCC83), 6*u, Offset(50*u, 8*u))
        } else if (owl) {
            drawCircle(Color(0xFFAEA0D7), 38*u, Offset(50*u, 50*u))
            drawCircle(Color(0xFFF7EDDD), 21*u, Offset(35*u, 49*u))
            drawCircle(Color(0xFFF7EDDD), 21*u, Offset(65*u, 49*u))
            drawCircle(Color(0xFF343B54), 9*u, Offset(39*u, 48*u))
            drawCircle(Color(0xFF343B54), 9*u, Offset(61*u, 48*u))
            drawCircle(Color.White, 3*u, Offset(42*u, 45*u))
            drawCircle(Color.White, 3*u, Offset(64*u, 45*u))
            val beak=Path().apply{moveTo(45*u,61*u);lineTo(55*u,61*u);lineTo(50*u,70*u);close()}
            drawPath(beak,Color(0xFFF5B96A))
        } else {
            val left=Path().apply{moveTo(19*u,40*u);lineTo(24*u,5*u);lineTo(46*u,29*u);close()}
            val right=Path().apply{moveTo(54*u,29*u);lineTo(76*u,5*u);lineTo(81*u,40*u);close()}
            drawPath(left, Color(0xFFEA9C69));drawPath(right,Color(0xFFEA9C69))
            drawCircle(Color(0xFFF3AD7E), 37*u, Offset(50*u,50*u))
            drawCircle(Color(0xFFFFF5E9), 17*u, Offset(37*u,62*u))
            drawCircle(Color(0xFFFFF5E9), 17*u, Offset(63*u,62*u))
            drawCircle(Color(0xFF493B45), 5*u, Offset(39*u,48*u))
            drawCircle(Color(0xFF493B45), 5*u, Offset(61*u,48*u))
            drawCircle(Color.White, 1.8f*u, Offset(40*u,46*u))
            drawCircle(Color.White, 1.8f*u, Offset(62*u,46*u))
            drawCircle(Color(0xFF5D4650), 5*u, Offset(50*u,63*u))
        }
    }
}

@Composable
internal fun ArenaLiveScoreV2(
    hostName: String, guestName: String, hostScore: Int, guestScore: Int,
    isHostTurn: Boolean, countdown: Int?, completed: Int, total: Int, cooperative: Boolean
) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
        .background(Color.White).padding(10.dp),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
            ArenaMascotV2("robot",Modifier.size(40.dp))
            Text(hostName,color=ArenaV2Colors.ink,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(hostScore.toString(),color=ArenaV2Colors.violet,fontSize=19.sp,fontWeight=FontWeight.Bold)
            if(isHostTurn&&!cooperative) Text("● SIRA ONDA",color=ArenaV2Colors.mint,fontSize=9.sp)
        }
        Column(Modifier.width(86.dp).background(Color(0xFFF1EDFF),RoundedCornerShape(13.dp))
            .padding(9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            val timeLabel = if(countdown==null) "∞" else "00:" + countdown.coerceAtLeast(0).toString().padStart(2,'0')
            Text(timeLabel,color=ArenaV2Colors.violet,fontSize=19.sp,fontWeight=FontWeight.Bold)
            Text("$completed / $total",color=ArenaV2Colors.muted,fontSize=10.sp)
        }
        Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
            ArenaMascotV2("fox",Modifier.size(40.dp))
            Text(guestName,color=ArenaV2Colors.ink,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(guestScore.toString(),color=ArenaV2Colors.coral,fontSize=19.sp,fontWeight=FontWeight.Bold)
            if(!isHostTurn&&!cooperative) Text("● SIRA ONDA",color=ArenaV2Colors.mint,fontSize=9.sp)
        }
    }
}

@Composable
internal fun ArenaVictoryV2(winnerName: String, winnerIsHost: Boolean, isDraw: Boolean) {
    val jump=remember {Animatable(.85f)}
    LaunchedEffect(winnerName,isDraw){
        jump.snapTo(.85f);jump.animateTo(1.05f,tween(650));jump.animateTo(1f,tween(250))
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .background(Color(0xFFEDE9FF)).padding(16.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(if(isDraw)"BERABERLİK" else "🏆 ZAFER!",color=ArenaV2Colors.violet,
            fontWeight=FontWeight.Bold,fontSize=23.sp)
        Row(verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            ArenaMascotV2(if(winnerIsHost)"robot" else "fox",Modifier.size(86.dp).scale(jump.value))
            Text(if(isDraw)"İyi mücadele!" else winnerName,color=ArenaV2Colors.ink,
                fontSize=17.sp,fontWeight=FontWeight.Bold)
        }
        Text(if(isDraw)"İki oyuncu da harikaydı."
            else "Yeni bir maçta yeniden buluşalım!",color=ArenaV2Colors.muted,fontSize=12.sp)
    }
}
