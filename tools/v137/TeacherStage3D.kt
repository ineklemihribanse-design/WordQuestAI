package com.kelimebulmacasi.a1

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The main conversation screen NEVER constructs Filament.
 * Native :teacher3d death may not deliver ActivityResult. Read persisted
 * crash stage on ON_RESUME and first mount, and offer permanent manual report.
 * The AI/TTS/mic stay in chat; isolated 3D is a preview, not falsely
 * advertised as live synchronized speech until safe IPC is validated.
 */
@Composable
internal fun TeacherStage3D(
    actor: TeacherAvatar,
    speaking: Boolean,
    listening: Boolean,
    thinking: Boolean,
    speechRate: Float,
    modifier: Modifier = Modifier,
    mouthCue: AvatarMouthFrame = AvatarMouthFrame(),
    motion: AvatarMode = AvatarMode.IDLE,
    teacherProfile: String? = null,
    initialVoice: EnglishVoice = EnglishVoice.American,
    speechVolume: Float = 1f,
    onRemoteReturn: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var diagnostic by remember { mutableStateOf("") }
    var showDiagnostic by remember { mutableStateOf(false) }
    var refreshReport by remember { mutableIntStateOf(0) }

    // Main Activity resumes even if the separate GPU process died before
    // StartActivityForResult could return.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshReport++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // OS process-exit records can arrive later than the resumed UI.
    // Finite retries also run on reopening the app after a full-process death.
    LaunchedEffect(refreshReport) {
        for (pause in listOf(0L, 700L, 1800L)) {
            if (pause > 0) delay(pause)
            val report = withContext(Dispatchers.IO) {
                Teacher3DHealth.lastResult(context)
            }
            diagnostic = report
            if (report.isNotEmpty()) showDiagnostic = true
        }
    }

    val openViewer = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Callback is optional; ON_RESUME and manual control work without it.
        refreshReport++
        onRemoteReturn()
    }

    Box(modifier = modifier.background(Color(0xFF102B33))) {
        TeacherFallback2D(
            TeacherAvatar.Mert, speaking, listening, thinking, speechRate,
            Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                .heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Ethan · canlı AI konuşma öğretmeni. Ayrı 3D penceresi Gemini, mikrofon ve sesli yanıt içerir.",
                color = Color.White
            )
            TextButton(
                onClick = {
                    if (teacherProfile.isNullOrBlank()) {
                        Toast.makeText(
                            context, "Önce bir kullanıcı profili seçin.", Toast.LENGTH_SHORT
                        ).show()
                        return@TextButton
                    }
                    diagnostic = ""
                    showDiagnostic = false
                    Teacher3DHealth.mark(context, "requested_live_voice_${TeacherAvatar.Mert.name}")
                    openViewer.launch(
                        Intent(context, EthanIsolatedVoiceTeacherActivity::class.java)
                            .putExtra("teacher_profile", teacherProfile)
                            .putExtra("speech_rate", speechRate)
                            .putExtra("speech_volume", speechVolume)
                            .putExtra("voice", initialVoice.name)
                    )
                }
            ) {
                Text("Ethan ile canlı 3D konuş", color = Color(0xFFF2D9A5))
            }
            TextButton(onClick = {
                showDiagnostic = true
                refreshReport++
            }) {
                Text("3D hata raporunu göster / yenile", color = Color(0xFFF2D9A5))
            }
            if (showDiagnostic) {
                Text(
                    diagnostic.ifBlank {
                        "Henüz kaydedilmiş bir 3D denemesi yok. " +
                        "Modeli açtıktan sonra bu düğmeyle raporu kontrol edin."
                    },
                    color = Color.White,
                )
                if (diagnostic.isNotBlank()) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("WordQuest 3D hata raporu", diagnostic)
                        )
                        Toast.makeText(
                            context, "3D hata bilgisi kopyalandı", Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text("Hata bilgisini kopyala", color = Color(0xFFF2D9A5))
                    }
                }
            }
        }
    }
}
