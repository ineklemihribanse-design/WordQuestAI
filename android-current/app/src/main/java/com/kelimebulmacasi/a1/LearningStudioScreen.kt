package com.kelimebulmacasi.a1

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val StudioNight = Color(0xFF143A30)
private val StudioGold = Color(0xFFDFBB72)
private val StudioPaper = Color(0xFFF9F7EF)
private val StudioSoft = Color(0xFFDAE7DF)

/** Account-isolated word notebook organized into dated game folders and swipeable flashcards. */
@Composable
internal fun LearningScreen(session: AppSession, pronunciation: WordPronunciation, back: () -> Unit) {
    VocabularyNeonHome(session, pronunciation, back)
}

@Composable
private fun LearningFlashcardStudio(session: AppSession, pronunciation: WordPronunciation,
    folders: List<LearningFolder>, initialFolder: String?, initialFilter: String) {
    var selectedFolder by rememberSaveable(session.profileKey, initialFolder) { mutableStateOf(initialFolder ?: "all") }
    var selectedFilter by rememberSaveable(session.profileKey, initialFilter) { mutableStateOf(initialFilter) }
    var count by rememberSaveable(session.profileKey) { mutableIntStateOf(20) }
    var seed by rememberSaveable(session.profileKey) { mutableIntStateOf(1) }
    var position by rememberSaveable(session.profileKey) { mutableIntStateOf(0) }
    var flipped by rememberSaveable(session.profileKey) { mutableStateOf(false) }
    val allCards = if (selectedFolder == "all") session.searchLearning("", 0, 5000)
        .filter { selectedFilter == "" || when (selectedFilter) {
            "BILINMIYOR" -> it.forgotten > 0
            "YANLIS" -> it.wrong > 0
            else -> it.solved > 0 && it.forgotten == 0 && it.wrong == 0
        } }
    else session.learningFolderCards(selectedFolder, selectedFilter)
    val deck = remember(session.profileKey, seed, selectedFolder, selectedFilter, count,
        allCards.map { it.key }) { allCards.shuffled(Random(seed)).take(count) }
    val card = deck.getOrNull(position.coerceIn(0, (deck.size - 1).coerceAtLeast(0)))
    fun next(delta: Int) {
        position = (position + delta).coerceIn(0, (deck.size - 1).coerceAtLeast(0))
        flipped = false
    }
    LearningPanel {
        Text("Kartlarla Çalış", fontFamily = Sans, fontWeight = FontWeight.Bold,
            color = Forest, fontSize = 20.sp)
        BodyText("Türkçesini gör, kartı çevir, İngilizcesini hatırla. Kartı yana kaydırarak ilerle.")
        var folderMenu by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { folderMenu = true }) {
                Text(if (selectedFolder == "all") "Bütün klasörler" else
                    folders.firstOrNull { it.id == selectedFolder }?.title ?: "Bütün klasörler",
                    fontFamily = Sans)
                Text("  ▾")
            }
            DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                DropdownMenuItem(text = { Text("Bütün klasörler") }, onClick = {
                    selectedFolder = "all"; folderMenu = false; position = 0; flipped = false })
                for (folder in folders) DropdownMenuItem(text = { Text(folder.title) }, onClick = {
                    selectedFolder = folder.id; folderMenu = false; position = 0; flipped = false })
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            for ((value, label) in listOf("" to "Karışık", "BILINMIYOR" to "Bilinmeyen", "YANLIS" to "Yanlış", "YENI" to "Yeni")) {
                FilterChip(selected = selectedFilter == value, onClick = {
                    selectedFilter = value; position = 0; flipped = false
                }, label = { Text(label, fontFamily = Sans) })
            }
        }
        Text("Kaç kart çalışmak istersin?", color = Forest, fontFamily = Sans)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            for (number in listOf(20, 30, 40, 50)) {
                FilterChip(selected = count == number, onClick = { count = number; position = 0; flipped = false },
                    label = { Text("$number", fontFamily = Sans) }, modifier = Modifier.weight(1f))
            }
        }
        OutlinedButton(onClick = { seed += 1; position = 0; flipped = false },
            modifier = Modifier.fillMaxWidth()) { Text("Kartları yeniden karıştır", fontFamily = Sans) }
    }
    if (card == null) {
        LearningPanel { BodyText("Bu seçime uygun kelime bulunamadı. Başka klasör veya filtre seç.") }
        return
    }
    val rotation by animateFloatAsState(targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 340), label = "Kart dönüşü")
    var drag by remember(position, seed) { mutableFloatStateOf(0f) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BodyText("${position + 1} / ${deck.size} · ${if (flipped) "İngilizce" else "Türkçe"}")
        Box(Modifier.fillMaxWidth().height(285.dp).pointerInput(position, seed) {
            detectHorizontalDragGestures(onHorizontalDrag = { _, amount -> drag += amount },
                onDragEnd = { if (drag < -65) next(1) else if (drag > 65) next(-1); drag = 0f })
        }.graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
            .clip(RoundedCornerShape(24.dp))
            .background(if (flipped) StudioGold else StudioNight)
            .clickable { flipped = !flipped }.padding(20.dp),
            contentAlignment = Alignment.Center) {
            Column(Modifier.graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (rotation < 90f) "TÜRKÇE İPUCU" else "İNGİLİZCE CEVAP",
                    color = if (rotation < 90f) StudioGold else StudioNight,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(if (rotation < 90f) card.clue else card.answer,
                    color = if (rotation < 90f) StudioPaper else StudioNight,
                    fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(if (rotation < 90f) "Cevabı görmek için karta dokun" else "Telaffuzu dinleyebilir, ardından bilgini değerlendirebilirsin",
                    color = if (rotation < 90f) StudioSoft else StudioNight,
                    fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        if (flipped) {
            WordIpa(card.answer, session.preferences)
            ListenToWord(card.answer, session.preferences, pronunciation)
            if (card.context.isNotBlank()) BodyText(card.context)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { session.reviewAnyLearning(card, ReviewRating.Again); next(1) },
                    modifier = Modifier.weight(1f)) { Text("Tekrar çalış", fontFamily = Sans) }
                Button(onClick = { session.reviewAnyLearning(card, ReviewRating.Good); next(1) },
                    modifier = Modifier.weight(1f)) { Text("Hatırladım", fontFamily = Sans) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { next(-1) }, enabled = position > 0,
                modifier = Modifier.weight(1f)) { Text("‹ Önceki", fontFamily = Sans) }
            OutlinedButton(onClick = { next(1) }, enabled = position < deck.lastIndex,
                modifier = Modifier.weight(1f)) { Text("Sonraki ›", fontFamily = Sans) }
        }
        if (position == deck.lastIndex && flipped) BodyText("Son karttasın. Tekrar çalışmak için kartları yeniden karıştırabilirsin.")
    }
}
