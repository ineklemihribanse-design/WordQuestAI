package com.kelimebulmacasi.a1

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class Page { Home, NewGame, Modes, Packages, Topic, Progress, Learning, Account, Settings, Custom, Editor, Game, Verbs, VerbGame, Backup, Assistant, Arena, FirebaseArena, Practice, YdsCenter, Insights, PremiumArena }

@Composable
internal fun CrosswordApp(session: AppSession, ai: AiSession, arena: ArenaSession, pronunciation: WordPronunciation) {
    val appContext = LocalContext.current
    LaunchedEffect(session.profileKey) { ai.bindProfile(appContext, session.profileKey) }
    var page by rememberSaveable { mutableStateOf(Page.Home.name) }
    var origin by rememberSaveable { mutableStateOf(Page.Home.name) }
    var modeName by rememberSaveable { mutableStateOf(GameMode.Topics.name) }
    var level by rememberSaveable { mutableStateOf("A1") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddedGames by rememberSaveable { mutableStateOf(false) }
    var editorOrigin by rememberSaveable { mutableStateOf(Page.Custom.name) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var destinationMode by rememberSaveable { mutableStateOf<String?>(null) }
    var destinationLevel by rememberSaveable { mutableStateOf("") }
    var destinationCategory by rememberSaveable { mutableStateOf("") }
    var gameId by rememberSaveable { mutableStateOf<String?>(null) }
    var verbMode by rememberSaveable { mutableStateOf(VerbExercise.Match.name) }
    var verbPool by rememberSaveable { mutableStateOf(VerbPool.All.name) }
    var verbSeed by rememberSaveable { mutableIntStateOf(0) }
    var newGameLevel by rememberSaveable { mutableStateOf("A1") }
    var newGameTopic by rememberSaveable { mutableStateOf<String?>(null) }
    var newGameKind by rememberSaveable { mutableStateOf<String?>(null) }
    var newGameOrigin by rememberSaveable { mutableStateOf(Page.Home.name) }
    var packageOrigin by rememberSaveable { mutableStateOf(Page.Modes.name) }
    val topicState = rememberSaveableStateHolder()
    LaunchedEffect(session.preferences.speechRate, session.preferences.speechVolume) {
        pronunciation.configure(session.preferences.speechRate, session.preferences.speechVolume)
    }
    LaunchedEffect(session.preferences.pronunciationMode, session.preferences.nativeVoiceUs,
        session.preferences.nativeVoiceUk, session.preferences.allowOnlineVoice, session.profileKey) {
        pronunciation.configureEngine(session.preferences)
    }
    DisposableEffect(page, session.profileKey) {
        onDispose { pronunciation.stop() }
    }

    fun go(target: Page) { page = target.name }

    fun open(game: Game) {
        origin = page
        gameId = game.id
        go(Page.Game)
    }

    fun edit(destination: Destination?, game: Game? = null) {
        editorOrigin = page
        editingId = game?.id
        destinationMode = destination?.mode?.name
        destinationLevel = destination?.level.orEmpty()
        destinationCategory = destination?.category.orEmpty()
        go(Page.Editor)
    }

    fun back() {
        when (Page.valueOf(page)) {
            Page.Game -> {
                session.closeGame()
                page = origin
                gameId = null
            }
            Page.Packages -> { page = packageOrigin }
            Page.Topic -> go(Page.Packages)
            Page.NewGame -> { page = newGameOrigin }
            Page.Editor -> { page = editorOrigin }
            Page.VerbGame -> go(Page.Verbs)
            Page.Backup -> go(Page.Custom)
            Page.Home -> Unit
            else -> go(Page.Home)
        }
    }

    fun packages(mode: GameMode) {
        packageOrigin = page
        modeName = mode.name
        level = if (mode == GameMode.Yds) "Kolay" else "A1"
        category = null
        showAddedGames = false
        go(Page.Packages)
    }

    fun setup(selected: Destination? = null) {
        newGameOrigin = page
        newGameLevel = selected?.level ?: "A1"
        newGameTopic = selected?.category?.takeIf { selected.mode != GameMode.Kinds && it.isNotBlank() }
        newGameKind = selected?.category?.takeIf { selected.mode == GameMode.Kinds && it.isNotBlank() }
        topicState.removeState("new-game")
        go(Page.NewGame)
    }

    LaunchedEffect(arena.pendingInvite) {
        if (arena.pendingInvite.isNotBlank()) go(Page.Arena)
    }
    BackHandler(page != Page.Home.name) { back() }
    when (Page.valueOf(page)) {
        Page.Home -> HomeScreen(
            session, { setup() }, { go(Page.Modes) }, { packages(GameMode.Yds) }, ::open, { go(Page.Progress) },
            { go(Page.Account) }, { go(Page.Custom) }, { go(Page.Verbs) }, { go(Page.Assistant) }, { go(Page.PremiumArena) }, { go(Page.PremiumArena) },
            { go(Page.Learning) }, { go(Page.Practice) }, { go(Page.YdsCenter) }, { go(Page.Insights) }, { go(Page.Settings) },
        )
        Page.NewGame -> topicState.SaveableStateProvider("new-game") {
            NewGameScreen(session, newGameLevel, newGameTopic, newGameKind, ::back, ::open)
        }
        Page.Modes -> ScreenPage("Hazır Paketler", "Bir paket açıp hazır bulmacalardan birini seç", ::back) {
            MenuItem("Konuya Göre", "İnsan vücudu, aile, ev ve daha fazlası", "◇") { packages(GameMode.Topics) }
            MenuItem("Kelime Türüne Göre", "İsimler, fiiller, sıfatlar ve zarflar", "Aa") { packages(GameMode.Kinds) }
            MenuItem("YDS Hazırlık", "Akademik kelimeler, bağlaçlar ve kalıp ifadeler", "YDS") { packages(GameMode.Yds) }
        }
        Page.Packages -> PackageScreen(
            session, GameMode.valueOf(modeName), level,
            { level = it; category = null },
            { category = it; showAddedGames = false; go(Page.Topic) }, { setup(Destination(GameMode.valueOf(modeName), level, "")) }, ::back,
        )
        Page.Topic -> {
            val selected = Destination(GameMode.valueOf(modeName), level, category.orEmpty())
            topicState.SaveableStateProvider(modeName + ":" + level + ":" + category) {
                TopicScreen(session, selected, showAddedGames, ::back, { edit(selected) }, { edit(selected, it) }, ::open, { setup(selected) })
            }
        }
        Page.Progress -> ProgressScreen(session, ::back, ::open)
        Page.Learning -> LearningScreen(session, pronunciation, ::back)
        Page.Practice -> PracticeHubScreen(session, pronunciation, ::back, { go(Page.Assistant) }, ::open)
        Page.YdsCenter -> YdsCenterScreen(session, ::back) { prompt ->
            ai.mode = TeacherMode.Lesson
            ai.draft = prompt
            go(Page.Assistant)
        }
        Page.Insights -> LearningDashboardScreen(session, ai, ::back,
            { go(Page.YdsCenter) }, { go(Page.Learning) }, { go(Page.Practice) }, { prompt ->
                ai.mode = TeacherMode.Lesson
                ai.draft = prompt
                go(Page.Assistant)
            }, { go(Page.Account) }, { go(Page.Backup) })
        Page.Account -> AccountScreen(session, ai, ::back)
        Page.Settings -> WordQuestSettings(session, pronunciation, ai, ::back)
        Page.Custom -> ScreenPage("Bulmacalarım", "Kendi kelimelerinle oyna", ::back) {
            PrimaryAction("Bulmaca ekle", { edit(null) })
            OutlinedButton(onClick = { go(Page.Backup) }, modifier = Modifier.fillMaxWidth()) {
                Text("Yedekle ve aktar", fontFamily = Sans, fontWeight = FontWeight.Bold)
            }
            if (session.customGames.isEmpty()) BodyText("Kameradan, galeriden, PDF’den veya kendi kelime listenle bulmaca oluştur.")
            for (game in session.customGames) GameItem(game, session.record(game), ::open) { edit(it.destination, it) }
        }
        Page.Backup -> BackupScreen(session, ::back)
        Page.Assistant -> if (ai.boundProfile == session.profileKey) {
            ConversationScreen(ai, session, ::back)
        } else ScreenPage("AI Öğretmen", "Profil hazırlanıyor", ::back) {
            BodyText("Öğretmen geçmişi yükleniyor…")
        }
        Page.Arena -> ArenaScreen(arena, session.preferences, pronunciation, ::back, { go(Page.PremiumArena) })
        Page.PremiumArena -> PremiumArenaScreen(session, ::back, { go(Page.Account) },
            { go(Page.Learning) }, { prompt ->
                ai.mode = TeacherMode.Lesson
                ai.draft = prompt
                go(Page.Assistant)
            })
        Page.FirebaseArena -> FirebaseLiveArenaScreen(session, ::back, { go(Page.Arena) }, { go(Page.Account) })
        Page.Editor -> {
            val existing = editingId?.let(session::game)
            if (editingId != null && existing == null) {
                ScreenPage("Bulmacayı düzenle", "", ::back) { BodyText("Bu bulmaca açılamadı.") }
            } else {
                CustomEditor(
                    destinationMode?.let { Destination(GameMode.valueOf(it), destinationLevel, destinationCategory) },
                    existing, ::back, ai.hasKey, ai::forgetKey, ai::suggestWords, ai.provider,
                ) { games, expectedModifiedAt ->
                    val saved = session.saveCustom(games, expectedModifiedAt)
                    if (saved) {
                        showAddedGames = editorOrigin == Page.Topic.name
                        page = editorOrigin
                    }
                    saved
                }
            }
        }
        Page.Game -> {
            val game = gameId?.let(session::game)
            if (game == null) {
                ScreenPage("Bulmaca", "", ::back) { BodyText("Bu bulmaca açılamadı.") }
            } else {
                val saved = session.record(game) ?: Progress()
                val round = remember(game, saved.layout) { session.round(game, saved) }
                LaunchedEffect(game.id) { session.recordOpen(game) }
                key(game.progressId, game.modifiedAt, saved.layout, saved.layoutAt) {
                    val crossword = remember { Crossword(round.puzzle, round.onBoard(saved)) }
                    val clock = remember(game.id, session.profileKey) {
                        PuzzleClockStore(appContext, session.profileKey).load(game.id)
                    }
                    PuzzleScreen(game, crossword, round.onBoard(saved), ::back, { session.shuffle(game) },
                        { signal -> session.recordLearning(game, signal) }, session.preferences, pronunciation,
                        clock = clock,
                    ) { progress ->
                        round.onBoard(session.save(game, round.inStore(progress, saved.layoutAt)))
                    }
                }
            }
        }
        Page.Verbs -> VerbMenu(VerbPool.valueOf(verbPool), { verbPool = it.name }, ::back) { mode ->
            verbMode = mode.name
            verbSeed = Random.nextInt()
            go(Page.VerbGame)
        }
        Page.VerbGame -> VerbPracticeScreen(
            session.verbs, VerbPool.valueOf(verbPool), VerbExercise.valueOf(verbMode), verbSeed, ::back,
            { verbSeed = Random.nextInt() }, session.preferences,
        )
    }
    UpdateOverlayDialogs()
    // App-wide live notification: visible over any WordQuest screen without navigating
    // to Account or the competition panel. Dialogs cannot be missed while playing.
    val socialAlert = session.social.activeAlert
    if (socialAlert != null && session.accountName != null) {
        val isRoomInvite = socialAlert.type == SocialAlertType.ROOM_INVITE
        AlertDialog(
            onDismissRequest = session.social::closeAlert,
            title = { Text(if (isRoomInvite) "Oyun daveti geldi!" else "Arkadaşlık isteği geldi!") },
            text = { Text(if (isRoomInvite)
                "${socialAlert.senderName} seni Arena'daki oyununa davet ediyor. Katılmak için kabul etmen yeterli."
                else "${socialAlert.senderName} seninle arkadaş olmak istiyor.") },
            confirmButton = {
                TextButton(onClick = {
                    if (isRoomInvite) {
                        session.social.acceptRoomAlert()
                        go(if (socialAlert.arenaType == "premium") Page.PremiumArena else Page.FirebaseArena)
                    } else session.social.acceptFriendAlert()
                }) { Text("Kabul Et") }
            },
            dismissButton = {
                TextButton(onClick = session.social::declineAlert) { Text("Reddet") }
            },
        )
    }
}

@Composable
private fun HomeScreen(
    session: AppSession,
    newGame: () -> Unit,
    packages: () -> Unit,
    yds: () -> Unit,
    open: (Game) -> Unit,
    progress: () -> Unit,
    account: () -> Unit,
    custom: () -> Unit,
    verbs: () -> Unit,
    assistant: () -> Unit,
    arena: () -> Unit,
    firebaseArena: () -> Unit,
    learning: () -> Unit,
    practice: () -> Unit,
    ydsCenter: () -> Unit,
    insights: () -> Unit,
    settings: () -> Unit,
) {
    val unfinished = session.continueGame
    ScreenPage("", "", null) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth > 620.dp
            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HeroTiles(Modifier.size(31.dp).clip(RoundedCornerShape(8.dp)).background(LightLime), true)
                        Text("Kelime Bulmacası", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = settings) {
                            Icon(Icons.Default.Settings, contentDescription = "Ayarlar", tint = Forest)
                        }
                        OutlinedButton(onClick = account) {
                            Text(if (session.accountName == null) "Hesap" else session.profileName.take(16), fontFamily = Sans, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Board)
                        .heightIn(min = if (wide) 182.dp else 174.dp),
                ) {
                    HeroTiles(
                        Modifier.align(Alignment.CenterEnd).padding(end = if (wide) 18.dp else 4.dp)
                            .size(if (wide) 188.dp else 108.dp),
                    )
                    Column(
                        Modifier.align(Alignment.CenterStart).fillMaxWidth(if (wide) .67f else .77f)
                            .padding(horizontal = if (wide) 28.dp else 19.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "WordQuest AI", fontFamily = Serif, color = White,
                            fontSize = if (wide) 54.sp else 37.sp, lineHeight = if (wide) 55.sp else 39.sp,
                        )
                        Text(
                            "Türkçe ipucunu oku,\nİngilizcesini yaz.", Modifier.padding(top = 11.dp),
                            color = Color(0xFFE0ECDA), fontFamily = Sans, fontSize = 16.sp, lineHeight = 21.sp,
                        )
                    }
                }
                if (unfinished != null) {
                    MenuItem("Devam Et", unfinished.path + " · " + unfinished.title, "↗") { open(unfinished) }
                }
                PrimaryAction("Yeni Oyun", newGame)
                val play: @Composable () -> Unit = {
                    MenuItem("Hazır Paketler", "Konuya ve kelime türüne göre bulmacalar", "◇", packages)
                    MenuItem("YDS Hazırlık", "Kolay, orta ve zor bulmacalar", "YDS", yds)
                    MenuItem("YDS Sınav Merkezi", "Paragraf, çeviri, yakın anlam ve süreli mini denemeler", "✎", ydsCenter)
                    MenuItem("Bulmacalarım", "Kendi bulmacaların ve yedekleme", "+", custom)
                }
                val study: @Composable () -> Unit = {
                    MenuItem("Arena", "A1–A2 · Bulmaca ve 5 şıklı İngilizce yarışmaları", "★", firebaseArena)
                    // The older server connection remains in source for compatibility but is hidden from Home.
                    MenuItem("Konuşma Asistanı", "Yazarak veya sesli sohbet et", "✦", assistant)
                    MenuItem("Günlük Planım", "Günlük hedefler ve mini oyunlara doğrudan geç", "✦", practice)
                    MenuItem("Kelime Hafızam", "${session.learningDueCount()} kelime tekrar bekliyor · Kişisel defter", "↻", learning)
                    MenuItem("Fiillerin 1., 2. ve 3. Hâli", "Eşleştirme, boşluk doldurma ve yazma", "V1", verbs)
                    MenuItem("Öğrenme Merkezim", "Akıllı tekrar, YDS istatistikleri, kişisel ders ve yedekleme", "◎", insights)
                }
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { play() }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { study() }
                } else {
                    play()
                    study()
                }
            }
        }
    }
}

