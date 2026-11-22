package com.kelimebulmacasi.a1

import android.content.ActivityNotFoundException
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Replaces the v175 paragraph entry only at final package integration.
// The existing v175 word-dictation and TTS features remain available.
@Composable
internal fun Wq186ParagraphPracticeEntry(profileKey: String, ai: AiSession) {
    var show by rememberSaveable(profileKey) { mutableStateOf(false) }
    Surface(
        onClick = { show = true },
        modifier = Modifier.fillMaxWidth().heightIn(min = 78.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFF1E7),
        border = BorderStroke(1.dp, Color(0xFFFFC3AA)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Wq175GlossIcon("paragraph", Modifier.size(56.dp))
            Column(Modifier.weight(1f)) {
                Text("Paragraf Dinle ve Yaz · Çevir", color = Color(0xFF813E40),
                    fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("A1–C2 · Türkçe → İngilizce · Kendi paragrafını ekle",
                    color = Color(0xFF704D4B), fontSize = 12.sp)
            }
            Text("›", color = Color(0xFFB75E51), fontSize = 26.sp)
        }
    }
    if (show) Wq186ParagraphWorkspace(profileKey, ai) { show = false }
}

private val wq186SamplesTr = listOf(
    "Her sabah ailemle kahvaltı ederim. Çay içer ve planlarımızı konuşuruz. Sonra yürüyerek okula giderim.",
    "Arkadaşım Anna'nın küçük bir bahçesi var. Çiçek yetiştirir ve onları her gün sular. Pazar günleri ona yardım ederim.",
    "Geçen hafta sonu nehrin yakınındaki müzeyi ziyaret ettik. Rehber bize şehir hakkında hikâyeler anlattı. Turdan sonra küçük bir kafede öğle yemeği yedik.",
    "Daha sağlıklı öğünler hazırlamak için yemek yapmayı öğreniyorum. Başta birçok hata yaptım. Şimdi basit tariflere uyarak yeni sebzeler deniyorum.",
    "Birçok kişi şehirlerinde ulaşım için bisiklete biniyor. Bisiklet para tasarrufu sağlayıp trafiği azaltabilir, fakat güvenli yollara ihtiyaç vardır. Bazı kentler ayrı bisiklet yolları yapıyor.",
    "Toplum merkezimizde okuma kulübü açıldığında az sayıda komşu katıldı. Zamanla daha çok insan haftalık toplantılara gelmeye başladı. Üyeler artık birbirlerine kitap öneriyor.",
    "Çevrim içi kurslar esneklik sunar ancak güçlü öz disiplin gerektirir. Düzenli çalışmayı planlayan öğrencilerin ödevlerini bitirmesi daha olasıdır. Öğretmenlerin açık geri bildirimi motivasyonu artırabilir.",
    "Yerel yönetim gıda israfını azaltacak bir program başlattı. Restoranlar artan yemekleri bağışlarken gönüllüler bunları topluluk mutfaklarına götürdü. Program büyüdü ancak ulaşım sorunları sürüyor.",
    "Bilimsel bulgular, sınırlamaları tam değerlendirilmeden önce halkın ilgisini çekebilir. Tek bir araştırma nedensellik kanıtlamadan bir ilişki gösterebilir. Sorumlu habercilik ön kanıtlarla tekrar edilen araştırma sonuçlarını ayırır.",
    "Şehirler kalabalıklaştıkça planlamacılar konut ihtiyacı ile yeşil alanlara erişimi dengelemek zorundadır. Kısa vadede verimli görünen önlemlerin beklenmedik sağlık sonuçları olabilir. Halkla görüşmek gözden kaçan sorunları ortaya çıkarabilir.",
    "Sayısal bir tahminin görünüşteki kesinliği, gerçek kesinlik sanılmamalıdır. Her model, bazıları yöntemi bilmeyen okuyucular için gizli kalabilecek varsayımlara dayanır. Bu sınırları kabul etmek kanıtı reddetmek değil, onu sorumlu yorumlamanın ön koşuludur.",
    "Politikaları yalnızca kısa vadeli sonuçlarla değerlendiren kurumlar birikimli etkileri kaçırabilir. Başta başarısız görünen bir girişim tamamlayıcı sistemler kurulduğunda yarar sağlayabilir. İlk kazanımlar da teşvikler değişmezse geçici kalabilir.",
)

@Composable
private fun Wq186ParagraphWorkspace(profileKey: String, ai: AiSession, close: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(profileKey) { Wq186ParagraphStore(context, profileKey) }
    val samples = remember {
        Wq175ParagraphLibrary.items.mapIndexed { i, p ->
            Wq186StudyParagraph(p.level, wq186SamplesTr[i], p.english, "Hazır")
        }
    }
    var custom by remember(profileKey) { mutableStateOf(store.read()) }
    var level by rememberSaveable(profileKey) { mutableStateOf("A1") }
    var mode by rememberSaveable(profileKey) { mutableStateOf("DICTATION") }
    var index by rememberSaveable(profileKey) { mutableIntStateOf(0) }
    var input by rememberSaveable(profileKey) { mutableStateOf("") }
    var checked by rememberSaveable(profileKey) { mutableStateOf(false) }
    var accent by rememberSaveable(profileKey) { mutableStateOf("UK") }
    var add by rememberSaveable(profileKey) { mutableStateOf(false) }
    var tr by rememberSaveable(profileKey) { mutableStateOf("") }
    var en by rememberSaveable(profileKey) { mutableStateOf("") }
    var topic by rememberSaveable(profileKey) { mutableStateOf("Günlük yaşam") }
    var pages by rememberSaveable(profileKey) { mutableStateOf("1") }
    var target by rememberSaveable(profileKey) { mutableStateOf("TR") }
    var origin by rememberSaveable(profileKey) { mutableStateOf("Elle") }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var speaker by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val tts = TextToSpeech(context.applicationContext) {
            ready = it == TextToSpeech.SUCCESS
        }
        speaker = tts
        onDispose { tts.stop(); tts.shutdown(); speaker = null }
    }
    val entries = (samples + custom).filter { it.level == level }
    val paragraph = entries.getOrNull(index.coerceIn(0, entries.lastIndex))
    fun reset(next: Int) {
        speaker?.stop()
        index = next
        input = ""
        checked = false
    }

    fun importDraft(uri: android.net.Uri, camera: Boolean = false) {
        if (busy) return
        busy = true
        status = "Belge taranıyor…"
        val pdf = runCatching { isPdf(context, uri) }.getOrDefault(false)
        scope.launch {
            try {
                val found = wq186ExtractParagraphText(context, uri, pdf, pages)
                if (found.isBlank()) status = "Metin bulunamadı; daha net bir belge dene."
                else {
                    if (target == "TR") tr = found else en = found
                    origin = if (camera) "Kamera" else if (pdf) "PDF" else "Görsel"
                    status = "Taslak aktarıldı. Kaydetmeden önce iki dili kontrol et."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                status = "Okuma hatası: " + (e.message ?: "Dosya/izin hatası").take(90)
            } finally {
                if (camera) { cameraFile?.delete(); cameraFile = null }
                busy = false
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importDraft(uri)
    }
    val photo = rememberLauncherForActivityResult(remember { TakeScanPhoto() }) { ok ->
        val file = cameraFile
        if (ok && file != null && file.length() > 0) importDraft(scanPhotoUri(context, file), true)
        else { cameraFile?.delete(); cameraFile = null; status = "Kamera iptal edildi." }
    }
    fun capture() {
        try {
            cameraFile = createScanPhoto(context)
            photo.launch(scanPhotoUri(context, cameraFile!!))
        } catch (_: ActivityNotFoundException) {
            cameraFile?.delete(); cameraFile = null; status = "Kamera bulunamadı."
        } catch (_: Exception) {
            cameraFile?.delete(); cameraFile = null; status = "Kamera açılamadı."
        }
    }
    fun aiDraft() {
        if (busy) return
        if (!ai.hasKey) { status = "Ayarlar'dan AI sağlayıcı anahtarını aç."; return }
        busy = true
        status = "AI metni hazırlanıyor…"
        scope.launch {
            try {
                val prompt = "Create one accurate CEFR " + level + " English learning paragraph about " +
                    topic.take(80) + ". Output exactly two labelled lines: " +
                    "TR: natural Turkish paragraph; EN: its accurate English translation. " +
                    "Maximum 75 words in each; no other prose."
                val text = ai.reply("", prompt).answer
                val found = Regex("(?s)TR:\\s*(.*?)\\s*EN:\\s*(.*)").find(text)
                if (found == null) status = "AI çıktısı ayrıştırılamadı; elle ekleyebilirsin."
                else {
                    tr = found.groupValues[1].trim().take(3500)
                    en = found.groupValues[2].trim().take(3500)
                    origin = "AI"
                    status = "AI taslağı düzenlenebilir. Kaydedilmedi."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                status = "AI yanıtı alınamadı: " + (e.message ?: "Bağlantı").take(80)
            } finally { busy = false }
        }
    }

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(0.95f).padding(horizontal = 8.dp),
            shape = RoundedCornerShape(23.dp), color = Color(0xFFFFFBF7),
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Paragraf Çalışması", color = Color(0xFF813E40),
                        fontWeight = FontWeight.Bold, fontSize = 21.sp)
                    TextButton(onClick = close) { Text("Kapat") }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = mode == "DICTATION",
                        onClick = { mode = "DICTATION"; reset(0) },
                        label = { Text("Dinle ve Yaz") })
                    FilterChip(selected = mode == "TRANSLATION",
                        onClick = { mode = "TRANSLATION"; reset(0) },
                        label = { Text("TR → EN Çevir") })
                    FilterChip(selected = add, onClick = { add = !add },
                        label = { Text("＋ Paragraf Ekle") })
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (lv in englishLevels) FilterChip(
                        selected = level == lv, onClick = { level = lv; reset(0) },
                        label = { Text(lv) },
                    )
                }
                if (add) {
                    Text("Yeni paragraf · " + custom.size + "/120",
                        fontWeight = FontWeight.Bold, color = Color(0xFF31584C))
                    OutlinedTextField(topic, { topic = it.take(80) },
                        label = { Text("AI konu") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(pages, { pages = it.take(25) },
                        label = { Text("PDF sayfaları, örn. 1-3,6") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = target == "TR", onClick = { target = "TR" },
                            label = { Text("Türkçeye aktar") })
                        FilterChip(selected = target == "EN", onClick = { target = "EN" },
                            label = { Text("İngilizceye aktar") })
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { open.launch(arrayOf("application/pdf")) },
                            enabled = !busy) { Text("PDF") }
                        OutlinedButton(onClick = { open.launch(arrayOf("image/*")) },
                            enabled = !busy) { Text("Görsel") }
                        OutlinedButton(onClick = ::capture, enabled = !busy) { Text("Kamera") }
                        OutlinedButton(onClick = ::aiDraft, enabled = !busy) { Text("AI") }
                    }
                    OutlinedTextField(tr, { tr = it.take(3500) },
                        label = { Text("Türkçe paragraf") }, modifier = Modifier.fillMaxWidth(),
                        minLines = 3, maxLines = 8)
                    OutlinedTextField(en, { en = it.take(3500) },
                        label = { Text("Doğru İngilizce paragraf") }, modifier = Modifier.fillMaxWidth(),
                        minLines = 3, maxLines = 8)
                    Text("OCR ve AI çıktıları düzenlenmeden kaydedilmez. AI ile istek, bağlı öğretmen sohbet geçmişinde de görünebilir.",
                        fontSize = 12.sp, color = Color(0xFF574E49))
                    Button(onClick = {
                        val item = Wq186StudyParagraph(level, tr.trim(), en.trim(), origin)
                        if (store.save(item)) {
                            custom = store.read()
                            tr = ""; en = ""; add = false
                            reset(0)
                            status = "Paragraf iki çalışma türüne kaydedildi."
                        } else status = "Metni kontrol et. Kayıt tekrarı veya kapasite sınırı olabilir."
                    }, enabled = !busy && tr.trim().length >= 10 && en.trim().length >= 10,
                        modifier = Modifier.fillMaxWidth()) { Text("İncele ve Kaydet") }
                } else if (paragraph != null) {
                    Text("Paragraf " + (index + 1) + "/" + entries.size + " · " + paragraph.origin,
                        color = Color(0xFF31584C), fontWeight = FontWeight.SemiBold)
                    if (mode == "DICTATION") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (voice in listOf("US", "UK")) FilterChip(
                                selected = accent == voice, onClick = { accent = voice },
                                label = { Text(if (voice == "US") "Amerikan" else "Britanya") },
                            )
                        }
                        OutlinedButton(onClick = {
                            val tts = speaker
                            if (tts != null && ready) {
                                val locale = if (accent == "UK") Locale.UK else Locale.US
                                if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                                    tts.language = locale
                                    tts.speak(paragraph.english, TextToSpeech.QUEUE_FLUSH,
                                        null, "wq186_" + level + index)
                                } else status = "Bu aksanın ses paketi cihazda bulunamadı."
                            }
                        }, enabled = ready, modifier = Modifier.fillMaxWidth()) {
                            Text("▶ Paragrafı dinle")
                        }
                    } else {
                        Text("Türkçeden İngilizceye çevir:", fontWeight = FontWeight.Bold,
                            color = Color(0xFF31584C))
                        Text(paragraph.turkish, color = Color(0xFF23353A))
                    }
                    OutlinedTextField(input, { if (!checked) input = it.take(3500) },
                        modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 10,
                        readOnly = checked, label = { Text("İngilizce paragrafı yaz") })
                    if (!checked) Button(onClick = { checked = true; speaker?.stop() },
                        enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Text("Karşılaştır")
                    }
                    if (checked) {
                        if (mode == "DICTATION") {
                            Text("Dikte kelime doğruluğu: %" +
                                wq175GradeParagraph(paragraph.english, input).accuracy,
                                color = Color(0xFF31584C), fontWeight = FontWeight.Bold)
                        } else Text("Birden fazla doğru çeviri olabilir; karşılığı örnek yanıt olarak incele.",
                            color = Color(0xFF31584C))
                        Text("Örnek İngilizce metin:", fontWeight = FontWeight.Bold)
                        Text(paragraph.english, color = Color(0xFF23353A))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { reset(index) }) { Text("Tekrar dene") }
                            Button(onClick = { reset((index + 1) % entries.size) }) { Text("Sonraki") }
                        }
                        if (paragraph.origin != "Hazır") {
                            TextButton(onClick = {
                                if (store.delete(paragraph.english)) {
                                    custom = store.read()
                                    reset(0)
                                    status = "Paragraf silindi."
                                }
                            }) { Text("Kendi paragrafımı sil") }
                        }
                    }
                } else Text("Bu seviyede paragraf yok. Kendi paragrafını ekleyebilirsin.",
                    color = Color(0xFF31584C))
                if (status.isNotBlank()) Text(status, color = Color(0xFF31584C))
            }
        }
    }
}
