package com.kelimebulmacasi.a1

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ConversationScreen(ai: AiSession, session: AppSession, back: () -> Unit) {
    val context = LocalContext.current
    var scene3d by rememberSaveable { mutableStateOf(true) }
    if (scene3d) {
        LiveTeacherScreen(ai, session, back, openTextChat = { scene3d = false })
        return
    }
    val focus = LocalFocusManager.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val scope = rememberCoroutineScope()
    val messages = rememberLazyListState()
    var apiKey by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var audioError by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf("") }
    var recording by rememberSaveable { mutableStateOf(false) }
    var speaking by remember { mutableStateOf<Long?>(null) }
    var audio by remember { mutableStateOf<ConversationAudio?>(null) }
    var levelMenu by remember { mutableStateOf(false) }
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    var notesOpen by rememberSaveable { mutableStateOf(false) }
    var ipaLookup by rememberSaveable(session.profileKey) { mutableStateOf("") }
    val ipaSpeech = remember(context) { WordPronunciation(context) }
    DisposableEffect(ipaSpeech) { onDispose { ipaSpeech.close() } }
    LaunchedEffect(session.preferences.speechRate, session.preferences.speechVolume,
        session.preferences.pronunciationMode, session.preferences.nativeVoiceUs,
        session.preferences.nativeVoiceUk, session.preferences.allowOnlineVoice) {
        ipaSpeech.configure(session.preferences.speechRate, session.preferences.speechVolume)
        ipaSpeech.configureEngine(session.preferences)
    }

    var deleteCandidate by remember { mutableStateOf<TeacherThread?>(null) }
    val canEdit = !sending && !recording
    val canSend = canEdit && (ai.hasKey || apiKey.isNotBlank()) && ai.draft.isNotBlank()

    DisposableEffect(context, lifecycle) {
        val playback = ConversationAudio(context, { speaking = it }, { audioError = it },
            englishVoice = { session.preferences.voice },
            settings = { session.preferences.speechRate to session.preferences.speechVolume },
            pronunciationPreferences = { session.preferences })
        audio = playback
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) playback.stop()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            playback.close()
        }
    }

    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        recording = false
        if (result.resultCode == Activity.RESULT_OK) {
            val transcript = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull { it.isNotBlank() }?.trim()
            if (transcript == null) error = "Konuşma anlaşılmadı. Mikrofona tekrar dokun."
            else ai.draft = listOf(ai.draft.trim(), transcript).filter(String::isNotEmpty).joinToString(" ").take(maxMessageLength)
        }
    }

    fun listen() {
        if (!canEdit) return
        audio?.stop()
        focus.clearFocus()
        error = ""
        audioError = ""
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                if (ai.language == ConversationLanguage.English) session.preferences.voice.locale.toLanguageTag() else ai.language.tag)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "${ai.language.label} konuş")
        try {
            recording = true
            microphone.launch(intent)
        } catch (_: ActivityNotFoundException) {
            recording = false
            error = "Cihazda ses tanıma uygulaması bulunamadı. Mesajını yazabilirsin."
        } catch (_: SecurityException) {
            recording = false
            error = "Ses tanıma açılamadı. Cihazın mikrofon iznini kontrol et."
        }
    }

    fun send() {
        if (!canSend || sending) return
        val message = ai.draft.trim()
        val key = apiKey
        audio?.stop()
        focus.clearFocus()
        error = ""
        audioError = ""
        pending = message
        sending = true
        scope.launch {
            try {
                val optionalContext = try { session.teacherLearningContext() } catch (_: Exception) { "" }
                val turn = ai.reply(key, message, optionalContext, session.profileName)
                ai.draft = ""
                apiKey = ""
                if (ai.autoRead && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) audio?.play(turn)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (problem: WordRequestException) {
                error = problem.message.orEmpty()
            } catch (problem: Exception) {
                error = conversationFailureMessage(problem)
            } finally {
                pending = ""
                sending = false
            }
        }
    }

    LaunchedEffect(ai.turns.lastOrNull()?.id, sending, ai.hasKey) {
        if (!ai.hasKey) {
            messages.scrollToItem(0)
        } else {
            val count = ai.turns.size + if (sending) 1 else 0
            if (count > 0) messages.scrollToItem(count - 1)
        }
    }

    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 760.dp).fillMaxSize().padding(horizontal = 15.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TextButton(onClick = back, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("‹ Geri", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)
                }
                Text("Kişisel AI Öğretmen", modifier = Modifier.weight(1f), color = Ink, fontFamily = Serif, fontSize = 20.sp, lineHeight = 23.sp)
                TextButton(onClick = { audio?.stop(); scene3d = true }, contentPadding = PaddingValues(horizontal = 5.dp)) {
                    Text("3D Ders", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = {
                    audio?.stop()
                    ai.newConversation()
                    error = ""
                    audioError = ""
                }, enabled = canEdit && ai.turns.isNotEmpty(), contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("Yeni sohbet", fontFamily = Sans)
                }
            }

            BodyText("Sohbet profili: ${session.profileName}")
            if (!keyboardVisible) TeacherAvatarPicker(ai.avatar, ai::chooseAvatar,
                playing = speaking != null, enabled = canEdit)
            if (!keyboardVisible) Row(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { historyOpen = !historyOpen; if (historyOpen) ai.searchThreads("") },
                    enabled = canEdit) { Text(if (historyOpen) "Geçmişi kapat" else "Sohbet geçmişi") }
                OutlinedButton(onClick = { notesOpen = !notesOpen }, enabled = canEdit) {
                    Text(if (notesOpen) "Notları kapat" else "Öğrenme hafızam")
                }
            }
            if (historyOpen && !keyboardVisible) {
                OutlinedTextField(ai.search, onValueChange = ai::searchThreads,
                    label = { Text("Geçmişte ara") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Column(Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (ai.threads.isEmpty()) BodyText("Aramayla eşleşen kayıt bulunamadı.")
                    for (thread in ai.threads) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = {
                                audio?.stop()
                                ai.openThread(thread.id)
                                historyOpen = false
                                error = ""
                            }, enabled = canEdit, modifier = Modifier.weight(1f)) {
                                Text(thread.title, maxLines = 1, fontFamily = Sans)
                            }
                            TextButton(onClick = { deleteCandidate = thread }, enabled = canEdit) { Text("Sil") }
                        }
                    }
                }
            }
            if (notesOpen && !keyboardVisible) {
                OutlinedTextField(ai.teacherNotes, onValueChange = ai::saveTeacherNotes,
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4,
                    label = { Text("Hedeflerim, zorlandığım konular") },
                    supportingText = { Text("Yalnızca bu oyuncunun cihazındaki öğretmen hafızasına kaydedilir.") })
            }
            if (!keyboardVisible && ai.language == ConversationLanguage.English) {
                OutlinedTextField(
                    value = ipaLookup, onValueChange = { ipaLookup = it.take(64) },
                    label = { Text("IPA / telaffuz için İngilizce kelime", fontFamily = Sans) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                if (ipaLookup.isNotBlank()) {
                    WordIpa(ipaLookup, session.preferences)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { ipaSpeech.pronounce(listOf(ipaLookup), EnglishVoice.American, replay = true) },
                            enabled = session.preferences.pronunciation) { Text("US dinle") }
                        OutlinedButton(onClick = { ipaSpeech.pronounce(listOf(ipaLookup), EnglishVoice.British, replay = true) },
                            enabled = session.preferences.pronunciation) { Text("UK dinle") }
                    }
                    ipaSpeech.error?.let { BodyText(it, ErrorInk) }
                }
            }
            if (!keyboardVisible) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (learningMode in TeacherMode.entries) FilterChip(
                    selected = ai.mode == learningMode, onClick = { ai.mode = learningMode }, enabled = canEdit,
                    label = { Text(learningMode.label, fontFamily = Sans) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LightLime),
                )
            }
            if (!keyboardVisible) Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                for (language in ConversationLanguage.entries) FilterChip(
                    selected = ai.language == language, onClick = { ai.language = language; audio?.stop() }, enabled = canEdit,
                    label = { Text(language.label, fontFamily = Sans) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LightLime),
                )
                if (ai.language == ConversationLanguage.English) Box {
                    OutlinedButton(onClick = { levelMenu = true }, enabled = canEdit, contentPadding = PaddingValues(horizontal = 9.dp)) {
                        Text("${ai.level} ▾", fontFamily = Sans)
                    }
                    DropdownMenu(expanded = levelMenu, onDismissRequest = { levelMenu = false }) {
                        for (level in conversationLevels) DropdownMenuItem(
                            text = { Text(level) }, onClick = { ai.level = level; levelMenu = false },
                        )
                    }
                }
            }
            if (!keyboardVisible) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = ai.autoRead, onClick = {
                    ai.autoRead = !ai.autoRead
                    if (!ai.autoRead) audio?.stop()
                }, label = { Text("Yanıtları sesli oku", fontFamily = Sans) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LightLime))
                Text("AI: ${ai.provider.label}", fontFamily = Sans)
                if (ai.hasKey) Text("Anahtar Ayarlar'da kayıtlı", fontFamily = Sans)
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(), state = messages, contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!ai.hasKey) item(key = "key") {
                    Column(Modifier.fillMaxWidth()) { GeminiKeyInput(apiKey, { apiKey = it }, canEdit, { error = it }, ai.provider) }
                }
                if (ai.turns.isEmpty() && !sending) item {
                    BodyText(if (ai.language == ConversationLanguage.English) "Örnek: Can we practise ordering food?"
                        else "Örnek: Kendimi İngilizce nasıl tanıtabilirim?")
                }
                items(ai.turns, key = ConversationTurn::id) { turn ->
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        ConversationBubble(turn.message, "Sen")
                        ConversationBubble(turn.answer, "Asistan") {
                            TextButton(onClick = {
                                audioError = ""
                                if (speaking == turn.id) audio?.stop() else audio?.play(turn)
                            }, enabled = !recording && !sending) {
                                Text(if (speaking == turn.id) "Durdur" else "Dinle", fontFamily = Sans, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (sending) item(key = "pending") {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        ConversationBubble(pending, "Sen")
                        BodyText("Yanıt hazırlanıyor…")
                    }
                }
            }

            if (error.isNotBlank() || audioError.isNotBlank()) {
                BodyText(error.ifBlank { audioError }, ErrorInk)
            }
            OutlinedTextField(
                ai.draft, { ai.draft = it.take(maxMessageLength) }, modifier = Modifier.fillMaxWidth(),
                enabled = canEdit, minLines = 1, maxLines = 4,
                label = { Text("Mesajın", fontFamily = Sans) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = ::listen, enabled = canEdit, modifier = Modifier.weight(1f)) {
                    Text(if (recording) "Dinleniyor…" else "Mikrofon", fontFamily = Sans, fontWeight = FontWeight.Bold)
                }
                Button(onClick = ::send, enabled = canSend, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = White)) {
                    Text(if (sending) "Bekle…" else "Gönder", fontFamily = Sans, fontWeight = FontWeight.Bold)
                }
            }
            if (!keyboardVisible) Text("Son 8 sohbet turu, öğretmen notları ve öğrenme özeti seçili AI sağlayıcısına gönderilir. " +
                    "'Hatırla: ...' yazdığınız tercihler bu oyuncunun öğretmen hafızasına kaydedilir. Sohbet cihazda profil bazındadır.", Modifier.padding(bottom = 5.dp),
                color = Muted, fontFamily = Sans, fontSize = 11.sp, lineHeight = 14.sp)
        }
        deleteCandidate?.let { thread ->
            AlertDialog(
                onDismissRequest = { deleteCandidate = null },
                title = { Text("Sohbet silinsin mi?") },
                text = { Text("Bu sohbet ve mesajları yalnızca bu oyuncunun cihazından kalıcı olarak silinir.") },
                confirmButton = {
                    TextButton(onClick = {
                        ai.deleteThread(thread.id)
                        deleteCandidate = null
                    }) { Text("Sil") }
                },
                dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Vazgeç") } },
            )
        }
    }
}

@Composable
private fun ConversationBubble(message: String, label: String, action: (@Composable () -> Unit)? = null) {
    val mine = action == null
    val shape = RoundedCornerShape(17.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.TopEnd else Alignment.TopStart) {
        Column(
            Modifier.fillMaxWidth(if (mine) .87f else .96f).background(if (mine) LightLime else White, shape)
                .border(1.dp, Line, shape).padding(horizontal = 13.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(label, color = Forest, fontFamily = Sans, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            SelectionContainer { BodyText(message, Ink) }
            if (action != null) Box(Modifier.align(Alignment.End)) { action() }
        }
    }
}
