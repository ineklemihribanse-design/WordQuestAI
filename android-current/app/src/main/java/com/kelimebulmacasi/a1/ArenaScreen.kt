package com.kelimebulmacasi.a1

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

private enum class ArenaPage { Menu, Config, SignIn, Create, Join, Room, History, Ranking, Stats, Ai, Tutor }

@Composable
internal fun ArenaScreen(session: ArenaSession, preferences: WordQuestPreferences,
    pronunciation: WordPronunciation, leave: () -> Unit, firebase: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(ArenaPage.Menu.name) }
    var mode by rememberSaveable { mutableStateOf(ArenaMode.Fast.id) }
    var roomCode by rememberSaveable { mutableStateOf("") }
    var tutorCode by rememberSaveable { mutableStateOf("") }
    var afterLogin by rememberSaveable { mutableStateOf(ArenaPage.Menu.name) }
    val here = ArenaPage.valueOf(page)
    val context = LocalContext.current

    fun open(target: ArenaPage) {
        session.clearStatus()
        page = target.name
    }
    fun signInFor(target: ArenaPage) {
        afterLogin = target.name
        if (!session.configured) open(ArenaPage.Config)
        else if (!session.signedIn) open(ArenaPage.SignIn)
        else open(target)
    }
    fun enter(code: String) {
        roomCode = code
        open(ArenaPage.Room)
    }
    fun back() {
        when (here) {
            ArenaPage.Menu -> leave()
            ArenaPage.Tutor -> if (tutorCode.isNotBlank()) open(ArenaPage.Room) else open(ArenaPage.Menu)
            else -> open(ArenaPage.Menu)
        }
    }
    fun share(code: String) {
        val text = "WordQuest AI yarışmasına katıl. Oda kodu $code\n$ARENA_SCHEME://arena/join/$code"
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text), "WordQuest AI daveti"))
    }

    BackHandler(here != ArenaPage.Menu) { back() }
    LaunchedEffect(session.pendingInvite) {
        if (session.pendingInvite.isNotBlank()) signInFor(ArenaPage.Join)
    }
    LaunchedEffect(here, session.signedIn) {
        if (!session.signedIn && here !in setOf(ArenaPage.Menu, ArenaPage.Config, ArenaPage.SignIn)) {
            afterLogin = here.name
            page = ArenaPage.SignIn.name
        } else if (session.signedIn) {
            when (here) {
                ArenaPage.Menu -> session.loadHome()
                ArenaPage.Create -> session.loadCreate()
                ArenaPage.History, ArenaPage.Ranking -> session.loadHistory()
                ArenaPage.Stats -> session.loadStats()
                ArenaPage.Tutor -> session.loadTutor()
                ArenaPage.Ai -> session.loadHome()
                else -> Unit
            }
        }
    }
    val observeCode = roomCode.takeIf {
        session.signedIn && (here == ArenaPage.Room || (here == ArenaPage.Tutor && tutorCode == it))
    }.orEmpty()
    DisposableEffect(observeCode) {
        session.observe(observeCode)
        onDispose { session.observe("") }
    }

    when (here) {
        ArenaPage.Menu -> ScreenPage("Alternatif yarışma sunucusu", "Supabase + Cloudflare isteğe bağlı", ::back) {
            ArenaOutline("Firebase yarışma merkezine geç", action = firebase)
            val player = session.profile?.text("name").orEmpty()
            if (session.signedIn && player.isNotBlank()) {
                ArenaLabel(player)
                if (session.room?.text("code")?.isNotBlank() == true) {
                    ArenaOutline("Son odayı aç") { enter(session.room!!.getString("code")) }
                }
            } else {
                BodyText("İki Android cihazdan aynı odaya bağlan. Yarışma hesabı çevrimiçi bölümde kullanılır.")
                ArenaOutline(if (session.configured) "Yarışma hesabıyla giriş" else "Sunucuyu bağla") { signInFor(ArenaPage.Menu) }
            }
            PrimaryAction("Yarışma Odası Oluştur", { signInFor(ArenaPage.Create) })
            ArenaOutline("Odaya Katıl") { signInFor(ArenaPage.Join) }
            ArenaOutline("Arkadaşını Davet Et") {
                val code = session.room?.text("code").orEmpty()
                if (session.signedIn && code.isNotBlank() && session.room?.text("phase") == "lobby") share(code)
                else signInFor(ArenaPage.Create)
            }
            ArenaLabel("Yarışmalar")
            for (kind in ArenaMode.entries) MenuItem(kind.label, kind.caption, when (kind) {
                ArenaMode.Yds -> "YDS"
                ArenaMode.Oral -> "Aa"
                ArenaMode.Daily -> "↻"
                else -> "◇"
            }) {
                mode = kind.id
                signInFor(ArenaPage.Create)
            }
            ArenaLabel("Oyuncu hesabı")
            MenuItem("Yarışma Geçmişi", "Son odalar, sonuçlar ve ortak sohbet", "↗") { signInFor(ArenaPage.History) }
            MenuItem("Liderlik Tablosu", "Ortak yarışmalardaki puanlar ve galibiyetler", "★") { signInFor(ArenaPage.Ranking) }
            MenuItem("Kişisel Yarışma İstatistikleri", "Başarı oranı ve tekrar listesi", "✓") { signInFor(ArenaPage.Stats) }
            MenuItem("Özel AI Öğretmeni", "Çalışma önerileri ve özel sohbet geçmişi", "✦") {
                tutorCode = ""
                signInFor(ArenaPage.Tutor)
            }
            MenuItem("AI Sağlayıcıları", "Öğretmen ve hakem için model ve kota", "AI") { signInFor(ArenaPage.Ai) }
            ArenaOutline("Sunucu Ayarları") {
                afterLogin = ArenaPage.Menu.name
                open(ArenaPage.Config)
            }
            if (session.signedIn) TextButton(onClick = session::signOut) { Text("Yarışma hesabından çık") }
            SessionFeedback(session)
        }

        ArenaPage.Config -> ArenaConfigScreen(session, ::back) {
            if (session.signedIn) open(ArenaPage.valueOf(afterLogin)) else open(ArenaPage.SignIn)
        }
        ArenaPage.SignIn -> ArenaSignInScreen(session, ::back) { open(ArenaPage.valueOf(afterLogin)) }
        ArenaPage.Create -> ArenaCreateScreen(session, ArenaMode.of(mode), ::back, { mode = it.id }, ::enter)
        ArenaPage.Join -> ScreenPage("Odaya Katıl", "", ::back) {
            var draft by rememberSaveable(session.pendingInvite) { mutableStateOf(session.pendingInvite) }
            OutlinedTextField(
                draft, { draft = it.take(150) }, Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("Oda kodu veya davet bağlantısı") },
            )
            PrimaryAction("Katıl", { session.join(draft) { session.room?.text("code")?.let(::enter) } }, !session.busy && draft.isNotBlank())
            SessionFeedback(session)
        }
        ArenaPage.Room -> {
            val current = session.room?.takeIf { it.text("code") == roomCode }
            if (current == null) ScreenPage("Yarışma Odası", roomCode, ::back) {
                BodyText("Oda durumu yükleniyor…")
                SessionFeedback(session)
            }
            else ArenaRoomScreen(session, current, preferences, pronunciation, ::back, { share(roomCode) }, {
                tutorCode = roomCode
                open(ArenaPage.Tutor)
            }, {
                session.moveToRematch { session.room?.text("code")?.let(::enter) }
            })
        }
        ArenaPage.History -> ArenaHistory(session, ::back, ::enter)
        ArenaPage.Ranking -> ArenaRank(session, ::back)
        ArenaPage.Stats -> ArenaStats(session, ::back) {
            tutorCode = ""
            open(ArenaPage.Tutor)
        }
        ArenaPage.Ai -> ArenaAiSettings(session, ::back)
        ArenaPage.Tutor -> ArenaTutorScreen(session, tutorCode, ::back)
    }

}

