package com.kelimebulmacasi.a1

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Wq198Forest = Color(0xFF17483C)
private val Wq198Ink = Color(0xFF183D32)
private val Wq198Lime = Color(0xFFE8F4BB)
private val Wq198CardColor = Color(0xFFF5F0FA)
private val Wq198Edge = Color(0xFFDBD3E7)
internal val LocalWq198Short = staticCompositionLocalOf { false }

/** The generated artwork, loaded from six independently packaged WebP files.
 * All artwork is local and only used in the New Game configuration page.
 */
@Composable
internal fun Wq198Artwork(
    name: String, modifier: Modifier, description: String? = null,
    scale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current.applicationContext
    val bitmap = remember(context, name) {
        runCatching {
            context.assets.open("new_game_198/" + name + ".webp").use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap, contentDescription = description, modifier = modifier,
            contentScale = scale, filterQuality = FilterQuality.High)
    } else {
        Box(modifier)
    }
}

/** Responsive viewport. Header and original Start action are not scrollable.
 * Only form content scrolls on very short screens, large accessibility text,
 * or when custom numeric fields open the keyboard. Never clip an option.
 */
@Composable
internal fun Wq198NewGamePage(
    back: () -> Unit,
    levelText: String,
    available: Int,
    footer: @Composable ColumnScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Wq198CardColor)) {
        val wide = maxWidth >= 600.dp
        val landscape = maxWidth > maxHeight
        val short = maxHeight < 720.dp
        Wq198Artwork(if (landscape) "tablet" else "phone",
            Modifier.matchParentSize(), scale = ContentScale.FillBounds)
        CompositionLocalProvider(LocalWq198Short provides short) {
            Column(
                Modifier.fillMaxSize()
                    .padding(horizontal = if (wide) 14.dp else 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(if (short) 4.dp else 7.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = back,
                        modifier = Modifier.size(46.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    ) { Text("‹", fontSize = 32.sp, fontWeight = FontWeight.Bold) }
                    Column(Modifier.weight(1f)) {
                        Text("Yeni Oyun", color = Color.White, fontFamily = Serif,
                            fontSize = if (wide) 25.sp else 21.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(levelText + " · " + available + " kelime",
                            color = Color(0xFFF4FFF0), fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Wq198Artwork("level", Modifier.size(if (wide) 46.dp else 38.dp))
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (short) 5.dp else 7.dp),
                    content = body,
                )
                Surface(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    color = Wq198CardColor, border = BorderStroke(1.dp, Wq198Edge),
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp), content = footer)
                }
            }
        }
    }
}

@Composable
internal fun Wq198Card(body: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp),
        color = Wq198CardColor, border = BorderStroke(1.dp, Wq198Edge),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp), content = body,
        )
    }
}

@Composable
internal fun Wq198SectionHeading(name: String, title: String, subtitle: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Wq198Artwork(name, Modifier.size(if (LocalWq198Short.current) 34.dp else 41.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Wq198Ink, fontFamily = Serif,
                fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank())
                Text(subtitle, color = Wq198Forest, fontSize = 10.sp,
                    lineHeight = 12.sp, maxLines = 2)
        }
    }
}

@Composable
internal fun Wq198FilterTabs(
    isTopic: Boolean, enabled: Boolean, choose: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf(Triple("topic", "Konu Seç", "▤ Konu Seç"),
            Triple("kind", "Kelime Türü Seç", "◇ Kelime Türü Seç")).forEach { (icon, name, key) ->
            val active = (icon == "topic") == isTopic
            OutlinedButton(
                onClick = { choose(key) }, enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                shape = RoundedCornerShape(13.dp),
                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (active) Wq198Lime else Color(0xFFFFFEFA),
                    contentColor = Wq198Forest),
                border = BorderStroke(1.dp, if (active) Color(0xFFC2D88D) else Wq198Edge),
            ) {
                Wq198Artwork(icon, Modifier.size(30.dp))
                Spacer(Modifier.width(4.dp))
                Text((if (active) "✓ " else "") + name,
                    fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2, lineHeight = 13.sp)
            }
        }
    }
}
