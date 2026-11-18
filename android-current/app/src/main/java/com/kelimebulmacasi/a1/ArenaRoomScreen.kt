package com.kelimebulmacasi.a1

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

@Composable
internal fun ArenaRoomScreen(
    session: ArenaSession, state: JSONObject, preferences: WordQuestPreferences,
    pronunciation: WordPronunciation, back: () -> Unit, share: () -> Unit,
    teacher: () -> Unit, rematch: () -> Unit,
) {
    val code = state.getString("code")
    val mode = ArenaMode.of(state.text("mode"))
    val phase = state.text("phase")
    val seat = state.optInt("seat")
    val players = state.rows("players")
    var tick by remember(state.optLong("serverMs")) { mutableLongStateOf(session.time()) }
    var oralConsent by rememberSaveable(code) { mutableStateOf(false) }
    var summaryConsent by rememberSaveable(code) { mutableStateOf(false) }
    val starting = state.optLong("startMs") - tick
    val remaining = max(0, state.optLong("endMs") - tick + 999) / 1000
    val count = state.optInt("size")
    val mine = players.firstOrNull { it.optInt("seat") == seat }
    val ready = mine?.optBoolean("ready") == true
    val isReady = session.connected && !session.busy
    LaunchedEffect(code, phase) {
        if (phase == "playing" || phase == "grading") {
            while (true) {
                tick = session.time()
                delay(300)
            }
        }
    }
    ScreenPage(mode.label, "$code · ${state.text("level")} · ${state.text("topic").ifBlank { "Karışık" }}", back) {
        ArenaScore(players, seat, count)
        if (phase != "finished") BodyText(
            when {
                !session.connected -> "Bağlantı bekleniyor… Sunucudaki oda korunuyor."
                session.live -> "Canlı bağlantı"
                else -> "Oda durumu düzenli yenileniyor."
            },
        )
        when (phase) {
            "lobby" -> {
                val training = if (state.optBoolean("training")) "Eğitim" else "Rekabetçi"
                val referee = if (state.optBoolean("referee")) "AI hakemi açık" else "AI hakemi kapalı"
                BodyText("$count soru · ${state.optInt("seconds") / 60} dakika · $training · $referee")
                BodyText(when (mode) {
                    ArenaMode.Team -> "İlk doğru cevap takım puanına eklenir. Her oyuncu kelime başına bir cevap gönderir."
                    ArenaMode.Duel -> "Her kelime için bir cevap hakkı. Eşit puanla tahtayı bitirenler arasında en az bir saniye fark varsa süre belirler."
                    ArenaMode.Oral -> "Her iki oyuncu aynı soruları sırayla yanıtlar. Anlam 4, kelime kullanımı 3, dilbilgisi 3 puan."
                    else -> "Her doğru cevap 10, yanlış cevap 0 puan. Eşit puanda beraberlik."
                })
                if (state.optBoolean("training")) BodyText("İlk harfi açılan doğru cevap 5 puan.")
                if (mode == ArenaMode.Oral && !ready) ArenaCheck(
                    "Sınav cevap metnimin ortak AI hakemine gönderilmesini kabul ediyorum.", oralConsent,
                ) { oralConsent = it }
                PrimaryAction(if (ready) "Hazırım · geri al" else "Hazırım", {
                    session.updateRoom("ready", JSONObject().put("ready", !ready).put("consent", oralConsent))
                }, isReady && (ready || mode != ArenaMode.Oral || oralConsent))
                ArenaOutline("Arkadaşını Davet Et", action = share)
            }
            "playing" -> {
                when {
                    starting > 0 -> ArenaLabel("Başlangıca ${(starting + 999) / 1000} sn")
                    remaining == 0L -> ArenaLabel("Süre doldu · sonuçlar bekleniyor")
                    else -> {
                        ArenaLabel("${remaining / 60} dk ${remaining % 60} sn kaldı")
                        if (mode == ArenaMode.Team) BodyText("${state.rows("team").size} / $count kelime · ${state.optInt("teamPoints")} takım puanı")
                        if (mode == ArenaMode.Duel || mode == ArenaMode.Team) {
                            ArenaBoard(state, isReady, mode == ArenaMode.Team, preferences, pronunciation, session)
                        } else ArenaQuestion(state, isReady, mode, preferences, session::updateRoom)
                    }
                }
            }
            "grading" -> {
                ArenaLabel("Sözlü sınav değerlendirmesi")
                BodyText("İki oyuncunun cevap metinleri açıklanabilir ölçütlerle birlikte değerlendirilir. Hazır bir sağlayıcısı olan oyuncu hakemi çağırabilir.")
                PrimaryAction(if (session.aiBusy) "Hakem değerlendiriyor…" else "AI hakemine gönder", { session.referee("grade") },
                    isReady && !session.aiBusy)

                var confirmClose by rememberSaveable(code) { mutableStateOf(false) }
                ArenaOutline("Değerlendirmesiz kapat", isReady && !session.aiBusy) { confirmClose = true }
                if (confirmClose) androidx.compose.material3.AlertDialog(
                    onDismissRequest = { confirmClose = false },
                    title = { Text("Değerlendirmesiz kapat") },
                    text = { BodyText("Bu oturum puansız olarak yarışma geçmişinde saklanır.") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            confirmClose = false
                            session.updateRoom("close-oral")
                        }) { Text("Kapat") }
                    },
                    dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmClose = false }) { Text("Geri dön") } },
                )
            }
            "finished" -> {
                ArenaResults(state, mode, players)
                if (state.optBoolean("referee") && !state.optBoolean("gradingSkipped") &&
                    state.isNull("refereeAt")) {
                    ArenaCheck("Ortak yarışma istatistiklerini AI hakemine gönder.", summaryConsent) { summaryConsent = it }
                    ArenaOutline("Hakem yorumu iste", summaryConsent && isReady && !session.aiBusy) { session.referee("summary") }
                }
                ArenaOutline(if (state.text("rematchCode").isBlank()) "Yeniden yarış" else "Rövanş odasına geç", isReady, rematch)
            }
        }
        SessionFeedback(session)
        ArenaOutline("Özel AI Öğretmeni", action = teacher)
        val latestChat = state.rows("chat")
        val chat = (session.olderChat + latestChat).distinctBy { it.optLong("id") }
        ArenaChat(code, chat, players, isReady, session::updateRoom,
            if (latestChat.size >= 60 && latestChat.isNotEmpty()) ({
                session.loadOlderChat(chat.first().getLong("id"))
            }) else null,
            if (phase == "finished" && state.optBoolean("referee") && !state.optBoolean("gradingSkipped")) ({ message, done ->
                session.referee("referee-question", message, done)
            }) else null,
            session.aiBusy,
        )
    }
}

