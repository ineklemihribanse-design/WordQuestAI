package com.kelimebulmacasi.a1

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import java.util.UUID

/** Completely separate on-device teacher history for every local / signed-in profile.
 * No v0.7.5 database is opened, migrated, modified or replaced.
 */
internal class TeacherStore(context: Context, profileKey: String) : SQLiteOpenHelper(
    context.applicationContext,
    "wq_teacher_${stableHash(profileKey).take(24)}.db",
    null, 1,
) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE teacher_threads (
            id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL,
            created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
            language TEXT NOT NULL, level TEXT NOT NULL
        )""")
        db.execSQL("""CREATE TABLE teacher_turns (
            thread_id TEXT NOT NULL, turn_id INTEGER NOT NULL,
            question TEXT NOT NULL, answer TEXT NOT NULL,
            language TEXT NOT NULL, model_parts TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            PRIMARY KEY(thread_id, turn_id),
            FOREIGN KEY(thread_id) REFERENCES teacher_threads(id) ON DELETE CASCADE
        )""")
        db.execSQL("CREATE INDEX idx_teacher_threads_updated ON teacher_threads(updated_at DESC)")
        db.execSQL("CREATE INDEX idx_teacher_turns_thread ON teacher_turns(thread_id,turn_id)")
        db.execSQL("CREATE TABLE teacher_settings (setting TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException("No teacher database migration defined: $oldVersion -> $newVersion")
    }

    fun setting(name: String): String = readableDatabase.rawQuery(
        "SELECT value FROM teacher_settings WHERE setting=?", arrayOf(name),
    ).use { c -> if (c.moveToFirst()) c.getString(0) else "" }

    fun setSetting(name: String, value: String) {
        require(name in setOf("avatar", "notes", "personality"))
        writableDatabase.insertWithOnConflict("teacher_settings", null, ContentValues().apply {
            put("setting", name); put("value", value.take(1500))
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun notes(): String = readableDatabase.rawQuery(
        "SELECT value FROM teacher_settings WHERE setting='notes'", null,
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else "" }

    fun setNotes(notes: String) {
        writableDatabase.insertWithOnConflict("teacher_settings", null, ContentValues().apply {
            put("setting", "notes")
            put("value", notes.take(1_500))
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun threads(search: String = "", limit: Int = 100): List<TeacherThread> {
        val term = search.trim()
        val escaped = "%" + term.replace("\\", "\\\\").replace("%", "\\%")
            .replace("_", "\\_") + "%"
        val sql = if (term.isEmpty())
            "SELECT id,title,updated_at,language,level FROM teacher_threads ORDER BY updated_at DESC LIMIT ?"
        else """SELECT t.id,t.title,t.updated_at,t.language,t.level
            FROM teacher_threads t WHERE t.title LIKE ? ESCAPE '\' OR EXISTS (
                SELECT 1 FROM teacher_turns m WHERE m.thread_id=t.id AND
                (m.question LIKE ? ESCAPE '\' OR m.answer LIKE ? ESCAPE '\')
            ) ORDER BY t.updated_at DESC LIMIT ?"""
        val args = if (term.isEmpty()) arrayOf(limit.toString())
            else arrayOf(escaped, escaped, escaped, limit.toString())
        return readableDatabase.rawQuery(sql, args).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(TeacherThread(
                    cursor.getString(0), cursor.getString(1), cursor.getLong(2),
                    cursor.getString(3), cursor.getString(4),
                ))
            }
        }
    }

    fun turns(threadId: String): List<ConversationTurn> = readableDatabase.rawQuery(
        "SELECT turn_id,question,answer,language,model_parts FROM teacher_turns WHERE thread_id=? ORDER BY turn_id",
        arrayOf(threadId),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val language = ConversationLanguage.entries.firstOrNull { it.name == cursor.getString(3) }
                    ?: ConversationLanguage.English
                val parts = runCatching { JSONArray(cursor.getString(4)) }.getOrElse {
                    JSONArray().put(org.json.JSONObject().put("text", cursor.getString(2)))
                }
                add(ConversationTurn(cursor.getLong(0), cursor.getString(1), cursor.getString(2), language, parts))
            }
        }
    }

    /** A thread and its first message are inserted in a single transaction. */
    fun append(threadId: String?, turn: ConversationTurn, level: String): String {
        val id = threadId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (threadId == null) {
                db.insertOrThrow("teacher_threads", null, ContentValues().apply {
                    put("id", id)
                    put("title", turn.message.trim().replace(Regex("\\s+"), " ").take(56))
                    put("created_at", now)
                    put("updated_at", now)
                    put("language", turn.language.name)
                    put("level", level)
                })
            }
            db.insertOrThrow("teacher_turns", null, ContentValues().apply {
                put("thread_id", id)
                put("turn_id", turn.id)
                put("question", turn.message)
                put("answer", turn.answer)
                put("language", turn.language.name)
                put("model_parts", turn.modelParts.toString())
                put("created_at", now)
            })
            db.execSQL("UPDATE teacher_threads SET updated_at=? WHERE id=?", arrayOf(now, id))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return id
    }

    fun delete(threadId: String) {
        writableDatabase.delete("teacher_threads", "id=?", arrayOf(threadId))
    }
}

internal data class TeacherThread(
    val id: String, val title: String, val updatedAt: Long, val language: String, val level: String,
)
