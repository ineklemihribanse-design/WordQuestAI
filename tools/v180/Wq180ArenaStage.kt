package com.kelimebulmacasi.a1

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset

/** Warmer plum-lavender Arena palette; better than hard navy with readable cards. */
@Composable
internal fun Wq180ArenaStageBackground(modifier:Modifier=Modifier,dark:Boolean=false){
    Canvas(modifier){
        if(!dark){
            drawRect(Brush.verticalGradient(listOf(
                Color(0xFFF8F4FC),Color(0xFFF2EDFA),Color(0xFFFFFCFD))))
        } else {
            drawRect(Brush.verticalGradient(listOf(
                Color(0xFF352452),Color(0xFF604682),
                Color(0xFF5A497C),Color(0xFF3C315F))))
            val spotA=Offset(size.width*.13f,size.height*.03f)
            val spotB=Offset(size.width*.87f,size.height*.03f)
            val bottom=Offset(size.width*.50f,size.height*.78f)
            val radius=size.width*.68f
            drawCircle(Brush.radialGradient(
                listOf(Color(0x40E7BDFF),Color(0x14C19BFA),Color.Transparent),
                center=spotA,radius=radius),radius=radius,center=spotA)
            drawCircle(Brush.radialGradient(
                listOf(Color(0x38F9CBAA),Color(0x10F6B2C8),Color.Transparent),
                center=spotB,radius=radius),radius=radius,center=spotB)
            drawCircle(Brush.radialGradient(
                listOf(Color(0x1FD0C5FC),Color.Transparent),
                center=bottom,radius=size.width*.84f),
                radius=size.width*.84f,center=bottom)
            // Soft gold stage rim; no geometric web/diagonal lines over text.
            drawLine(Color(0x4BF3D3A5),Offset(0f,size.height*.05f),
                Offset(size.width,size.height*.05f),2f)
            repeat(5){i->
                val y=size.height*(.75f+i*.05f)
                drawLine(Color(0x0DF4D7EF),Offset(0f,y),Offset(size.width,y),1f)
            }
        }
    }
}
