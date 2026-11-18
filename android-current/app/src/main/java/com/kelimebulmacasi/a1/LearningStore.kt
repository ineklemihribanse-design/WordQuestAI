package com.kelimebulmacasi.a1

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

/** One private database per signed-in identity (and a separate guest database).
 * No server account IDs are exposed in on-disk database names.
 */
internal class LearningStore(context: Context, uid: String?, secondLocalProfile: Boolean = false) : SQLiteOpenHelper(
    context.applicationContext,
    when {
        uid != null -> "wq_learning_${stableHash(uid).take(24)}.db"
        secondLocalProfile -> "wq_learning_local_2.db"
        else -> "wq_learning_guest.db"
    },
    null, 4,
) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE learning_cards (
            word_key TEXT PRIMARY KEY NOT NULL,
            answer TEXT NOT NULL, clue TEXT NOT NULL, context TEXT NOT NULL,
            solved INTEGER NOT NULL DEFAULT 0, wrong INTEGER NOT NULL DEFAULT 0,
            hints INTEGER NOT NULL DEFAULT 0, remembered INTEGER NOT NULL DEFAULT 0,
            forgotten INTEGER NOT NULL DEFAULT 0,
            due_at INTEGER NOT NULL DEFAULT 0, last_at INTEGER NOT NULL DEFAULT 0,
            pinned INTEGER NOT NULL DEFAULT 0, search_text TEXT NOT NULL,
            stability REAL NOT NULL DEFAULT 0, difficulty REAL NOT NULL DEFAULT 0, reviewed_at INTEGER NOT NULL DEFAULT 0
        )""")
        db.execSQL("""CREATE TABLE learning_events (
            event_id TEXT PRIMARY KEY NOT NULL,
            word_key TEXT NOT NULL,
            game_id TEXT NOT NULL,
            type TEXT NOT NULL,
            occurred_at INTEGER NOT NULL,
            assisted INTEGER NOT NULL DEFAULT 0,
            attempt TEXT NOT NULL DEFAULT '',
            event_order INTEGER,
            FOREIGN KEY(word_key) REFERENCES learning_cards(word_key)
        )""")
        db.execSQL("CREATE INDEX idx_cards_due ON learning_cards(due_at)")
        db.execSQL("CREATE INDEX idx_events_word_time ON learning_events(word_key,occurred_at,event_order,event_id)")
        createFolderTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        require(oldVersion in 1..3 && newVersion == 4) { "Missing learning database migration $oldVersion -> $newVersion" }
        if (oldVersion < 3) {
        if (oldVersion == 1) migrateSearch(db)
        db.execSQL("ALTER TABLE learning_cards ADD COLUMN stability REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE learning_cards ADD COLUMN difficulty REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE learning_cards ADD COLUMN reviewed_at INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE learning_events ADD COLUMN event_order INTEGER")
        db.execSQL("DROP INDEX IF EXISTS idx_events_word_time")
        db.execSQL("CREATE INDEX idx_events_word_time ON learning_events(word_key,occurred_at,event_order,event_id)")
        db.rawQuery("SELECT word_key FROM learning_cards", null).use { cards ->
            val memory = ContentValues()
            while (cards.moveToNext()) {
                val key = cards.getString(0)
                val previous = db.rawQuery(
                    "SELECT type,occurred_at FROM learning_events WHERE word_key=? AND type LIKE 'REVIEW_%' ORDER BY occurred_at,event_id",
                    arrayOf(key),
                ).use { events ->
                    var state: ReviewMemory? = null
                    while (events.moveToNext()) {
                        val rating = LearningKind.valueOf(events.getString(0)).rating ?: continue
                        state = Fsrs.update(state, rating, events.getLong(1))
                    }
                    state
                }
                if (previous != null) {
                    memory.put("stability", previous.stability)
                    memory.put("difficulty", previous.difficulty)
                    memory.put("reviewed_at", previous.reviewedAt)
                    db.update("learning_cards", memory, "word_key=?", arrayOf(key))
                }
            }
        }
        }
        createFolderTables(db)
        rebuildFolders(db)
    }

    private fun migrateSearch(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE learning_cards ADD COLUMN search_text TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE learning_events ADD COLUMN attempt TEXT NOT NULL DEFAULT ''")
        db.execSQL("DROP INDEX IF EXISTS idx_events_word")
        db.rawQuery("SELECT word_key,answer,clue FROM learning_cards", null).use { cursor ->
            while (cursor.moveToNext()) {
                val search = learningSearchTerm(cursor.getString(1) + "\n" + cursor.getString(2))
                db.execSQL("UPDATE learning_cards SET search_text=? WHERE word_key=?",
                    arrayOf(search, cursor.getString(0)))
            }
        }
    }

    /** Atomically updates the card only if the unique event was inserted. */
    fun record(
        gameId: String, signal: LearningSignal, now: Long = System.currentTimeMillis(),
        expectedDueAt: Long? = null,
    ): Boolean {
        val key = learningWordKey(signal.entry)
        val id = if (signal.kind.rating != null)
            UUID.randomUUID().toString() else learningEventId(gameId, signal)
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val old = loadCard(db, key)
            if (expectedDueAt != null && (old?.dueAt != expectedDueAt || expectedDueAt !in 1..now)) {
                return false
            }
            val base = old ?: LearningCard(key, signal.entry.answer, signal.entry.clue, signal.entry.context)
            if (old == null) writeCard(db, base)
            // Order new events from the same millisecond so a backup can replay them deterministically.
            val at = maxOf(now, base.lastAt)
            val order = db.rawQuery(
                "SELECT COALESCE(MAX(event_order),0) FROM learning_events WHERE word_key=? AND occurred_at=?",
                arrayOf(key, at.toString()),
            ).use { it.moveToFirst(); it.getLong(0) + 1 }
            val event = ContentValues().apply {
                put("event_id", id)
                put("word_key", key)
                put("game_id", gameId)
                put("type", signal.kind.name)
                put("occurred_at", at)
                put("assisted", if (signal.assisted) 1 else 0)
                put("attempt", if (signal.kind == LearningKind.WRONG_CHECK) englishLetters(signal.attempt) else "")
                put("event_order", order)
            }
            val inserted = db.insertWithOnConflict("learning_events", null, event, SQLiteDatabase.CONFLICT_IGNORE) != -1L
            if (inserted) {
                writeCard(db, LearningSchedule.next(base, signal.kind, at, signal.assisted))
                addFolderEvent(db, gameId, key, signal.kind, at)
            }
            db.setTransactionSuccessful()
            inserted
        } finally {
            db.endTransaction()
        }
    }

    @Synchronized
    fun exportArchive(): LearningArchive {
        val db = readableDatabase
        db.beginTransactionNonExclusive()
        try {
            val archive = exportArchive(db)
            db.setTransactionSuccessful()
            return archive
        } finally {
            db.endTransaction()
        }
    }

    private fun exportArchive(db: SQLiteDatabase): LearningArchive {
        val allCards = db.rawQuery(
            "SELECT $CARD_COLUMNS FROM learning_cards",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    require(size < 50_000) { "Too many cards for one backup" }
                    add(cursor.learningCard())
                }
            }
        }
        val events = db.rawQuery(
            "SELECT $EVENT_COLUMNS FROM learning_events ORDER BY occurred_at,event_order,event_id",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    require(size < 200_000) { "Too many events for one backup" }
                    add(cursor.learningEvent())
                }
            }
        }
        return LearningArchive(allCards, events)
    }

    /** Merge distinct events, then rebuild derived scheduling in chronological order. */
    fun importArchive(archive: LearningArchive) {
        val db = writableDatabase
        val importedCounts = archive.events.groupingBy { it.key }.eachCount()
        db.beginTransaction()
        try {
            for (card in archive.cards) {
                val existing = loadCard(db, card.key)
                if (existing == null) writeCard(db, card)
                else if (card.pinned && !existing.pinned) writeCard(db, existing.copy(pinned = true))
            }
            for (event in archive.events) {
                val values = ContentValues().apply {
                    put("event_id", event.id)
                    put("word_key", event.key)
                    put("game_id", event.gameId)
                    put("type", event.kind.name)
                    put("occurred_at", event.at)
                    put("assisted", if (event.assisted) 1 else 0)
                    put("attempt", event.attempt)
                    event.order?.let { put("event_order", it) }
                }
                db.insertWithOnConflict("learning_events", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                if (event.attempt.isNotEmpty()) {
                    val detail = ContentValues().apply { put("attempt", event.attempt) }
                    db.update("learning_events", detail, "event_id=? AND word_key=? AND type=? AND attempt=''",
                        arrayOf(event.id, event.key, LearningKind.WRONG_CHECK.name))
                }
            }
            for (card in archive.cards) {
                val metadata = loadCard(db, card.key) ?: continue
                var derived = LearningCard(metadata.key, metadata.answer, metadata.clue,
                    metadata.context, pinned = metadata.pinned)
                var eventCount = 0
                db.rawQuery(
                    "SELECT type,occurred_at,assisted FROM learning_events WHERE word_key=? ORDER BY occurred_at,event_order,event_id",
                    arrayOf(card.key),
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        eventCount++
                        derived = LearningSchedule.next(derived,
                            LearningKind.valueOf(cursor.getString(0)), cursor.getLong(1), cursor.getInt(2) == 1)
                    }
                }
                // Keep a stored date when the history behind it is unchanged.
                if (eventCount == 0 || derived.copy(dueAt = metadata.dueAt) == metadata) continue
                if (card.dueAt > 0 && eventCount == importedCounts[card.key]) {
                    derived = derived.copy(dueAt = card.dueAt)
                }
                writeCard(db, derived)
            }
            rebuildFolders(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun review(card: LearningCard, rating: ReviewRating, now: Long = System.currentTimeMillis()): Boolean = record(
        "personal_review",
        LearningSignal(LearningKind.forRating(rating),
            Entry(card.answer, card.clue, card.context)),
        now = now, expectedDueAt = card.dueAt,
    )

    fun pin(card: LearningCard, pinned: Boolean) {
        val values = ContentValues().apply { put("pinned", if (pinned) 1 else 0) }
        writableDatabase.update("learning_cards", values, "word_key=?", arrayOf(card.key))
    }

    fun countCards(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM learning_cards", null)
        .use { it.moveToFirst(); it.getInt(0) }

    fun countDue(now: Long = System.currentTimeMillis()): Int = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM learning_cards WHERE due_at > 0 AND due_at <= ?",
        arrayOf(now.toString()),
    ).use { it.moveToFirst(); it.getInt(0) }

    fun cards(search: String, offset: Int, limit: Int): List<LearningCard> = readableDatabase.rawQuery(
        """SELECT $CARD_COLUMNS
           FROM learning_cards WHERE instr(search_text,?) > 0
           ORDER BY pinned DESC,last_at DESC,word_key LIMIT ? OFFSET ?""",
        arrayOf(learningSearchTerm(search.take(80)), limit.toString(), offset.toString()),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.learningCard())
        }
    }

    fun dueCards(now: Long = System.currentTimeMillis(), limit: Int = 100): List<LearningCard> = readableDatabase.rawQuery(
        """SELECT $CARD_COLUMNS
           FROM learning_cards WHERE due_at > 0 AND due_at <= ? ORDER BY due_at,word_key LIMIT ?""",
        arrayOf(now.toString(), limit.coerceIn(1, 500).toString()),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.learningCard())
        }
    }

    fun card(key: String): LearningCard? = loadCard(readableDatabase, key)

    fun history(key: String, offset: Int, limit: Int): List<ArchivedLearningEvent> = readableDatabase.rawQuery(
        """SELECT $EVENT_COLUMNS FROM learning_events
           WHERE word_key=? ORDER BY occurred_at DESC,event_order DESC,event_id DESC LIMIT ? OFFSET ?""",
        arrayOf(key, limit.toString(), offset.toString()),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.learningEvent())
        }
    }

    private fun Cursor.learningEvent() = ArchivedLearningEvent(
        getString(0), getString(1), getString(2), LearningKind.valueOf(getString(3)),
        getLong(4), getInt(5) == 1, getString(6), if (isNull(7)) null else getLong(7),
    )

    private fun loadCard(db: SQLiteDatabase, key: String): LearningCard? = db.rawQuery(
        "SELECT $CARD_COLUMNS FROM learning_cards WHERE word_key=?", arrayOf(key),
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.learningCard() else null
    }

    private fun Cursor.learningCard() = LearningCard(
        getString(0), getString(1), getString(2), getString(3),
        getInt(4), getInt(5), getInt(6), getInt(7), getInt(8),
        getLong(9), getLong(10), getInt(11) == 1,
        getLong(14).takeIf { it > 0 }?.let { ReviewMemory(getDouble(12), getDouble(13), it) },
    )

    private fun writeCard(db: SQLiteDatabase, card: LearningCard) {
        val values = ContentValues().apply {
            put("word_key", card.key)
            put("answer", card.answer)
            put("clue", card.clue)
            put("context", card.context)
            put("solved", card.solved)
            put("wrong", card.wrong)
            put("hints", card.hints)
            put("remembered", card.remembered)
            put("forgotten", card.forgotten)
            put("due_at", card.dueAt)
            put("last_at", card.lastAt)
            put("pinned", if (card.pinned) 1 else 0)
            put("search_text", learningSearchTerm(card.answer + "\n" + card.clue))
            put("stability", card.memory?.stability ?: 0.0)
            put("difficulty", card.memory?.difficulty ?: 0.0)
            put("reviewed_at", card.memory?.reviewedAt ?: 0L)
        }
        // Do not use REPLACE: it deletes the old row and may violate event FKs.
        if (db.update("learning_cards", values, "word_key=?", arrayOf(card.key)) == 0) {
            db.insertOrThrow("learning_cards", null, values)
        }
    }

    private fun createFolderTables(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS learning_folders (
            folder_id TEXT PRIMARY KEY NOT NULL,
            title TEXT NOT NULL, created_at INTEGER NOT NULL,
            kind TEXT NOT NULL, customized INTEGER NOT NULL DEFAULT 0
        )""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS learning_folder_words (
            folder_id TEXT NOT NULL, word_key TEXT NOT NULL,
            status TEXT NOT NULL, added_at INTEGER NOT NULL,
            PRIMARY KEY(folder_id, word_key),
            FOREIGN KEY(folder_id) REFERENCES learning_folders(folder_id),
            FOREIGN KEY(word_key) REFERENCES learning_cards(word_key)
        )""")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_folder_words ON learning_folder_words(folder_id,added_at)")
    }

    /** Backfill from durable event history; existing cards and personal review schedules are untouched. */
    private fun rebuildFolders(db: SQLiteDatabase) {
        db.rawQuery("""SELECT game_id,word_key,type,occurred_at FROM learning_events
            WHERE type IN ('SOLVED','WRONG_CHECK','HINT','UNKNOWN')
            ORDER BY occurred_at,event_order,event_id""", null).use { cursor ->
            while (cursor.moveToNext()) {
                val kind = LearningKind.valueOf(cursor.getString(2))
                addFolderEvent(db, cursor.getString(0), cursor.getString(1), kind, cursor.getLong(3))
            }
        }
    }

    private fun ensureFolder(db: SQLiteDatabase, id: String, at: Long) {
        val data = ContentValues().apply {
            put("folder_id", id)
            put("title", LearningCollections.defaultTitle(id, at))
            put("created_at", at)
            put("kind", if (id == LearningCollections.NEW_WORDS) "normal" else "premium")
            put("customized", 0)
        }
        db.insertWithOnConflict("learning_folders", null, data, SQLiteDatabase.CONFLICT_IGNORE)
    }

    private fun addFolderEvent(db: SQLiteDatabase, gameId: String, key: String,
        kind: LearningKind, at: Long) {
        val folder = LearningCollections.folderFor(gameId, kind) ?: return
        ensureFolder(db, folder, at)
        val status = LearningCollections.status(kind)
        val old = db.rawQuery("SELECT status FROM learning_folder_words WHERE folder_id=? AND word_key=?",
            arrayOf(folder, key)).use { if (it.moveToFirst()) it.getString(0) else null }
        if (old == null) {
            val values = ContentValues().apply {
                put("folder_id", folder); put("word_key", key)
                put("status", status); put("added_at", at)
            }
            db.insertWithOnConflict("learning_folder_words", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        } else if (old != LearningCollections.mergeStatus(old, status)) {
            val values = ContentValues().apply { put("status", LearningCollections.mergeStatus(old, status)) }
            db.update("learning_folder_words", values, "folder_id=? AND word_key=?", arrayOf(folder, key))
        }
    }

    /** An entire Arena game gets one dated folder, rather than one duplicate per word. */
    fun setPremiumFolderTitle(gameId: String, mode: String, now: Long = System.currentTimeMillis()) {
        if (!gameId.startsWith("premium-")) return
        val db = writableDatabase
        ensureFolder(db, gameId, now)
        val current = db.rawQuery("SELECT title,customized FROM learning_folders WHERE folder_id=?",
            arrayOf(gameId)).use { if (it.moveToFirst()) Pair(it.getString(0), it.getInt(1)) else null }
        if (current?.second == 0 && current.first.endsWith("Arena")) {
            val title = LearningCollections.defaultTitle(gameId, now).removeSuffix("Arena") +
                LearningCollections.safeName(mode)
            val data = ContentValues().apply { put("title", title) }
            db.update("learning_folders", data, "folder_id=?", arrayOf(gameId))
        }
    }

    fun attachPremiumWords(gameId: String, words: List<PremiumReviewWord>, now: Long = System.currentTimeMillis()) {
        if (!gameId.startsWith("premium-")) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            ensureFolder(db, gameId, now)
            for (word in words) {
                val key = learningWordKey(word.entry)
                if (loadCard(db, key) == null)
                    writeCard(db, LearningCard(key, word.entry.answer, word.entry.clue, word.entry.context))
                // Preserve both tags when a player answered incorrectly then chose Bilmiyorum.
                if (word.wrongAttempts > 0) addFolderEvent(db, gameId, key, LearningKind.WRONG_CHECK, now)
                if (word.unknown) addFolderEvent(db, gameId, key, LearningKind.UNKNOWN, now)
                if (!word.unknown && word.wrongAttempts == 0)
                    addFolderEvent(db, gameId, key, LearningKind.SOLVED, now)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun folders(): List<LearningFolder> = readableDatabase.rawQuery("""SELECT f.folder_id,f.title,
        f.created_at,COUNT(w.word_key),
        COALESCE(SUM(CASE WHEN instr(w.status,'YANLIS')>0 THEN 1 ELSE 0 END),0),
        COALESCE(SUM(CASE WHEN instr(w.status,'BILINMIYOR')>0 THEN 1 ELSE 0 END),0),f.kind
        FROM learning_folders f LEFT JOIN learning_folder_words w ON w.folder_id=f.folder_id
        GROUP BY f.folder_id ORDER BY CASE WHEN f.kind='normal' THEN 0 ELSE 1 END,
        f.created_at DESC""", null).use { cursor ->
        buildList { while (cursor.moveToNext()) {
            add(LearningFolder(cursor.getString(0), cursor.getString(1), cursor.getLong(2),
                cursor.getInt(3), cursor.getInt(4), cursor.getInt(5), cursor.getString(6)))
        } }
    }

    fun folderCards(folderId: String, status: String = "", limit: Int = 5000): List<LearningCard> =
        readableDatabase.rawQuery("""SELECT $FOLDER_CARD_COLUMNS FROM learning_cards c
            INNER JOIN learning_folder_words w ON w.word_key=c.word_key
            WHERE w.folder_id=? AND (?='' OR instr(w.status,?)>0)
            ORDER BY w.added_at DESC,c.word_key LIMIT ?""",
            arrayOf(folderId, status, status, limit.coerceIn(1, 5000).toString())
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.learningCard()) } }

    fun renameFolder(folderId: String, newTitle: String): Boolean {
        val value = LearningCollections.safeName(newTitle)
        if (value.isEmpty()) return false
        val data = ContentValues().apply { put("title", value); put("customized", 1) }
        return writableDatabase.update("learning_folders", data, "folder_id=?", arrayOf(folderId)) == 1
    }

    /** Explicit practice may occur before a card's due date. */
    fun practiceAny(card: LearningCard, rating: ReviewRating): Boolean = record(
        "personal_card_practice", LearningSignal(LearningKind.forRating(rating),
            Entry(card.answer, card.clue, card.context)))

    private companion object {
        const val CARD_COLUMNS = "word_key,answer,clue,context,solved,wrong,hints,remembered,forgotten," +
            "due_at,last_at,pinned,stability,difficulty,reviewed_at"
        val FOLDER_CARD_COLUMNS = CARD_COLUMNS.split(",").joinToString(",") { "c.$it" }
        const val EVENT_COLUMNS = "event_id,word_key,game_id,type,occurred_at,assisted,attempt,event_order"
    }
}