@Composable
private fun HeroTiles(modifier: Modifier, brand: Boolean = false) {
    Canvas(modifier) {
        val side = size.minDimension / if (brand) 4.1f else 7.5f
        val step = side * 1.13f
        val layout = if (brand) listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1) else listOf(
            1 to 0, 1 to 1, 1 to 2, 1 to 3, 1 to 4, 1 to 5, 0 to 3, 2 to 3, 3 to 3, 4 to 3, 5 to 3,
            3 to 0, 3 to 1, 3 to 2, 3 to 4, 4 to 0, 5 to 0, 5 to 1, 5 to 2,
        )
        val start = if (brand) .24f else .08f
        for ((row, column) in layout) {
            val color = when {
                brand -> if (row + column == 2) Color(0xFF8BA85B) else Forest
                row == 1 && column == 3 -> Lime
                row == 3 || column == 3 -> Color(0xFF8CA783)
                else -> Color(0xFF43675A)
            }
            drawRoundRect(color, Offset(size.width*start + column*step, size.height*start + row*step),
                Size(side, side), CornerRadius(side*.13f))
        }
    }
}

@Composable
private fun PackageScreen(
    session: AppSession, mode: GameMode, level: String,
    chooseLevel: (String) -> Unit, openTopic: (String) -> Unit, newGame: () -> Unit, back: () -> Unit,
) {
    val levels = if (mode == GameMode.Yds) ydsLevels else englishLevels
    val records = session.records
    val games = session.games.filter { it.mode == mode && it.level == level }.groupBy(Game::category)
    ScreenPage(mode.title, if (mode == GameMode.Yds) "Zorluk seviyesini seç" else "İngilizce seviyesini seç", back) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            for (value in levels) {
                val active = value == level
                val shape = RoundedCornerShape(12.dp)
                Box(
                    Modifier.weight(1f).clip(shape).background(if (active) Forest else White)
                        .border(1.dp, if (active) Forest else Line, shape)
                        .clickable(role = Role.Button) { chooseLevel(value) }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(value, color = if (active) White else Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)
                }
            }
        }
        for ((group, entries) in games) {
            val complete = entries.count { records[it.id]?.completed == true }
            val regular = entries.count { !it.mixed }
            val mixed = entries.count { it.mixed }
            val caption = if (mixed > 0) "$regular bulmaca · $mixed karışık · $complete tamamlandı"
                else "$regular bulmaca · $complete tamamlandı"
            MenuItem(group, caption, if (mode == GameMode.Yds) "YDS" else "◇") { openTopic(group) }
        }
        MenuItem("$level bulmacası oluştur", "Seviyenin kelime havuzundan yeni bir tahta", "◇", newGame)
    }
}

