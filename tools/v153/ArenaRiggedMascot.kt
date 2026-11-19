package com.kelimebulmacasi.a1

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI

private data class MascotPaint(
    val body: Color, val highlight: Color, val shadow: Color,
    val face: Color, val accent: Color, val eye: Color
)

private fun arenaPaint(kind: String): MascotPaint = when(ArenaMascotRig.safe(kind)){
    "fox" -> MascotPaint(
        Color(0xFFF3A264),Color(0xFFFFD09D),Color(0xFFBE6E4F),
        Color(0xFFFFF5E6),Color(0xFFB97BD9),Color(0xFF49394E))
    "owl" -> MascotPaint(
        Color(0xFFAFA0DF),Color(0xFFE3D9FF),Color(0xFF8170AC),
        Color(0xFFFFF5EA),Color(0xFFF3C46D),Color(0xFF373852))
    "rabbit" -> MascotPaint(
        Color(0xFFE8D9F9),Color(0xFFFFF8FF),Color(0xFFB4A3D6),
        Color(0xFFFFFBFF),Color(0xFFF2A4C1),Color(0xFF55436B))
    "bear" -> MascotPaint(
        Color(0xFFC99679),Color(0xFFF5C9A0),Color(0xFF92644C),
        Color(0xFFFBECD9),Color(0xFF7BBEA5),Color(0xFF443844))
    else -> MascotPaint(
        Color(0xFF9E93E6),Color(0xFFF4F2FF),Color(0xFF7365BE),
        Color(0xFFFAF9FF),Color(0xFF72E1D6),Color(0xFF30385B))
}

private fun DrawScope.mascotTrophy(u:Float) {
    val gold=Color(0xFFFFC343)
    val rim=Color(0xFFFFE79A)
    // The trophy is drawn in forearm/wrist local coordinates: it cannot drift.
    drawRoundRect(Brush.verticalGradient(listOf(rim,gold,Color(0xFFDE9830))),
        Offset(-14*u,-44*u),Size(28*u,27*u),CornerRadius(8*u))
    drawArc(gold, startAngle=58f,sweepAngle=255f,useCenter=false,
        topLeft=Offset(-26*u,-42*u),size=Size(22*u,23*u),
        style=Stroke(4*u))
    drawArc(gold,startAngle=-133f,sweepAngle=255f,useCenter=false,
        topLeft=Offset(4*u,-42*u),size=Size(22*u,23*u),
        style=Stroke(4*u))
    drawRoundRect(Color(0xFFE9A73B),Offset(-3*u,-17*u),
        Size(6*u,11*u),CornerRadius(2*u))
    drawRoundRect(Color(0xFF9465C9),Offset(-14*u,-7*u),
        Size(28*u,6*u),CornerRadius(3*u))
    val star=Path().apply {
        moveTo(0f,-38*u);lineTo(3*u,-33*u);lineTo(9*u,-31*u)
        lineTo(4*u,-27*u);lineTo(4*u,-21*u);lineTo(0f,-25*u)
        lineTo(-4*u,-21*u);lineTo(-4*u,-27*u);lineTo(-9*u,-31*u)
        lineTo(-3*u,-33*u);close()
    }
    drawPath(star,Color.White)
}

private fun DrawScope.mascotArm(
    x:Float, y:Float, u:Float, angle:Float, elbow:Float,
    brushColor:Color, highlight:Color, holdingCup:Boolean=false
) {
    withTransform({ translate(x*u,y*u); rotate(angle) }) {
        drawRoundRect(brushColor,
            Offset(-10*u,-2*u),Size(20*u,37*u),CornerRadius(11*u))
        drawRoundRect(highlight.copy(alpha=.50f),
            Offset(-6*u,1*u),Size(7*u,30*u),CornerRadius(4*u))
        drawCircle(brushColor,10*u,Offset(0f,31*u))
        withTransform({ translate(0f,31*u); rotate(elbow) }) {
            drawRoundRect(brushColor,Offset(-9*u,-1*u),Size(18*u,28*u),
                CornerRadius(10*u))
            drawRoundRect(highlight.copy(alpha=.35f),Offset(-6*u,1*u),
                Size(5*u,23*u),CornerRadius(4*u))
            drawCircle(highlight,10*u,Offset(0f,26*u))
            if(holdingCup) {
                withTransform({
                    translate(0f,24*u)
                    // Wrist counter-rotation keeps the cup upright while the
                    // parent shoulder and elbow continue to articulate.
                    rotate(-angle-elbow+6f)
                }) {
                    mascotTrophy(u)
                }
            }
        }
    }
}

