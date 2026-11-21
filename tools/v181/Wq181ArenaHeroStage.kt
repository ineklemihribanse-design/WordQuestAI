package com.kelimebulmacasi.a1

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Reuses the exact original Arena illustration that decorates the hero/header.
 * It is behind the real Compose controls, not a screenshot with fake buttons.
 */
@Composable
internal fun Wq181ArenaHeroStage(modifier:Modifier=Modifier,active:Boolean=false){
    Box(modifier){
        if(active){
            Image(painter=painterResource(R.drawable.wq175_arena_hero),
                contentDescription=null,modifier=Modifier.matchParentSize(),
                contentScale=ContentScale.Crop,alignment=Alignment.TopCenter)
            // Consistent dark glass tint guarantees readable live controls on
            // both the bright spotlights and gold banners in the source artwork.
            Canvas(Modifier.matchParentSize()){
                drawRect(brush=Brush.verticalGradient(
                    colors=listOf(
                        Color(0x8C271649),Color(0x942B154B),
                        Color(0xA52D184D),Color(0xBC1A1337)),
                    startY=0f,endY=size.height
                ))
            }
        } else {
            Wq180ArenaStageBackground(Modifier.matchParentSize(),dark=false)
        }
    }
}