@Composable
private fun TopicScreen(
    session: AppSession, selected: Destination, showAddedGames: Boolean,
    back: () -> Unit, add: () -> Unit, edit: (Game) -> Unit, open: (Game) -> Unit, newGame: () -> Unit,
) {
    val entries = session.games.filter { it.mode == selected.mode && it.level == selected.level && it.category == selected.category }
    val standard = entries.filterNot(Game::mixed)
    val mixed = entries.filter(Game::mixed)
    val personal = session.customGames.filter { it.destination == selected }
    val records = session.records
    var show by rememberSaveable { mutableIntStateOf(0) }
    var pendingReplay by rememberSaveable { mutableStateOf<String?>(null) }
    val scroll = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(showAddedGames) {
        if (showAddedGames) {
            show = 2
            scroll.scrollToItem(0)
        }
    }

    fun play(game: Game) {
        if (records[game.id]?.lockedWords?.size == game.wordCount) pendingReplay = game.id else open(game)
    }

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 800.dp).fillMaxWidth(), state = scroll,
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                TextButton(onClick = back) { Text("‹  Geri", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold) }
                Text(selected.category, fontFamily = Serif, color = Ink, fontSize = 36.sp, lineHeight = 40.sp)
                BodyText(if (selected.mode == GameMode.Yds) "YDS · " + selected.level else selected.level)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val choices = (mixed.ifEmpty { standard } + personal).distinctBy(Game::id)
                            val candidate = choices.randomOrNull()
                            if (candidate != null) play(candidate)
                        },
                        enabled = entries.isNotEmpty() || personal.isNotEmpty(), modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = White),
                    ) { Text("Karıştır", fontFamily = Sans, fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = add, modifier = Modifier.weight(1f)) {
                        Text("Bulmaca ekle", fontFamily = Sans, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                OutlinedButton(onClick = newGame, modifier = Modifier.fillMaxWidth()) {
                    Text("Bu paketten yeni oyun", fontFamily = Sans, fontWeight = FontWeight.SemiBold)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Alt bölümler", "Karışık", "Eklediklerim").forEachIndexed { index, label ->
                        FilterChip(
                            selected = show == index, onClick = { show = index; scope.launch { scroll.scrollToItem(0) } },
                            label = { Text(label, fontFamily = Sans, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LightLime),
                        )
                    }
                }
            }
            val visible = when (show) { 1 -> mixed; 2 -> personal; else -> standard }
            if (visible.isEmpty()) item {
                BodyText(if (show == 2) "Bu konuya henüz bulmaca eklemedin." else "Bu bölüm için listelenen bulmaca yok.")
            }
            items(visible, key = Game::id) { game ->
                GameItem(game, records[game.id], ::play, if (game.mode == GameMode.Custom) edit else null)
            }
        }
    }
    pendingReplay?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingReplay = null },
            title = { Text("Tekrar oyna", color = Ink, fontFamily = Serif) },
            text = { BodyText("Tahta boş olarak açılır. Daha önce kazandığın puanlar saklanır.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingReplay = null
                    session.game(id)?.let { game -> session.replay(game); open(game) }
                }) { Text("Tekrar oyna", fontFamily = Sans) }
            },
            dismissButton = { TextButton(onClick = { pendingReplay = null }) { Text("Vazgeç", fontFamily = Sans) } },
            containerColor = White,
        )
    }
}

