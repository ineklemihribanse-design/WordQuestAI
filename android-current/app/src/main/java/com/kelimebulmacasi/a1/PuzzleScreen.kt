package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

@Composable
internal fun PuzzleScreen(
    game: Game,
    crossword: Crossword,
    persisted: Progress?,
    back: () -> Unit,
    shuffle: suspend () -> Boolean,
    onLearning: (LearningSignal) -> Unit,
    preferences: WordQuestPreferences,
    pronunciation: WordPronunciation,
    save: (Progress) -> Progress,
) {
    val context = LocalContext.current
    val keyboard = remember(context) { KeyboardInput(context) }
    val correctSound = remember(context) { CorrectAnswerSound(context) }
    DisposableEffect(correctSound) { onDispose { correctSound.close() } }
    val density = LocalDensity.current
    val keyboardVisible = WindowInsets.ime.getBottom(density) > 0
    var engine by remember(crossword) { mutableStateOf(crossword) }
    var view by remember(crossword) { mutableStateOf(crossword.view()) }
    var confirmReset by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var lastSpokenWord by rememberSaveable(game.progressId) { mutableStateOf<String?>(null) }
    val board = crossword.puzzle
    var expanded by remember(game.id) { mutableStateOf(board.words.size > 20) }
    var direction by remember(game.id) { mutableStateOf<Direction?>(null) }
    var shuffling by remember { mutableStateOf(false) }
    var shuffleError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var completionAudioJob by remember { mutableStateOf<Job?>(null) }
    val pageScroll = rememberScrollState()
    val word = board.words[view.wordIndex]

    fun reflect(progress: Progress) {
        if (progress != engine.progress()) {
            val current = view.wordIndex
            engine = Crossword(board, progress).apply { selectWord(current) }
        }
        view = engine.view()
    }

    LaunchedEffect(persisted) {
        if (persisted != null) reflect(engine.progress().merge(persisted))
    }

    fun edit(typedWord: Boolean = false, action: (Crossword) -> Unit) {
        if (shuffling) return
        val wasComplete = engine.complete
        val activeAtStart = engine.wordIndex
        val before = engine.progress()
        action(engine)
        val after = engine.progress()
        if (after != before) {
            // Record only newly locked words produced by THIS local action, not cloud restores.
            val newlySolved = after.lockedWords - before.lockedWords
            val completed = newlySolved.sortedBy { it != activeAtStart }
            for (index in completed) {
                onLearning(LearningSignal(LearningKind.SOLVED, board.words[index].entry,
                    after.resetAt, assisted = index in after.hinted))
            }
            // Keep the newly solved word selected. Only the player chooses the next word.
            // The keyboard's explicit Next action remains available for that choice.
            reflect(save(after))
            val spoken = if (typedWord) completed.map { board.words[it].entry.answer } else emptyList()
            if (spoken.isNotEmpty()) {
                lastSpokenWord = spoken.last()
                completionAudioJob?.cancel()
                val soundOn = preferences.correctSoundEnabled && preferences.correctSoundVolume > 0f
                val speechOn = preferences.pronunciation && preferences.autoPronunciation
                if (soundOn || speechOn) completionAudioJob = scope.launch {
                    if (soundOn) {
                        correctSound.play(true, preferences.correctSoundVolume)
                        // Original acknowledgement sound is 340 ms. Never overlap speech with it.
                        if (speechOn) delay(410L)
                    }
                    if (speechOn) pronunciation.pronounce(spoken, preferences.voice)
                }
            }
        } else view = engine.view()
        if (view.complete && !wasComplete) {
            keyboard.closeKeyboard()
            finished = true
        }
    }

    fun checkAndLearn() {
        val active = board.words[view.wordIndex]
        if (view.wordIndex !in view.solvedIndices) {
            val typed = active.cells.joinToString("") { view.letters[it]?.toString() ?: "_" }
            if ('_' !in typed && typed != active.entry.letters) {
                val progress = engine.progress()
                onLearning(LearningSignal(LearningKind.WRONG_CHECK, active.entry,
                    resetAt = progress.resetAt, attempt = typed, boardUpdatedAt = progress.updatedAt))
                correctSound.wrong(preferences.correctSoundEnabled, preferences.correctSoundVolume)
            }
        }
        val solvedBeforeCheck = view.solvedIndices.toSet()
        engine.check()
        view = engine.view()
        if (view.solvedIndices.any { it !in solvedBeforeCheck }) {
            correctSound.play(preferences.correctSoundEnabled, preferences.correctSoundVolume)
        }
    }

    fun revealAndLearn() = edit { crossword ->
        val revealed = crossword.reveal()
        if (revealed != null) {
            val progress = crossword.progress()
            for (index in board.wordsAt.getValue(revealed)) {
                onLearning(LearningSignal(LearningKind.HINT, board.words[index].entry,
                    resetAt = progress.resetAt, boardUpdatedAt = progress.updatedAt))
            }
        }
    }

    val replayWord = if (view.wordIndex in view.solvedIndices) word.entry.answer else lastSpokenWord
    val listen = replayWord?.takeIf { preferences.pronunciation }?.let { answer ->
        { pronunciation.pronounce(listOf(answer), preferences.voice, replay = true) }
    }

    fun selectWord(index: Int) {
        completionAudioJob?.cancel()
        engine.selectWord(index)
        view = engine.view()
        // Touching an already solved word replays its entire answer, not its letters.
        if (index in view.solvedIndices && preferences.pronunciation) {
            pronunciation.pronounce(listOf(board.words[index].entry.answer), preferences.voice, replay = true)
        }
        if (board.words.size <= 20) expanded = false
        scope.launch { pageScroll.scrollTo(0) }
        if (index !in view.solvedIndices) keyboard.openKeyboard()
    }

    fun leave() {
        completionAudioJob?.cancel()
        keyboard.closeKeyboard()
        back()
    }

    fun shuffleRound() {
        if (shuffling) return
        keyboard.closeKeyboard()
        shuffling = true
        shuffleError = false
        scope.launch {
            try {
                shuffleError = !shuffle()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                shuffleError = true
            } finally {
                shuffling = false
            }
        }
    }

    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        AndroidView(
            factory = { keyboard },
            modifier = Modifier.align(Alignment.TopStart).size(1.dp),
            update = {
                it.onLetters = { input -> edit(typedWord = true) { state -> state.enter(input) } }
                it.onDelete = { edit(action = Crossword::delete) }
                it.onNextWord = {
                    engine.moveToNextWord()
                    view = engine.view()
                }
            },
        )
        BoxWithConstraints(Modifier.widthIn(max = 1100.dp).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val wide = maxWidth >= 760.dp && !keyboardVisible
            val keyboardBoard = (maxHeight * .42f).coerceIn(116.dp, 330.dp)
            val keyboardKeys = game.puzzle.words.size > 8
            val available = (maxWidth - 36.dp).coerceAtLeast(220.dp)
            val viewWidth = if (wide) (maxWidth * .55f - 22.dp).coerceAtMost(570.dp) else available.coerceAtMost(590.dp)
            val boardHeight = if (keyboardVisible) keyboardBoard else minOf(viewWidth, 505.dp)
            Column(
                Modifier.fillMaxWidth().verticalScroll(pageScroll)
                    .padding(horizontal = if (wide) 23.dp else 16.dp, vertical = if (keyboardVisible) 7.dp else 13.dp),
                verticalArrangement = Arrangement.spacedBy(if (keyboardVisible) 8.dp else 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!keyboardVisible) GameHeading(game, view, ::leave, ::shuffleRound, shuffling)
                if (shuffleError) BodyText("Bu kelimelerle farklı bir tahta oluşturulamadı. Mevcut tahta açık kaldı.", ErrorInk)
                if (wide) {
                    Row(
                        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(Modifier.weight(1.22f)) {
                            BoardFrame(board, view, viewWidth, boardHeight, expanded, { expanded = !expanded }) {
                                engine.selectCell(it)
                                view = engine.view()
                                if (view.wordIndex in view.solvedIndices && preferences.pronunciation)
                                    pronunciation.pronounce(listOf(board.words[view.wordIndex].entry.answer), preferences.voice, replay = true)
                                else if (view.wordIndex !in view.solvedIndices) keyboard.openKeyboard()
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActiveClue(word, view.wordIndex in view.solvedIndices, false) {
                                if (view.wordIndex in view.solvedIndices) listen?.invoke()
                                else keyboard.openKeyboard()
                            }
                            if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)
                            Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)
                            ClueChoices(board, view, direction, { direction = it }, ::selectWord)
                        }
                    }
                } else {
                    if (keyboardVisible) {
                        ActiveClue(word, view.wordIndex in view.solvedIndices, true) {
                            if (view.wordIndex !in view.solvedIndices) keyboard.openKeyboard()
                        }
                        WordFields(word, view) { engine.selectLetter(it); view = engine.view(); keyboard.openKeyboard() }
                    }
                    BoardFrame(
                        board, view, viewWidth, boardHeight, expanded || (keyboardVisible && keyboardKeys),
                        if (keyboardVisible) null else ({ expanded = !expanded }),
                    ) {
                        engine.selectCell(it)
                        view = engine.view()
                        if (view.wordIndex in view.solvedIndices && preferences.pronunciation)
                            pronunciation.pronounce(listOf(board.words[view.wordIndex].entry.answer), preferences.voice, replay = true)
                        else if (view.wordIndex !in view.solvedIndices) keyboard.openKeyboard()
                    }
                    if (!keyboardVisible) {
                        ActiveClue(word, view.wordIndex in view.solvedIndices, false) {
                            if (view.wordIndex in view.solvedIndices) listen?.invoke()
                            else keyboard.openKeyboard()
                        }
                    }
                    if (view.wordIndex in view.solvedIndices) WordIpa(word.entry.answer, preferences)
                    Actions(view, ::checkAndLearn, ::revealAndLearn, listen, pronunciation.error)
                    if (keyboardVisible) {
                        TextButton(onClick = {
                            engine.moveToNextWord()
                            view = engine.view()
                        }, enabled = !view.complete, modifier = Modifier.align(Alignment.End)) {
                            Text("Sonraki ipucu", fontFamily = Sans, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        ClueChoices(board, view, direction, { direction = it }, ::selectWord)
                    }
                }
                if (!keyboardVisible && view.letters.isNotEmpty()) {
                    TextButton(onClick = { confirmReset = true }, modifier = Modifier.align(Alignment.End)) {
                        Text("Yeniden başla", fontFamily = Sans, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Yeniden başla", fontFamily = Serif, color = Ink) },
            text = { BodyText("Tahta temizlenecek. Kazandığın puanlar hesabında kalır.") },
            confirmButton = {
                TextButton(onClick = {
                    edit(action = Crossword::reset)
                    lastSpokenWord = null
                    pronunciation.stop()
                    confirmReset = false
                }) {
                    Text("Baştan başla", fontFamily = Sans)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Vazgeç", fontFamily = Sans) } },
            containerColor = White,
        )
    }
    if (finished) {
        AlertDialog(
            onDismissRequest = { finished = false },
            title = { Text("Bulmacayı tamamladın!", fontFamily = Serif, color = Ink) },
            text = { BodyText("Bu bulmacadaki puanın: " + view.points + "/" + board.words.size * 10) },
            confirmButton = { TextButton(onClick = { finished = false }) { Text("Tamam", fontFamily = Sans) } },
            dismissButton = listen?.let { { TextButton(onClick = it) { Text("Tekrar dinle", fontFamily = Sans) } } },
            containerColor = White,
        )
    }
}

@Composable
private fun GameHeading(game: Game, view: CrosswordView, back: () -> Unit, shuffle: () -> Unit, shuffling: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = back) { Text("‹  Geri", fontFamily = Sans, fontWeight = FontWeight.Bold) }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = shuffle, enabled = !shuffling, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text(if (shuffling) "Karışıyor…" else "Karıştır", fontFamily = Sans, fontWeight = FontWeight.Bold)
                }
                Text(
                    view.points.toString() + " puan", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(LightLime, CircleShape).padding(horizontal = 9.dp, vertical = 6.dp),
                )
            }
        }
        Text(game.path, color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(game.title, color = Ink, fontFamily = Serif, fontSize = 33.sp, lineHeight = 36.sp)
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            val total = game.puzzle.words.size
            Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(Line)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(view.solvedIndices.size.toFloat() / total).background(Forest))
            }
            Text(view.solvedIndices.size.toString() + "/" + total, color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BoardFrame(
    puzzle: Puzzle, view: CrosswordView, width: Dp, height: Dp, enlarged: Boolean,
    toggleZoom: (() -> Unit)?, onCell: (Cell) -> Unit,
) {
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val density = LocalDensity.current
    val regularTile = minOf((width - 12.dp) / puzzle.columns, (height - 12.dp) / puzzle.rows)
    val tile = if (enlarged) maxOf(regularTile, 30.dp) else regularTile
    val gap = if (tile < 25.dp) 1.dp else 2.dp
    val actualWidth = tile * puzzle.columns + 12.dp
    val actualHeight = tile * puzzle.rows + 12.dp
    val windowWidth = minOf(width, actualWidth)
    val windowHeight = minOf(height, actualHeight)
    val boardShape = RoundedCornerShape(16.dp)
    val selected = view.cell
    LaunchedEffect(selected, enlarged, windowWidth, windowHeight) {
        if (enlarged) {
            val centerX = with(density) { (tile * (selected.column + .5f) - windowWidth / 2).roundToPx() }
            val centerY = with(density) { (tile * (selected.row + .5f) - windowHeight / 2).roundToPx() }
            horizontal.scrollTo(centerX.coerceIn(0, horizontal.maxValue))
            vertical.scrollTo(centerY.coerceIn(0, vertical.maxValue))
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.width(windowWidth).height(windowHeight).clip(boardShape).background(Board),
            contentAlignment = Alignment.TopStart,
        ) {
            val active = puzzle.words[view.wordIndex].cells.toSet()
            Column(
                Modifier.horizontalScroll(horizontal).verticalScroll(vertical).requiredWidth(actualWidth).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                repeat(puzzle.rows) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(puzzle.columns) { column ->
                            val position = Cell(row, column)
                            val size = Modifier.size(tile - gap)
                            if (position in puzzle.solution) {
                                LetterCell(
                                    size, tile, puzzle.numbers[position], view.letters[position],
                                    view.cell == position, position in active, position in view.locked,
                                    view.isWrong(position, puzzle),
                                    "${row + 1}. satır ${column + 1}. sütun",
                                ) { onCell(position) }
                            } else {
                                Box(size.background(Block, RoundedCornerShape(2.dp)))
                            }
                        }
                    }
                }
            }
        }
        if (toggleZoom != null && maxOf(puzzle.columns, puzzle.rows) >= 12) {
            TextButton(onClick = toggleZoom, modifier = Modifier.align(Alignment.End)) {
                Text(if (enlarged) "Tüm tahtayı göster" else "Tahtayı büyüt", color = Forest, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WordFields(word: Word, view: CrosswordView, select: (Cell) -> Unit) {
    val words = word.entry.answer.split(' ', '-').filter { it.isNotBlank() }
    val breaks = words.map { englishLetters(it).length }.runningFold(0, Int::plus).drop(1).dropLast(1)
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(view.cell) {
        val index = word.cells.indexOf(view.cell)
        val offset = with(density) { (34.dp * (index - 4).coerceAtLeast(0)).roundToPx() }
        scroll.scrollTo(offset.coerceAtMost(scroll.maxValue))
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        word.cells.forEachIndexed { index, cell ->
            if (index in breaks) Spacer(Modifier.width(8.dp))
            LetterCell(
                Modifier.size(30.dp), 30.dp, null, view.letters[cell], cell == view.cell, true,
                cell in view.locked, false, "Cevap, " + (index + 1) + ". harf", cell !in view.locked,
            ) { select(cell) }
        }
    }
}

@Composable
private fun LetterCell(
    modifier: Modifier, tile: Dp, number: Int?, letter: Char?, selected: Boolean, active: Boolean,
    locked: Boolean, wrong: Boolean, description: String, enabled: Boolean = true, onClick: () -> Unit,
) {
    val color = when {
        locked -> Color(0xFFCDE8D2)
        wrong -> ErrorFill
        selected -> Lime
        active -> LightLime
        else -> White
    }
    val shape = RoundedCornerShape(if (tile < 21.dp) 2.dp else 4.dp)
    val outline = if (selected) Modifier.border(if (tile < 20.dp) 1.dp else 2.dp, if (wrong) ErrorInk else Forest, shape) else Modifier
    Box(
        modifier.clip(shape).background(color).then(outline).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = when {
                    locked -> "Sabit harf"
                    wrong -> "Yanlış harf"
                    selected -> "Seçili kutu"
                    letter == null -> "Boş kutu"
                    else -> "Dolu kutu"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (number != null) {
            val font = cellFont(tile, .30f, .42f, 5.5f, 11f)
            Text(number.toString(), Modifier.align(Alignment.TopStart).padding(start = 1.dp),
                color = Forest, fontFamily = Sans, fontSize = font, lineHeight = font, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        if (letter != null) {
            val font = cellFont(tile, .52f, .69f, 8f, 24f)
            Text(letter.toString(), Modifier.padding(top = if (number == null) 0.dp else 3.dp),
                color = if (wrong) ErrorInk else Ink, fontFamily = Sans, fontWeight = FontWeight.Bold,
                fontSize = font, lineHeight = font, maxLines = 1)
        }
    }
}

@Composable
internal fun cellFont(tile: Dp, fraction: Float, cap: Float, min: Float, max: Float) = with(LocalDensity.current) {
    val preferred = (tile.value * fraction).coerceIn(min, max).sp
    val fitted = (tile * cap).toSp()
    minOf(preferred.value, fitted.value).sp
}

@Composable
private fun ActiveClue(word: Word, solved: Boolean, compact: Boolean, type: () -> Unit) {
    val shape = RoundedCornerShape(17.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(LightLime).clickable(enabled = !solved, role = Role.Button, onClick = type)
            .padding(horizontal = 13.dp, vertical = if (compact) 7.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(if (compact) 27.dp else 34.dp).background(Forest, CircleShape), contentAlignment = Alignment.Center) {
                Text(word.number.toString(), color = White, fontFamily = Sans, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    word.entry.clue, color = Ink, fontFamily = Serif,
                    fontSize = if (compact) 17.sp else 22.sp, lineHeight = if (compact) 20.sp else 25.sp,
                )
                Text(word.direction.label + " · " + word.entry.lengthLabel + " harf",
                    color = Forest, fontFamily = Sans, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(if (solved) "✓" else "Yaz", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        if (word.entry.context.isNotBlank()) {
            Text(word.entry.context, color = Forest, fontFamily = Sans, fontSize = if (compact) 12.sp else 14.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun Actions(view: CrosswordView, check: () -> Unit, reveal: () -> Unit,
    listen: (() -> Unit)?, audioError: String?) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = check, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = White),
            ) { Text("Kontrol et", fontFamily = Sans, fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = reveal, enabled = !view.complete && view.wordIndex !in view.solvedIndices, modifier = Modifier.weight(1f)) {
                Text("Harf aç", fontFamily = Sans, fontWeight = FontWeight.Bold)
            }
        }
        if (listen != null) TextButton(onClick = listen) { Text("Tekrar dinle", fontFamily = Sans) }
        if (audioError != null) BodyText(audioError, ErrorInk)
        val status = when {
            view.complete -> "Bulmacayı tamamladın!"
            view.hasErrors -> "Yanlış harfler kırmızıyla işaretlendi."
            view.checked && view.letters.isEmpty() -> "Kontrol etmek için önce bir harf yaz."
            view.checked -> "Yazdığın harfler doğru."
            else -> ""
        }
        if (status.isNotEmpty()) BodyText(status, if (view.hasErrors) ErrorInk else Forest)
    }
}

@Composable
private fun ClueChoices(
    puzzle: Puzzle, view: CrosswordView, direction: Direction?, filter: (Direction?) -> Unit, select: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(null, Direction.Across, Direction.Down).forEach { value ->
                FilterChip(
                    selected = direction == value, onClick = { filter(value) },
                    label = { Text(value?.label ?: "Tümü", color = Forest, fontFamily = Sans, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LightLime),
                )
            }
        }
        for ((index, word) in puzzle.words.withIndex()) {
            if (direction != null && word.direction != direction) continue
            val solved = index in view.solvedIndices
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(if (index == view.wordIndex) LightLime else White)
                    .border(1.dp, Line, shape).clickable(role = Role.Button) { select(index) }
                    .padding(horizontal = 9.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(word.number.toString(), color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)
                Column(Modifier.weight(1f)) {
                    Text(word.entry.clue, color = Ink, fontFamily = Sans, fontSize = 15.sp)
                    Text(word.direction.label, color = Muted, fontFamily = Sans, fontSize = 11.sp)
                }
                Text(if (solved) "✓" else word.entry.lengthLabel, color = Forest, fontFamily = Sans, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
