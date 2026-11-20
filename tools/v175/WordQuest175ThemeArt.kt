package com.kelimebulmacasi.a1

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
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

/** Native vector gradient + original illustration in the right-hand art column.
 * Text, selection controls, scores and real avatar are composed separately.
 */
@Composable
internal fun Wq175ThemeBanner(slug:String,modifier:Modifier) {
    Box(modifier.clip(RoundedCornerShape(22.dp))) {
        Image(painter=painterResource(Wq175ThemeArt.banner(slug)),
            contentDescription=null,modifier=Modifier.matchParentSize(),
            contentScale=ContentScale.FillBounds)
        val alpha=when(slug) {
            "yeni_oyun"->.28f
            "eslestirmelerim"->.42f
            else->.96f
        }
        Wq175ThemeIllustration(slug,Modifier.align(Alignment.CenterEnd)
            .fillMaxHeight().width(155.dp).padding(4.dp).alpha(alpha))
    }
}
