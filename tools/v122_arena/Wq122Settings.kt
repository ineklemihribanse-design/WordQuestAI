package com.kelimebulmacasi.a1

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Wq122Ink=Color(0xFFFFF4FF)
internal val Wq122Gold=Color(0xFFFFD875)
internal val Wq122Border=Color(0xFF9A70D7)
internal val Wq122Scheme=darkColorScheme(
    primary=Wq122Gold,onPrimary=Color(0xFF28103E),secondary=Color(0xFFDAB6FF),
    background=Color(0xFF150B3E),onBackground=Wq122Ink,
    surface=Color(0xFF251347),onSurface=Wq122Ink,
    surfaceVariant=Color(0xFF38205E),onSurfaceVariant=Color(0xFFE2CFFF),
    outline=Wq122Border)

@Composable
private fun wq122Painter(name:String):BitmapPainter? {
    val context=LocalContext.current
    return remember(context,name) {
        runCatching { context.assets.open("arena_v122/$name.webp").use {
            BitmapFactory.decodeStream(it)?.asImageBitmap()?.let { bitmap -> BitmapPainter(bitmap) }
        } }.getOrNull()
    }
}

@Composable
internal fun Wq122Art(name:String,modifier:Modifier=Modifier,scale:ContentScale=ContentScale.Fit,alpha:Float=1f) {
    val p=wq122Painter(name)
    if(p!=null)Image(p,null,modifier,contentScale=scale,alpha=alpha)
}

@Composable
internal fun Wq122SettingsBackdrop(modifier:Modifier) {
    val c=LocalConfiguration.current
    val tablet=c.screenWidthDp>c.screenHeightDp && c.screenWidthDp>=600
    Wq122Art(if(tablet)"tablet" else "phone",modifier,ContentScale.Crop)
}

@Composable
internal fun wq122SettingsModifier():Modifier {
    val c=LocalConfiguration.current
    val p=wq122Painter(if(c.screenWidthDp>c.screenHeightDp && c.screenWidthDp>=600)"tablet" else "phone")
    val bg=Modifier.background(Color(0xFF150B3E))
    return if(p!=null)bg.paint(p,sizeToIntrinsics=false,contentScale=ContentScale.Crop) else bg
}

@Composable
internal fun Wq122Choice(label:String,selected:Boolean,modifier:Modifier=Modifier,icon:String?=null,onClick:()->Unit) {
    val shape=RoundedCornerShape(13.dp)
    Box(modifier.heightIn(min=40.dp).clip(shape)
        .background(Brush.verticalGradient(if(selected)listOf(Color(0xFFA22CE9),Color(0xFF5410AB)) else listOf(Color(0xFF392765),Color(0xFF20163F))))
        .border(if(selected)2.dp else 1.dp,if(selected)Wq122Gold else Wq122Border,shape)
        .clickable(onClick=onClick),contentAlignment=Alignment.Center) {
        Wq122Art(if(selected)"selected" else "unselected",Modifier.matchParentSize(),ContentScale.FillBounds,if(selected).55f else .12f)
        Row(Modifier.padding(horizontal=3.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            if(icon!=null)Wq122Art(icon,Modifier.size(20.dp))
            Text(label,color=Color.White,fontWeight=FontWeight.Bold,fontSize=13.sp,textAlign=TextAlign.Center,maxLines=2)
            if(selected)Text("✓",color=Wq122Gold,fontSize=12.sp,fontWeight=FontWeight.Black)
        }
    }
}

@Composable
internal fun Wq122Section(icon:String,title:String,help:String?=null) {
    var info by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        Wq122Art(icon,Modifier.size(27.dp))
        Text(title,Modifier.weight(1f),color=Wq122Ink,fontWeight=FontWeight.Bold,fontSize=14.sp)
        if(help!=null)Box(Modifier.size(40.dp).clickable{info=true},contentAlignment=Alignment.Center) {
            Wq122Art("info",Modifier.size(24.dp))
        }
    }
    if(info && help!=null)AlertDialog(onDismissRequest={info=false},title={Text(title)},text={Text(help)},confirmButton={TextButton(onClick={info=false}){Text("Kapat")}})
}

@Composable
internal fun Wq122Primary(text:String,enabled:Boolean,onClick:()->Unit,green:Boolean=false,modifier:Modifier=Modifier) {
    val shape=RoundedCornerShape(19.dp)
    Box(modifier.fillMaxWidth().heightIn(min=52.dp).clip(shape)
        .background(Brush.verticalGradient(if(enabled)listOf(Color(0xFFFFE9A2),Color(0xFFFFB82C),Color(0xFFE39A18)) else listOf(Color(0xFF51405F),Color(0xFF342842))))
        .border(2.dp,if(enabled)Color(0xFFFFE6A0) else Wq122Border,shape)
        .clickable(enabled=enabled,onClick=onClick),contentAlignment=Alignment.Center) {
        Wq122Art("start_plate",Modifier.matchParentSize(),ContentScale.FillBounds,.14f)
        Row(Modifier.padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            Wq122Art("start",Modifier.size(38.dp))
            Text(text,color=if(enabled)Color(0xFF401E09) else Wq122Ink,fontWeight=FontWeight.Black,fontSize=18.sp)
        }
    }
}
