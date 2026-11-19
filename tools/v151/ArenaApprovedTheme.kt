package com.kelimebulmacasi.a1

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Exact approved Arena color direction: white/ivory, lilac, leafy green, royal violet. */
internal object ArenaApproved {
    val cream=Color(0xFFFFFBFA)
    val ink=Color(0xFF302056)
    val sub=Color(0xFF726A94)
    val purple=Color(0xFF8250D1)
    val plum=Color(0xFF5929A6)
    val lilac=Color(0xFFECE1FF)
    val stroke=Color(0xFFE7DCF8)
    val green=Color(0xFF238244)
    val lightGreen=Color(0xFFE8F9DF)
    val peach=Color(0xFFFFEEE7)
    val softPurple=Color(0xFFF0E9FF)
    val gold=Color(0xFFFFC343)
    val purpleGradient=Brush.verticalGradient(
        listOf(Color(0xFFB274F6),Color(0xFF7C3BCA),Color(0xFF6331AC)))
    val greenGradient=Brush.horizontalGradient(
        listOf(Color(0xFF3EAB75),Color(0xFF20783D)))
    val background=Brush.verticalGradient(
        listOf(Color(0xFFFFFCFA),Color(0xFFFDF8FF),Color(0xFFFFFBFA)))
}

@Composable
internal fun ArenaApprovedPanel(
    heading: String,
    icon: String,
    content: @Composable ColumnScope.()->Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .shadow(6.dp,RoundedCornerShape(23.dp),ambientColor=Color(0x1C7852C4))
            .clip(RoundedCornerShape(23.dp))
            .background(Color.White)
            .border(1.dp,ArenaApproved.stroke,RoundedCornerShape(23.dp))
            .padding(horizontal=16.dp,vertical=13.dp),
        verticalArrangement=Arrangement.spacedBy(11.dp)
    ) {
        Row(verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)){
            Text(icon,fontSize=23.sp,color=ArenaApproved.green)
            Text(heading,color=ArenaApproved.ink,fontSize=18.sp,
                fontWeight=FontWeight.ExtraBold)
        }
        content()
    }
}

@Composable
internal fun ArenaApprovedOption(
    title:String, selected:Boolean, modifier:Modifier=Modifier,
    symbol:String?=null, description:String?=null,
    compact:Boolean=false, onClick:()->Unit
) {
    val shape=RoundedCornerShape(if(compact)14.dp else 17.dp)
    val selectedBg=Brush.horizontalGradient(
        listOf(Color(0xFFF4FFE7),Color(0xFFE9F9D7)))
    val normalBg=Brush.horizontalGradient(
        listOf(Color(0xFFFFFEFF),Color(0xFFF9F5FF)))
    Row(
        modifier.shadow(if(selected)2.dp else 0.dp,shape)
            .clip(shape)
            .background(if(selected)selectedBg else normalBg)
            .border(if(selected)1.8.dp else 1.dp,
                if(selected)Color(0xFF70BB37) else ArenaApproved.stroke,shape)
            .clickable(onClick=onClick)
            .padding(horizontal=if(compact)10.dp else 12.dp,
                vertical=if(compact)10.dp else 14.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(8.dp)
    ) {
        if(symbol!=null) {
            Box(
                Modifier.size(if(compact)29.dp else 38.dp)
                    .background(if(selected)Color(0xFFE8FACB) else Color(0xFFF0E8FF),
                        CircleShape),
                contentAlignment=Alignment.Center
            ){
                Text(symbol,fontSize=if(compact)16.sp else 21.sp,
                    color=if(selected)ArenaApproved.green else ArenaApproved.purple)
            }
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text(title,color=ArenaApproved.ink,fontSize=if(compact)12.sp else 15.sp,
                fontWeight=FontWeight.Bold,maxLines=if(compact)2 else 2,
                overflow=TextOverflow.Ellipsis)
            if(description!=null) Text(description,color=ArenaApproved.sub,
                fontSize=if(compact)10.sp else 11.sp,maxLines=2,
                overflow=TextOverflow.Ellipsis)
        }
        if(selected && !compact) {
            Box(Modifier.size(23.dp).background(Color(0xFF6CBC2C),CircleShape),
                contentAlignment=Alignment.Center) {
                Text("✓",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun ArenaApprovedMascot(
    kind:String,
    selected:Boolean=false,
    modifier:Modifier=Modifier,
    sizeDp:Int=65
) {
    val border=if(selected)ArenaApproved.lilac else Color.Transparent
    Box(modifier,contentAlignment=Alignment.Center) {
        Box(Modifier.size(sizeDp.dp)
            .shadow(if(selected)5.dp else 0.dp,CircleShape)
            .background(if(selected)Color(0xFFF4ECFF) else Color.Transparent,CircleShape)
            .border(if(selected)1.dp else 0.dp,border,CircleShape)
            .padding(if(selected)5.dp else 0.dp),
            contentAlignment=Alignment.Center) {
            ArenaMascotV2(kind,Modifier.fillMaxSize())
        }
    }
}

/** Drawn in Compose, so the approved hero mascot and trophy remain crisp on mobile. */
@Composable
internal fun ArenaApprovedHeroArt(modifier:Modifier=Modifier) {
    Box(modifier,contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w=size.width
            val h=size.height
            drawCircle(Color(0xFFE6D7FF),radius=w*.35f,center=Offset(w*.73f,h*.7f))
            drawCircle(Color(0xFFDAC6FA),radius=w*.22f,center=Offset(w*.3f,h*.9f))
            drawCircle(Color(0xFF99D956),radius=4.dp.toPx(),
                center=Offset(w*.22f,h*.28f))
            drawCircle(Color(0xFFFFCD45),radius=4.dp.toPx(),
                center=Offset(w*.90f,h*.13f))
            drawCircle(Color(0xFFA883EE),radius=2.dp.toPx(),
                center=Offset(w*.86f,h*.85f))
            drawCircle(ArenaApproved.purple,radius=w*.19f,
                center=Offset(w*.68f,h*.72f),style=Stroke(2.dp.toPx()))
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("🏆",fontSize=39.sp,
                modifier=Modifier.offset(x=22.dp,y=(-28).dp))
            ArenaMascotV2("robot",Modifier.size(116.dp).offset(y=17.dp))
        }
    }
}

/** Reusable purple raised button like the approved lobby mockup. */
@Composable
internal fun ArenaApprovedPrimary(
    text:String,enabled:Boolean,onClick:()->Unit,
    green:Boolean=false,modifier:Modifier=Modifier
){
    val shape=RoundedCornerShape(22.dp)
    Box(modifier.fillMaxWidth().height(60.dp)
        .shadow(if(enabled)7.dp else 0.dp,shape,
            ambientColor=if(green)Color(0x55257D44) else Color(0x558550C6))
        .clip(shape)
        .background(if(!enabled)Brush.horizontalGradient(
            listOf(Color(0xFFB9B2C3),Color(0xFF9D92B2)))
            else if(green)ArenaApproved.greenGradient
            else ArenaApproved.purpleGradient)
        .clickable(enabled=enabled,onClick=onClick),
        contentAlignment=Alignment.Center){
        Text("▶  $text",fontSize=18.sp,fontWeight=FontWeight.ExtraBold,
            color=Color.White)
    }
}
