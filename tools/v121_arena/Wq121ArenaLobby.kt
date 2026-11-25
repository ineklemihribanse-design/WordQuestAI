package com.kelimebulmacasi.a1

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlin.math.max

internal object Wq121ArenaLayout {
    fun wide(w:Float,h:Float,smallest:Int)=w>h && (smallest>=600 || w>=600)
    // Coordinate mapping for a center-cropped, undistorted approved stadium.
    fun project(w:Float,h:Float,wide:Boolean,x:Float,y:Float):Pair<Float,Float> {
        val aspect=if(wide)1.6f else 1080f/2340f
        val aw=max(w,h*aspect)
        val ah=aw/aspect
        return Pair((w-aw)/2f+aw*x,(h-ah)/2f+ah*y)
    }
}

@Composable
private fun Wq121Art(name:String,modifier:Modifier,scale:ContentScale=ContentScale.Fit,desc:String?=null) {
    val context=LocalContext.current
    val image=remember(context,name) {
        runCatching { context.assets.open("arena_v120/$name.webp").use {
            BitmapFactory.decodeStream(it)?.asImageBitmap()
        } }.getOrNull()
    }
    if(image!=null) Image(image,desc,modifier,contentScale=scale)
}

@Composable
private fun Wq121Bars() {
    val context=LocalContext.current
    DisposableEffect(context) {
        var c:android.content.Context=context
        while(c is ContextWrapper && c !is Activity) c=c.baseContext
        val window=(c as? Activity)?.window
        val ctrl=window?.let{WindowCompat.getInsetsController(it,it.decorView)}
        val status=window?.statusBarColor
        val nav=window?.navigationBarColor
        val lightStatus=ctrl?.isAppearanceLightStatusBars
        val lightNav=ctrl?.isAppearanceLightNavigationBars
        window?.statusBarColor=0xFF150B3E.toInt()
        window?.navigationBarColor=0xFF150B3E.toInt()
        ctrl?.isAppearanceLightStatusBars=false
        ctrl?.isAppearanceLightNavigationBars=false
        onDispose {
            if(status!=null)window?.statusBarColor=status
            if(nav!=null)window?.navigationBarColor=nav
            if(lightStatus!=null)ctrl?.isAppearanceLightStatusBars=lightStatus
            if(lightNav!=null)ctrl?.isAppearanceLightNavigationBars=lightNav
        }
    }
}
@Composable
private fun Wq121Mode(kind:String,title:String,subtitle:String,wide:Boolean,selected:Boolean,modifier:Modifier,textSize:Float,onClick:()->Unit) {
    val shape=RoundedCornerShape(16.dp)
    Box(modifier.clip(shape).background(Color(0xFF34145B)).clickable(onClick=onClick)) {
        // Only the empty decorative card plate is stretched; icons and lettering are independent.
        Wq121Art((if(wide)"tablet" else "phone")+"_card_"+kind,Modifier.matchParentSize(),ContentScale.FillBounds)
        Row(Modifier.fillMaxSize().padding(horizontal=9.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
            Wq121Art(kind,Modifier.fillMaxWidth(if(wide).33f else .24f).fillMaxHeight())
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f)) {
                Text(title,color=Color.White,fontWeight=FontWeight.ExtraBold,fontSize=textSize.sp,lineHeight=(textSize+2).sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(subtitle,color=Color(0xFFF6E9FF),fontSize=(textSize*.66f).coerceAtLeast(10f).sp,maxLines=2,overflow=TextOverflow.Ellipsis)
            }
            Text(if(selected)"✓" else "›",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color(0xFFFFD56B))
        }
    }
}
@Composable
private fun Wq121Shortcut(icon:String,label:String,modifier:Modifier,wide:Boolean,onClick:()->Unit) {
    val shape=RoundedCornerShape(12.dp)
    Box(modifier.clip(shape).background(Color(0xD92C145B)).border(1.dp,Color(0xFFD8AD62),shape).clickable(onClick=onClick),contentAlignment=Alignment.Center) {
        if(wide) Row(Modifier.padding(4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            Wq121Art(icon,Modifier.size(28.dp))
            Text(label,color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=2,textAlign=TextAlign.Center)
        } else Column(Modifier.padding(3.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Wq121Art(icon,Modifier.size(25.dp))
            Text(label,color=Color.White,fontSize=9.sp,fontWeight=FontWeight.Bold,maxLines=2,textAlign=TextAlign.Center)
        }
    }
}

@Composable
internal fun Wq121ArenaLobby(selectedAvatar:String,mode:String,solo:Boolean,onMode:(String)->Unit,onBack:()->Unit,onMascot:()->Unit,onFriends:()->Unit,profile:String) {
    Wq121Bars()
    val config=LocalConfiguration.current
    var panel by remember { mutableStateOf("") }
    val context=LocalContext.current
    if(panel.isNotEmpty()) AlertDialog(onDismissRequest={panel=""},title={Text(panel)},
        text={
            Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                when(panel) {
                    "Ayarlar" -> {
                        Wq179ArenaAudioSettings()
                        TextButton(onClick={panel="";onMode(mode)}) {Text("Maç Ayarları")}
                    }
                    "Bilgi" -> Text("Bulmaca Düellosu: rakibinle yarış. Ortak Bulmaca: aynı tahtayı birlikte çöz. 5 Şıklı Düello: hızlı cevap ver. Maç türünü seçerek mevcut ayarları açabilirsin.")
                    "Sıralama","Son Maçlarım" -> Wq121ArenaRecords(context,profile,panel=="Sıralama")
                }
            }
        },confirmButton={TextButton(onClick={panel=""}){Text("Kapat")}})
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF150B3E))) {
        val w=maxWidth
        val h=maxHeight
        val wide=Wq121ArenaLayout.wide(w.value,h.value,config.smallestScreenWidthDp)
        val pos=Wq121ArenaLayout.project(w.value,h.value,wide,if(wide).50f else .73f,if(wide).505f else .385f)
        val feet=pos.second.dp.coerceIn(h*.29f,h*.56f)
        val mascotWidth=w*(if(wide).26f else .43f)
        val mascotHeight=minOf(h*(if(wide).43f else .32f),feet-h*.075f).coerceAtLeast(40.dp)
        val mascotX=(pos.first.dp-mascotWidth/2).coerceIn(0.dp,(w-mascotWidth).coerceAtLeast(0.dp))
        val crestWidth=w*(if(wide).29f else .51f)
        val crestHeight=minOf(h*(if(wide).36f else .27f),feet-h*.09f).coerceAtLeast(36.dp)
        val headerHeight=(h*.075f).coerceIn(40.dp,65.dp)
        val gap=if(wide)8.dp else 5.dp
        val footerHeight=(h*(if(wide).085f else .085f)).coerceIn(44.dp,70.dp)
        val modeTop=if(wide)h*.595f else h*.455f
        val modeHeaderHeight=(h*.045f).coerceIn(22.dp,40.dp)
        val cardArea=(h-footerHeight-gap*3-modeTop-modeHeaderHeight).coerceAtLeast(60.dp)
        val cardHeight=if(wide)cardArea else (cardArea-gap*2)/3
        val textSize=(if(wide)w.value*.018f else w.value*.044f).coerceIn(13f,26f)
        Wq121Art(if(wide)"tablet" else "phone",Modifier.matchParentSize(),ContentScale.Crop)
        // Brand, real back action and existing mascot picker are above the same artwork.
        Row(Modifier.fillMaxWidth().height(headerHeight).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
            Row(Modifier.width(if(wide)124.dp else 66.dp).heightIn(min=40.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xCC2D165C)).clickable(onClick=onBack).padding(5.dp),verticalAlignment=Alignment.CenterVertically) {
                Wq121Art("back",Modifier.size(25.dp))
                Text("Geri",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Color.White)
            }
            Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                Wq121Art("book",Modifier.size(if(wide)42.dp else 25.dp))
                Text("WordQuest AI",color=Color(0xFFFFDF94),fontSize=if(wide)25.sp else 15.sp,fontWeight=FontWeight.Black,maxLines=1)
            }
            Row(Modifier.width(if(wide)148.dp else 94.dp).heightIn(min=40.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xCC2D165C)).clickable(onClick=onMascot).padding(5.dp),verticalAlignment=Alignment.CenterVertically) {
                Wq2DPortrait(selectedAvatar,Modifier.size(27.dp))
                Text("Kişisel maskot",color=Color.White,fontSize=if(wide)12.sp else 10.sp,fontWeight=FontWeight.Bold,maxLines=2)
            }
        }
        Wq121Art("crest",Modifier.offset(x=w*.025f,y=headerHeight).width(crestWidth).height(crestHeight),desc="Arena")
        Text("Rakibine meydan oku!",Modifier.offset(x=w*.025f,y=headerHeight+crestHeight).width(crestWidth),color=Color.White,fontSize=if(wide)17.sp else 11.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,maxLines=2)
        Wq2DPortrait(selectedAvatar,Modifier.offset(x=mascotX,y=feet-mascotHeight).width(mascotWidth).height(mascotHeight),victory=false)
        Column(Modifier.offset(y=modeTop).fillMaxWidth().padding(horizontal=if(wide)14.dp else 8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(gap)) {
            Row(Modifier.height(modeHeaderHeight).clip(RoundedCornerShape(20.dp)).background(Color(0xDA28134F)).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Wq121Art("modes",Modifier.size(24.dp))
                Text("Oyun Modları",color=Color(0xFFFFDF94),fontSize=if(wide)19.sp else 15.sp,fontWeight=FontWeight.Black)
            }
            if(wide) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(gap)) {
                val card=Modifier.weight(1f).height(cardHeight)
                Wq121Mode("duel","Bulmaca Düellosu","İki oyuncu, tek zafer.",true,mode=="duel",card,textSize){onMode("duel")}
                Wq121Mode("team","Ortak Bulmaca",if(solo)"Tek kişilik ortak-tahta alıştırması." else "Aynı tahtayı birlikte çöz.",true,mode=="team",card,textSize){onMode("team")}
                Wq121Mode("quiz","5 Şıklı Düello","Hızlı cevap ver, puanı topla.",true,mode=="quiz",card,textSize){onMode("quiz")}
            } else {
                val card=Modifier.fillMaxWidth().height(cardHeight)
                Wq121Mode("duel","Bulmaca Düellosu","İki oyuncu, tek zafer.",false,mode=="duel",card,textSize){onMode("duel")}
                Wq121Mode("team","Ortak Bulmaca",if(solo)"Tek kişilik ortak-tahta alıştırması." else "Aynı tahtayı birlikte çöz.",false,mode=="team",card,textSize){onMode("team")}
                Wq121Mode("quiz","5 Şıklı Düello","Hızlı cevap ver, puanı topla.",false,mode=="quiz",card,textSize){onMode("quiz")}
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal=8.dp,vertical=gap).height(footerHeight),horizontalArrangement=Arrangement.spacedBy(gap)) {
            val cell=Modifier.weight(1f).fillMaxHeight()
            Wq121Shortcut("ranking","Sıralama",cell,wide){panel="Sıralama"}
            Wq121Shortcut("friends","Arkadaşlarım",cell,wide,onFriends)
            Wq121Shortcut("history","Son Maçlarım",cell,wide){panel="Son Maçlarım"}
            Wq121Shortcut("settings","Ayarlar",cell,wide){panel="Ayarlar"}
            Wq121Shortcut("info","Bilgi",cell,wide){panel="Bilgi"}
        }
    }
}
