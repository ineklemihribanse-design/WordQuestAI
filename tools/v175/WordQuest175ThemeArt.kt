package com.kelimebulmacasi.a1

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Native scalable icons and title-free banner art built from the approved 1.0.75 SVG motifs. */
internal object Wq175ThemeArt {
    fun fromTitle(title:String):String? {
        val v=title.lowercase(java.util.Locale.forLanguageTag("tr"))
        return when {
            "eşleştir" in v -> "eslestirmelerim"
            "arena" in v -> "arena"
            "yds" in v || "akademi" in v -> "yds_akademi"
            "dinle" in v -> "dinle_ve_yaz"
            "dilbilgisi" in v || "gramer" in v -> "dilbilgisi"
            "kelime hafıza" in v || "kelime hafızam" in v -> "kelime_hafizam"
            "günlük" in v || "plan" in v -> "gunluk_plan"
            "bulmacalarım" in v -> "bulmacalarim"
            "yeni oyun" in v -> "yeni_oyun"
            else -> null
        }
    }
    /** Distinct native banners for secondary rooms that have no large hero asset. */
    fun secondaryIcon(title:String):String? {
        val s=title.lowercase(java.util.Locale.forLanguageTag("tr"))
        return when {
            "ayar" in s -> "settings"
            "profil" in s || "hesap" in s -> "account"
            "arkadaş" in s -> "friends"
            "yedek" in s -> "backup"
            "istatistik" in s || "ilerleme" in s -> "analytics"
            "öğrenme merkez" in s -> "insights"
            "geçmiş" in s -> "history"
            "sözlük" in s || "defter" in s -> "library"
            "mesaj" in s || "sohbet" in s -> "messages"
            "bildirim" in s || "seri" in s -> "streak"
            else -> null
        }
    }
    fun icon(slug:String):Int=when(slug){
        "ana_sayfa"->R.drawable.wq175_ana_sayfa_icon
        "yeni_oyun"->R.drawable.wq175_yeni_oyun_icon
        "arena"->R.drawable.wq175_arena_icon
        "yds_akademi"->R.drawable.wq175_yds_akademi_icon
        "dinle_ve_yaz"->R.drawable.wq175_dinle_ve_yaz_icon
        "dilbilgisi"->R.drawable.wq175_dilbilgisi_icon
        "kelime_hafizam"->R.drawable.wq175_kelime_hafizam_icon
        "eslestirmelerim"->R.drawable.wq175_eslestirmelerim_icon
        "gunluk_plan"->R.drawable.wq175_gunluk_plan_icon
        "bulmacalarim"->R.drawable.wq175_bulmacalarim_icon
        else->R.drawable.wq175_ana_sayfa_icon
    }
    fun banner(slug:String):Int=when(slug){
        "ana_sayfa"->R.drawable.wq175_theme_ana_sayfa_banner
        "yeni_oyun"->R.drawable.wq175_theme_yeni_oyun_banner
        "arena"->R.drawable.wq175_theme_arena_banner
        "yds_akademi"->R.drawable.wq175_theme_yds_akademi_banner
        "dinle_ve_yaz"->R.drawable.wq175_theme_dinle_ve_yaz_banner
        "dilbilgisi"->R.drawable.wq175_theme_dilbilgisi_banner
        "kelime_hafizam"->R.drawable.wq175_theme_kelime_hafizam_banner
        "eslestirmelerim"->R.drawable.wq175_theme_eslestirmelerim_banner
        "gunluk_plan"->R.drawable.wq175_theme_gunluk_plan_banner
        "bulmacalarim"->R.drawable.wq175_theme_bulmacalarim_banner
        else->R.drawable.wq175_theme_ana_sayfa_banner
    }
    fun originalHero(slug:String):Int=when(slug){
        "ana_sayfa"->R.drawable.wq175_ana_sayfa_hero
        "yeni_oyun"->R.drawable.wq175_yeni_oyun_hero
        "arena"->R.drawable.wq175_arena_hero
        "yds_akademi"->R.drawable.wq175_yds_akademi_hero
        "dinle_ve_yaz"->R.drawable.wq175_dinle_ve_yaz_hero
        "dilbilgisi"->R.drawable.wq175_dilbilgisi_hero
        "kelime_hafizam"->R.drawable.wq175_kelime_hafizam_hero
        "eslestirmelerim"->R.drawable.wq175_eslestirmelerim_hero
        "gunluk_plan"->R.drawable.wq175_gunluk_plan_hero
        "bulmacalarim"->R.drawable.wq175_bulmacalarim_hero
        else->R.drawable.wq175_ana_sayfa_hero
    }

}


