package com.kelimebulmacasi.a1

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/** The 33 transparent user-approved illustrations, 6 x 6 atlas, row-major.
 * Only opt-in replacements are used. Existing premium icons retain precedence.
 */
internal object Wq182Icons {
    private val names = listOf(
        "wq_ana", "wq_arena", "wq_bes_sikli_duello",
        "wq_bulmaca_duellosu", "wq_bulmacalarim", "wq_cumle_olustur",
        "wq_dilbilgisi_hata_avcisi", "wq_dinle_ve_yaz", "wq_eslestirmelerim",
        "wq_gunluk", "wq_gunluk_planim", "wq_hazir_paketler",
        "wq_hikayeyi_tamamla", "wq_ilerlemem", "wq_kategori_genel",
        "wq_kelime_hafizam", "wq_kesfet_ve_ogren",
        "wq_kisisel_ai_ogretmenim", "wq_konusma_alistirmasi",
        "wq_konusma_asistani", "wq_ortak_bulmaca", "wq_profil",
        "wq_seviye_a1", "wq_seviye_a2", "wq_seviye_b1",
        "wq_seviye_b2", "wq_seviye_c1", "wq_seviye_c2",
        "wq_seviye_rozeti_genel", "wq_yanlislarimdan_bulmaca",
        "wq_yds_akademi", "wq_yds_hazirlik", "wq_yeni_oyun"
    )
    /** Replace ONLY generic, misplaced or repeated icon references. */
    private val replacements = mapOf(
        "arena_duel" to "wq_bulmaca_duellosu",
        "arena_team" to "wq_ortak_bulmaca",
        "arena_quiz" to "wq_bes_sikli_duello",
        "packages" to "wq_hazir_paketler",
        "assistant" to "wq_konusma_asistani",
        "ai_teacher" to "wq_kisisel_ai_ogretmenim",
        "sentence" to "wq_cumle_olustur",
        "story" to "wq_hikayeyi_tamamla",
        "speak" to "wq_konusma_alistirmasi",
        "mistakes" to "wq_yanlislarimdan_bulmaca",
        "analytics" to "wq_ilerlemem",
        "insights" to "wq_kesfet_ve_ogren",
        "account" to "wq_profil",
        "level_a1" to "wq_seviye_a1",
        "level_a2" to "wq_seviye_a2",
        "level_b1" to "wq_seviye_b1",
        "level_b2" to "wq_seviye_b2",
        "level_c1" to "wq_seviye_c1",
        "level_c2" to "wq_seviye_c2"
    )
    fun preferred(name:String):String?=replacements[name]
    fun catalogSize():Int=names.size
    fun index(name:String):Int=names.indexOf(name)
    fun has(name:String)=name in names
}
@Composable
internal fun Wq182Artwork(name:String, modifier:Modifier=Modifier):Boolean {
    val context=LocalContext.current
    val atlas=remember(context){
        runCatching { context.assets.open("wq182_approved_33_icons_atlas.webp").use {
            BitmapFactory.decodeStream(it)
        }}.getOrNull()
    }
    val index=Wq182Icons.index(name)
    val cropped=remember(atlas,index){
        if(atlas==null || index<0 || atlas.width!=1152 || atlas.height!=1152) null
        else runCatching {
            val dimension=192
            Bitmap.createBitmap(atlas,(index%6)*dimension,(index/6)*dimension,
                dimension,dimension).asImageBitmap()
        }.getOrNull()
    }
    if(cropped != null){
        Image(bitmap=cropped,contentDescription=null,modifier=modifier,
            contentScale=ContentScale.Fit)
    }
    return cropped!=null
}
