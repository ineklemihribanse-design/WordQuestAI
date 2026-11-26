package com.kelimebulmacasi.a1

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun Wq120Art(name:String, modifier:Modifier, scale:ContentScale=ContentScale.Fit, description:String?=null) {
    val context=LocalContext.current
    val bitmap=remember(context,name) {
        runCatching {
            context.assets.open("arena_v120/$name.webp").use {
                val options=BitmapFactory.Options().apply {
                    inSampleSize=if(name=="phone" || name=="tablet" || name=="crest" || name.contains("_card_")) 1 else 2
                }
                BitmapFactory.decodeStream(it,null,options)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if(bitmap!=null) Image(bitmap,description,modifier,contentScale=scale)
}

@Composable
private fun Wq120Mode(kind:String,title:String,subtitle:String,wide:Boolean,selected:Boolean,modifier:Modifier,onClick:()->Unit) {
    Box(modifier.clip(RoundedCornerShape(17.dp)).background(Color(0xFF34145B)).clickable(onClick=onClick)) {
        Wq120Art((if(wide)"tablet" else "phone")+"_card_"+kind,Modifier.matchParentSize(),ContentScale.FillBounds)
        Row(Modifier.fillMaxSize().padding(horizontal=8.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically) {
            Wq120Art(kind,Modifier.fillMaxWidth(if(wide).38f else .25f).fillMaxHeight())
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.Center) {
                Text(title,color=Color.White,fontWeight=FontWeight.ExtraBold,fontSize=if(wide)16.sp else 16.sp,lineHeight=18.sp)
                Text(subtitle,color=Color(0xFFF6E9FF),fontSize=if(wide)11.sp else 10.sp,lineHeight=12.sp)
            }
            Text(if(selected)"✓" else "›",fontSize=19.sp,fontWeight=FontWeight.Bold,color=Color(0xFFFFD56B))
        }
    }
}
/** Visual-only lobby: original profile renderer, mode callbacks and settings routes retained. */
@Composable
internal fun Wq120ArenaLobby(selectedAvatar:String,mode:String,solo:Boolean,onMode:(String)->Unit) {
    val config=LocalConfiguration.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide=config.smallestScreenWidthDp>=600 && config.screenWidthDp>config.screenHeightDp && maxWidth>=560.dp
        val w=maxWidth
        val h=if(wide)w*.625f else w*(19.5f/9f)
        val heroBottom=if(wide).505f else .385f
        val mascotX=if(wide).50f else .73f
        val mascotWidth=w*(if(wide).26f else .43f)
        val mascotHeight=h*(if(wide).46f else .335f)
        Box(Modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(18.dp)).background(Color(0xFF180A42))) {
            Wq120Art(if(wide)"tablet" else "phone",Modifier.matchParentSize(),ContentScale.FillBounds)
            val crestWidth=w*(if(wide).29f else .52f)
            val crestHeight=h*(if(wide).42f else .30f)
            Wq120Art("crest",Modifier.offset(x=w*(if(wide).035f else .015f),y=h*.045f).width(crestWidth).height(crestHeight),description="Arena")
            Text("Rakibine meydan oku!",Modifier.offset(x=w*(if(wide).035f else .015f),y=h*(if(wide).46f else .348f)).width(crestWidth),color=Color.White,fontSize=if(wide)15.sp else 12.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
            Wq2DPortrait(selectedAvatar,Modifier.offset(x=w*mascotX-mascotWidth/2,y=h*heroBottom-mascotHeight).width(mascotWidth).height(mascotHeight),victory=false)
            Column(Modifier.offset(y=h*(if(wide).59f else .46f)).fillMaxWidth().padding(horizontal=if(wide)14.dp else 9.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    Wq120Art("modes",Modifier.size(27.dp))
                    Text("Oyun Modları",color=Color(0xFFFFDF94),fontSize=17.sp,fontWeight=FontWeight.Black)
                }
                if(wide) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    val card=Modifier.weight(1f).height(h*.22f)
                    Wq120Mode("duel","Bulmaca Düellosu","İki oyuncu, tek zafer.",true,mode=="duel",card){onMode("duel")}
                    Wq120Mode("team","Ortak Bulmaca",if(solo)"Tek kişilik ortak-tahta alıştırması." else "Aynı tahtayı birlikte çöz.",true,mode=="team",card){onMode("team")}
                    Wq120Mode("quiz","5 Şıklı Düello","Hızlı cevap ver, puanı topla.",true,mode=="quiz",card){onMode("quiz")}
                } else {
                    val card=Modifier.fillMaxWidth().height(h*.102f)
                    Wq120Mode("duel","Bulmaca Düellosu","İki oyuncu, tek zafer.",false,mode=="duel",card){onMode("duel")}
                    Wq120Mode("team","Ortak Bulmaca",if(solo)"Tek kişilik ortak-tahta alıştırması." else "Aynı tahtayı birlikte çöz.",false,mode=="team",card){onMode("team")}
                    Wq120Mode("quiz","5 Şıklı Düello","Hızlı cevap ver, puanı topla.",false,mode=="quiz",card){onMode("quiz")}
                }
            }
        }
    }
}
