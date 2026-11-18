package com.kelimebulmacasi.a1

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhotoRegion(
    val rotation: Int,
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
)

private sealed interface PreviewState {
    data object Loading : PreviewState
    data object Unavailable : PreviewState
    data class Ready(val photo: Bitmap, val rotation: Int) : PreviewState
}

@Composable
internal fun PhotoCrop(uri: Uri, close: () -> Unit, scan: (PhotoRegion?) -> Unit) {
    val context = LocalContext.current
    var rotation by rememberSaveable(uri.toString()) { mutableIntStateOf(0) }
    var region by remember(uri) { mutableStateOf<PhotoRegion?>(null) }
    var preview by remember(uri, rotation) { mutableStateOf<PreviewState>(PreviewState.Loading) }
    LaunchedEffect(uri, rotation) {
        preview = try {
            val photo = withContext(Dispatchers.IO) { readPhoto(context, uri, PhotoRegion(rotation)) }
            PreviewState.Ready(photo, rotation)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            PreviewState.Unavailable
        }
    }

    fun rotate() {
        rotation = (rotation + 270) % 360
        region = null
    }

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.widthIn(max = 625.dp).fillMaxWidth(.94f).heightIn(max = 730.dp),
            shape = RoundedCornerShape(22.dp), color = White,
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text("Kelime tablosunu seç", color = Ink, fontFamily = Serif, fontSize = 28.sp)
                BodyText("Fotoğrafı döndürebilir, sürükleyerek tablonun çevresini seçebilirsin.")
                when (val result = preview) {
                    PreviewState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(10.dp), color = Forest)
                    PreviewState.Unavailable -> BodyText("Bu görselin önizlemesi açılamadı.", ErrorInk)
                    is PreviewState.Ready -> {
                        val photo = result.photo.asImageBitmap()
                        val aspect = photo.width.toFloat() / photo.height
                        val maxWidth = (430 * aspect).dp
                        Box(
                            Modifier.widthIn(max = maxWidth).fillMaxWidth().aspectRatio(aspect).align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(9.dp)).background(Board),
                        ) {
                            Image(photo, contentDescription = "Taranacak görsel", Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
                            Canvas(
                                Modifier.matchParentSize().pointerInput(result.rotation, uri) {
                                    var start = Offset.Zero
                                    detectDragGestures(
                                        onDragStart = { start = it },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val end = change.position
                                            fun x(v: Float) = (v / size.width).coerceIn(0f, 1f)
                                            fun y(v: Float) = (v / size.height).coerceIn(0f, 1f)
                                            val left = x(minOf(start.x, end.x))
                                            val top = y(minOf(start.y, end.y))
                                            val right = x(maxOf(start.x, end.x))
                                            val bottom = y(maxOf(start.y, end.y))
                                            if (right - left >= .075f && bottom - top >= .075f) {
                                                region = PhotoRegion(result.rotation, left, top, right, bottom)
                                            }
                                        },
                                    )
                                },
                            ) {
                                region?.let {
                                    val left = it.left * size.width
                                    val top = it.top * size.height
                                    val right = it.right * size.width
                                    val bottom = it.bottom * size.height
                                    val shade = Color(0x650D2E21)
                                    drawRect(shade, Offset.Zero, Size(size.width, top))
                                    drawRect(shade, Offset(0f, bottom), Size(size.width, size.height - bottom))
                                    drawRect(shade, Offset(0f, top), Size(left, bottom - top))
                                    drawRect(shade, Offset(right, top), Size(size.width - right, bottom - top))
                                    drawRect(Lime, Offset(left, top), Size(right - left, bottom - top), style = Stroke(2.dp.toPx()))
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = ::rotate, modifier = Modifier.weight(1f), enabled = preview is PreviewState.Ready) {
                        Text("Döndür", fontFamily = Sans, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { region = null }, modifier = Modifier.weight(1f), enabled = region != null) {
                        Text("Seçimi temizle", fontFamily = Sans)
                    }
                }
                PrimaryAction(
                    if (region != null) "Seçili alanı tara" else "Fotoğrafı tara",
                    { scan(region ?: if (rotation != 0) PhotoRegion(rotation) else null) },
                    preview !is PreviewState.Unavailable,
                )
                TextButton(onClick = close, modifier = Modifier.align(Alignment.End)) { Text("Vazgeç", fontFamily = Sans) }
            }
        }
    }
}