private fun DrawScope.mascotLeg(
    x:Float,y:Float,u:Float,angle:Float,paint:MascotPaint
){
    withTransform({translate(x*u,y*u);rotate(angle)}){
        drawRoundRect(paint.shadow,Offset(-11*u,0f),Size(22*u,25*u),
            CornerRadius(10*u))
        withTransform({translate(0f,21*u);rotate(-angle*.65f)}){
            drawRoundRect(paint.body,Offset(-9*u,0f),Size(18*u,23*u),
                CornerRadius(10*u))
            drawOval(paint.highlight,Offset(-16*u,21*u),Size(32*u,12*u))
        }
    }
}

private fun DrawScope.mascotEars(kind:String,paint:MascotPaint,u:Float,wobble:Float){
    if(kind=="robot") {
        drawLine(paint.shadow,Offset(0f,-32*u),Offset(0f,-46*u),4*u)
        drawCircle(Color(0xFFFFD47E),7*u,Offset(0f,-48*u))
        return
    }
    if(kind=="fox" || kind=="rabbit") {
        for(side in listOf(-1f,1f)) {
            withTransform({
                translate(side*26*u,-25*u)
                rotate(side*wobble)
            }) {
                val ear=Path().apply{
                    moveTo(-13*u,7*u);lineTo(-8*u,if(kind=="rabbit")-57*u else -29*u)
                    quadraticTo(0f,if(kind=="rabbit")-65*u else -34*u,7*u,
                        if(kind=="rabbit")-57*u else -29*u)
                    lineTo(14*u,7*u);close()
                }
                drawPath(ear,paint.body)
                val inner=Path().apply {
                    moveTo(-6*u,4*u);lineTo(-3*u,if(kind=="rabbit")-43*u else -19*u)
                    lineTo(4*u,if(kind=="rabbit")-43*u else -19*u)
                    lineTo(7*u,4*u);close()
                }
                drawPath(inner,if(kind=="rabbit")paint.accent else paint.highlight)
            }
        }
    } else {
        // Owl feather tufts and teddy ears use independent head-bone offsets.
        for(side in listOf(-1f,1f)) {
            withTransform({
                translate(side*30*u,-24*u)
                rotate(side*wobble)
            }) {
                drawCircle(paint.body,if(kind=="bear")17*u else 14*u,
                    Offset.Zero)
                drawCircle(if(kind=="bear")paint.face else paint.highlight,8*u,
                    Offset.Zero)
            }
        }
    }
}

