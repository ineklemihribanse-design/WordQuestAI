package com.kelimebulmacasi.a1

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared, compact explanations for Arena, settings, sound, update and
 * conversation. Keep interactive controls and error messages visible.
 */
@Composable
internal fun Wq187InfoTip(title: String, detail: String, modifier: Modifier = Modifier) {
    var open by remember(title) { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(title,modifier=Modifier.weight(1f),fontSize=13.sp,
            color=Color(0xFF315648),fontWeight=FontWeight.Medium)
        IconButton(onClick={open=true},modifier=Modifier.size(40.dp)) {
            Surface(shape=CircleShape,color=Color(0xFFECE4F9)) {
                Text("ⓘ",modifier=Modifier.padding(horizontal=9.dp,vertical=5.dp),
                    fontSize=19.sp,color=Color(0xFF6841A4))
            }
        }
    }
    if(open) AlertDialog(
        onDismissRequest={open=false},
        title={Text(title)},
        text={Text(detail)},
        confirmButton={TextButton(onClick={open=false}) {Text("Anladım")}}
    )
}
