package com.kelimebulmacasi.a1

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
internal fun WordQuestSettings(session: AppSession, pronunciation: WordPronunciation, ai: AiSession, back: () -> Unit) {
    val preferences = session.preferences
    val context = LocalContext.current
    val correctSound = remember(context) { CorrectAnswerSound(context) }
    DisposableEffect(correctSound) { onDispose { correctSound.close() } }
    LaunchedEffect(Unit) { pronunciation.inspect() }
    LaunchedEffect(preferences.speechRate, preferences.speechVolume,
        preferences.pronunciationMode, preferences.nativeVoiceUs, preferences.nativeVoiceUk,
        preferences.allowOnlineVoice) {
        pronunciation.configure(preferences.speechRate, preferences.speechVolume)
        pronunciation.configureEngine(preferences)
    }
    ScreenPage("Ayarlar", session.learningProfileName, back) {
        if (session.accountName == null) LocalProfilePicker(session)
        UpdateSettingsSection()
        AiProviderSettingsSection(ai)
        Text("Yazı boyutu: %d%%".format((preferences.textScale * 100).roundToInt()),
            color = Ink, fontFamily = Serif, fontSize = 25.sp)
        Text("Daha küçük yazıdan büyük yazıya kadar ayarla. Değişiklik anında uygulanır.",
            fontFamily = Sans, fontSize = 13.sp)
        Slider(
            value = preferences.textScale.coerceIn(0.75f, 1.4f),
            onValueChange = { newScale ->
                val rounded = (newScale * 20f).roundToInt() / 20f
                session.changePreferences(preferences.copy(textScale = rounded.coerceIn(0.75f, 1.4f)))
            },
            valueRange = 0.75f..1.4f,
            steps = 12,
            modifier = Modifier.semantics { contentDescription = "Yazı boyutu" },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (size in AppTextSize.entries) {
                FilterChip(
                    selected = kotlin.math.abs(preferences.textScale - size.scale) < 0.015f,
                    onClick = { session.changePreferences(preferences.copy(textSize = size, textScale = size.scale)) },
                    label = { Text(size.label, fontFamily = Sans) },
                )
            }
        }
        Text("Örnek: The quick brown fox — İngilizce öğreniyorum.", fontFamily = Sans)
        Text("Yazılı telaffuz (IPA)", color = Ink, fontFamily = Serif, fontSize = 23.sp)
        SoundSwitch("Amerikan ve Britanya IPA göster", preferences.showIpa, true) {
            session.changePreferences(preferences.copy(showIpa = it))
        }
        SoundSwitch("Türkçe yaklaşık okunuşu da göster", preferences.showTurkishApprox, preferences.showIpa) {
            session.changePreferences(preferences.copy(showTurkishApprox = it))
        }
        WordIpa("water", preferences)
        Text("IPA, önceki ses paketinin en-US/en-GB fonetik verilerinden türetilmiştir. " +
            "Cihazdaki seçili sesin her ayrıntısıyla eşleşeceği garanti edilmez. " +
            "İnsan editörlü sözlük doğrulaması değildir.", fontFamily = Sans, fontSize = 12.sp)
        Text("Doğru cevap efekti", color = Ink, fontFamily = Serif, fontSize = 23.sp)
        SoundSwitch("Doğru ve yanlış cevaplarda oyun sesleri", preferences.correctSoundEnabled, true) {
            session.changePreferences(preferences.copy(correctSoundEnabled = it))
        }
        Text("Efekt ses seviyesi: %d%%".format((preferences.correctSoundVolume * 100).roundToInt()),
            fontFamily = Sans)
        Slider(value = preferences.correctSoundVolume, onValueChange = {
            session.changePreferences(preferences.copy(correctSoundVolume = it))
        }, valueRange = 0f..1f, enabled = preferences.correctSoundEnabled,
            modifier = Modifier.semantics { contentDescription = "Doğru cevap efekti ses seviyesi" })
        Text("Telaffuzdan bağımsızdır; iki sesi ayrı ayrı kapatabilirsiniz.",
            fontFamily = Sans, fontSize = 12.sp)
        OutlinedButton(onClick = {
            correctSound.play(preferences.correctSoundEnabled, preferences.correctSoundVolume)
        }, enabled = preferences.correctSoundEnabled) {
            Text("Doğru cevap efektini dinle", fontFamily = Sans)
        }
        Text("İngilizce telaffuz", color = Ink, fontFamily = Serif, fontSize = 25.sp)
        SoundSwitch("Telaffuz sesini aç/kapat", preferences.pronunciation, true) {
            if (!it) pronunciation.stop()
            session.changePreferences(preferences.copy(pronunciation = it))
        }
        SoundSwitch("Doğru kelimeyi otomatik oku", preferences.autoPronunciation, preferences.pronunciation) {
            session.changePreferences(preferences.copy(autoPronunciation = it))
        }
        Text("Ses motoru", color = Ink, fontFamily = Serif, fontSize = 20.sp)
        Text("Yeni: Telefonda kurulu daha kaliteli sesi seçebilirsiniz. " +
            "Sesin doğal olması telefonun ses motoruna bağlıdır. " +
            "Uygun ses bulunmazsa aksanı değiştirmeden eski kayıt çalınır.",
            fontFamily = Sans, fontSize = 12.sp)
        for (mode in PronunciationMode.entries) {
            FilterChip(selected = preferences.pronunciationMode == mode,
                enabled = preferences.pronunciation,
                onClick = {
                    pronunciation.stop()
                    session.changePreferences(preferences.copy(pronunciationMode = mode))
                }, label = { Text(mode.label, fontFamily = Sans) })
        }
        if (preferences.pronunciationMode == PronunciationMode.Device) {
            SoundSwitch("Cihaz motorunun internet isteyen seslerine izin ver",
                preferences.allowOnlineVoice, preferences.pronunciation) {
                session.changePreferences(preferences.copy(allowOnlineVoice = it))
            }
            Text("İnternet seçeneği varsayılan olarak kapalıdır. Uygulama ücretli TTS API kullanmaz. " +
                "İnternet isteyen cihaz seslerinin çevrimdışı çalışması beklenmez.",
                fontFamily = Sans, fontSize = 12.sp)
            for (accent in EnglishVoice.entries) {
                val installed = pronunciation.deviceVoices.filter {
                    it.region == accent && (preferences.allowOnlineVoice || !it.networkRequired)
                }
                val preferredId = if (accent == EnglishVoice.American)
                    preferences.nativeVoiceUs else preferences.nativeVoiceUk
                Text("${accent.label} cihaz sesleri", color = Ink, fontFamily = Serif, fontSize = 18.sp)
                FilterChip(selected = preferredId.isBlank(), enabled = preferences.pronunciation,
                    onClick = {
                        session.changePreferences(if (accent == EnglishVoice.American)
                            preferences.copy(nativeVoiceUs = "") else preferences.copy(nativeVoiceUk = ""))
                    }, label = { Text("En kaliteli uygun sesi otomatik seç", fontFamily = Sans) })
                if (installed.isEmpty()) {
                    Text("Cihaz motoru bu aksana uygun bir ses veya bölgesel dil desteği bildirmedi. " +
                        "Klasik çevrimdışı kayıtlar yedek olarak korunuyor.",
                        fontFamily = Sans, fontSize = 12.sp)
                }
                for (option in installed.take(12)) {
                    FilterChip(selected = preferredId == option.id,
                        enabled = preferences.pronunciation,
                        onClick = {
                            pronunciation.stop()
                            session.changePreferences(if (accent == EnglishVoice.American)
                                preferences.copy(nativeVoiceUs = option.id)
                                else preferences.copy(nativeVoiceUk = option.id))
                        }, label = { Text(option.description, fontFamily = Sans) })
                }
            }
            Text("Android ses motorları kadın/erkek bilgisini standart şekilde bildirmediği için " +
                "seslerin cinsiyeti yanlış etiketlenmiyor. Örnekleri dinleyerek seçim yapın.",
                fontFamily = Sans, fontSize = 12.sp)
        }
        Text("Telaffuz aksanı", color = Ink, fontFamily = Serif, fontSize = 20.sp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (voice in EnglishVoice.entries) {
                FilterChip(
                    selected = preferences.voice == voice, enabled = preferences.pronunciation,
                    onClick = {
                        pronunciation.stop()
                        session.changePreferences(preferences.copy(voice = voice))
                    },
                    label = { Text(voice.label, fontFamily = Sans) },
                )
            }
        }
        OutlinedButton(
            onClick = { pronunciation.pronounce(listOf("schedule"), preferences.voice, replay = true) },
            enabled = preferences.pronunciation,
        ) { Text("Örnek sesi dinle", fontFamily = Sans) }
        Text("Konuşma hızı: %.2f".format(preferences.speechRate), fontFamily = Sans)
        Slider(value = preferences.speechRate, onValueChange = {
            session.changePreferences(preferences.copy(speechRate = it))
        }, valueRange = 0.5f..1.5f, enabled = preferences.pronunciation)
        Text("Ses seviyesi: %d%%".format((preferences.speechVolume * 100).toInt()), fontFamily = Sans)
        Slider(value = preferences.speechVolume, onValueChange = {
            session.changePreferences(preferences.copy(speechVolume = it))
        }, valueRange = 0f..1f, enabled = preferences.pronunciation)
        Text("Amerikan / Britanya karşılaştırması", color = Ink, fontFamily = Serif, fontSize = 23.sp)
        Text("Cihaz sesi seçiliyse telefonun İngilizce sesleri kullanılır. " +
            "Eski US/UK OGG kayıtları bağlantısız yedek olarak saklanır.", fontFamily = Sans)
        for (word in listOf("Water", "Tomato", "Better", "Can't", "Schedule")) {
            Text(word, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
            WordIpa(word, preferences)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pronunciation.pronounce(listOf(word), EnglishVoice.American, replay = true) },
                    enabled = preferences.pronunciation) { Text("US dinle") }
                OutlinedButton(onClick = { pronunciation.pronounce(listOf(word), EnglishVoice.British, replay = true) },
                    enabled = preferences.pronunciation) { Text("UK dinle") }
            }
        }
        Text("Cihaz / çevrimdışı ses durumu", color = Ink, fontFamily = Serif, fontSize = 19.sp)
        Text(pronunciation.accentDiagnostics, fontFamily = Sans)
        Text(pronunciation.voiceDetails, fontFamily = Sans)
        Text("Eski yedek kayıtlar eSpeak ile sentezlenmiştir ve robotiktir. " +
            "Cihaz sesinin de doğal duyulması garanti edilemez. " +
            "Amerikan/Britanya farkını Tomato, Can't, Schedule ile karşılaştırın.",
            fontFamily = Sans, fontSize = 12.sp)
        pronunciation.error?.let { BodyText(it, ErrorInk) }
    }
}

@Composable
private fun SoundSwitch(text: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, Modifier.weight(1f), color = Ink, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
        Switch(checked, onCheckedChange = change, enabled = enabled,
            modifier = Modifier.semantics { contentDescription = text })
    }
}
