package com.kelimebulmacasi.a1

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * One voice settings surface for Lena and all available Android TTS voices.
 * Choices persist through the same profile preferences used by playback.
 */
@Composable
internal fun Wq186LenaVoiceSettings(session:AppSession,onDismiss:()->Unit) {
    val context=LocalContext.current
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var voices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var ready by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        var disposed=false
        var instance:TextToSpeech?=null
        instance=TextToSpeech(context) { status ->
            if(!disposed) {
                ready=status==TextToSpeech.SUCCESS
                voices=if(status==TextToSpeech.SUCCESS)
                    runCatching{instance?.voices.orEmpty()
                        .filter{it.locale.language==Locale.ENGLISH.language}
                        .sortedBy{it.locale.toLanguageTag()+it.name}}.getOrDefault(emptyList())
                    else emptyList()
            }
        }
        engine=instance
        onDispose {
            disposed=true
            instance?.stop()
            instance?.shutdown()
        }
    }
    val settings=session.preferences
    val accent=settings.voice
    val available=voices.filter {
        it.locale.country==accent.locale.country
    }.filter { settings.allowOnlineVoice || !it.isNetworkConnectionRequired }
    val selected=if(accent==EnglishVoice.British)
        settings.nativeVoiceUk else settings.nativeVoiceUs
    var voiceListOpen by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Lena · Ses Ayarları")},
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=500.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Text("Aksan",fontSize=13.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    EnglishVoice.entries.forEach { candidate ->
                        FilterChip(selected=accent==candidate,
                            onClick={session.changePreferences(settings.copy(voice=candidate))},
                            label={Text(candidate.label)})
                    }
                }
                Text("Ses yöntemi",fontSize=13.sp)
                PronunciationMode.entries.forEach { candidate ->
                    FilterChip(selected=settings.pronunciationMode==candidate,
                        onClick={session.changePreferences(
                            session.preferences.copy(pronunciationMode=candidate))},
                        label={Text(candidate.label)})
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("Çevrimiçi cihaz seslerine izin ver",fontSize=12.sp,
                        modifier=Modifier.weight(1f))
                    Switch(checked=settings.allowOnlineVoice,onCheckedChange={
                        session.changePreferences(session.preferences.copy(allowOnlineVoice=it))
                    })
                }
                Text("Cihazdaki sesler · ${accent.label}",fontSize=13.sp)
                Text(if(!ready) "Ses motoru yükleniyor veya mevcut değil."
                    else if(available.isEmpty()) "Bu aksanda yüklü cihaz sesi bulunamadı."
                    else "${available.size} kullanılabilir ses bulundu.",
                    fontSize=11.sp)
                OutlinedButton(onClick={voiceListOpen=!voiceListOpen},
                    modifier=Modifier.fillMaxWidth(),enabled=ready) {
                    Text(if(voiceListOpen)"Ses listesini kapat"
                        else "Tüm uygun sesleri göster")
                }
                if(voiceListOpen) available.forEach { voice ->
                    val name=voice.name
                    val checked=selected==name
                    Row(Modifier.fillMaxWidth()) {
                        RadioButton(selected=checked,onClick={
                            val current=session.preferences
                            session.changePreferences(
                                if(accent==EnglishVoice.British)
                                    current.copy(nativeVoiceUk=name)
                                else current.copy(nativeVoiceUs=name)
                            )
                        })
                        Column(Modifier.weight(1f)) {
                            Text(name,fontSize=11.sp)
                            Text(voice.locale.displayName,fontSize=10.sp)
                        }
                    }
                }
                Text("Konuşma hızı · " +
                    String.format(Locale.ROOT,"%.1fx",settings.speechRate))
                Slider(value=settings.speechRate,onValueChange={
                    session.changePreferences(
                        session.preferences.copy(speechRate=it.coerceIn(.5f,1.5f)))
                },valueRange=.5f..1.5f)
                Text("Ses düzeyi · ${(settings.speechVolume*100).toInt()}%")
                Slider(value=settings.speechVolume,onValueChange={
                    session.changePreferences(
                        session.preferences.copy(speechVolume=it.coerceIn(0f,1f)))
                },valueRange=0f..1f)
                OutlinedButton(onClick={
                    val tts=engine ?: return@OutlinedButton
                    val voice=available.firstOrNull{it.name==selected}
                        ?: available.firstOrNull()
                    if(voice!=null) {
                        tts.voice=voice
                        tts.setSpeechRate(session.preferences.speechRate)
                        val audioParams=android.os.Bundle().apply {
                            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,
                                session.preferences.speechVolume.coerceIn(0f,1f))
                        }
                        tts.speak("Hello! Let's practise English together.",
                            TextToSpeech.QUEUE_FLUSH,audioParams,"wq186_voice_preview")
                    }
                },enabled=ready && available.isNotEmpty(),
                    modifier=Modifier.fillMaxWidth()) {
                    Text("Seçilen sesi dene")
                }
            }
        },
        confirmButton={TextButton(onClick=onDismiss){Text("Tamam")}}
    )
}
