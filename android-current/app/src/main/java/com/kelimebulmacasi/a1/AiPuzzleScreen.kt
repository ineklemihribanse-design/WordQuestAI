package com.kelimebulmacasi.a1

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun AiPuzzleScreen(
    initialTopic: String, initialLevel: String, initialCount: Int, used: Set<String>, hasKey: Boolean, clearKey: () -> Unit,
    back: () -> Unit, request: suspend (String, String, String, Int, Set<String>) -> List<WordPair>,
    provider: AiProvider = AiProvider.Gemini,
    useWords: (String, List<WordPair>) -> Unit,
) {
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var topic by rememberSaveable { mutableStateOf(initialTopic.take(100)) }
    var level by rememberSaveable { mutableStateOf(initialLevel.takeIf(aiLevels::contains) ?: "A2") }
    var count by rememberSaveable { mutableIntStateOf(initialCount) }
    var levelMenu by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var result by remember { mutableStateOf(emptyList<WordPair>()) }
    var resultTopic by remember { mutableStateOf("") }

    fun generate() {
        focus.clearFocus()
        result = emptyList()
        error = ""
        val key = apiKey.trim()
        val subject = topic.trim()
        busy = true
        scope.launch {
            try {
                val suggestions = request(key, subject, level, count, used)
                result = suggestions
                resultTopic = subject
                if (suggestions.isEmpty()) error = "Yeni kelime bulunamadı. Daha belirgin bir konu yazıp yeniden dene."
                apiKey = ""
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (problem: WordRequestException) {
                error = problem.message.orEmpty()
            } catch (_: IllegalArgumentException) {
                error = "Konu, seviye ve kelime sayısını kontrol et."
            } catch (_: Exception) {
                error = "Kelime önerileri alınamadı. İnternet bağlantını kontrol edip tekrar dene."
            } finally {
                busy = false
            }
        }
    }

    ScreenPage("Yapay zekâyla kelime üret", "Konuya uygun İngilizce cevaplar ve Türkçe ipuçları", back) {
        OutlinedTextField(
            topic, { topic = it.take(100) }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
            label = { Text("Konu") }, placeholder = { Text("Örnek: havaalanında yolculuk") }, singleLine = true,
        )
        OutlinedButton(onClick = { levelMenu = true }, enabled = !busy) {
            Text("Seviye · $level", fontFamily = Sans, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded = levelMenu, onDismissRequest = { levelMenu = false }) {
            for (option in aiLevels) DropdownMenuItem(text = { Text(option) }, onClick = { level = option; levelMenu = false })
        }
        AnswerCountPicker("Üretilecek kelime sayısı", count, { count = it }, !busy)
        if (!hasKey) {
            GeminiKeyInput(apiKey, { apiKey = it }, !busy, { error = it }, provider)
        } else {
            TextButton(onClick = { clearKey(); apiKey = "" }, enabled = !busy) { Text("Anahtarı unut", fontFamily = Sans) }
        }
        BodyText("Konu, seviye ve kelime sayısı seçtiğiniz AI sağlayıcısına gönderilir. 'give up' gibi ifadelerin boşlukları korunur.")
        PrimaryAction(if (busy) "Kelimeler hazırlanıyor…" else "Kelime üret", ::generate,
            !busy && topic.trim().length >= 2 && (hasKey || apiKey.isNotBlank()))
        if (error.isNotBlank()) BodyText(error, ErrorInk)
        if (result.isNotEmpty()) {
            Text("$resultTopic · ${result.size} öneri", fontFamily = Sans, fontWeight = FontWeight.Bold, color = Forest)
            PrimaryAction("Kelimeleri bulmacaya ekle", { useWords(resultTopic, result) }, !busy)
            for (pair in result) BodyText("${pair.answer} · ${pair.clue}", Ink)
        }
    }
}

@Composable
internal fun GeminiKeyInput(value: String, onValueChange: (String) -> Unit, enabled: Boolean,
                            onError: (String) -> Unit, provider: AiProvider = AiProvider.Gemini) {
    val context = LocalContext.current
    OutlinedTextField(
        value, { onValueChange(it.take(4_097)) }, modifier = Modifier.fillMaxWidth(), enabled = enabled,
        label = { Text("${provider.label} API anahtarı") }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
    )
    TextButton(onClick = {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(provider.homepage)))
        } catch (_: ActivityNotFoundException) {
            onError("Tarayıcı açılamadı. Sağlayıcının anahtar sayfasını ziyaret edin.")
        }
    }, enabled = enabled) { Text("${provider.label} anahtar sayfası", fontFamily = Sans) }
    BodyText("Bu alandan girilen anahtar geçicidir. Sürekli kullanmak için Ayarlar → Yapay zekâ sağlayıcısı bölümüne kaydedin.")
}
