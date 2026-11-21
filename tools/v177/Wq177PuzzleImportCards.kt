package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Wq177SourceTile(
    val title: String, val detail: String, val icon: String,
    val surface: Color, val accent: Color, val onClick: () -> Unit
)

/** A genuine card-based import dashboard for the user's actual puzzle editor.
 * The existing scanner, document picker, AI generator and paste handlers are
 * supplied directly as callbacks; no click routes to Word Memory or other pages.
 */
@Composable
internal fun Wq177PuzzleImportCards(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onPdf: () -> Unit,
    onPaste: () -> Unit,
    onAi: () -> Unit
) {
    val options = listOf(
        Wq177SourceTile("Kamerayla çek","Yeni kelime tablosu tara",
            "camera",Color(0xFFFFECDF),Color(0xFFB95B35),onCamera),
        Wq177SourceTile("Galeriden seç","Hazır fotoğrafı işle",
            "scan",Color(0xFFE4F8F4),Color(0xFF187E73),onGallery),
        Wq177SourceTile("PDF'den aktar","Sayfa seç ve kelimeleri incele",
            "pdf",Color(0xFFE6F0FF),Color(0xFF416BC6),onPdf),
        Wq177SourceTile("Kelime listesi","İngilizce–Türkçe satırları yapıştır",
            "notebook",Color(0xFFFFF3DB),Color(0xFF976329),onPaste),
        Wq177SourceTile("Yapay zekâ","Konuya özel yeni kelimeler üret",
            "ai_teacher",Color(0xFFF0E6FF),Color(0xFF7045B7),onAi)
    )
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Kelime kaynağını seç",fontSize=18.sp,
            fontWeight=FontWeight.ExtraBold,color=Color(0xFF204C43))
        Text("Kelimeler burada önizlenir ve doğrudan yeni bulmacana eklenir.",
            fontSize=12.sp,color=Color(0xFF617970))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns=if(maxWidth>=740.dp)3 else 2
            val compact=maxWidth<400.dp || LocalDensity.current.fontScale>1.3f
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                options.chunked(columns).forEach { group ->
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        group.forEach { tile ->
                            Surface(onClick=tile.onClick,
                                modifier=Modifier.weight(1f)
                                    .heightIn(min=if(compact)141.dp else 151.dp),
                                shape=RoundedCornerShape(23.dp),
                                color=tile.surface,
                                shadowElevation=3.dp,
                                border=BorderStroke(1.dp,tile.accent.copy(alpha=.22f))) {
                                Column(Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                    Wq175GlossIcon(tile.icon,Modifier.size(if(compact)45.dp else 55.dp))
                                    Text(tile.title,fontSize=if(compact)13.sp else 15.sp,
                                        fontWeight=FontWeight.ExtraBold,color=tile.accent,
                                        maxLines=2,overflow=TextOverflow.Ellipsis)
                                    Text(tile.detail,fontSize=11.sp,
                                        lineHeight=14.sp,maxLines=3,
                                        color=Color(0xFF5E6F73),
                                        overflow=TextOverflow.Ellipsis)
                                }
                            }
                        }
                        repeat(columns-group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
