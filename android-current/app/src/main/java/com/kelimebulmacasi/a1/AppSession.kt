package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSession(context: Context, internal val accounts: Accounts, private val scope: CoroutineScope) {
    private val context = context.applicationContext
    private val catalog = Catalog(context)
    internal val verbs by lazy {
        this.context.assets.open("verb-forms.txt").bufferedReader().use { loadVerbForms(it.readLines()) }
    }
    private val guest = PlayerStore(this.context)
    private val secondPlayer by lazy { PlayerStore(this.context, secondLocalProfile = true) }
    private val localProfiles = this.context.getSharedPreferences("wordquest_local_profiles", Context.MODE_PRIVATE)
    private val preferenceStore = PreferenceStore(this.context)
    var localProfile by mutableIntStateOf(localProfiles.getInt("active", 1))
        private set
    internal var profileKey by mutableStateOf("local_$localProfile")
        private set
    internal var preferences by mutableStateOf(preferenceStore.load(profileKey))
        private set
    private var player = localPlayer()
    private var cloud: CloudSync? = null
    private var learning = localLearningStore()
    private var learningRevision by mutableIntStateOf(0)
    internal var requestedLearningFolder by mutableStateOf<String?>(null)
        private set
    internal fun openLearningFolder(folderId: String) { requestedLearningFolder = folderId }
    internal fun clearLearningFolderRequest() { requestedLearningFolder = null }
    internal fun learningFolders() = learningRevision.let { learning.folders() }
    internal fun learningCardCount() = learningRevision.let { learning.countCards() }
    internal fun learningFolderCards(folderId: String, status: String = "") =
        learningRevision.let { learning.folderCards(folderId, status) }
    internal fun renameLearningFolder(id: String, name: String): Boolean = runCatching {
        learning.renameFolder(id, name).also { if (it) learningRevision++ }
    }.getOrElse { learningError = "Klasör adı kaydedilemedi."; false }
    internal fun reviewAnyLearning(card: LearningCard, rating: ReviewRating) {
        runCatching { learning.practiceAny(card, rating) }
            .onSuccess { if (it) learningRevision++ ; learningError = null }
            .onFailure { learningError = "Kart çalışması kaydedilemedi." }
    }
    var learningError by mutableStateOf<String?>(null)
        private set

    internal fun searchLearning(query: String, offset: Int, limit: Int) =
        learningRevision.let { learning.cards(query, offset, limit) }
    internal fun learningCard(key: String) = learningRevision.let { learning.card(key) }
    internal fun learningHistory(key: String, offset: Int, limit: Int) =
        learningRevision.let { learning.history(key, offset, limit) }
    internal fun dueLearningCards(now: Long) = learningRevision.let { learning.dueCards(now) }
    internal fun learningDueCount(now: Long = System.currentTimeMillis()) =
        learningRevision.let { learning.countDue(now) }

    /** Short, bounded evidence summary; derived only from the currently active player's learning DB. */
    internal fun teacherLearningContext(): String = runCatching {
        val cards = searchLearning("", 0, 500)
        val trouble = cards.filter { it.wrong > 0 || it.hints > 0 || it.forgotten > 0 }
            .sortedByDescending { it.wrong * 3 + it.forgotten * 3 + it.hints }
            .take(8)
        val lines = trouble.joinToString("; ") { card ->
            "${card.answer.take(40)} (${card.clue.take(60)}): ${card.wrong} yanlış, ${card.hints} ipucu, ${card.forgotten} tekrar hatası"
        }
        "Tekrar bekleyen kelime: ${learningDueCount()}; üzerinde çalışılacak kelimeler: ${lines.ifBlank { "Henüz kayıtlı hata yok" }}"
            .take(1_500)
    }.getOrDefault("Öğrenme kayıtları şu anda okunamadı.")

    internal fun recordLearning(game: Game, signal: LearningSignal) {
        runCatching { learning.record(game.progressId, signal) }
            .onSuccess {
                if (it) learningRevision++
                learningError = null
            }
            .onFailure { learningError = "Kelime öğrenme kaydı yazılamadı: depolama alanını kontrol et." }
    }

    /** YDS vocabulary joins the same per-player FSRS notebook as crossword mistakes. */
    internal fun recordYdsAttempt(question: YdsQuestion, correct: Boolean, selected: String) {
        val focus = question.focusWord.trim().take(45)
        if (focus.isBlank() || !isEnglishAnswer(focus)) return
        val signal = LearningSignal(
            if (correct) LearningKind.SOLVED else LearningKind.WRONG_CHECK,
            Entry(focus, "YDS: ${question.topic.label}", question.explanation.take(150)),
            resetAt = System.currentTimeMillis(),
            attempt = if (correct) "" else selected.take(70),
            boardUpdatedAt = System.currentTimeMillis(),
        )
        runCatching { learning.record("yds-${question.id}", signal) }
            .onSuccess { if (it) learningRevision++ }
            .onFailure { learningError = "YDS tekrar kaydı saklanamadı." }
    }

    /** Save one completed Arena match without duplicating events on recomposition.
     * Stores mistakes and explicit 'Bilmiyorum' separately, never a rival's mistakes.
     */
    internal fun recordPremiumMatchLearning(gameId: String, summary: PremiumMatchSummary,
        modeLabel: String = "Arena"): Int {
        runCatching {
            learning.setPremiumFolderTitle(gameId, modeLabel)
            learning.attachPremiumWords(gameId, summary.all)
        }.onFailure { learningError = "Oyun klasörü oluşturulamadı." }

        var added = 0
        for (word in summary.review) {
            if (word.wrongAttempts > 0) {
                val signal = LearningSignal(LearningKind.WRONG_CHECK, word.entry,
                    resetAt = 0L, attempt = "", boardUpdatedAt = (word.index + 1).toLong())
                runCatching { learning.record(gameId, signal) }
                    .onSuccess { if (it) added++ }
                    .onFailure { learningError = "Yarışma kelimeleri kaydedilemedi. Depolama alanını kontrol et." }
            }
            if (word.unknown) {
                val signal = LearningSignal(LearningKind.UNKNOWN, word.entry,
                    resetAt = 0L, boardUpdatedAt = (word.index + 1).toLong())
                runCatching { learning.record(gameId, signal) }
                    .onSuccess { if (it) added++ }
                    .onFailure { learningError = "Bilinmeyen kelimeler kaydedilemedi. Depolama alanını kontrol et." }
            }
        }
        if (added > 0 || summary.all.isNotEmpty()) { learningRevision++; if (learningError == null) learningError = null }
        return added
    }

    internal fun reviewLearning(card: LearningCard, rating: ReviewRating) {
        runCatching { learning.review(card, rating) }
            .onSuccess {
                if (it) learningRevision++
                learningError = null
            }
            .onFailure { learningError = "Tekrar kaydı saklanamadı." }
    }

    internal fun pinLearning(card: LearningCard, pinned: Boolean) {
        runCatching { learning.pin(card, pinned) }
            .onSuccess {
                learningRevision++
                learningError = null
            }
            .onFailure { learningError = "Kelime defteri güncellenemedi." }
    }

    val accountAvailable get() = accounts.available
    val oauthWebClientId get() = accounts.clientId
    fun isValidOAuthId(id: String): Boolean = id.trim().matches(Regex("^[0-9]+-[A-Za-z0-9-]+\\.apps\\.googleusercontent\\.com$"))
    fun configureOAuth(id: String): Boolean = accounts.configureWebId(id)
    internal val social = FriendsHub(this.context, accounts)
    val profileName get() = if (accountName != null) social.nickname.ifBlank { accountName.orEmpty() }
        else "Misafir"
    val learningProfileName get() = profileName
    var accountName by mutableStateOf<String?>(null)
        private set
    var accountError by mutableStateOf<String?>(null)
        private set
    var accountStatus by mutableStateOf("")
        private set
    var signInBusy by mutableStateOf(false)
        private set
    var accountDeleteBusy by mutableStateOf(false)
        private set
    var accountDeleteError by mutableStateOf<String?>(null)
        private set
    private var revision by mutableIntStateOf(0)

    init { openAccount() }

    internal fun changePreferences(next: WordQuestPreferences) {
        preferenceStore.save(profileKey, next)
        preferences = next
    }

    val games: List<Game> get() = catalog.games + savedGames + originalGame
    private val savedGames: List<Game> get() = revision.let { player.savedGames() }
    val customGames: List<Game> get() = savedGames.filter { it.mode == GameMode.Custom }
    internal suspend fun vocabulary(): VocabularyBank = withContext(Dispatchers.Default) { catalog.vocabulary }
    val records: Map<String, Progress> get() {
        val progress = revision.let { player.progress() }
        return games.mapNotNull { game -> progress[game.progressId]?.let { game.id to it } }.toMap()
    }
    val totalPoints get() = revision.let { player.progress().values.sumOf(Progress::points) }
    val finishedCount get() = records.values.count(Progress::completed)
    val continueGame: Game? get() {
        val progress = records
        val allGames = games
        val last = allGames.firstOrNull { it.id == player.lastGame }
        if (last != null) {
            val record = progress[last.id]
            if (record == null || record.lockedWords.size != last.wordCount) return last
            nextGeneratedPart(last)?.let { return it }
        }
        return allGames.filter { game ->
            val record = progress[game.id]
            record != null && record.started && record.lockedWords.size != game.wordCount
        }.maxByOrNull { progress.getValue(it.id).updatedAt }
    }

    fun game(id: String) = catalog.byId[id] ?: originalGame.takeIf { it.id == id } ?: savedGames.firstOrNull { it.id == id }

    internal suspend fun newGame(level: String, topic: String?, kind: String?, size: Int): Game? {
        val playerAtStart = player
        val previous = savedGames.lastOrNull { it.mode == GameMode.Generated && it.id == player.lastGame }
            ?.puzzle?.words?.map { it.entry.letters }?.toSet().orEmpty()
        val game = withContext(Dispatchers.Default) {
            generateGame(catalog.vocabulary.pool(level, topic, kind), size, level, topic, kind, previous)
        } ?: return null
        return game.takeIf { playerAtStart === player && saveCustom(listOf(it), null) }
    }

    /** A 21–70 word session is several ordinary 8–20 word crossword boards.
     * All boards are generated and validated BEFORE saving any of them. This
     * avoids a half-created session if a group of words cannot form a board.
     */
    internal suspend fun newGameSeries(
        level: String, topic: String?, kind: String?, count: Int,
    ): Game? {
        require(count in 21..70)
        val playerAtStart = player
        val source = catalog.vocabulary.pool(level, topic, kind).distinctBy(Entry::letters)
        if (source.size < count) return null
        val split = automaticSplit(count, 20)
        require(split.sum() == count && split.all { it in answerCounts })
        val sessionId = java.util.UUID.randomUUID().toString()
        val parts = withContext(Dispatchers.Default) {
            var output: List<Game>? = null
            repeat(3) { attempt ->
                if (output != null) return@repeat
                val pool = source.shuffled(kotlin.random.Random(System.nanoTime() + attempt))
                val taken = mutableSetOf<String>()
                val built = mutableListOf<Game>()
                for ((position, target) in split.withIndex()) {
                    val choices = pool.filterNot { it.letters in taken }
                    val candidate = generateGame(
                        choices, target, level, topic, kind,
                        random = kotlin.random.Random(System.nanoTime() + position),
                    ) ?: break
                    if (candidate.wordCount != target) break
                    taken += candidate.puzzle.words.map { it.entry.letters }
                    built += Game(
                        id = "round_${sessionId}_part${position + 1}of${split.size}",
                        mode = GameMode.Generated,
                        level = candidate.level,
                        category = candidate.category,
                        title = "${candidate.title} · ${position + 1}/${split.size}",
                        puzzle = candidate.puzzle,
                        destination = candidate.destination,
                    )
                }
                if (built.size == split.size && taken.size == count) output = built
            }
            output
        } ?: return null
        return parts.first().takeIf {
            playerAtStart === player && saveCustom(parts, null)
        }
    }

    /** Only locate the next part within the same exact generated session. */
    internal fun nextGeneratedPart(game: Game): Game? {
        if (game.mode != GameMode.Generated) return null
        val match = Regex("^round_([0-9a-f-]{36})_part([1-9][0-9]*)of([1-9][0-9]*)$")
            .matchEntire(game.id) ?: return null
        val (groupId, numberText, totalText) = match.destructured
        val number = numberText.toIntOrNull() ?: return null
        val total = totalText.toIntOrNull() ?: return null
        if (number >= total || total !in 2..9) return null
        val nextId = "round_${groupId}_part${number + 1}of${total}"
        return savedGames.firstOrNull { it.id == nextId && it.mode == GameMode.Generated }
    }

    fun record(game: Game): Progress? = revision.let { player.find(game.progressId) }

    internal fun round(game: Game, progress: Progress): BoardRound = BoardRound.open(game.puzzle, progress.layout)

    internal suspend fun shuffle(game: Game): Boolean {
        val store = player
        val before = store.read(game.progressId)
        val board = round(game, before)
        val next = withContext(Dispatchers.Default) { shuffleBoard(board.puzzle) } ?: return false
        val current = this.game(game.id)
        val latest = store.read(game.progressId)
        if (store !== player || current?.progressId != game.progressId || latest.layout != before.layout) return false
        save(game, board.moveProgress(latest, next))
        return true
    }

    fun recordOpen(game: Game) {
        player.lastGame = game.id
        cloud?.saveSession()
        revision++
    }

    fun save(game: Game, progress: Progress): Progress {
        val previous = player.read(game.progressId)
        val merged = previous.merge(progress)
        if (previous != merged) {
            player.save(game.progressId, merged)
            cloud?.schedule(game.progressId)
            revision++
        }
        return merged
    }

    fun saveCustom(games: List<Game>, expectedModifiedAt: Long?): Boolean {
        if (expectedModifiedAt != null && game(games.single().id)?.modifiedAt != expectedModifiedAt) return false
        if (!player.saveCustom(games)) return false
        games.forEach { cloud?.saveCustom(it) }
        revision++
        return true
    }

    internal suspend fun backup(): PlayerBackup {
        val base = PlayerBackup(player.savedGames(), player.progress(), player.lastGame, player.lastGameAt)
        val learningForExport = learning
        val archive = withContext(Dispatchers.IO) { learningForExport.exportArchive() }
        return base.copy(learning = archive,
            yds = YdsProgressStore(context, profileKey).exportStats(),
            daily = DailyPlanStore(context, profileKey).exportDays())
    }

    internal fun restore(backup: PlayerBackup) {
        if (backup.yds.isNotEmpty()) YdsProgressStore(context, profileKey).importStats(backup.yds)
        if (backup.daily.isNotEmpty()) DailyPlanStore(context, profileKey).importDays(backup.daily)
        backup.learning?.let { archive ->
            learning.importArchive(archive)
            learningRevision++
        }
        player.importBackup(backup)
        val restoredIds = backup.games.map(Game::id).toSet()
        player.savedGames().filter { it.id in restoredIds }.forEach { cloud?.saveCustom(it) }
        backup.progress.keys.forEach { cloud?.schedule(it) }
        cloud?.saveSession()
        revision++
    }

    fun replay(game: Game) {
        val old = player.read(game.progressId)
        if (old.lockedWords.size == game.wordCount) {
            val round = round(game, old)
            val crossword = Crossword(round.puzzle, round.onBoard(old))
            crossword.reset()
            save(game, round.inStore(crossword.progress(), old.layoutAt))
        }
    }

    fun closeGame() {
        flush()
    }

    fun flush() = cloud?.flush()

    fun close() {
        cloud?.close()
        cloud = null
        social.disconnect()
        learning.close()
    }

    fun signIn() {
        if (signInBusy) return
        accountError = null
        scope.launch {
            signInBusy = true
            try {
                accountError = accounts.signIn()
                if (accounts.uid != null) openAccount()
            } finally {
                signInBusy = false
            }
        }
    }

    fun signInWithPassword(email: String, password: String) {
        if (signInBusy) return
        accountError = null
        scope.launch {
            signInBusy = true
            try {
                accountError = accounts.signInWithPassword(email, password)
                if (accountError == null && accounts.uid != null) openAccount()
            } finally { signInBusy = false }
        }
    }

    fun registerWithPassword(nickname: String, email: String, password: String) {
        if (signInBusy) return
        if (!ArenaNickname.isValid(nickname)) {
            accountError = "2-24 karakterli bir kullanıcı adı belirle."
            return
        }
        accountError = null
        scope.launch {
            signInBusy = true
            try {
                accountError = accounts.registerWithPassword(email, password)
                if (accountError == null && accounts.uid != null) {
                    openAccount()
                    social.saveName(nickname)
                }
            } finally { signInBusy = false }
        }
    }

    fun signOut() {
        close()
        accounts.signOut(scope)
        accountName = null
        accountStatus = ""
        accountError = null
        openLocalProfile()
    }

    /** Called only after the explicit two-button confirmation in Account screen. */
    fun deleteAccount(onBeforeLocalPurge: () -> Unit) {
        if (signInBusy || accountDeleteBusy || accounts.uid == null) return
        accountDeleteError = null
        accountDeleteBusy = true
        // Stop cloud listeners before removing their backing database nodes.
        cloud?.close()
        cloud = null
        social.disconnect()
        scope.launch {
            try {
                when (val result = accounts.deleteWordQuestAccount()) {
                    is AccountDeletionResult.Success -> {
                        val formerProfile = profileKey
                        // Close learning database and allow AI teacher to release its DB first.
                        learning.close()
                        onBeforeLocalPurge()
                        purgeDeletedAccountData(result.uid, formerProfile)
                        accountName = null
                        accountStatus = ""
                        accountError = null
                        openLocalProfile()
                    }
                    is AccountDeletionResult.Failed -> {
                        accountDeleteError = result.message
                        // On a failed deletion, renew account sync if still authenticated.
                        if (accounts.uid != null) {
                            val currentUid = accounts.uid!!
                            // If the identity could not be deleted after clearing cloud data,
                            // do not reupload old progress by reconnecting CloudSync.
                            if (!result.cloudCleared) {
                                cloud = accounts.connect(currentUid, player, scope,
                                    { revision++ }, { accountStatus = it })
                            }
                            social.connect(currentUid)
                        }
                        if (result.cloudCleared) {
                            accountStatus = "Bulut kayıtları temizlendi; hesap kimliği silinemedi. Tekrar giriş yapıp silme işlemini tamamla."
                        }
                    }
                }
            } catch (_: Exception) {
                accountDeleteError = "Hesap silinemedi. İnternet bağlantını kontrol et."
            } finally { accountDeleteBusy = false }
        }
    }

    private fun purgeDeletedAccountData(uid: String, formerProfile: String) {
        // Preserve ALL other profiles' and guests' data; only this Auth UID is removed.
        context.getSharedPreferences("player_v2_account_$uid", Context.MODE_PRIVATE)
            .edit().clear().commit()
        val digest = stableHash(uid).take(24)
        context.deleteDatabase("wq_learning_$digest.db")
        context.deleteDatabase("wq_teacher_${stableHash(formerProfile).take(24)}.db")
        context.getSharedPreferences("wq_daily_${stableHash(formerProfile).take(24)}", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("wq_yds_${stableHash(formerProfile).take(24)}", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("firebase_arena_nicknames", Context.MODE_PRIVATE)
            .edit().remove(uid).apply()
        context.getSharedPreferences("firebase_arena_budget", Context.MODE_PRIVATE)
            .edit().remove("rooms").apply()
        val prefs = context.getSharedPreferences("wordquest_preferences", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("$formerProfile.") }.forEach(editor::remove)
        editor.apply()
    }

    private fun openAccount() {
        val uid = accounts.uid ?: return
        close()
        val personal = PlayerStore(context, uid)
        if (localProfile == 1) personal.importGuest(guest)
        player = personal
        // Do not auto-copy guest learning history into another person's account.
        learning = LearningStore(context, uid)
        learningError = null
        learningRevision++
        accountName = if (accounts.isGuest) null else accounts.displayName ?: "Kayıtlı hesap"
        if (accountName != null) social.connect(uid)
        loadPreferences("account_${stableHash(uid)}")
        accountStatus = CloudSyncMessage.pending
        cloud = accounts.connect(uid, personal, scope, { revision++ }, { accountStatus = it })
        if (cloud == null) accountStatus = "Firebase veritabanı yapılandırılamadı; kayıtlar cihazda güvende."
        revision++
    }

    internal fun selectLocalProfile(profile: Int) {
        require(profile in 1..2)
        if (accountName != null || signInBusy || profile == localProfile) return
        learning.close()
        localProfile = profile
        localProfiles.edit().putInt("active", profile).apply()
        openLocalProfile()
    }

    private fun localPlayer() = if (localProfile == 2) secondPlayer else guest

    private fun localLearningStore() = LearningStore(context, null, secondLocalProfile = localProfile == 2)

    private fun openLocalProfile() {
        player = localPlayer()
        learning = localLearningStore()
        learningError = null
        learningRevision++
        loadPreferences("local_$localProfile")
        revision++
    }

    private fun loadPreferences(profile: String) {
        profileKey = profile
        preferences = preferenceStore.load(profile)
    }
}