@Composable
private fun ArenaScore(players: List<JSONObject>, own: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        for (number in 1..2) {
            val p = players.firstOrNull { it.optInt("seat") == number }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(15.dp)).background(LightLime)
                .padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                ArenaLabel(p?.text("name")?.let { if (number == own) "$it · Sen" else it } ?: "Oyuncu $number")
                if (p != null) {
                    BodyText("${p.optInt("points")} puan", Forest)
                    BodyText("${p.optInt("correct")} doğru · ${p.optInt("wrong")} yanlış")
                    BodyText("${p.optInt("answered")}/$total · " +
                        (if (p.optBoolean("online")) "Bağlı" else "Çevrimdışı") +
                        if (p.optBoolean("ready") && p.optInt("answered") == 0) " · Hazır" else "")
                } else BodyText("Katılım bekleniyor")
            }
        }
    }
}

@Composable
private fun ArenaQuestion(
    state: JSONObject, enabled: Boolean, mode: ArenaMode, preferences: WordQuestPreferences,
    send: (String, JSONObject, () -> Unit) -> Unit,
) {
    val item = state.rows("items").firstOrNull()
    val ownTurn = mode != ArenaMode.Oral || state.optInt("turnSeat") == state.optInt("seat")
    if (item == null) { BodyText("Diğer oyuncunun cevapları bekleniyor."); return }
    val index = item.optInt("ordinal")
    val code = state.text("code")
    val audio = mode == ArenaMode.Oral
    var draft by rememberSaveable(code,index) { mutableStateOf("") }
    var voiceStatus by rememberSaveable(code,index) { mutableStateOf("") }
    val context = LocalContext.current
    val audioPreferences by androidx.compose.runtime.rememberUpdatedState(preferences)
    val speaker = remember(context, audio) {
        if (audio) ConversationAudio(context, {}, { voiceStatus = it },
            englishVoice = { audioPreferences.voice },
            settings = { audioPreferences.speechRate to audioPreferences.speechVolume },
            pronunciationPreferences = { audioPreferences }) else null
    }
    DisposableEffect(speaker) { onDispose { speaker?.close() } }
    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val recognized = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (recognized.isNotBlank()) draft = recognized.take(900)
            else voiceStatus = "Ses metne çevrilemedi."
        }
    }
    val prompt = item.text("prompt")
    ArenaLabel("Soru $index/${state.optInt("size")}")
    BodyText(prompt, Ink)
    if (audio) ArenaOutline("Soruyu dinle") {
        speaker?.play(ConversationTurn(index.toLong(), "", prompt, ConversationLanguage.English, JSONArray()))
    }
    if (!ownTurn) { BodyText("Diğer oyuncunun sırası."); return }
    val hints = state.obj("hints")
    if (!hints.isNull(index.toString())) BodyText("İlk harf · ${hints.text(index.toString())}", Forest)
    val options = item.optJSONArray("options")
    if (options != null && options.length() != 0) for (i in 0 until options.length()) {
        val option = options.optString(i)
        ArenaOutline(option, enabled) {
            send("answer", JSONObject().put("ordinal", index).put("answer", option)) {}
        }
    } else {
        OutlinedTextField(draft, { draft = it.take(if (audio) 900 else 150) }, Modifier.fillMaxWidth(),
            label = { Text(if (audio) "İngilizce cevap" else "İngilizce karşılığı") }, singleLine = !audio, minLines = if (audio) 2 else 1)
        if (audio) ArenaOutline("Sesle yaz", enabled) {
            voiceStatus = ""
            try {
                recognizer.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, audioPreferences.voice.locale.toLanguageTag())
                    .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true))
            } catch (_: ActivityNotFoundException) { voiceStatus = "Cihazda ses tanıma hizmeti bulunamadı." }
        }
        if (voiceStatus.isNotBlank()) BodyText(voiceStatus, ErrorInk)
        PrimaryAction("Cevabı gönder", {
            send("answer", JSONObject().put("ordinal", index).put("answer", draft)) { draft = "" }
        }, enabled && draft.isNotBlank())
    }
    if (state.optBoolean("training") && !audio && hints.isNull(index.toString())) ArenaOutline("İlk harfi aç · 5 puan", enabled) {
        send("hint", JSONObject().put("ordinal", index)) {}
    }
}

