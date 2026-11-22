package com.kelimebulmacasi.a1

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** The user's approved original glossy English Word ↔ English Meaning icon. */
@Composable
internal fun Wq186EnglishMeaningIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.wq186_english_meaning),
        contentDescription = "İngilizce kelime ve İngilizce anlam eşleştirme",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
