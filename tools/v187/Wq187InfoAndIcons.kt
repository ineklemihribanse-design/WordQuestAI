package com.kelimebulmacasi.a1

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Help is available on demand in an accessible, labeled info action.
 * Settings controls stay on the page; only lengthy explanations move here.
 */
@Composable
internal fun Wq187InfoButton(
    title: String, details: String, modifier: Modifier = Modifier
) {
    var open by remember(title) { mutableStateOf(false) }
    TextButton(onClick={open=true},modifier=modifier,
        contentPadding=PaddingValues(horizontal=8.dp,vertical=1.dp)) {
        Text("ⓘ $title",fontSize=12.sp,fontWeight=FontWeight.SemiBold,
            color=Color(0xFF4E426D))
    }
    if(open) AlertDialog(
        onDismissRequest={open=false},
        title={Text(title)},
        text={Text(details)},
        confirmButton={TextButton(onClick={open=false}) {Text("Anladım")}}
    )
}

/** Original vector-style glossy purple/gold game-settings emblem. */
@Composable
internal fun Wq187ArenaSettingsIcon(modifier: Modifier=Modifier.size(40.dp)) {
    Canvas(modifier) {
        val c=Offset(size.width*.45f,size.height*.44f)
        val r=size.minDimension*.31f
        for(i in 0..7) {
            val a=i*2*Math.PI/8
            val x1=c.x+cos(a).toFloat()*r*.85f
            val y1=c.y+sin(a).toFloat()*r*.85f
            val x2=c.x+cos(a).toFloat()*r*1.18f
            val y2=c.y+sin(a).toFloat()*r*1.18f
            drawLine(Color(0xFFFFD570),Offset(x1,y1),Offset(x2,y2),strokeWidth=size.minDimension*.13f)
            drawLine(Color(0xFF8531D3),Offset(x1,y1),Offset(x2,y2),strokeWidth=size.minDimension*.08f)
        }
        drawCircle(Brush.radialGradient(listOf(Color(0xFFE3A6FF),
            Color(0xFF8E3FDD),Color(0xFF5316A4)),center=c,radius=r*1.2f),radius=r,center=c)
        drawCircle(Color(0xFFFFD56A),radius=r*.61f,center=c)
        drawCircle(Color(0xFF5617A5),radius=r*.46f,center=c)
        val w=size.width*.53f
        val h=size.height*.28f
        val x=size.width*.40f
        val y=size.height*.69f
        drawRoundRect(Color(0xFFFFDA72),Offset(x-1.5f,y-1.5f),Size(w+3,h+3),CornerRadius(h*.44f))
        drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF9B4CED),Color(0xFF5B20B7))),
            Offset(x,y),Size(w,h),CornerRadius(h*.42f))
        drawLine(Color(0xFFFFDF82),Offset(x+w*.22f,y+h*.28f),
            Offset(x+w*.22f,y+h*.72f),size.width*.043f)
        drawLine(Color(0xFFFFDF82),Offset(x+w*.14f,y+h*.50f),
            Offset(x+w*.30f,y+h*.50f),size.width*.043f)
        drawCircle(Color(0xFF25B6F0),radius=size.minDimension*.047f,
            center=Offset(x+w*.76f,y+h*.40f))
        drawCircle(Color(0xFFFFCF5A),radius=size.minDimension*.047f,
            center=Offset(x+w*.84f,y+h*.65f))
    }
}

/** Prefer the already-approved icon pack; no generic repeated star fallback. */
@Composable
internal fun Wq187SettingsCategoryIcon(
    title:String,originalName:String,modifier:Modifier=Modifier.size(47.dp)
) {
    val approved=when {
        title.contains("Yapay zekâ",ignoreCase=true)->"wq_kisisel_ai_ogretmenim"
        title.contains("telaffuz",ignoreCase=true)->"wq_dinle_ve_yaz"
        title.contains("ses",ignoreCase=true)->"wq_konusma_alistirmasi"
        title.contains("Görünüm",ignoreCase=true)->"wq_kategori_genel"
        title.contains("Güncelleme",ignoreCase=true)->"wq_gunluk_planim"
        else->null
    }
    if(approved!=null) {
        if(!Wq183HighResArtwork(approved,modifier)) {
            if(!Wq182Artwork(approved,modifier)) {
                if(originalName=="settings") Wq187ArenaSettingsIcon(modifier)
                else Wq175GlossIcon(originalName,modifier)
            }
        }
    } else if(originalName=="settings") {
        Wq187ArenaSettingsIcon(modifier)
    } else {
        Wq175GlossIcon(originalName,modifier)
    }
}
