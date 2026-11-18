package com.kelimebulmacasi.a1

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val draftSaver = Saver<List<WordPair>, ArrayList<String>>(
    save = { rows ->
        ArrayList(rows.flatMap { listOf(it.answer, it.clue, if (it.review) "1" else "") })
    },
    restore = { values -> values.chunked(3).map { WordPair(it[0], it[1], it[2] == "1") } },
)

@Composable
internal fun CustomEditor(
    destination: Destination?, existing: Game?, back: () -> Unit,
    hasAiKey: Boolean, clearAiKey: () -> Unit, ai: suspend (String, String, String, Int, Set<String>) -> List<WordPair>,
    provider: AiProvider = AiProvider.Gemini,
    save: (List<Game>, Long?) -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val list = rememberLazyListState()
    val formItems = 3
    val original = remember(existing?.id) { existing }
    val editingVersion = rememberSaveable { original?.modifiedAt ?: -1L }.takeIf { it >= 0 }
    var title by rememberSaveable { mutableStateOf(original?.title.orEmpty()) }
    var rows by rememberSaveable(stateSaver = draftSaver) {
        mutableStateOf(original?.puzzle?.words?.map { WordPair(it.entry.answer, it.entry.clue) }.orEmpty())
    }
    var size by rememberSaveable { mutableIntStateOf(20) }
    var customSizes by rememberSaveable { mutableStateOf(false) }
    var sizesText by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var notice by rememberSaveable { mutableStateOf("") }
    var pdf by rememberSaveable { mutableStateOf<String?>(null) }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var pageSelection by rememberSaveable { mutableStateOf("") }
    var pasteOpen by rememberSaveable { mutableStateOf(false) }
    var aiOpen by rememberSaveable { mutableStateOf(false) }
    var pasteText by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf("") }
    var partial by remember { mutableStateOf<PuzzleBatch?>(null) }
    var confirmRebuild by rememberSaveable { mutableStateOf(false) }
    var scrollToAddedRow by remember { mutableStateOf(false) }
    val nonEmpty = rows.filter { it.answer.isNotBlank() || it.clue.isNotBlank() }
    val flagged = nonEmpty.count { it.review || !it.valid }
    val good = nonEmpty.filter(WordPair::valid).distinctBy { englishLetters(it.answer) }
    val minPairs = if (original == null) answerCounts.first else minOf(answerCounts.first, original.wordCount)
    val sizes = if (customSizes) manualSplit(sizesText, good.size) else automaticSplit(good.size, size)
    LaunchedEffect(rows.size, scrollToAddedRow) {
        if (scrollToAddedRow) {
            list.animateScrollToItem(formItems + (if (notice.isNotBlank()) 1 else 0) + (if (flagged > 0) 1 else 0) + rows.lastIndex)
            scrollToAddedRow = false
        }
    }

    fun addImported(imported: List<WordPair>, label: String, fromAi: Boolean = false) {
        val combined = rows.filter { it.answer.isNotBlank() || it.clue.isNotBlank() }.toMutableList()
        var added = 0
        for (pair in imported) {
            val index = combined.indexOfFirst { englishLetters(it.answer) == englishLetters(pair.answer) }
            if (index < 0) {
                combined.add(pair)
                added++
            } else if (!combined[index].valid && pair.valid) {
                combined[index] = pair
                added++
            }
        }
        rows = combined
        if (title.isBlank()) title = label.take(60)
        notice = when {
            added == 0 -> "Bu kelimeler listende zaten var."
            fromAi -> "Yapay zekâ $added kelime ekledi. Kaydetmeden önce anlamları gözden geçir."
            else -> "$added satır eklendi. Kaydetmeden önce kelimeleri ve anlamları gözden geçir."
        }
    }

    fun capturedFile() = cameraFileName?.let { File(context.cacheDir, "scan_photos/$it") }

    fun clearCapture() {
        capturedFile()?.delete()
        cameraFileName = null
    }

    fun scan(uri: Uri, isDocument: Boolean, photoRegion: PhotoRegion? = null, fromCamera: Boolean = false) {
        val selectedPages = pageSelection
        focus.clearFocus()
        error = ""
        notice = ""
        busy = if (isDocument) "PDF açılıyor…" else "Fotoğraf okunuyor…"
        scope.launch {
            try {
                val found = scanPairs(context, uri, isDocument, selectedPages, photoRegion) { index, count ->
                    withContext(Dispatchers.Main) { busy = "Taranıyor · $index/$count" }
                }
                if (found.isEmpty()) {
                    error = "İngilizce–Türkçe kelime çifti bulunamadı. Tabloyu daha yakından ve düz çekerek yeniden seçebilirsin."
                } else {
                    val name = withContext(Dispatchers.IO) { runCatching { documentName(context, uri) }.getOrDefault("") }
                    val label = name.substringBeforeLast('.').ifBlank { if (isDocument) "PDF bulmacası" else "Fotoğraf bulmacası" }
                    addImported(found, label)
                    list.scrollToItem(0)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: IllegalArgumentException) {
                error = exception.message?.takeIf { it.startsWith("Bu PDF") || it.startsWith("Bu görsel") }
                    ?: "Dosya okunamadı. Görseli veya PDF’yi yeniden seç."
            } catch (_: SecurityException) {
                error = "Dosyaya erişilemedi. PDF şifreliyse açık bir kopyasını seç."
            } catch (_: Exception) {
                error = "Dosya taranamadı. Daha net bir görsel veya açılabilen bir PDF ile tekrar dene."
            } finally {
                if (fromCamera) clearCapture()
                busy = ""
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && busy.isEmpty()) {
            busy = "Dosya açılıyor…"
            scope.launch {
                val document = withContext(Dispatchers.IO) { runCatching { isPdf(context, uri) }.getOrDefault(false) }
                if (document) {
                    pdf = uri.toString()
                    pageSelection = ""
                } else photo = uri.toString()
                busy = ""
            }
        }
    }

    val camera = rememberLauncherForActivityResult(remember { TakeScanPhoto() }) { taken ->
        val file = capturedFile()
        if (taken && file != null && file.length() > 0) {
            photo = scanPhotoUri(context, file).toString()
        } else {
            clearCapture()
            if (taken) error = "Çekilen fotoğraf açılamadı. Yeniden çekebilirsin."
        }
    }

    fun takePhoto() {
        focus.clearFocus()
        error = ""
        try {
            val file = createScanPhoto(context)
            cameraFileName = file.name
            camera.launch(scanPhotoUri(context, file))
        } catch (_: ActivityNotFoundException) {
            clearCapture()
            error = "Bu cihazda kamera uygulaması açılamadı. Galeriden bir fotoğraf seçebilirsin."
        } catch (_: Exception) {
            clearCapture()
            error = "Kamera açılamadı. Yeniden deneyebilirsin."
        }
    }

    BackHandler(aiOpen) { aiOpen = false }
    if (aiOpen) {
        val initialTopic = destination?.category ?: title
        val initialLevel = destination?.let { if (it.mode == GameMode.Yds) "YDS · ${it.level}" else it.level } ?: "A2"
        AiPuzzleScreen(initialTopic, initialLevel, size, good.map { englishLetters(it.answer) }.toSet(),
            hasAiKey, clearAiKey, { aiOpen = false }, ai, provider) { topic, found ->
            addImported(found, topic, fromAi = true)
            aiOpen = false
        }
        return
    }

    fun saveResult(games: List<Game>) {
        if (!save(games, editingVersion)) error = "Bulmaca başka bir yerde güncellendi. Son hâlini açıp yeniden düzenle."
    }

    fun generate(rebuildConfirmed: Boolean = false) {
        focus.clearFocus()
        error = ""
        if (nonEmpty.any { !it.valid }) {
            error = "İşaretli satırların cevabını veya anlamını düzelt ya da satırı kaldır."
            return
        }
        if (good.size != nonEmpty.size) {
            error = "Aynı İngilizce cevap birden fazla satırda var. Tekrar eden satırları kaldır."
            return
        }
        if (original == null && sizes == null) {
            error = "Her tahta için 8–20 arasında bir sayı gir. Sayıların toplamı ${good.size} olmalı."
            return
        }
        if (original != null && good.size > 20) {
            error = "Bir bulmacada en fazla 20 cevap olabilir. Fazla satırları kaldır."
            return
        }
        if (original != null && !rebuildConfirmed && !original.hasSameAnswers(good)) {
            confirmRebuild = true
            return
        }
        val name = title.trim()
        busy = "Kelimeler tahtaya yerleştiriliyor…"
        scope.launch {
            try {
                val noLayout = "Bu kelimeler aynı tahtada kesişmedi. Ortak harfi olan birkaç kelime ekleyip yeniden dene."
                if (original == null) {
                    val batch = withContext(Dispatchers.Default) { pairBatches(good, name, destination, checkNotNull(sizes)) }
                    when {
                        batch.games.isEmpty() -> error = noLayout
                        batch.remaining.isNotEmpty() -> partial = batch
                        else -> saveResult(batch.games)
                    }
                } else {
                    val updated = withContext(Dispatchers.Default) { reviseCustomGame(original, good, name) }
                    if (updated == null) error = noLayout else saveResult(listOf(updated))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Bulmaca oluşturulamadı. Kelime listesini kontrol edip yeniden dene."
            } finally {
                busy = ""
            }
        }
    }

    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 820.dp).fillMaxWidth().padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = back) { Text("‹  Geri", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold) }
                Text(if (good.isEmpty()) "" else good.size.toString() + " geçerli çift", fontFamily = Sans, color = Forest, fontSize = 13.sp)
            }
            LazyColumn(
                Modifier.weight(1f), state = list,
                contentPadding = PaddingValues(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(if (original == null) "Bulmaca ekle" else "Bulmacayı düzenle", fontFamily = Serif, color = Ink, fontSize = 38.sp, lineHeight = 42.sp)
                    destination?.let {
                        Text(it.path, fontFamily = Sans, fontSize = 15.sp, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                item {
                    Column(
                        Modifier.fillMaxWidth().background(LightLime, RoundedCornerShape(16.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        PrimaryAction("Kamerayla çek", ::takePhoto, busy.isEmpty() && cameraFileName == null)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f), enabled = busy.isEmpty(),
                            ) { Text("Galeriden seç", fontFamily = Sans, fontWeight = FontWeight.SemiBold) }
                            OutlinedButton(
                                onClick = { picker.launch(arrayOf("application/pdf")) }, modifier = Modifier.weight(1f), enabled = busy.isEmpty(),
                            ) { Text("PDF seç", fontFamily = Sans, fontWeight = FontWeight.SemiBold) }
                        }
                        OutlinedButton(onClick = { pasteOpen = true }, modifier = Modifier.fillMaxWidth(), enabled = busy.isEmpty()) {
                            Text("Kelime listesi yapıştır", fontFamily = Sans, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(onClick = { aiOpen = true }, modifier = Modifier.fillMaxWidth(), enabled = busy.isEmpty()) {
                            Text("Yapay zekâyla kelime üret", fontFamily = Sans, fontWeight = FontWeight.Bold)
                        }
                        BodyText("İngilizce kelime ve Türkçe anlamı aynı satırda olan tabloları seç.")
                    }
                }
                item {
                    OutlinedTextField(
                        title, { title = it.take(60) }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Bulmacanın adı", fontFamily = Sans) }, singleLine = true, enabled = busy.isEmpty(),
                    )
                }
                if (notice.isNotBlank()) item { BodyText(notice) }
                if (flagged > 0) item { BodyText("$flagged satırı kontrol et", ErrorInk) }
                if (rows.isEmpty()) item { BodyText("Elle eklemek için satır aç. Bir bulmaca oluşturmak için en az $minPairs çift gerekir.") }
                itemsIndexed(rows) { index, pair ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                pair.answer, { value ->
                                    rows = rows.toMutableList().apply { set(index, pair.copy(answer = value.take(42), review = false)) }
                                },
                                modifier = Modifier.weight(.84f),
                                label = { Text((index + 1).toString() + ". İngilizce", fontFamily = Sans, fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                                isError = pair.answer.isNotBlank() && !isEnglishAnswer(pair.answer),
                                singleLine = true, enabled = busy.isEmpty(),
                            )
                            OutlinedTextField(
                                pair.clue, { value ->
                                    rows = rows.toMutableList().apply { set(index, pair.copy(clue = value.take(125), review = false)) }
                                },
                                modifier = Modifier.weight(1.16f),
                                label = { Text("Türkçe anlam", fontFamily = Sans, fontSize = 12.sp) },
                                singleLine = true, enabled = busy.isEmpty(),
                            )
                            TextButton(
                                onClick = { rows = rows.filterIndexed { i, _ -> i != index } },
                                modifier = Modifier.widthIn(max = 57.dp), enabled = busy.isEmpty(),
                                contentPadding = PaddingValues(0.dp),
                            ) { Text("Sil", fontFamily = Sans, color = ErrorInk) }
                        }
                        if (pair.review) Text("Bu satırı gözden geçir", fontFamily = Sans, color = ErrorInk, fontSize = 12.sp)
                    }
                }
                item(key = "add-row") {
                    OutlinedButton(onClick = {
                        rows = rows + WordPair("", "")
                        scrollToAddedRow = true
                    }, enabled = busy.isEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text("+ Satır ekle", fontFamily = Sans, fontWeight = FontWeight.Bold)
                    }
                }
                if (original == null) item {
                    AnswerCountPicker("Bir bulmacadaki cevap sayısı", size, { size = it }, busy.isEmpty() && !customSizes)
                    if (good.size >= 2 * answerCounts.first || customSizes) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !customSizes, onClick = { customSizes = false }, label = { Text("Otomatik") }, enabled = busy.isEmpty())
                            FilterChip(selected = customSizes, onClick = {
                                if (!customSizes) sizesText = automaticSplit(good.size, size).joinToString(" + ")
                                customSizes = true
                            }, label = { Text("Elle böl") }, enabled = busy.isEmpty())
                        }
                    }
                    if (customSizes) OutlinedTextField(
                        sizesText, { sizesText = it.take(4096) }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Her tahtadaki cevap sayısı") }, placeholder = { Text("10 + 12 + 8") },
                        supportingText = { Text("Her sayı 8–20 arasında olmalı. Toplam ${good.size}.") },
                        isError = sizes == null, singleLine = true, enabled = busy.isEmpty(),
                    )
                    if (sizes != null && good.size >= minPairs) {
                        BodyText("${sizes.size} bulmaca · ${sizes.take(10).joinToString(" + ")}${if (sizes.size > 10) " + …" else ""} cevap", Forest)
                    }
                }
            }
            if (error.isNotEmpty()) BodyText(error, ErrorInk)
            if (busy.isNotEmpty()) {
                Row(Modifier.padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Forest, strokeWidth = 2.dp)
                    BodyText(busy)
                }
            }
            PrimaryAction(
                if (original == null) "Bulmacayı oluştur" else "Değişiklikleri kaydet",
                { generate() }, title.isNotBlank() && good.size >= minPairs && busy.isEmpty() && (original != null || sizes != null),
            )
        }
    }

    pdf?.let { value ->
        AlertDialog(
            onDismissRequest = { pdf = null },
            title = { Text("PDF sayfaları", fontFamily = Serif, color = Ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BodyText("Kelime tablosunun bulunduğu sayfaları seç. Alan boş kalırsa PDF’nin tamamı taranır.")
                    OutlinedTextField(pageSelection, { pageSelection = it.take(80) }, label = { Text("Örnek 2–4, 7") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { pdf = null; scan(Uri.parse(value), true) }) { Text("Tara", fontFamily = Sans) } },
            dismissButton = { TextButton(onClick = { pdf = null }) { Text("Vazgeç", fontFamily = Sans) } },
            containerColor = White,
        )
    }
    photo?.let { value ->
        PhotoCrop(Uri.parse(value), { photo = null; clearCapture() }) { region ->
            photo = null
            scan(Uri.parse(value), false, region, fromCamera = cameraFileName != null)
        }
    }
    if (confirmRebuild) {
        AlertDialog(
            onDismissRequest = { confirmRebuild = false },
            title = { Text("Tahta yeniden oluşturulsun mu?", fontFamily = Serif, color = Ink) },
            text = { BodyText("İngilizce cevaplar değişti. Yeni tahta boş açılacak. Eski puanların toplam puanında kalacak.") },
            confirmButton = {
                TextButton(onClick = { confirmRebuild = false; generate(rebuildConfirmed = true) }) { Text("Kaydet", fontFamily = Sans) }
            },
            dismissButton = { TextButton(onClick = { confirmRebuild = false }) { Text("Düzenlemeye dön", fontFamily = Sans) } },
            containerColor = White,
        )
    }
    if (pasteOpen) {
        AlertDialog(
            onDismissRequest = { pasteOpen = false },
            title = { Text("Kelime listesi yapıştır", fontFamily = Serif, color = Ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BodyText("Her satıra bir çift yaz. Örnek\npredict: öngörmek\nglobal warming: küresel ısınma")
                    OutlinedTextField(pasteText, { pasteText = it }, label = { Text("Kelime listesi") }, minLines = 4, maxLines = 8)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pairs = pastedPairs(pasteText)
                    if (pairs.isEmpty()) error = "Yapıştırılan metinde kelime çifti bulunamadı."
                    else { error = ""; addImported(pairs, "Kelime listem"); pasteText = "" }
                    pasteOpen = false
                }) { Text("Listeyi aktar", fontFamily = Sans) }
            },
            dismissButton = { TextButton(onClick = { pasteOpen = false }) { Text("Vazgeç", fontFamily = Sans) } },
            containerColor = White,
        )
    }
    partial?.let { result ->
        AlertDialog(
            onDismissRequest = { partial = null },
            title = { Text("Bazı kelimeler yerleşmedi", fontFamily = Serif, color = Ink) },
            text = {
                val names = result.remaining.joinToString(", ") { it.answer }
                BodyText("Yerleşen " + result.used + " kelimeden " + result.games.size + " bulmaca hazır.\n\nKalanlar: " + names.take(350))
            },
            confirmButton = { TextButton(onClick = { partial = null; saveResult(result.games) }) { Text("Hazır olanları kaydet", fontFamily = Sans) } },
            dismissButton = { TextButton(onClick = { partial = null }) { Text("Listeye dön", fontFamily = Sans) } },
            containerColor = White,
        )
    }
}

@Composable
internal fun AnswerCountPicker(label: String, value: Int, choose: (Int) -> Unit, enabled: Boolean = true) {
    BodyText(label)
    for (line in answerCounts.chunked(7)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (count in line) {
                FilterChip(
                    modifier = Modifier.weight(1f), selected = value == count, onClick = { choose(count) }, enabled = enabled,
                    label = { Text(count.toString(), fontFamily = Sans) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LightLime),
                )
            }
            if (line.size < 7) Spacer(Modifier.weight((7 - line.size).toFloat()))
        }
    }
}
