package com.kelimebulmacasi.a1

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Runs in Android :teacher3d, NEVER in the app's main process.
 * The real human GLB, AI credential lookup, user-initiated voice recognition
 * and Android TTS operate in the same visible 3D teacher screen.
 * The explicit, nonexported Activity receives a profile ID, NOT API keys.
 * A native renderer abort can kill this Activity's process, not the launcher.
 */
internal class EthanIsolatedVoiceTeacherActivity : ComponentActivity() {
    private val ai: AiSession by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = intent.getStringExtra("teacher_profile").orEmpty()
        if (profile.isBlank() ||
            runCatching { ai.bindProfile(applicationContext, profile) }.isFailure) {
            setContent { Text("Ethan profili açılamadı. Ana uygulamaya dönün.") }
            return
        }
        val voice = if (intent.getStringExtra("voice") == EnglishVoice.British.name)
            EnglishVoice.British else EnglishVoice.American
        val rate = intent.getFloatExtra("speech_rate", 1f).coerceIn(.55f, 1.6f)
        val volume = intent.getFloatExtra("speech_volume", 1f).coerceIn(0f, 1f)
        setContent {
            Ethan3DConversation(
                ai = ai, initialVoice = voice, rate = rate, volume = volume,
                close = { finish() },
            )
        }
    }
}

@Composable
private fun Ethan3DConversation(
    ai: AiSession,
    initialVoice: EnglishVoice,
    rate: Float,
    volume: Float,
    close: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val main = remember { Handler(Looper.getMainLooper()) }
    var rendered by remember { mutableStateOf(true) }
    var voice by remember { mutableStateOf(initialVoice) }
    var draft by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf("") }
    var replyText by remember { mutableStateOf(ai.turns.lastOrNull()?.answer.orEmpty()) }
    var mouthCue by remember { mutableStateOf(AvatarMouthFrame()) }

    val mode = when {
        playing != null -> AvatarMode.SPEAKING
        thinking -> AvatarMode.THINKING
        listening -> AvatarMode.LISTENING
        error.isNotBlank() -> AvatarMode.ERROR
        else -> AvatarMode.IDLE
    }
    val audio = remember(context) {
        ConversationAudio(
            context = context,
            onPlaying = { id -> main.post {
                playing = id
                if (id == null) mouthCue = AvatarMouthFrame()
            }},
            onError = { problem ->
                main.post { error = "Ses oynatma hatası: " + problem.take(100) }
            },
            englishVoice = { voice },
            settings = { rate to volume },
            onActualSpeechRange = { _, spoken -> main.post {
                mouthCue = AvatarMouthFrame(
                    viseme = AvatarLipSyncController.fromTextRange(spoken),
                    weight = .68f,
                    quality = LipSyncQuality.TEXT_RANGE_APPROXIMATION,
                    cueAtMs = SystemClock.uptimeMillis(),
                )
            }},
        )
    }
    DisposableEffect(audio) { onDispose { audio.close() } }

    val send: (String) -> Unit = { raw ->
        val message = raw.trim().take(maxMessageLength)
        if (!ai.hasKey) {
            error = "AI anahtarını ana uygulamanın Ayarlar bölümünden kaydedin."
        } else if (message.isNotBlank() && !thinking) {
            draft = ""
            error = ""
            thinking = true
            audio.stop()
            scope.launch {
                try {
                    // Stored per-profile Gemini/provider key, never Intent extra.
                    val reply = ai.reply("", message)
                    replyText = reply.answer
                    if (ai.autoRead) audio.play(reply)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = "AI yanıtı alınamadı (" + e.javaClass.simpleName + ")."
                } finally {
                    thinking = false
                }
            }
        }
    }
    val microphone = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        listening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (spoken.isNullOrBlank()) error = "Konuşma algılanamadı."
            else send(spoken)
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF102B33))) {
        if (rendered) {
            EthanLive3DStage(
                motion = mode, mouthCue = mouthCue,
                speechRate = rate, modifier = Modifier.fillMaxSize(),
                onRecoverableError = { rendered = false },
            )
        } else {
            TeacherFallback2D(
                TeacherAvatar.Mert, playing != null, listening, thinking,
                rate, Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xEE102B33))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Ethan · Canlı 3D yapay zekâ öğretmeni",
                color = Color(0xFFF2D9A5))
            val status = when (mode) {
                AvatarMode.SPEAKING -> "Ethan konuşuyor"
                AvatarMode.THINKING -> "Ethan yanıt hazırlıyor"
                AvatarMode.LISTENING -> "Ethan seni dinliyor"
                AvatarMode.ERROR -> "Bağlantı veya ses sorunu"
                else -> "İngilizce sor veya yaz"
            }
            Text(status, color = Color.White)
            if (replyText.isNotBlank()) {
                Text(
                    replyText.take(330), color = Color.White, maxLines = 4,
                    modifier = Modifier.heightIn(max = 92.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
            if (error.isNotBlank()) {
                Text(error, color = Color(0xFFFFB7AE))
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(maxMessageLength) },
                label = { Text("Ethan'a söyle") },
                modifier = Modifier.fillMaxWidth(), maxLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Button(onClick = {
                    try {
                        error = ""
                        listening = true
                        microphone.launch(
                            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                                    if (voice == EnglishVoice.British) "en-GB" else "en-US")
                                .putExtra(RecognizerIntent.EXTRA_PROMPT,
                                    "Ethan seni dinliyor")
                        )
                    } catch (_: ActivityNotFoundException) {
                        listening = false
                        error = "Cihazda konuşma tanıma hizmeti yok."
                    } catch (_: Exception) {
                        listening = false
                        error = "Mikrofon başlatılamadı."
                    }
                }, enabled = !thinking) { Text("Konuş") }
                Button(
                    onClick = { send(draft) },
                    enabled = !thinking && draft.isNotBlank(),
                ) { Text("Gönder") }
                OutlinedButton(onClick = { audio.stop() }) { Text("Sesi kes") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                FilterChip(selected = voice == EnglishVoice.American,
                    onClick = { voice = EnglishVoice.American },
                    label = { Text("US") })
                FilterChip(selected = voice == EnglishVoice.British,
                    onClick = { voice = EnglishVoice.British },
                    label = { Text("UK") })
                OutlinedButton(onClick = {
                    audio.stop()
                    ai.newConversation()
                    replyText = ""
                    error = ""
                }) { Text("Yeni sohbet") }
                OutlinedButton(onClick = {
                    audio.stop()
                    close()
                }) { Text("Geri dön") }
            }
            if (!rendered) {
                Text(
                    "3D açılamadı; Ethan güvenli 2D görünümde konuşmaya devam ediyor.",
                    color = Color.White,
                )
            }
        }
    }
}