private fun DrawScope.mascotHead(
    kind:String,paint:MascotPaint,u:Float,pose:ArenaMascotRig.Pose
){
    withTransform({translate(80*u,60*u);rotate(pose.headAngle)}){
        mascotEars(kind,paint,u,pose.ears)
        val faceBrush=Brush.radialGradient(
            listOf(paint.highlight,paint.body,paint.shadow),
            center=Offset(-14*u,-15*u),radius=100*u
        )
        if(kind=="robot"){
            drawRoundRect(faceBrush,Offset(-39*u,-33*u),
                Size(78*u,68*u),CornerRadius(24*u))
            drawRoundRect(Color(0xFFFAF9FF),Offset(-35*u,-27*u),
                Size(70*u,62*u),CornerRadius(22*u))
            drawRoundRect(paint.eye,Offset(-29*u,-15*u),
                Size(58*u,42*u),CornerRadius(16*u))
            for(side in listOf(-1f,1f)) {
                val x=side*15*u
                if(pose.eyelid>.5f){
                    drawLine(paint.accent,Offset(x-7*u,6*u),Offset(x+7*u,6*u),3*u)
                }else{
                    drawCircle(paint.accent,7.4f*u,Offset(x,4*u))
                    drawCircle(Color.White,2.4f*u,Offset(x-2*u,1*u))
                }
            }
            drawArc(paint.accent,10f,160f,false,Offset(-9*u,10*u),
                Size(18*u,12*u),style=Stroke(2.7f*u))
        }else{
            drawOval(faceBrush,Offset(-43*u,-35*u),Size(86*u,75*u))
            if(kind=="fox" || kind=="bear"){
                drawOval(paint.face,Offset(-32*u,5*u),Size(32*u,25*u))
                drawOval(paint.face,Offset(0f,5*u),Size(32*u,25*u))
            }
            if(kind=="owl") {
                drawCircle(paint.face,23*u,Offset(-17*u,-1*u))
                drawCircle(paint.face,23*u,Offset(17*u,-1*u))
            }
            if(kind=="rabbit") {
                drawOval(paint.face,Offset(-27*u,-10*u),Size(54*u,47*u))
            }
            for(side in listOf(-1f,1f)) {
                val x=side*17*u
                if(pose.eyelid>.5f) {
                    drawLine(paint.eye,Offset(x-7*u,0f),Offset(x+7*u,0f),3*u)
                }else{
                    drawCircle(paint.eye,if(kind=="owl")8*u else 5.8f*u,
                        Offset(x,-1*u))
                    drawCircle(Color.White,2*u,Offset(x-2*u,-3*u))
                }
            }
            if(kind=="owl") {
                val beak=Path().apply {
                    moveTo(-7*u,10*u);lineTo(7*u,10*u)
                    lineTo(0f,22*u);close()
                }
                drawPath(beak,paint.accent)
            }else{
                drawOval(if(kind=="rabbit")paint.accent else paint.eye,
                    Offset(-5*u,10*u),Size(10*u,8*u))
                drawArc(paint.eye,6f,165f,false,Offset(-9*u,16*u),
                    Size(18*u,11*u),style=Stroke(2*u))
            }
            drawCircle(Color(0xFFFFB0B7).copy(alpha=.4f),6*u,
                Offset(-27*u,15*u))
            drawCircle(Color(0xFFFFB0B7).copy(alpha=.4f),6*u,
                Offset(27*u,15*u))
        }
    }
}

/**
 * True hierarchical procedural 2.5D motion (not a speaking photo or a static
 * frame animation): independent head, ears, eyes, shoulders, elbows, wrists,
 * knees and feet. The held trophy inherits arm+elbow transforms.
 */
@Composable
internal fun ArenaRiggedMascot(
    kind:String,modifier:Modifier=Modifier,holdingTrophy:Boolean=false
) {
    val id=ArenaMascotRig.safe(kind)
    val cycle=rememberInfiniteTransition()
    val phase by cycle.animateFloat(
        initialValue=0f,targetValue=(2*PI).toFloat(),
        animationSpec=infiniteRepeatable(
            animation=tween(durationMillis=4750,easing=LinearEasing),
            repeatMode=RepeatMode.Restart
        )
    )
    val pose=ArenaMascotRig.pose(id,phase,holdingTrophy)
    val paint=arenaPaint(id)
    Canvas(modifier) {
        val u=minOf(size.width/160f,size.height/180f)
        if(u<=0f)return@Canvas
        val left=(size.width-160*u)/2f
        val top=(size.height-180*u)/2f
        withTransform({translate(left,top+pose.bodyBob*u)}){
            drawOval(Color(0xFF4C3D68).copy(alpha=.13f),
                Offset(35*u,166*u),Size(90*u,10*u))
            mascotLeg(65f,127f,u,pose.legLeft,paint)
            mascotLeg(95f,127f,u,pose.legRight,paint)
            mascotArm(45f,98f,u,pose.armRight,pose.elbowRight,
                paint.body,paint.highlight)
            // Separate torso and chest layers create light/shadow parallax.
            withTransform({translate(80*u,115*u);rotate(pose.torsoAngle)}){
                val bodyGradient=Brush.radialGradient(
                    listOf(paint.highlight,paint.body,paint.shadow),
                    center=Offset(-22*u,-18*u),radius=90*u
                )
                drawOval(bodyGradient,Offset(-39*u,-41*u),
                    Size(78*u,67*u+pose.breathing*u))
                drawOval(paint.face.copy(alpha=.58f),
                    Offset(-24*u,-23*u),Size(48*u,38*u))
                drawRoundRect(paint.accent,Offset(-23*u,6*u),
                    Size(46*u,7*u),CornerRadius(5*u))
            }
            mascotArm(115f,98f,u,pose.armLeft,pose.elbowLeft,
                paint.body,paint.highlight,holdingCup=holdingTrophy)
            mascotHead(id,paint,u,pose)
        }
    }
}
