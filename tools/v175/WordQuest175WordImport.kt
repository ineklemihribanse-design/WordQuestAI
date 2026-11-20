package com.kelimebulmacasi.a1

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

/**
 * Entry, AI and existing tested PDF/photo OCR flow all converge on ONE editable
 * preview. Nothing is stored without explicit confirmation.
 */
@Composable
internal fun Wq175WordImportDialog(
    session: AppSession, ai: AiSession, dismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("manual") }
    var english by remember { mutableStateOf("") }
    var turkish by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var aiLevel by remember { mutableStateOf("A1") }
    var aiTopic by remember { mutableStateOf("Günlük Yaşam") }
    var pdfPages by remember { mutableStateOf("") }
    val preview = remember { mutableStateListOf<WordPair>() }
    val selected = remember { mutableStateMapOf<Int, Boolean>() }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var photoFile by remember { mutableStateOf<File?>(null) }

    fun updatePreview(words: List<WordPair>) {
        preview.clear()
        selected.clear()
        preview.addAll(words.take(250))
        preview.indices.forEach { selected[it] = true }
        message = if (preview.isEmpty()) "Geçerli İngilizce-Türkçe kelime çifti bulunamadı."
            else preview.size.toString() + " kelime bulundu. Düzenle, seç ve onayla."
    }

    fun scan(uri: android.net.Uri) {
        if (busy) return
        busy = true
        error = ""
        message = "Kelimeler taranıyor…"
        scope.launch {
            try {
                val words = scanPairs(
                    context, uri, isPdf(context, uri), pdfPages, null
                ) { current, total ->
                    message = "Taranan sayfa: " + current.toString() + "/" + total.toString()
                }
                updatePreview(words)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                error = "Belge taranamadı: " + (e.localizedMessage ?: "Dosya biçimini kontrol et.")
            } finally {
                busy = false
                photoFile?.delete()
                photoFile = null
            }
        }
    }

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scan(uri)
    }
    val camera = rememberLauncherForActivityResult(TakeScanPhoto()) { success ->
        val file = photoFile
        if (success && file != null) scan(scanPhotoUri(context, file))
        else {
            file?.delete()
            photoFile = null
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) dismiss() },
        modifier = Modifier.fillMaxWidth(),
        title = { Text("Kelime Hafızam · Kelime Ekle",
            color = Color(0xFF193A65), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kendi kelimelerini ekle veya bir kaynaktan çıkar. Hiçbir kelime onayın olmadan kaydedilmez.",
                    fontSize = 12.sp, color = Color(0xFF59718D))
                listOf(
                    listOf("manual" to "Elle", "pdf" to "PDF", "photo" to "Görsel"),
                    listOf("camera" to "Kamera", "ai" to "Yapay zekâ")
                ).forEach { options ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        options.forEach { (id, label) ->
                            FilterChip(selected = tab == id, onClick = {
                                tab = id; error = ""; message = ""
                            }, label = { Text(label, fontSize = 11.sp) })
                        }
                    }
                }
                if (tab == "manual") {
                    OutlinedTextField(english, { english = it.take(70) },
                        label = { Text("İngilizce kelime") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(turkish, { turkish = it.take(125) },
                        label = { Text("Türkçe karşılığı") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(example, { example = it.take(160) },
                        label = { Text("Örnek / not (isteğe bağlı)") },
                        modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        val pair = WordPair(english.trim(), turkish.trim())
                        if (!pair.valid) error = "Geçerli İngilizce kelime ve Türkçe anlam gir."
                        else {
                            updatePreview(listOf(pair))
                            error = ""
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                        Text("Kaydetmeden önce kontrol et")
                    }
                } else if (tab == "pdf") {
                    OutlinedTextField(pdfPages, { pdfPages = it.take(60) },
                        label = { Text("Sayfalar (boşsa tümü; örn. 1-3, 5)") },
                        modifier = Modifier.fillMaxWidth())
                    Button(onClick = { documentPicker.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                        Text("PDF dosyası seç")
                    }
                } else if (tab == "photo") {
                    Button(onClick = { documentPicker.launch(arrayOf("image/*")) },
                        modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                        Text("Galeriden görsel seç")
                    }
                } else if (tab == "camera") {
                    Button(onClick = {
                        try {
                            val file = createScanPhoto(context)
                            photoFile = file
                            camera.launch(scanPhotoUri(context, file))
                        } catch (e: Exception) {
                            error = "Kamera açılamadı: " +
                                (e.localizedMessage ?: "İzinleri kontrol et.")
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                        Text("Kamerayla tara")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { level ->
                            // Compact selection even in small dialog.
                            TextButton(onClick = { aiLevel = level },
                                contentPadding = PaddingValues(2.dp)) {
                                Text(level, fontSize = 10.sp, color = if (aiLevel == level)
                                    Color(0xFF5D28B4) else Color(0xFF627289))
                            }
                        }
                    }
                    OutlinedTextField(aiTopic, { aiTopic = it.take(60) },
                        label = { Text("Konu") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        if (busy) return@Button
                        busy = true
                        error = ""
                        scope.launch {
                            try {
                                updatePreview(ai.suggestWords("", aiTopic, aiLevel, 15,
                                    session.searchLearning("", 0, 5000)
                                        .map { it.answer }.toSet()))
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (e: Exception) {
                                error = "Yapay zekâ önerisi alınamadı. Ayarlardan API bağlantısını kontrol et."
                            } finally { busy = false }
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text("15 kelime öner")
                    }
                }
                if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (message.isNotBlank()) Text(message,
                    color = Color(0xFF267E72), fontSize = 12.sp)
                if (error.isNotBlank()) Text(error,
                    color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                if (preview.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Kaydetmeden Önce Düzenle", fontWeight = FontWeight.Bold)
                    preview.forEachIndexed { i, pair ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = selected[i] ?: true,
                                    onCheckedChange = { selected[i] = it })
                                Text("Kelime " + (i + 1).toString(),
                                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (pair.review) Text(" · Kontrol et",
                                    fontSize = 10.sp, color = Color(0xFFB17C2E))
                            }
                            OutlinedTextField(pair.answer,
                                { preview[i] = pair.copy(answer = it.take(70)) },
                                label = { Text("İngilizce") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(pair.clue,
                                { preview[i] = pair.copy(clue = it.take(125)) },
                                label = { Text("Türkçe") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth())
                            HorizontalDivider(color = Color(0xFFE6EBF4))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val chosen = preview.filterIndexed { i, _ -> selected[i] == true }
                val invalid = chosen.any { !it.valid }
                if (invalid || chosen.isEmpty()) {
                    error = "Kaydetmek için geçerli kelimeleri seç ve düzelt."
                } else {
                    var saved = 0
                    var duplicates = 0
                    chosen.forEach { pair ->
                        if (session.addManualLearning(pair.answer, pair.clue,
                                if (tab == "manual") example else "Kullanıcı kelime listesi")) saved++
                        else duplicates++
                    }
                    if (saved > 0) {
                        message = saved.toString() + " kelime eklendi; " +
                            duplicates.toString() + " mevcut kayıt atlandı."
                        preview.clear()
                        selected.clear()
                        english = ""; turkish = ""; example = ""
                    } else error = "Yeni kelime eklenmedi. Aynı kayıtlar zaten mevcut olabilir."
                }
            }, enabled = !busy && preview.isNotEmpty()) { Text("Seçilenleri Ekle") }
        },
        dismissButton = {
            TextButton(onClick = dismiss, enabled = !busy) { Text("Kapat") }
        },
        containerColor = Color(0xFFF9FBFF),
        shape = RoundedCornerShape(27.dp)
    )
}
