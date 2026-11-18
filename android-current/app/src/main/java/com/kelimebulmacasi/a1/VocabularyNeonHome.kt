package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

internal val MemoryNavy = Color(0xFF061830)
internal val MemoryBlue = Color(0xFF123B6B)
internal val MemoryCyan = Color(0xFF37D8EC)
internal val MemoryLilac = Color(0xFFA88DEB)
internal val MemoryGold = Color(0xFFFFD175)
internal val MemoryWhite = Color(0xFFF8F8FF)
internal val MemoryMuted = Color(0xFFC7D3E7)

private data class StudyTile(val title: String, val subtitle: String, val symbol: String, val mode: String, val accent: Color)
private val studyTiles = listOf(
    StudyTile("Kartlarla Çalış", "Çevir, öğren, hatırla", "▤", "card", MemoryCyan),
    StudyTile("Yazarak Öğren", "Yaz ve kontrol et", "✎", "write", MemoryLilac),
    StudyTile("Tür Eşleştir", "İsim, fiil, sıfat…", "◇", "type", Color(0xFF54E1C7)),
    StudyTile("Karma Antrenman", "Farklı soru türleri", "◎", "mixed", MemoryGold),
)

/** The selection belongs to the logged-in or local profile, not the AI teacher. */
internal object ProfileRobotSelection {
    private const val STORE = "wordquest_profile_robots"
    fun read(context: Context, profile: String): Int =
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .getInt("robot_$profile", 0).coerceIn(0, 3)
    fun write(context: Context, profile: String, selected: Int) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putInt("robot_$profile", selected.coerceIn(0, 3)).apply()
    }
    val names = listOf("Nova", "Orbit", "Mira", "Atlas")
    val accents = listOf(MemoryCyan, MemoryLilac, MemoryGold, Color(0xFF56D6A2))
}

@Composable
internal fun ProfileRobotPortrait(robot: Int, modifier: Modifier = Modifier) {
    val glow = ProfileRobotSelection.accents[robot.coerceIn(0, 3)]
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawCircle(glow.copy(alpha = .16f), radius = w * .43f,
            center = Offset(w * .5f, h * .43f))
        drawRoundRect(Color(0xFFB6C9E0), Offset(w * .13f, h * .31f),
            Size(w * .16f, h * .35f), CornerRadius(w * .08f))
        drawRoundRect(Color(0xFFB6C9E0), Offset(w * .71f, h * .31f),
            Size(w * .16f, h * .35f), CornerRadius(w * .08f))
        drawRoundRect(Brush.verticalGradient(listOf(Color.White,
            Color(0xFFAABAD4), Color(0xFF637BA0))),
            Offset(w * .20f, h * .11f), Size(w * .60f, h * .68f),
            CornerRadius(w * .18f))
        drawRoundRect(Color(0xFF091326), Offset(w * .25f, h * .23f),
            Size(w * .5f, h * .40f), CornerRadius(w * .14f))
        drawRoundRect(glow.copy(alpha = .30f), Offset(w * .255f, h * .245f),
            Size(w * .49f, h * .37f), CornerRadius(w * .14f),
            style = Stroke(w * .012f))
        val stroke = Stroke(width = w * .027f, cap = StrokeCap.Round)
        drawArc(glow, 12f, 150f, false, Offset(w * .33f, h * .35f),
            Size(w * .12f, h * .09f), style = stroke)
        drawArc(glow, 12f, 150f, false, Offset(w * .55f, h * .35f),
            Size(w * .12f, h * .09f), style = stroke)
        drawArc(glow, 15f, 150f, false, Offset(w * .42f, h * .47f),
            Size(w * .16f, h * .09f), style = stroke)
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF314F80),
            Color(0xFF0B2144))), Offset(w * .23f, h * .76f),
            Size(w * .54f, h * .23f), CornerRadius(w * .10f))
        drawRoundRect(glow.copy(alpha = .8f), Offset(w * .4f, h * .81f),
            Size(w * .20f, h * .045f), CornerRadius(w * .025f))
    }
}