/**
 * Build-native, illustrated headers for utility/secondary screens. These reuse
 * original generated icons without attaching unrelated room hero backgrounds.
 */
@Composable
internal fun Wq175SecondaryRoomBanner(title:String,iconName:String) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().heightIn(min=108.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(
                Color(0xFF203A56),Color(0xFF476898),Color(0xFF9278B7))))
    ) {
        val compact=maxWidth<300.dp || LocalDensity.current.fontScale>1.4f
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=13.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f),
                verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("WORDQUEST AI · KEŞFET",
                    fontSize=10.sp,fontWeight=FontWeight.Black,
                    color=Color(0xFFFFE3A5))
                Text(title,color=Color.White,
                    fontSize=if(compact)18.sp else 22.sp,
                    lineHeight=if(compact)22.sp else 26.sp,
                    fontWeight=FontWeight.ExtraBold,maxLines=3,
                    overflow=TextOverflow.Ellipsis)
            }
            Wq175GlossIcon(iconName,
                Modifier.size(if(compact)60.dp else 82.dp))
        }
    }
}

@Composable
internal fun Wq175ThemeIcon(slug:String, modifier:Modifier=Modifier.size(48.dp)) {
    Image(painter=painterResource(Wq175ThemeArt.icon(slug)),
        contentDescription=null,modifier=modifier,contentScale=ContentScale.Fit)
}

/** The actual supplied 500x680 WebP artwork; never cropped as a wide wallpaper. */
@Composable
internal fun Wq175ThemeIllustration(slug:String,modifier:Modifier) {
    Image(painter=painterResource(Wq175ThemeArt.originalHero(slug)),
        contentDescription=null,modifier=modifier,contentScale=ContentScale.Fit)
}

/** Blend real original 1.0.75 illustrations into each themed room's backdrop.
 * The two feathered edges avoid the pasted-on rectangle seen on earlier screens.
 * All buttons, text, scores and avatars remain native, independent Compose layers.
 */
@Composable
internal fun Wq175ThemeBanner(slug:String,modifier:Modifier) {
    Box(modifier.clip(RoundedCornerShape(22.dp))) {
        Image(painter=painterResource(Wq175ThemeArt.banner(slug)),
            contentDescription=null,modifier=Modifier.matchParentSize(),
            contentScale=ContentScale.FillBounds)
        val artAlpha=when(slug) {
            "yeni_oyun"->.82f
            "eslestirmelerim"->.90f
            else->.88f
        }
        Wq175ThemeIllustration(slug,Modifier.align(Alignment.CenterEnd)
            .fillMaxHeight().width(178.dp)
            .graphicsLayer {
                compositingStrategy=CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(brush=Brush.horizontalGradient(
                    0f to Color.Transparent,
                    .34f to Color.White,
                    1f to Color.White
                ),blendMode=BlendMode.DstIn)
                drawRect(brush=Brush.verticalGradient(
                    0f to Color.Transparent,
                    .13f to Color.White,
                    .87f to Color.White,
                    1f to Color.Transparent
                ),blendMode=BlendMode.DstIn)
            }
            .alpha(artAlpha))
    }
}
