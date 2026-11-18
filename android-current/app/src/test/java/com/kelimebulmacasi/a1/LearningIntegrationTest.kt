package com.kelimebulmacasi.a1

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class LearningIntegrationTest {
    private val now = 1_700_000_000_000L
    private val word = Entry("MILK", "SÜT")
    private val wrong = LearningSignal(LearningKind.WRONG_CHECK, word, attempt = "MLIK", boardUpdatedAt = 5)
    private val key = learningWordKey(word)
    private lateinit var context: Context

    @Before
    fun reset() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("wq_learning_guest.db")
        context.deleteDatabase("wq_learning_local_2.db")
    }

    @Test
    fun historiesAndReviewStayInTheSelectedProfile() {
        LearningStore(context, null).use { first ->
            LearningStore(context, null, secondLocalProfile = true).use { second ->
                assertTrue(first.record("a1_food_v1", wrong, now))
                assertFalse(first.record("a1_food_v1", wrong, now + 1))
                for (at in 6L..7L) {
                    assertTrue(first.record("a1_food_v1", LearningSignal(LearningKind.HINT, word, boardUpdatedAt = at), now + at))
                }
                assertTrue(first.record("a1_food_v1", LearningSignal(LearningKind.SOLVED, word, assisted = true), now + 8))
                val card = first.cards("süt", 0, 2).single()
                assertEquals(card, first.cards("SUT", 0, 2).single())
                assertEquals(card, first.cards("milk", 0, 2).single())
                assertTrue(first.cards("%", 0, 2).isEmpty())
                assertEquals(1, card.solved)
                assertEquals(1, card.wrong)
                assertEquals(2, card.hints)
                assertEquals(4, first.history(key, 0, 10).size)
                assertEquals("MLIK", first.history(key, 0, 10).last().attempt)
                assertEquals(0, first.countDue(now))
                assertEquals(1, first.countDue(card.dueAt))
                assertFalse(first.review(card, ReviewRating.Good, card.dueAt - 1))
                assertTrue(first.review(card, ReviewRating.Good, card.dueAt))
                assertFalse(first.review(card, ReviewRating.Good, card.dueAt + 1))
                assertEquals(card.dueAt + 2 * LearningSchedule.DAY, first.card(key)?.dueAt)

                assertNull(second.card(key))
                assertTrue(second.record("a1_food_v1", wrong.copy(attempt = "MELK"), now))
                assertEquals("MELK", second.history(key, 0, 10).single().attempt)
                assertEquals("MLIK", first.history(key, 0, 10).last().attempt)
            }
        }
        LearningStore(context, null).use { reopened ->
            assertEquals(5, reopened.history(key, 0, 10).size)
        }
    }

    @Test
    fun backupsRoundTripAndOldJsonAndXmlStillOpen() {
        LearningStore(context, null).use { first ->
            assertTrue(first.record("a1_food_v1", wrong, now))
            first.pin(first.card(key)!!, true)
            val puzzle = Crossword(foodPuzzle).apply { enter("WATER") }.progress()
            val previous = PlayerBackup(emptyList(), mapOf("a1_food_v1" to puzzle), "a1_food_v1", now)
            val legacyJson = decodeBackupFile(previous.encode().toByteArray())
            assertEquals(previous, legacyJson)
            val xml = """<map><string name="progress:a1_food_v1">${puzzle.toJson()}</string><string name="last_game">a1_food_v1</string></map>"""
            assertEquals(puzzle, decodeBackupFile(xml.toByteArray()).progress["a1_food_v1"])

            val encoded = previous.copy(learning = first.exportArchive()).encode()
            val imported = decodeBackupFile(encoded.toByteArray())
            assertEquals(previous.progress, imported.progress)
            val archive = imported.learning!!
            LearningStore(context, null, secondLocalProfile = true).use { second ->
                val json = JSONObject(encoded).getJSONObject("learning")
                json.getJSONArray("cards").getJSONObject(0).remove("dueAt")
                val oldEvent = json.getJSONArray("events").getJSONArray(0)
                json.put("events", JSONArray().put(JSONArray(List(6) { oldEvent[it] })))
                val oldArchive = json.toLearningArchive()
                assertEquals("", oldArchive.events.single().attempt)
                second.importArchive(oldArchive)
                second.importArchive(archive)
                second.importArchive(archive)
                assertEquals(first.card(key), second.card(key))
                assertEquals("MLIK", second.history(key, 0, 10).single().attempt)
            }
        }
    }

    @Test
    fun migratesPreviousSqliteSchemaWithoutRecreatingHistory() {
        val dbFile = context.getDatabasePath("wq_learning_guest.db")
        dbFile.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL("""CREATE TABLE learning_cards (
                word_key TEXT PRIMARY KEY NOT NULL, answer TEXT NOT NULL, clue TEXT NOT NULL, context TEXT NOT NULL,
                solved INTEGER NOT NULL DEFAULT 0, wrong INTEGER NOT NULL DEFAULT 0, hints INTEGER NOT NULL DEFAULT 0,
                remembered INTEGER NOT NULL DEFAULT 0, forgotten INTEGER NOT NULL DEFAULT 0, step INTEGER NOT NULL DEFAULT 0,
                due_at INTEGER NOT NULL DEFAULT 0, last_at INTEGER NOT NULL DEFAULT 0, pinned INTEGER NOT NULL DEFAULT 0)""")
            db.execSQL("""CREATE TABLE learning_events (
                event_id TEXT PRIMARY KEY NOT NULL, word_key TEXT NOT NULL REFERENCES learning_cards(word_key),
                game_id TEXT NOT NULL, type TEXT NOT NULL, occurred_at INTEGER NOT NULL, assisted INTEGER NOT NULL DEFAULT 0)""")
            db.execSQL("CREATE INDEX idx_events_word ON learning_events(word_key)")
            val legacyDue = now + 3 * LearningSchedule.DAY
            db.execSQL("INSERT INTO learning_cards (word_key,answer,clue,context,wrong,remembered,due_at,last_at) VALUES (?,?,?,?,1,1,?,?)",
                arrayOf(key, word.answer, word.clue, "", legacyDue, now + 1))
            db.execSQL("INSERT INTO learning_events VALUES (?,?,?,'WRONG_CHECK',?,0)",
                arrayOf("existing_event", key, "a1_food_v1", now))
            db.execSQL("INSERT INTO learning_events VALUES (?,?,?,'REVIEW_GOOD',?,0)",
                arrayOf("existing_review", key, "personal_review", now + 1))
            db.version = 1
        }
        LearningStore(context, null).use { migrated ->
            val card = migrated.cards("sut", 0, 2).single()
            assertEquals(1, card.wrong)
            assertEquals(1, card.remembered)
            assertEquals(now + 3 * LearningSchedule.DAY, card.dueAt)
            assertEquals(2.3065, card.memory!!.stability, 1e-10)
            assertEquals(listOf("existing_review", "existing_event"), migrated.history(key, 0, 2).map { it.id })
            assertEquals("", migrated.history(key, 0, 2).last().attempt)
            assertEquals(4, migrated.readableDatabase.version)
            LearningStore(context, null, secondLocalProfile = true).use { other ->
                other.importArchive(migrated.exportArchive().toJson().toLearningArchive())
                assertEquals(card, other.card(key))
            }
        }
    }

    @Test
    fun settingsAreIndependentAndPersistAcrossInstances() {
        val store = PreferenceStore(context)
        val first = WordQuestPreferences(AppTextSize.ExtraLarge, pronunciation = false, voice = EnglishVoice.British)
        val second = WordQuestPreferences(AppTextSize.Large, pronunciation = true, autoPronunciation = false)
        store.save("local_1", first)
        store.save("local_2", second)
        val reopened = PreferenceStore(context)
        assertEquals(first, reopened.load("local_1"))
        assertEquals(second, reopened.load("local_2"))
        assertEquals(WordQuestPreferences(), reopened.load("account_example"))
    }

    @Test
    fun newReviewRatingsAndSameMillisecondOrderSurviveJson() {
        LearningStore(context, null).use { original ->
            original.record("a1_food_v1", wrong, now)
            original.record("a1_food_v1", LearningSignal(LearningKind.HINT, word, boardUpdatedAt = 6), now)
            val due = original.card(key)!!.dueAt
            assertTrue(original.review(original.card(key)!!, ReviewRating.Hard, due))
            val next = original.card(key)!!.dueAt
            assertTrue(original.review(original.card(key)!!, ReviewRating.Easy, next))
            val ordering = original.history(key, 0, 10).map { it.kind }
            assertEquals(listOf(LearningKind.REVIEW_EASY, LearningKind.REVIEW_HARD, LearningKind.HINT,
                LearningKind.WRONG_CHECK), ordering)
            LearningStore(context, null, secondLocalProfile = true).use { other ->
                other.importArchive(original.exportArchive().toJson().toLearningArchive())
                assertEquals(original.card(key), other.card(key))
                assertEquals(ordering, other.history(key, 0, 10).map { it.kind })
            }
        }
    }
}
