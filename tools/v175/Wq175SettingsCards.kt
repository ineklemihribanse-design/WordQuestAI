package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Collapsible cards keep all existing preferences, diagnostics and voice controls intact. */
@Composable
internal fun Wq175SettingsCard(
    title: String,
    subtitle: String,
    iconName: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    Surface(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(23.dp),
        color=Color(0xFFFFFCF6),
        border=BorderStroke(1.dp,Color(0xFFE6DDD6)),
        shadowElevation=2.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Surface(onClick={expanded=!expanded},
                shape=RoundedCornerShape(17.dp),
                color=if(expanded) Color(0xFFF3EDF9) else Color(0xFFFFFCF6)
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal=5.dp,vertical=8.dp),
                    horizontalArrangement=Arrangement.spacedBy(12.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Wq175GlossIcon(iconName,Modifier.size(47.dp))
                    Column(Modifier.weight(1f),
                        verticalArrangement=Arrangement.spacedBy(2.dp)) {
                        Text(title,color=Color(0xFF263D47),fontSize=17.sp,
                            fontWeight=FontWeight.ExtraBold)
                        Text(subtitle,color=Color(0xFF607279),fontSize=11.sp,
                            lineHeight=15.sp,maxLines=2)
                    }
                    Text(if(expanded)"⌃" else "⌄",
                        color=Color(0xFF73509C),fontSize=26.sp)
                }
            }
            if(expanded) {
                HorizontalDivider(color=Color(0xFFE9E0EC))
                Column(Modifier.fillMaxWidth().padding(
                    horizontal=6.dp,vertical=8.dp),
                    verticalArrangement=Arrangement.spacedBy(10.dp),
                    content=content)
            }
        }
    }
}