@Composable
internal fun VocabularyNeonHome(
    session: AppSession, pronunciation: WordPronunciation, back: () -> Unit
) {
    val context = LocalContext.current
    val ai: AiSession = viewModel()
    LaunchedEffect(session.profileKey) {
        if (ai.boundProfile != session.profileKey) ai.bindProfile(context, session.profileKey)
    }
    var page by rememberSaveable(session.profileKey) { mutableStateOf("home") }
    var collection by rememberSaveable(session.profileKey) { mutableStateOf("all") }
    var activeFolder by rememberSaveable(session.profileKey) { mutableStateOf<String?>(null) }
    var cardDetail by rememberSaveable(session.profileKey) { mutableStateOf<String?>(null) }
    var robot by remember(session.profileKey) {
        mutableIntStateOf(ProfileRobotSelection.read(context, session.profileKey))
    }
    var robotPicker by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf<VocabularyStudyConfig?>(null) }
    val folders = session.learningFolders()
    val total = session.learningCardCount()
    val due = session.learningDueCount(System.currentTimeMillis())
    val allCards = session.searchLearning("", 0, 5000)
    val learned = allCards.count { it.remembered > 0 && it.forgotten == 0 }
    val fresh = allCards.count { it.remembered == 0 && it.forgotten == 0 }
    val struggling = allCards.count { it.wrong > 0 || it.forgotten > 0 }
    val favorites = allCards.count { it.pinned }
    val progress = if (total == 0) 0f else learned.toFloat() / total.toFloat()

    LaunchedEffect(session.requestedLearningFolder, session.profileKey) {
        session.requestedLearningFolder?.let {
            activeFolder = it
            page = "history"
            session.clearLearningFolderRequest()
        }
    }
    val selectedHistory = cardDetail?.let(session::learningCard)
    if (selectedHistory != null) {
        LearningHistoryScreen(session, selectedHistory, pronunciation) { cardDetail = null }
        return
    }
    val active = config
    if (active != null) {
        NeonVocabularyPractice(session, pronunciation, active, ai) { config = null }
        return
    }
    fun start(mode: String, filter: String = "", folder: String = "all") {
        if (vocabularyStudyCards(session, folder, filter).isNotEmpty()) {
            config = VocabularyStudyConfig(folder, filter, mode, "tr-en", 20,
                System.currentTimeMillis().toInt())
        }
    }
    if (robotPicker) {
        AlertDialog(onDismissRequest = { robotPicker = false },
            title = { Text("Profil robotun") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Yalnızca aktif profilin için kaydedilir.")
                    ProfileRobotSelection.names.forEachIndexed { index, name ->
                        OutlinedButton(onClick = {
                            robot = index
                            ProfileRobotSelection.write(context, session.profileKey, index)
                            robotPicker = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text((if (robot == index) "✓  " else "") + name)
                        }
                    }
                }
            }, confirmButton = {
                TextButton(onClick = { robotPicker = false }) { Text("Kapat") }
            })
    }

    Column(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(MemoryNavy, Color(0xFF09264B), MemoryNavy))
    ).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                if (page == "home") back()
                else { page = "home"; activeFolder = null }
            }) { Text("‹  Geri", color = MemoryWhite) }
            Spacer(Modifier.weight(1f))
            Text(session.learningProfileName, color = MemoryMuted, fontSize = 12.sp)
        }
        when (page) {
            "home" -> {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(width = 128.dp, height = 151.dp)
                        .clickable { robotPicker = true }) {
                        ProfileRobotPortrait(robot, Modifier.fillMaxSize())
                        Text("Robotunu değiştir", modifier = Modifier.align(Alignment.BottomCenter),
                            fontSize = 9.sp, color = MemoryMuted)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Kelime\nHafızam", fontSize = 30.sp, lineHeight = 33.sp,
                            fontWeight = FontWeight.Black, color = MemoryGold)
                        Text("Kelimelerini öğren, pekiştir ve kalıcı hâle getir.",
                            fontSize = 12.sp, color = MemoryMuted, lineHeight = 17.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MemoryStat("▤", "$total", "Kelimem", MemoryCyan, Modifier.weight(1f))
                    MemoryStat("↻", "$due", "Tekrar", MemoryLilac, Modifier.weight(1f))
                    MemoryStat("✓", "$learned", "Çalışılmış", MemoryGold, Modifier.weight(1f))
                }
                NeonPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Bugünkü Tekrarın", color = MemoryWhite,
                                fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("$due kelime seni bekliyor.", color = MemoryMuted, fontSize = 12.sp)
                        }
                        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { progress },
                                modifier = Modifier.fillMaxSize(), color = MemoryCyan,
                                trackColor = Color.White.copy(alpha = .16f), strokeWidth = 6.dp)
                            Text("${(progress * 100).toInt()}%", color = MemoryWhite,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(onClick = { start("card", "DUE") }, enabled = due > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MemoryGold,
                            contentColor = MemoryNavy), modifier = Modifier.fillMaxWidth()) {
                        Text(if (due > 0) "Tekrara Başla" else "Bugünlük tekrar tamam",
                            fontWeight = FontWeight.Bold)
                    }
                }
                Text("Çalışma Modları", color = MemoryWhite, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
                for (pair in studyTiles.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { tile ->
                            MemoryModeTile(tile, Modifier.weight(1f), total > 0) {
                                start(tile.mode)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Akıllı Koleksiyonlar", color = MemoryWhite,
                        fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { collection = "all"; page = "collection" }) {
                        Text("Tümünü Gör ›", color = MemoryLilac, fontSize = 12.sp)
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple("Yeni", fresh, "new"),
                        Triple("Öğreniyorum", allCards.count {
                            it.remembered > 0 && it.forgotten > 0 }, "learning"),
                        Triple("Zorlandıklarım", struggling, "hard"),
                        Triple("Çalışılmış", learned, "learned"),
                        Triple("Favorilerim", favorites, "favorite")
                    ).forEach { (label, amount, code) ->
                        Column(Modifier.width(98.dp).clip(RoundedCornerShape(15.dp))
                            .background(Color.White.copy(alpha = .085f))
                            .border(1.dp, MemoryCyan.copy(alpha = .24f), RoundedCornerShape(15.dp))
                            .clickable { collection = code; page = "collection" }.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(when (code) {
                                "new" -> "◈"; "learning" -> "▤"; "hard" -> "⚡"
                                "learned" -> "✓"; else -> "☆"
                            }, fontSize = 23.sp, color = MemoryCyan)
                            Text(label, fontSize = 10.sp, color = MemoryWhite,
                                maxLines = 1, textAlign = TextAlign.Center)
                            Text("$amount", fontSize = 21.sp, color = MemoryWhite,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                NeonPanel(click = { page = "history"; activeFolder = null }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("▣", fontSize = 26.sp, color = MemoryGold)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Geçmiş Oyunlarım", color = MemoryWhite,
                                fontWeight = FontWeight.Bold)
                            Text("${folders.size} arşiv · Oyun kayıtların korunur",
                                fontSize = 11.sp, color = MemoryMuted)
                        }
                        Text("›", fontSize = 24.sp, color = MemoryMuted)
                    }
                }
                session.learningError?.let {
                    Text(it, color = MemoryGold, fontSize = 12.sp)
                }
            }
            "collection" -> {
                Text("Kelime Koleksiyonum", color = MemoryWhite,
                    fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to "Tümü", "new" to "Yeni",
                        "learning" to "Öğreniyorum", "hard" to "Zorlandıklarım",
                        "learned" to "Çalışılmış", "favorite" to "Favorilerim"
                    ).forEach { (id, label) ->
                        FilterChip(selected = collection == id,
                            onClick = { collection = id }, label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MemoryBlue, labelColor = MemoryWhite,
                                selectedContainerColor = MemoryLilac,
                                selectedLabelColor = MemoryNavy))
                    }
                }
                val cards = allCards.filter {
                    when (collection) {
                        "new" -> it.remembered == 0 && it.forgotten == 0
                        "learning" -> it.remembered > 0 && it.forgotten > 0
                        "hard" -> it.wrong > 0 || it.forgotten > 0
                        "learned" -> it.remembered > 0 && it.forgotten == 0
                        "favorite" -> it.pinned
                        else -> true
                    }
                }
                Text("${cards.size} kelime", color = MemoryMuted)
                val activeSmartFilter = when (collection) {
                    "new" -> "SMART_NEW"
                    "learning" -> "SMART_LEARNING"
                    "hard" -> "SMART_HARD"
                    "learned" -> "SMART_LEARNED"
                    "favorite" -> "SMART_FAVORITE"
                    else -> ""
                }
                if (cards.isNotEmpty()) {
                    Button(onClick = { start("card", activeSmartFilter) },
                        colors = ButtonDefaults.buttonColors(containerColor = MemoryCyan,
                            contentColor = MemoryNavy), modifier = Modifier.fillMaxWidth()) {
                        Text("Bu Kelimelerle Çalış")
                    }
                }
                cards.take(150).forEach { card ->
                    NeonPanel(click = { cardDetail = card.key }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(card.answer, color = MemoryWhite,
                                    fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text(card.clue, color = MemoryMuted, fontSize = 12.sp)
                            }
                            Text("›", color = MemoryCyan, fontSize = 21.sp)
                        }
                    }
                }
            }
            "history" -> {
                Text("Geçmiş Oyunlarım", color = MemoryWhite, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold)
                if (activeFolder != null) {
                    val folder = folders.firstOrNull { it.id == activeFolder }
                    if (folder == null) {
                        Text("Bu oyun arşivi bulunamadı.", color = MemoryMuted)
                    } else {
                        Text(folder.title, color = MemoryGold)
                        Button(onClick = { start("card", folder = folder.id) }) {
                            Text("Bu kelimelerle çalış")
                        }
                        session.learningFolderCards(folder.id).take(160).forEach { card ->
                            NeonPanel(click = { cardDetail = card.key }) {
                                Text(card.answer, color = MemoryWhite, fontWeight = FontWeight.Bold)
                                Text(card.clue, color = MemoryMuted, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    folders.forEach { folder ->
                        NeonPanel(click = { activeFolder = folder.id }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(folder.title, color = MemoryWhite,
                                        fontWeight = FontWeight.Bold)
                                    Text("${folder.total} kelime · ${folder.wrong} yanlış",
                                        color = MemoryMuted, fontSize = 12.sp)
                                }
                                Text("›", color = MemoryGold, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MemoryStat(symbol: String, amount: String, title: String,
    accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(15.dp))
        .background(Color(0xFF0B2B51))
        .border(1.dp, accent.copy(alpha = .55f), RoundedCornerShape(15.dp))
        .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(symbol, color = accent, fontSize = 22.sp)
        Text(amount, color = MemoryWhite, fontSize = 23.sp,
            fontWeight = FontWeight.Bold)
        Text(title, color = MemoryMuted, fontSize = 10.sp)
    }
}

@Composable
private fun MemoryModeTile(tile: StudyTile, modifier: Modifier,
    enabled: Boolean, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(15.dp))
        .background(Brush.linearGradient(
            listOf(tile.accent.copy(alpha = .29f), Color(0xFF0D2347))))
        .border(1.dp, tile.accent.copy(alpha = .60f), RoundedCornerShape(15.dp))
        .clickable(enabled = enabled, onClick = onClick).padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tile.symbol, color = tile.accent, fontSize = 24.sp)
            Spacer(Modifier.weight(1f))
            Text("›", fontSize = 22.sp, color = MemoryWhite)
        }
        Text(tile.title, color = MemoryWhite, fontSize = 13.sp,
            fontWeight = FontWeight.Bold, maxLines = 1)
        Text(tile.subtitle, color = MemoryMuted, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
internal fun NeonPanel(click: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    val base = Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp))
        .background(Brush.linearGradient(listOf(
            Color(0xFF133D69), Color(0xFF102344))))
        .border(1.dp, Color(0xFF4177A8), RoundedCornerShape(19.dp))
    Column((if (click != null) base.clickable(onClick = click) else base)
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp),
        content = content)
}