@Composable
private fun ProgressScreen(session: AppSession, back: () -> Unit, open: (Game) -> Unit) {
    val records = session.records
    val played = session.games.filter { it.mode == GameMode.Generated || records[it.id]?.let { r -> r.started || r.points > 0 } == true }
        .sortedByDescending { records[it.id]?.updatedAt }
    ScreenPage("İlerlemem", "", back) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScoreCard(Modifier.weight(1f), session.totalPoints.toString(), "Toplam puan")
            ScoreCard(Modifier.weight(1f), session.finishedCount.toString(), "Tamamlanan")
        }
        BodyText("Her cevabın ilk çözümü 10 puan. Harf açarak çözdüğün cevap 5 puan.")
        if (played.isEmpty()) BodyText("Kaydettiğin ve çözmeye başladığın bulmacalar burada görünür.")
        for (game in played) GameItem(game, records[game.id], open)
    }
}

@Composable
private fun ScoreCard(modifier: Modifier, value: String, label: String) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(LightLime).padding(16.dp)) {
        Text(value, fontFamily = Serif, color = Ink, fontSize = 34.sp)
        Text(label, fontFamily = Sans, color = Forest, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun AccountScreen(session: AppSession, ai: AiSession, back: () -> Unit) {
    val appContext = LocalContext.current
    var confirmDeletion by remember { mutableStateOf(false) }
    var registering by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var nameEditing by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val social = session.social

    LaunchedEffect(social.nickname) {
        if (!nameEditing) username = social.nickname
    }
    if (confirmDeletion) AlertDialog(
        onDismissRequest = { if (!session.accountDeleteBusy) confirmDeletion = false },
        title = { Text("Hesap silme onayı") },
        text = { Text(AccountDeletionCopy.prompt) },
        confirmButton = {
            TextButton(onClick = {
                confirmDeletion = false
                session.deleteAccount { ai.bindProfile(appContext, "local_1") }
            }, enabled = !session.accountDeleteBusy) {
                Text(AccountDeletionCopy.confirm, color = ErrorInk)
            }
        },
        dismissButton = { TextButton(onClick = { confirmDeletion = false }) { Text(AccountDeletionCopy.cancel) } },
    )
    ScreenPage("Hesap", "", back) {
        val account = session.accountName
        if (account == null) {
            LocalProfilePicker(session)
            PrimaryAction("Google ile giriş", session::signIn,
                session.accountAvailable && !session.signInBusy)
            HorizontalDivider()
            BodyText("E-posta ve şifreyle hesap oluşturabilir veya giriş yapabilirsin. Kullanıcı adın tüm oyun ve sohbet bölümlerinde gösterilir.")
            TextButton(onClick = { registering = !registering }, enabled = !session.signInBusy) {
                Text(if (registering) "Zaten hesabım var · Giriş yap" else "Yeni hesap oluştur")
            }
            if (registering) OutlinedTextField(username, { username = it.take(ArenaNickname.maximumLength) },
                modifier = Modifier.fillMaxWidth(), label = { Text("Kullanıcı adın") }, singleLine = true)
            OutlinedTextField(email, { email = it.take(254) }, modifier = Modifier.fillMaxWidth(),
                label = { Text("E-posta") }, singleLine = true)
            OutlinedTextField(password, { password = it.take(128) }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Şifre (en az 8 karakter)") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation())
            PrimaryAction(if (registering) "Kullanıcı adı, e-posta ve şifreyle kayıt ol" else "E-posta ve şifreyle giriş yap", {
                if (registering) session.registerWithPassword(username, email, password)
                else session.signInWithPassword(email, password)
                password = ""
            }, session.accountAvailable && !session.signInBusy && email.isNotBlank() &&
                password.length >= (if (registering) 8 else 1) &&
                (!registering || ArenaNickname.isValid(username)))
            if (session.signInBusy) CircularProgressIndicator(Modifier.size(26.dp), color = Forest, strokeWidth = 2.dp)
            session.accountError?.let { BodyText(it, ErrorInk) }
            OutlinedButton(onClick = back, modifier = Modifier.fillMaxWidth()) { Text("Misafir olarak devam et") }
            BodyText("Misafir kayıtların cihazında saklanır. Hesap şifreleri Firebase Authentication tarafından yönetilir; uygulama içinde saklanmaz.")
        } else {
            MenuItem(session.profileName, session.accountStatus.ifEmpty { "Kayıtlı hesap" }, "✓")
            BodyText("Oturum: $account")
            BodyText("Kullanıcı adı, sohbet ve yarışma odalarında ortak kullanılır; dilediğin zaman değiştirebilirsin.")
            OutlinedTextField(username, { username = it.take(ArenaNickname.maximumLength); nameEditing = true },
                modifier = Modifier.fillMaxWidth(), label = { Text("Kullanıcı adın") }, singleLine = true,
                enabled = social.enabled && social.nameLoaded && !social.nameSaving)
            PrimaryAction(if (social.nameSaving) "Kaydediliyor…" else "Kullanıcı adını kaydet", {
                nameEditing = false
                social.saveName(username)
            }, social.enabled && social.nameLoaded && !social.nameSaving &&
                ArenaNickname.isValid(username) && ArenaNickname.normalize(username) != social.nickname)
            if (social.nickname.isBlank()) BodyText("Arkadaşlık ve yarışma için önce kullanıcı adını belirle.")
            else BodyText("Görünecek adın: ${social.nickname}", Forest)
            HorizontalDivider()
            FriendsPanel(social)
            HorizontalDivider()
            OutlinedButton(onClick = { session.signOut(); back() }, modifier = Modifier.fillMaxWidth(),
                enabled = !session.accountDeleteBusy) { Text("Çıkış yap") }
            BodyText("Hesabını silmeden önce ilerlemeni yedekleyebilirsin.")
            OutlinedButton(onClick = { confirmDeletion = true }, modifier = Modifier.fillMaxWidth(),
                enabled = !session.accountDeleteBusy) { Text("WordQuest AI hesabımı sil", color = ErrorInk) }
            if (session.accountDeleteBusy) {
                CircularProgressIndicator(Modifier.size(26.dp), color = Forest)
                BodyText("Hesap siliniyor. Uygulamayı kapatma.")
            }
            session.accountDeleteError?.let { BodyText(it, ErrorInk) }
        }
    }
}

@Composable
internal fun LocalProfilePicker(session: AppSession) {
    // Hide the old in-device 'Oyuncu 2' switch. Never delete either player's saved data.
    androidx.compose.runtime.LaunchedEffect(session.localProfile, session.accountName) {
        if (session.accountName == null && session.localProfile != 1) session.selectLocalProfile(1)
    }
    BodyText("Yerel oyuncu · Kayıtların bu cihazda saklanır.")
}

@Composable
private fun GameItem(game: Game, record: Progress?, open: (Game) -> Unit, edit: ((Game) -> Unit)? = null) {
    val status = when {
        record?.lockedWords?.size == game.wordCount -> "Tamamlandı · " + record.points + " puan"
        record?.started == true -> record.lockedWords.size.toString() + "/" + game.wordCount + " çözüldü"
        else -> game.wordCount.toString() + " cevap"
    }
    val icon = if (record?.completed == true) "✓" else "↗"
    if (edit == null) {
        MenuItem(game.title, game.path + " · " + status, icon) { open(game) }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { MenuItem(game.title, game.path + " · " + status, icon) { open(game) } }
            TextButton(onClick = { edit(game) }) { Text("Düzenle", fontFamily = Sans, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
internal fun ScreenPage(title: String, subtitle: String, back: (() -> Unit)?, body: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 800.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (back != null) {
                TextButton(onClick = back) { Text("‹  Geri", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold) }
            }
            if (title.isNotEmpty()) Text(title, fontFamily = Serif, color = Ink, fontSize = 38.sp, lineHeight = 41.sp)
            if (subtitle.isNotEmpty()) BodyText(subtitle)
            body()
        }
    }
}

@Composable
internal fun BodyText(text: String, color: Color = Muted) {
    Text(text, fontFamily = Sans, fontSize = 15.sp, lineHeight = 21.sp, color = color)
}

@Composable
internal fun PrimaryAction(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = White),
    ) {
        Text(text, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
internal fun MenuItem(title: String, subtitle: String, icon: String, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(16.dp)
    val click = if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(White).border(1.dp, Line, shape).then(click)
            .padding(horizontal = 13.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(LightLime, CircleShape), contentAlignment = Alignment.Center) {
            Text(icon, color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = if (icon.length > 2) 11.sp else 17.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Ink, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                subtitle, color = Muted, fontFamily = Sans, fontSize = 13.sp, lineHeight = 17.sp,
                maxLines = if (LocalDensity.current.fontScale > 1.2f) 3 else 2, overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) Text("›", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 23.sp)
    }
}
