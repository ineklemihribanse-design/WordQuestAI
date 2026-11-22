package com.kelimebulmacasi.a1

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** Reuse the approved 33-icon atlas first, new purpose-specific artwork only where missing. */
@Composable
internal fun Wq187SettingsArtwork(title:String, original:String, modifier:Modifier) {
    val approved = when(title) {
        "Yapay zekâ" -> "wq_kisisel_ai_ogretmenim"
        "Yazılı telaffuz (IPA)" -> "wq_dinle_ve_yaz"
        "Oyun sesleri" -> "wq_seviye_rozeti_genel"
        "Ses ve telaffuz" -> "wq_konusma_alistirmasi"
        else -> null
    }
    when {
        title=="Güncellemeler" -> Image(painterResource(R.drawable.wq187_update_icon),
            contentDescription=null,modifier=modifier,contentScale=ContentScale.Fit)
        title=="Görünüm" -> Image(painterResource(R.drawable.wq187_settings_icon),
            contentDescription=null,modifier=modifier,contentScale=ContentScale.Fit)
        approved!=null -> {
            if(!Wq183HighResArtwork(approved,modifier) && !Wq182Artwork(approved,modifier))
                Wq175GlossIcon(original,modifier)
        }
        else -> Wq175GlossIcon(original,modifier)
    }
}

@Composable
internal fun Wq187ArenaSettingIcon(kind:String,modifier:Modifier) {
    val approved = when(kind) {
        "level" -> "wq_seviye_rozeti_genel"
        "topic" -> "wq_kategori_genel"
        "count" -> "wq_yeni_oyun"
        "hint" -> "wq_kesfet_ve_ogren"
        else -> null
    }
    if(kind=="time") {
        Image(painterResource(R.drawable.wq187_update_icon),
            contentDescription=null,modifier=modifier,contentScale=ContentScale.Fit)
    } else if(approved!=null) {
        if(!Wq183HighResArtwork(approved,modifier) && !Wq182Artwork(approved,modifier)) {
            Wq175GlossIcon(if(kind=="count")"puzzle" else "cards",modifier)
        }
    } else {
        Image(painterResource(R.drawable.wq187_settings_icon),
            contentDescription=null,modifier=modifier,contentScale=ContentScale.Fit)
    }
}
