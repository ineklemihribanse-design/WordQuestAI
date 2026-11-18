package com.kelimebulmacasi.a1

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.SystemClock
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val LiveBackground = Color(0xFF0B2027)
private val LiveDark = Color(0xFF102F35)
private val LiveGold = Color(0xFFE8C98F)
private val LiveMint = Color(0xFFACD9CB)
private val LiveWhite = Color(0xFFF6F2E8)

/** A new, dedicated full-screen interactive 3D lesson. Original text conversation remains available. */
@Composable
internal fun LiveTeacherScreen(
    ai: AiSession,
    session: AppSession,
    back: () -> Unit,
    openTextChat: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var audio by remember { mutableStateOf<ConversationAudio?>(null) }
    var speaking by remember { mutableStateOf<Long?>(null) }
    val avatarMotion = remember { AvatarStateController(ai.avatar.name) }
    val lipSync = remember { AvatarLipSyncController() }
    var mouthCue by remember { mutableStateOf(AvatarMouthFrame()) }
    var sending by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var audioError by remember { mutableStateOf("") }
    var ephemeralKey by remember { mutableStateOf("") }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var threadsOpen by rememberSaveable { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<TeacherThread?>(null) }
    val canEdit = !sending && !listening
    val canSend = canEdit && (ai.hasKey || ephemeralKey.isNotBlank())
    val currentTurn = ai.turns.lastOrNull()

    LaunchedEffect(ai.avatar) {
        if (avatarMotion.actorId != ai.avatar.name) {
            audio?.stop()
            avatarMotion.selectActor(ai.avatar.name)
            lipSync.stop()
            mouthCue = AvatarMouthFrame()
        }
        if (avatarMotion.mode == AvatarMode.IDLE) {
            // A single complete wave on entry, not a 5.5-second loop.
            avatarMotion.greeting()
            kotlinx.coroutines.delay(2550L)
            avatarMotion.greetingFinished()
        }
    }

    DisposableEffect(context, lifecycle) {
        val playback = ConversationAudio(context, { id ->
            speaking = id
            if (id == null) {
                avatarMotion.audioFinished(null)
                lipSync.stop()
                mouthCue = AvatarMouthFrame()
            } else if (avatarMotion.audioStarted(id)) {
                lipSync.start(id)
            }
        }, { audioError = it },
            englishVoice = { session.preferences.voice },
            settings = { session.preferences.speechRate to session.preferences.speechVolume },
            pronunciationPreferences = { session.preferences },
            onActualSpeechRange = { id, spoken ->
                val moment = SystemClock.uptimeMillis()
                val viseme = AvatarLipSyncController.fromTextRange(spoken)
                if (lipSync.push(id, viseme, moment, LipSyncQuality.TEXT_RANGE_APPROXIMATION)) {
                    mouthCue = lipSync.frame(moment)
                }
            })
        audio = playback
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) playback.stop()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            playback.close()
            audio = null
        }
    }

    fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty() || sending || listening || (!ai.hasKey && ephemeralKey.isBlank())) {
            if (!ai.hasKey && ephemeralKey.isBlank()) error = "Önce Ayarlar → Yapay zekâ bölümüne API anahtarını kaydet."
            return
        }
        val key = ephemeralKey
        audio?.stop()
        focus.clearFocus()
        sending = true
        avatarMotion.responsePending()
        error = ""
        audioError = ""
        ai.draft = ""
        scope.launch {
            try {
                val optionalContext = try { session.teacherLearningContext() } catch (_: Exception) { "" }
                val turn = ai.reply(key, message, optionalContext, session.profileName)
                ephemeralKey = ""
                avatarMotion.responseAvailable()
                if (ai.autoRead && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) audio?.play(turn)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (problem: WordRequestException) {
                ai.draft = message
                error = problem.message ?: "Yapay zekâ yanıtı alınamadı."
            } catch (problem: Exception) {
                ai.draft = message
                error = conversationFailureMessage(problem)
            } finally {
                sending = false
            }
        }
    }

    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        listening = false
        avatarMotion.micCancelled()
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull { it.isNotBlank() }?.trim()
            if (text.isNullOrBlank()) error = "Ses anlaşılamadı. Yeniden deneyebilir veya yazabilirsin."
            else send(text)
        }
    }
    fun startListening() {
        if (!canEdit) return
        if (!ai.hasKey && ephemeralKey.isBlank()) {
            error = "Sesli ders için önce AI anahtarını Ayarlar'a ekle."
            return
        }
        focus.clearFocus()
        audio?.stop()
        error = ""
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                if (ai.language == ConversationLanguage.English) session.preferences.voice.locale.toLanguageTag()
                else ai.language.tag)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "${ai.avatar.label} seni dinliyor. Konuşabilirsin.")
        try {
            recognizer.launch(intent)
            listening = true
            avatarMotion.micStarted()
        } catch (_: ActivityNotFoundException) {
            listening = false
            error = "Bu telefonda ses tanıma uygulaması bulunamadı. Yazılı sohbeti kullanabilirsin."
        } catch (_: SecurityException) {
            listening = false
            error = "Mikrofon kullanılamıyor. Cihazının ses tanıma izinlerini kontrol et."
        }
    }

    Column(
        Modifier.fillMaxSize().background(LiveBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { audio?.stop(); back() }, contentPadding = PaddingValues(4.dp)) {
                Text("‹ Geri", color = LiveMint, fontSize = 15.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("CANLI AI ÖĞRETMEN", color = LiveGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(ai.avatar.label, color = LiveWhite, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { audio?.stop(); threadsOpen = true; ai.searchThreads("") }, enabled = canEdit) {
                Text("Sohbetler", color = LiveWhite, fontSize = 13.sp)
            }
            TextButton(onClick = { audio?.stop(); settingsOpen = true }, enabled = canEdit) {
                Text("Ayarlar", color = LiveGold, fontSize = 13.sp)
            }
        }

        // Large, independently rendered 3D scene with subtle animated face/body.
        Box(
            Modifier.fillMaxWidth().weight(1f)
                .background(Brush.verticalGradient(listOf(Color(0xFF193E45), LiveBackground))),
        ) {
            TeacherStage2D(ai.avatar, speaking != null, listening, sending,
                session.preferences.speechRate, Modifier.fillMaxSize(),
                mouthCue = mouthCue, motion = avatarMotion.mode,
                teacherProfile = ai.boundProfile,
                initialVoice = session.preferences.voice,
                speechVolume = session.preferences.speechVolume,
                onRemoteReturn = { ai.refreshExternalEthanThreads() })
            Text("3D · Döndür · İki parmakla yakınlaş", color = LiveWhite.copy(alpha = .73f), fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))
            Column(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, LiveBackground.copy(alpha = .96f))))
                    .padding(start = 18.dp, end = 18.dp, top = 32.dp, bottom = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    when {
                        listening -> "Seni dinliyorum…"
                        sending -> "${ai.avatar.label} yanıtını hazırlıyor…"
                        speaking != null -> "${ai.avatar.label} konuşuyor"
                        currentTurn == null -> "Merhaba! Bugün ne çalışmak istersin?"
                        else -> "${ai.avatar.label} hazır · Dilediğin zaman konuşabilirsin"
                    },
                    color = LiveGold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                )
                if (currentTurn != null && !keyboardOpen) {
                    Text(
                        currentTurn.answer.replace(Regex("\\s+"), " ").take(165),
                        color = LiveWhite, fontSize = 13.sp, lineHeight = 19.sp,
                        maxLines = 3, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(LiveDark, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!keyboardOpen) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    listOf(TeacherAvatar.Mert).forEach { actor ->
                        val chosen = ai.avatar == actor
                        Box(
                            Modifier.weight(1f)
                                .border(1.dp, if (chosen) LiveGold else LiveWhite.copy(alpha = .2f), RoundedCornerShape(13.dp))
                                .background(if (chosen) Color(0xFF2C5557) else LiveBackground, RoundedCornerShape(13.dp))
                                .clickable(enabled = canEdit) { ai.chooseAvatar(actor) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(actor.label, color = if (chosen) LiveGold else LiveWhite, fontSize = 14.sp,
                                fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            if (!ai.hasKey && !keyboardOpen) {
                Text("AI anahtarın Ayarlar → Yapay zekâ bölümünde kayıtlı değil. Buradan geçici anahtar da girebilirsin.",
                    color = LiveGold, fontSize = 11.sp, lineHeight = 15.sp)
                OutlinedTextField(
                    value = ephemeralKey, onValueChange = { ephemeralKey = it.take(300) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("${ai.provider.label} API anahtarı", color = LiveMint) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = darkFieldColors(),
                )
            }
            if (error.isNotBlank()) Text(error, color = Color(0xFFFFB6A3), fontSize = 12.sp)
            if (audioError.isNotBlank()) Text(audioError, color = Color(0xFFFFD5A1), fontSize = 12.sp)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ai.draft, onValueChange = { ai.draft = it.take(maxMessageLength) },
                    enabled = canEdit, modifier = Modifier.weight(1f),
                    maxLines = 3, singleLine = false,
                    placeholder = { Text("Öğretmenine bir şey sor…", color = LiveWhite.copy(alpha = .63f)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(ai.draft) }),
                    colors = darkFieldColors(),
                )
                Button(
                    onClick = { send(ai.draft) }, enabled = canSend && ai.draft.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LiveGold, contentColor = LiveBackground),
                ) {
                    Text("Gönder", fontWeight = FontWeight.Bold)
                }
            }
            if (!keyboardOpen) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = ::startListening, enabled = canEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF387B74), contentColor = LiveWhite),
                        modifier = Modifier.weight(1f)) {
                        Text(if (listening) "Dinleniyor" else "🎙 Konuş", fontSize = 14.sp)
                    }
                    OutlinedButton(onClick = {
                        if (currentTurn != null) {
                            audioError = ""
                            if (speaking == currentTurn.id) audio?.stop() else audio?.play(currentTurn)
                        }
                    }, enabled = !sending && currentTurn != null, modifier = Modifier.weight(1f)) {
                        Text(if (speaking != null) "■ Durdur" else "▶ Dinle", color = LiveWhite, fontSize = 14.sp)
                    }
                    TextButton(onClick = { ai.autoRead = !ai.autoRead; if (!ai.autoRead) audio?.stop() }) {
                        Text(if (ai.autoRead) "Oto ses ✓" else "Oto ses —", color = LiveGold, fontSize = 12.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${ai.provider.label} · ${ai.level} · ${ai.personality.label}",
                        color = LiveMint, fontSize = 11.sp)
                    TextButton(onClick = { audio?.stop(); openTextChat() }) {
                        Text("Ayrıntılı metin sohbeti ›", color = LiveGold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (settingsOpen) {
        Dialog(onDismissRequest = { settingsOpen = false }) {
            Surface(color = Color(0xFFF6F2E8), shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(17.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Öğretmenini özelleştir", fontSize = 20.sp, color = Forest, fontWeight = FontWeight.Bold)
                    Text("Karakter", color = Ink, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(TeacherAvatar.Mert).forEach { person ->
                            FilterChip(selected = person == ai.avatar, onClick = { ai.chooseAvatar(person) },
                                label = { Text(person.label) })
                        }
                    }
                    Text("Öğretme tarzı", color = Ink, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        TeacherPersonality.entries.forEach { p ->
                            FilterChip(selected = p == ai.personality, onClick = { ai.choosePersonality(p) },
                                label = { Text(p.label) })
                        }
                    }
                    Text("Ders türü", color = Ink, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                        TeacherMode.entries.forEach { m ->
                            Box(Modifier.weight(1f)) {
                                FilterChip(selected = m == ai.mode, onClick = { ai.mode = m },
                                    label = { Text(m.label, fontSize = 10.sp) })
                            }
                        }
                    }
                    Text("İngilizce seviyesi", color = Ink, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        conversationLevels.forEach { level ->
                            FilterChip(selected = ai.level == level, onClick = { ai.level = level },
                                label = { Text(level) })
                        }
                    }
                    Text("Yanıt dili", color = Ink, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ConversationLanguage.entries.forEach { language ->
                            FilterChip(selected = ai.language == language,
                                onClick = { ai.language = language; audio?.stop() }, label = { Text(language.label) })
                        }
                    }
                    OutlinedTextField(
                        value = ai.teacherNotes, onValueChange = ai::saveTeacherNotes,
                        label = { Text("Öğretmenin hatırlamasını istediğin hedefler") },
                        modifier = Modifier.fillMaxWidth(), maxLines = 5, minLines = 2,
                    )
                    Text("Hafıza ve sohbet geçmişi yalnızca bu cihazdaki ilgili profile aittir. " +
                        "AI yanıtı için mesajlar seçtiğin sağlayıcıya gönderilir.",
                        fontSize = 11.sp, color = Muted)
                    Button(onClick = { settingsOpen = false }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Forest)) { Text("Tamam") }
                }
            }
        }
    }

    if (threadsOpen) {
        Dialog(onDismissRequest = { threadsOpen = false }) {
            Surface(color = Color(0xFFF8F5EF), shape = RoundedCornerShape(21.dp),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sohbet geçmişin", color = Forest, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = ai.search, onValueChange = ai::searchThreads,
                        label = { Text("Geçmişte ara") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    val list = rememberLazyListState()
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp), state = list,
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (ai.threads.isEmpty()) item { Text("Henüz kayıtlı bir sohbet yok.", color = Muted) }
                        items(ai.threads, key = TeacherThread::id) { thread ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { audio?.stop(); ai.openThread(thread.id); threadsOpen = false },
                                    modifier = Modifier.weight(1f)) {
                                    Text(thread.title, color = Forest, maxLines = 2)
                                }
                                TextButton(onClick = { deleteCandidate = thread }) { Text("Sil", color = ErrorInk) }
                            }
                        }
                    }
                    Button(onClick = { ai.newConversation(); threadsOpen = false },
                        modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
                        Text("Yeni sohbet başlat")
                    }
                    TextButton(onClick = { threadsOpen = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Kapat", color = Forest)
                    }
                }
            }
        }
    }

    deleteCandidate?.let { thread ->
        AlertDialog(onDismissRequest = { deleteCandidate = null },
            title = { Text("Sohbet silinsin mi?") },
            text = { Text("Bu sohbet yalnızca bu cihazdaki hesabından silinir.") },
            confirmButton = { TextButton(onClick = { ai.deleteThread(thread.id); deleteCandidate = null }) {
                Text("Sil")
            } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Vazgeç") } })
    }
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LiveWhite,
    unfocusedTextColor = LiveWhite,
    disabledTextColor = LiveWhite.copy(alpha = .5f),
    cursorColor = LiveGold,
    focusedBorderColor = LiveGold,
    unfocusedBorderColor = LiveMint.copy(alpha = .55f),
    focusedLabelColor = LiveGold,
    unfocusedLabelColor = LiveMint,
)
