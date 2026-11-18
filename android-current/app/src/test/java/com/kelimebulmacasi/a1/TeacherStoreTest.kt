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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class TeacherStoreTest {
    @Test
    fun searchesPersistsAndSeparatesTwoLocalPlayers() {
        val context = RuntimeEnvironment.getApplication()
        val firstProfile = "local_1_teacher_test"
        val secondProfile = "local_2_teacher_test"
        val firstDb = "wq_teacher_${stableHash(firstProfile).take(24)}.db"
        val secondDb = "wq_teacher_${stableHash(secondProfile).take(24)}.db"
        context.deleteDatabase(firstDb)
        context.deleteDatabase(secondDb)
        val reply = ConversationTurn(1, "I has water", "I have water. 'Have' is correct here.",
            ConversationLanguage.English, JSONArray().put(JSONObject().put("text", "I have water.")))
        lateinit var savedId: String
        TeacherStore(context, firstProfile).use { first ->
            TeacherStore(context, secondProfile).use { second ->
                first.setNotes("A2 • learn past tense")
                savedId = first.append(null, reply, "A2")
                assertTrue(first.threads("water").any { it.id == savedId })
                assertTrue(first.threads("correct").any { it.id == savedId })
                assertTrue(first.threads("%wrong%").isEmpty())
                assertEquals(1, first.turns(savedId).size)
                assertEquals("I have water.", first.turns(savedId).single().modelParts.getJSONObject(0).getString("text"))
                assertEquals(0, second.threads().size)
                assertTrue(second.notes().isEmpty())
                assertEquals(0, second.turns(savedId).size)
            }
        }
        TeacherStore(context, firstProfile).use { reopened ->
            assertEquals("A2 • learn past tense", reopened.notes())
            assertEquals("I has water", reopened.turns(savedId).single().message)
            reopened.delete(savedId)
            assertTrue(reopened.threads().isEmpty())
            assertTrue(reopened.turns(savedId).isEmpty())
        }
    }

    @Test
    fun teacherPromptIncludesBoundedErrorEvidenceAndCorrectiveMode() {
        val request = conversationRequest(emptyList(), "Correct my sentence", ConversationLanguage.Turkish, "A2",
            TeacherMode.Correction, "My goal is B1", "Water: 3 wrong")
        val instructions = request.getJSONObject("systemInstruction")
            .getJSONArray("parts").getJSONObject(0).getString("text")
        assertTrue(instructions.contains("Correct the learner's English"))
        assertTrue(instructions.contains("My goal is B1"))
        assertTrue(instructions.contains("Water: 3 wrong"))
    }
}
