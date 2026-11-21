package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class Wq176ContentRotationTest {
    @Test fun containsAtLeastTwoThousandDistinctQuestionsWithExplanations() {
        val all=YdsContent.questions
        assertEquals(2038,all.size)
        assertEquals(all.size,all.map { it.id }.toSet().size)
        assertEquals(all.size,all.map { it.question.lowercase().trim() }.toSet().size)
        assertTrue(all.count { it.topic==YdsTopic.Vocabulary } >= 800)
        assertTrue(all.count { it.topic==YdsTopic.Translation } >= 800)
        assertTrue(all.count { it.topic==YdsTopic.Grammar } >= 240)
        all.forEach { q ->
            assertEquals(4,q.options.size)
            assertEquals(4,q.options.map(String::lowercase).toSet().size)
            assertTrue(q.answer in 0..3)
            assertTrue(q.explanation.length>=20)
        }
    }

    @Test fun twoConsecutiveDailySessionsDoNotReuseQuestionsAndProfileIsIsolated() {
        val context=RuntimeEnvironment.getApplication()
        val pool=YdsContent.questions.filter { it.topic==YdsTopic.Vocabulary }
        val profile="rotation_"+UUID.randomUUID().toString()
        val first=Wq176QuestionRotation.pick(context,profile,pool,40)
        val second=Wq176QuestionRotation.pick(context,profile,pool,40)
        assertEquals(40,first.size)
        assertEquals(40,second.size)
        assertTrue(first.map { it.id }.toSet().intersect(second.map { it.id }.toSet()).isEmpty())
        val other=Wq176QuestionRotation.pick(context,profile+"-other",pool,40)
        assertEquals(40,other.size)
        assertEquals(40,other.map { it.id }.toSet().size)
    }
}
