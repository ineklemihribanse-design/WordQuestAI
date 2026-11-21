package com.kelimebulmacasi.a1

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/** Reuses the premium Arena hero art, with a velvet scrim to keep live controls readable. */
@Composable
internal fun Wq180ArenaStageBackground(modifier:Modifier=Modifier,dark:Boolean=false){
    val context=LocalContext.current
    val bitmap=remember(context){
        runCatching {
            context.assets.open("wq180_arena_background.webp").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
    Box(modifier) {
        if(bitmap!=null) {
            Image(bitmap=bitmap,contentDescription=null,modifier=Modifier.fillMaxSize(),
                contentScale=ContentScale.Crop)
        }
        Canvas(Modifier.fillMaxSize()) {
            if(dark){
                drawRect(Brush.verticalGradient(listOf(
                    Color(0xC8190D34),Color(0xB01D0B3B),Color(0xD91A0D32))))
            } else {
                drawRect(Brush.verticalGradient(listOf(
                    Color(0xC5F9F2FF),Color(0xD6F5EDFF),Color(0xEFFDF8FF))))
            }
        }
    }
}
