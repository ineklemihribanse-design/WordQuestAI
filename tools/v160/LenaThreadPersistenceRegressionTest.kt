package com.kelimebulmacasi.a1

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** Regression: previously the first successful Lena response raised
 * IllegalArgumentException AFTER being saved, at setSetting("ethan_thread_ids").
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class LenaThreadPersistenceRegressionTest {
    private val profile = "local_lena_thread_persistence_regression_v160"
    private val dbName = "wq_teacher_${stableHash(profile).take(24)}.db"

    @Test fun firstLenaResponseSavesThreadIdAndReopensWithoutException() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(dbName)
        lateinit var threadId: String
        try {
            TeacherStore(context, profile).use { store ->
                val reply = ConversationTurn(
                    1L, "Hello Lena", "Hello! How are you?",
                    ConversationLanguage.English,
                    JSONArray().put(JSONObject().put("text", "Hello! How are you?"))
                )
                threadId = store.append(null, reply, "A2")
                // This is the exact marker AiSession.persistEthanThreadIds writes.
                store.setSetting("ethan_thread_ids", threadId)
                assertEquals(threadId, store.setting("ethan_thread_ids"))
                assertEquals(1, store.turns(threadId).size)
            }
            TeacherStore(context, profile).use { reopened ->
                assertEquals(threadId, reopened.setting("ethan_thread_ids"))
                assertEquals("Hello Lena", reopened.turns(threadId).single().message)
                assertEquals("Hello! How are you?", reopened.turns(threadId).single().answer)
            }
        } finally {
            context.deleteDatabase(dbName)
        }
    }

    @Test fun unrelatedSettingsStayForbidden() {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(dbName)
        try {
            TeacherStore(context, profile).use { store ->
                try {
                    store.setSetting("api_key", "should-not-be-stored")
                    fail("Should reject unapproved settings")
                } catch (_: IllegalArgumentException) { }
                store.setSetting("personality", "Kind")
                assertEquals("Kind", store.setting("personality"))
            }
        } finally {
            context.deleteDatabase(dbName)
        }
    }
}