@Composable
private fun ArenaConfigScreen(session: ArenaSession, back: () -> Unit, done: () -> Unit) {
    val settings = remember { session.settingValues() }
    var url by rememberSaveable { mutableStateOf(settings[0]) }
    var key by rememberSaveable { mutableStateOf(settings[1]) }
    var worker by rememberSaveable { mutableStateOf(settings[2]) }
    ScreenPage("Sunucu Ayarları", "", back) {
        BodyText("Projedeki YARISMA_KURULUMU.md dosyasında Supabase ve Cloudflare kurulumu adım adım anlatılıyor.")
        OutlinedTextField(url, { url = it.take(240) }, Modifier.fillMaxWidth(), label = { Text("Supabase proje adresi") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
        OutlinedTextField(key, { key = it.take(4096) }, Modifier.fillMaxWidth(), label = { Text("Supabase Publishable Key") }, singleLine = true)
        BodyText("Buraya yalnızca yayımlanabilir anahtarı gir. Servis ve AI anahtarları Cloudflare Secrets alanına girilir.")
        OutlinedTextField(worker, { worker = it.take(240) }, Modifier.fillMaxWidth(), label = { Text("Cloudflare workers.dev adresi") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
        PrimaryAction("Ayarları kaydet", { session.configure(url, key, worker, done) }, !session.busy)
        if (session.configured) ArenaOutline("Kayıtlı AI sunucusunu kontrol et", !session.busy, session::testWorker)
        SessionFeedback(session)
    }
}

@Composable
private fun ArenaSignInScreen(session: ArenaSession, back: () -> Unit, done: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    ScreenPage("Yarışma Hesabı", "Kurulumda tanımlanan iki hesaptan biriyle giriş yap.", back) {
        OutlinedTextField(email, { email = it.take(254) }, Modifier.fillMaxWidth(), label = { Text("E-posta") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(password, { password = it.take(256) }, Modifier.fillMaxWidth(), label = { Text("Şifre") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = PasswordVisualTransformation())
        PrimaryAction("Giriş yap", { session.signIn(email, password) { password = ""; done() } },
            email.isNotBlank() && password.isNotBlank() && !session.busy)
        SessionFeedback(session)
    }
}

@Composable
private fun ArenaCreateScreen(session: ArenaSession, mode: ArenaMode, back: () -> Unit,
    change: (ArenaMode) -> Unit, enter: (String) -> Unit,
) {
    val modes = session.packs.map { it.mode }.distinct()
    val levels = session.packs.filter { it.mode == mode }.map { it.level }.distinct()
    var selectedLevel by rememberSaveable(mode) { mutableStateOf("") }
    val level = selectedLevel.takeIf { it in levels } ?: ("B1".takeIf { it in levels } ?: levels.firstOrNull()).orEmpty()
    val packs = session.packs.filter { it.mode == mode && it.level == level }
    var selectedTopic by rememberSaveable(mode, level) { mutableStateOf("") }
    val pack = packs.firstOrNull { it.topic == selectedTopic } ?: packs.firstOrNull()
    var count by rememberSaveable(mode, level, selectedTopic) { mutableIntStateOf(10) }
    var seconds by rememberSaveable(mode) { mutableIntStateOf(if (mode == ArenaMode.Daily) 120 else 180) }
    var training by rememberSaveable(mode) { mutableStateOf(false) }
    var referee by rememberSaveable(mode) { mutableStateOf(mode == ArenaMode.Oral) }
    val num = count.takeIf { it in pack?.counts.orEmpty() } ?: pack?.counts?.firstOrNull() ?: 0
    ScreenPage("Yarışma Odası Oluştur", "", back) {
        ArenaPicker("Oyun", mode.label, modes.map { it.label }) { label -> modes.firstOrNull { it.label == label }?.let(change) }
        ArenaPicker("Seviye", level, levels) { selectedLevel = it }
        ArenaPicker("Konu", pack?.topic.orEmpty().ifBlank { "Karışık" },
            packs.map { it.topic.ifBlank { "Karışık" } }) { selectedTopic = it.takeUnless { it == "Karışık" }.orEmpty() }
        ArenaPicker("Soru sayısı", num.toString(), pack?.counts.orEmpty().map(Int::toString)) { count = it.toInt() }
        ArenaPicker("Süre", "${seconds / 60} dk", (if (mode == ArenaMode.Daily) listOf(120) else listOf(60, 120, 180, 300, 600, 900))
            .map { "${it / 60} dk" }) { seconds = it.substringBefore(' ').toInt() * 60 }
        if (mode !in listOf(ArenaMode.Daily, ArenaMode.Oral)) ArenaCheck("Eğitim modu · ipucuyla doğru cevap 5 puan", training) { training = it }
        if (mode != ArenaMode.Oral) ArenaCheck("İsteğe bağlı AI hakemi", referee) { referee = it }
        if (mode == ArenaMode.Oral) BodyText("Sözlü sınavın sonunda yapılandırılmış ücretsiz bir AI hakemi gerekir. İki oyuncu cevap metinlerinin değerlendirilmesini onaylar.")
        if (pack == null && !session.busy && session.status.isBlank()) BodyText("Bu mod için sunucuda soru paketi bulunamadı.")
        PrimaryAction("Odayı oluştur", {
            if (pack != null) session.create(pack, num, seconds, training, referee) {
                session.room?.text("code")?.let(enter)
            }
        }, pack != null && !session.busy)
        SessionFeedback(session)
    }
}

@Composable
private fun ArenaHistory(session: ArenaSession, back: () -> Unit, enter: (String) -> Unit) {
    val rooms = session.history.rows("rooms")
    ScreenPage("Yarışma Geçmişi", "", back) {
        if (rooms.isEmpty() && !session.busy) BodyText("Yarışma odaların burada listelenir.")
        for (row in rooms) {
            val code = row.getString("code")
            val mode = ArenaMode.of(row.text("mode"))
            val outcome = when (row.text("phase")) {
                "finished" -> "${row.obj("score").optInt("points")} puan"
                "playing" -> "Devam ediyor"
                "grading" -> "Hakem bekleniyor"
                else -> "Lobi"
            }
            MenuItem(mode.label, "$code · ${row.text("level")} · $outcome", "↗") { enter(code) }
        }
        ArenaOutline("Listeyi yenile", !session.busy, session::loadHistory)
        SessionFeedback(session)
    }
}

@Composable
private fun ArenaRank(session: ArenaSession, back: () -> Unit) {
    ScreenPage("Liderlik Tablosu", "Ortak tamamlanan yarışmalara göre", back) {
        val players = session.history.rows("rank")
        if (players.isEmpty() && !session.busy) BodyText("İlk yarışma tamamlandığında puanlar burada görünür.")
        players.forEachIndexed { index, row ->
            MenuItem(row.getString("name"), "${row.optInt("points")} puan · ${row.optInt("wins")} galibiyet · ${row.optInt("matches")} maç", "${index + 1}")
        }
        SessionFeedback(session)
    }
}

@Composable
private fun ArenaStats(session: ArenaSession, back: () -> Unit, tutor: () -> Unit) {
    val row = session.history.rows("rank").firstOrNull { it.optBoolean("me") }
    ScreenPage("Kişisel Yarışma İstatistikleri", "", back) {
        if (row != null) {
            val good = row.optInt("correct")
            val bad = row.optInt("wrong")
            val percentage = if (good + bad == 0) 0 else 100 * good / (good + bad)
            BodyText("${row.optInt("matches")} tamamlanan · ${row.optInt("wins")} galibiyet · %$percentage doğru")
            BodyText("$good doğru · $bad yanlış · ${row.optInt("points")} puan")
        }
        ArenaLabel("Tekrar et")
        if (session.review.isEmpty() && !session.busy) BodyText("Tamamladığın yarışmalardaki yanlış kelimeler burada birikir.")
        for (word in session.review) MenuItem(word.text("answer"), "${word.text("clue")} · ${word.optInt("misses")} yanlış", "↻")
        ArenaOutline("Öğretmenle tekrar et", action = tutor)
        ArenaOutline("İstatistikleri yenile", !session.busy) { session.loadStats() }
        SessionFeedback(session)
    }
}

internal data class ArenaModel(val provider: String, val id: String, val label: String)
private val arenaModels = listOf(
    ArenaModel("none", "", "Kapalı"),
    ArenaModel("gemini", "gemini-3.1-flash-lite", "Gemini · 3.1 Flash-Lite"),
    ArenaModel("groq", "openai/gpt-oss-20b", "Groq · GPT-OSS 20B"),
    ArenaModel("groq", "openai/gpt-oss-120b", "Groq · GPT-OSS 120B"),
    ArenaModel("openrouter", "openrouter/free", "OpenRouter · Free"),
    ArenaModel("cloudflare", "@cf/meta/llama-3.1-8b-instruct-fp8", "Workers AI · Llama 3.1 8B FP8"),
)

@Composable
private fun ArenaAiSettings(session: ArenaSession, back: () -> Unit) {
    val profile = session.profile
    fun label(id: String) = arenaModels.firstOrNull { it.id == profile?.text(id) }?.label ?: arenaModels.first().label
    var tutor by rememberSaveable(profile?.text("model")) { mutableStateOf(label("model")) }
    var referee by rememberSaveable(profile?.text("refereeModel")) { mutableStateOf(label("refereeModel")) }
    var fallback by rememberSaveable(profile?.optBoolean("fallback")) { mutableStateOf(profile?.optBoolean("fallback") == true) }
    ScreenPage("AI Sağlayıcıları", "", back) {
        BodyText("Bu seçimler yarışma hesabına aittir. Anahtarlar sunucuda oyuncu başına ayrı yapılandırılır.")
        ArenaPicker("Özel öğretmen", tutor, arenaModels.map(ArenaModel::label)) { tutor = it }
        ArenaPicker("Ortak hakem", referee, arenaModels.map(ArenaModel::label)) { referee = it }
        ArenaCheck("Sorun olduğunda yapılandırılmış başka ücretsiz sağlayıcıyı dene", fallback) { fallback = it }
        PrimaryAction("Tercihleri kaydet", {
            val teacher = arenaModels.first { it.label == tutor }
            val judge = arenaModels.first { it.label == referee }
            session.saveAi(JSONObject().put("provider", teacher.provider).put("model", teacher.id)
                .put("refereeProvider", judge.provider).put("refereeModel", judge.id).put("fallback", fallback))
        }, !session.busy && profile != null)
        val usage = profile?.obj("usage") ?: JSONObject()
        ArenaLabel("Bugünkü kullanım")
        BodyText("Bu hesap için günlük 30 deneme, sağlayıcı başına 15 deneme ayrılır. Sayaç UTC gece yarısında yenilenir.")
        for (name in listOf("gemini", "groq", "openrouter", "cloudflare")) BodyText("$name · ${usage.optInt(name)} / 15")
        ArenaOutline("Kullanımı yenile", !session.busy, session::loadHome)
        SessionFeedback(session)
    }
}

@Composable
private fun ArenaTutorScreen(session: ArenaSession, code: String, back: () -> Unit) {
    var consent by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }
    val active = session.room?.let { it.text("code") == code && it.text("phase") in listOf("playing", "grading") } == true
    ScreenPage("Özel AI Öğretmeni", "", back) {
        BodyText("Öğretmen sohbeti bu yarışma hesabına özeldir. Çalışma önerilerinde son sohbetler ve kendi tekrar listen kullanılır.")
        ArenaCheck("Bu istek için kendi çalışma verilerimin seçili AI sağlayıcısına gönderilmesini kabul ediyorum.", consent) { consent = it }
        if (active) {
            BodyText("Yarışma sırasında genel İngilizce çalışabilir veya oyun ekranında eğitim ipucu alabilirsin.")
            for ((topic, title) in listOf("word-order" to "Cümle sırası", "tenses" to "Zamanlar",
                "connectors" to "Bağlaçlar", "study" to "Kelime çalışma yöntemi")) {
                ArenaOutline(title, consent && !session.aiBusy) { session.tutorRequest(code, "", topic) {} }
            }
        } else {
            OutlinedTextField(message, { message = it.take(900) }, Modifier.fillMaxWidth(),
                label = { Text("İngilizce hakkında sor veya yarışmanı değerlendir") }, minLines = 2)
            PrimaryAction(if (session.aiBusy) "Öğretmen yanıtlıyor…" else "Öğretmene gönder",
                { session.tutorRequest(code, message, "") { message = "" } }, consent && message.isNotBlank() && !session.aiBusy)
        }
        SessionFeedback(session)
        ArenaLabel("Özel sohbet geçmişi")
        if (session.tutor.isEmpty() && !session.busy) BodyText("Sohbetin burada kaydedilir.")
        for (turn in session.tutor.asReversed()) {
            ArenaLabel(turn.text("prompt").removePrefix("@wq: "))
            BodyText(turn.text("reply"))
        }
    }
}

@Composable
internal fun ArenaLabel(text: String) {
    Text(text, color = Ink, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 17.sp)
}

@Composable
internal fun ArenaOutline(text: String, enabled: Boolean = true, action: () -> Unit) {
    OutlinedButton(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(text, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun ArenaPicker(label: String, value: String, choices: List<String>, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        ArenaOutline(listOf(label, value).filter(String::isNotBlank).joinToString(" · ") + " ▾", choices.isNotEmpty()) { expanded = true }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            for (choice in choices) DropdownMenuItem(text = { Text(choice, fontFamily = Sans) },
                onClick = { expanded = false; select(choice) })
        }
    }
}

@Composable
internal fun ArenaCheck(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Checkbox(checked, change)
        BodyText(label)
    }
}

@Composable
internal fun SessionFeedback(session: ArenaSession) {
    if (session.busy) BodyText("Bağlanıyor…")
    if (session.aiBusy) BodyText("AI yanıtı hazırlanıyor…")
    if (session.status.isNotBlank()) BodyText(session.status, Forest)
    if (session.aiStatus.isNotBlank()) BodyText(session.aiStatus, ErrorInk)
}