private fun JSONObject.arenaCells(): List<Cell> {
    val row = optInt("row")
    val column = optInt("col")
    val across = text("direction") == "A"
    return List(optInt("length")) { Cell(row + if (across) 0 else it, column + if (across) it else 0) }
}

@Composable
private fun ArenaBoard(
    state: JSONObject, enabled: Boolean, shared: Boolean, preferences: WordQuestPreferences,
    pronunciation: WordPronunciation, session: ArenaSession,
) {
    val items = state.rows("items")
    if (items.isEmpty()) return
    val code = state.text("code")
    val known = items.associate { it.optInt("ordinal") to it.arenaCells() }
    val mine = state.rows("mine").associateBy { it.optInt("ordinal") }
    val team = state.rows("team").associateBy { it.optInt("ordinal") }
    val locked = items.mapNotNull { word ->
        val number = word.optInt("ordinal")
        val supplied = team[number]?.text("answer") ?: mine[number]?.text("solution")
        supplied?.takeIf { it.isNotEmpty() }?.let { known.getValue(number).zip(englishLetters(it).toList()) }
    }.flatten().toMap()
    val occupied = known.values.flatten().toSet()
    val numbered = items.associate { it.arenaCells().first() to it.optInt("number") }
    var selected by rememberSaveable(code) { mutableIntStateOf(1) }
    val chosen = items.firstOrNull { it.optInt("ordinal") == selected } ?: items.first()
    val cells = known.getValue(chosen.optInt("ordinal"))
    var draft by rememberSaveable(code, selected) { mutableStateOf("") }
    var lastSpokenWord by rememberSaveable(code) { mutableStateOf<String?>(null) }
    val audioPreferences by androidx.compose.runtime.rememberUpdatedState(preferences)
    fun available(number: Int) = number !in mine && number !in team
    val h = state.optInt("height").coerceIn(1, 30)
    val w = state.optInt("width").coerceIn(1, 30)
    val hints = state.obj("hints")
    val clue = "${chosen.optInt("number")} ${if (chosen.text("direction") == "A") "Yatay" else "Dikey"} · ${chosen.text("prompt")}"
    ArenaLabel(clue)
    if (!hints.isNull(selected.toString())) BodyText("İlk harf · ${hints.text(selected.toString())}", Forest)
    Box(Modifier.fillMaxWidth().height((h * 26 + 12).dp.coerceAtMost(330.dp))
        .clip(RoundedCornerShape(13.dp)).background(Board).horizontalScroll(rememberScrollState())
        .verticalScroll(rememberScrollState()).padding(6.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            for (row in 0 until h) Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                for (column in 0 until w) {
                    val cell = Cell(row, column)
                    val style = RoundedCornerShape(3.dp)
                    val marked = cell in locked
                    val inUse = cell in occupied
                    val color = when {
                        marked -> LightLime
                        cell in cells -> Lime
                        inUse -> White
                        else -> Block
                    }
                    val select = if (inUse) Modifier.clickable(
                        indication = null, interactionSource = remember { MutableInteractionSource() }, role = Role.Button,
                    ) {
                        val at = known.keys.filter { cell in known.getValue(it) }
                        selected = if (selected in at && at.size == 2) at.first { it != selected } else
                            at.firstOrNull(::available) ?: at.first()
                    }.semantics { contentDescription = "${row + 1}. satır ${column + 1}. sütun" } else Modifier
                    Box(Modifier.size(25.dp).clip(style).background(color).then(select),
                        contentAlignment = Alignment.Center) {
                        numbered[cell]?.let {
                            val font = cellFont(25.dp, .32f, .40f, 7f, 11f)
                            Text(it.toString(), Modifier.align(Alignment.TopStart).padding(start = 1.dp),
                                fontFamily = Sans, color = Forest, fontSize = font, lineHeight = font)
                        }
                        locked[cell]?.let { Text(it.toString(), fontFamily = Sans, color = Ink,
                            fontSize = cellFont(25.dp, .60f, .66f, 8f, 18f), fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
    if (available(selected)) {
        BodyText(cells.joinToString(" ") { locked[it]?.toString() ?: "_" })
        OutlinedTextField(draft, { draft = it.take(150) }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Kelimenin tamamını yaz") })
        PrimaryAction("Cevabı gönder", {
            val sentOrdinal = selected
            session.updateRoom("answer", JSONObject().put("ordinal", sentOrdinal).put("answer", draft)) {
                val result = session.room?.takeIf { it.text("code") == code }?.rows("mine")
                    ?.firstOrNull { it.optInt("ordinal") == sentOrdinal && it.optBoolean("correct") }
                val answer = result?.text("solution").orEmpty()
                if (answer.isNotBlank()) {
                    lastSpokenWord = answer
                    if (audioPreferences.pronunciation && audioPreferences.autoPronunciation) {
                        pronunciation.pronounce(listOf(answer), audioPreferences.voice)
                    }
                }
                draft = ""
                if (selected < items.size) selected = items.map { it.optInt("ordinal") }.firstOrNull { it > selected && available(it) } ?: selected
            }
        }, enabled && draft.isNotBlank())
        if (state.optBoolean("training") && hints.isNull(selected.toString())) ArenaOutline("İlk harfi aç · 5 puan", enabled) {
            session.updateRoom("hint", JSONObject().put("ordinal", selected))
        }
    } else BodyText(if (shared && selected in team || mine[selected]?.optBoolean("correct") == true) "Doğru · harfler sabitlendi" else "Yanlış cevap kaydedildi.")
    val replay = mine[selected]?.takeIf { it.optBoolean("correct") }?.text("solution")
        ?.takeIf { it.isNotBlank() } ?: lastSpokenWord
    if (preferences.pronunciation && replay != null) {
        TextButton(onClick = { pronunciation.pronounce(listOf(replay), preferences.voice, replay = true) }) {
            Text("Tekrar dinle", fontFamily = Sans)
        }
    }
    pronunciation.error?.let { BodyText(it, ErrorInk) }
    ArenaPicker("İpucu seç", "${chosen.optInt("number")} ${chosen.text("direction").let { if (it == "A") "Yatay" else "Dikey" }}",
        items.map { "${it.optInt("ordinal")}. ${it.optInt("number")} ${if (it.text("direction") == "A") "Yatay" else "Dikey"} · ${it.text("prompt")}" }
    ) { selected = it.substringBefore('.').toInt() }
}

@Composable
private fun ArenaResults(state: JSONObject, mode: ArenaMode, players: List<JSONObject>) {
    val winner = state.optInt("winner")
    when {
        state.optBoolean("gradingSkipped") -> ArenaLabel("Hakem puanı verilmedi")
        mode == ArenaMode.Team -> ArenaLabel("${state.optInt("teamPoints")} takım puanı · ${state.rows("team").size}/${state.optInt("size")} kelime")
        winner == 0 -> ArenaLabel("Berabere")
        else -> ArenaLabel("${players.firstOrNull { it.optInt("seat") == winner }?.text("name").orEmpty()} kazandı")
    }
    for (p in players) {
        val answers = p.optInt("answered")
        val accuracy = if (answers != 0) p.optInt("correct") * 100 / answers else 0
        BodyText("${p.text("name")} · %$accuracy başarı · ${p.optInt("averageMs") / 1000.0} sn ortalama · ${p.optInt("hintCount")} ipucu")
    }
    if (state.rows("breakdown").isNotEmpty()) {
        ArenaLabel("Konu ve kelime türüne göre")
        for (row in state.rows("breakdown")) BodyText(
            "${row.optInt("seat")}. oyuncu · ${row.text("topic")} · ${row.text("kind")} · ${row.optInt("correct")}/${row.optInt("attempts")}",
        )
    }
    val answers = state.rows("mine")
    if (answers.isNotEmpty()) ArenaLabel(if (mode == ArenaMode.Oral) "Kendi değerlendirmem" else "Kendi cevaplarım")
    for (answer in answers) {
        val rubric = answer.optJSONObject("rubric")
        if (rubric != null) {
            BodyText(answer.text("prompt"), Ink)
            BodyText(answer.text("entry"))
            BodyText("Anlam ${rubric.optInt("meaning")}/4 · kelime ${rubric.optInt("vocabulary")}/3 · dilbilgisi ${rubric.optInt("grammar")}/3")
            BodyText(rubric.text("feedback"))
        } else if (mode != ArenaMode.Oral && !answer.optBoolean("correct")) {
            BodyText("${answer.text("prompt")} → ${answer.text("solution")}", Ink)
            if (answer.text("explanation").isNotBlank()) BodyText(answer.text("explanation"))
        }
    }
}

@Composable
private fun ArenaChat(
    code: String, chat: List<JSONObject>, players: List<JSONObject>, enabled: Boolean,
    send: (String, JSONObject, () -> Unit) -> Unit, older: (() -> Unit)?,
    ask: ((String, () -> Unit) -> Unit)?, aiBusy: Boolean,
) {
    var message by rememberSaveable(code) { mutableStateOf("") }
    val shape = RoundedCornerShape(13.dp)
    ArenaLabel("Ortak sohbet")
    if (chat.isNotEmpty()) Column(
        Modifier.fillMaxWidth().height((chat.size * 62).dp.coerceIn(74.dp, 240.dp)).clip(shape).border(1.dp, Line, shape)
            .background(White).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        for (msg in chat.asReversed()) {
            val sender = msg.optInt("seat")
            val who = if (sender == 0) {
                if (msg.text("body").startsWith("AI hakemi · ")) "AI hakemi" else "Yarışma"
            } else players.firstOrNull { it.optInt("seat") == sender }?.text("name") ?: "Oyuncu $sender"
            Column(Modifier.defaultMinSize(minHeight = 36.dp)) {
                Text(who, color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                BodyText(msg.text("body").removePrefix("AI hakemi · "), Ink)
            }
        }
    }
    if (older != null) ArenaOutline("Önceki mesajları yükle", enabled, older)
    OutlinedTextField(message, { message = it.take(600) }, Modifier.fillMaxWidth(),
        label = { Text("Ortak mesaj") }, minLines = 1, maxLines = 3)
    ArenaOutline("Mesajı gönder", enabled && message.isNotBlank()) {
        send("chat", JSONObject().put("message", message)) { message = "" }
    }
    if (ask != null) ArenaOutline("Bu mesajı ortak AI hakemine sor", enabled && !aiBusy && message.isNotBlank()) {
        ask(message) { message = "" }
    }
}
