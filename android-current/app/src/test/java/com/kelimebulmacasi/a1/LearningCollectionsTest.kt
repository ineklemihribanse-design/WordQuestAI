package com.kelimebulmacasi.a1

import android.content.Context
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
class LearningCollectionsTest {
    private lateinit var context: Context
    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("wq_learning_guest.db")
    }

    @Test fun regularPuzzleGoesToNewWordsAndPremiumToDatedFolder() {
        LearningStore(context, null).use { store ->
            val milk = Entry("MILK", "Süt")
            val apple = Entry("APPLE", "Elma")
            assertTrue(store.record("a1-food-1", LearningSignal(LearningKind.SOLVED, milk)))
            assertEquals(1, store.folderCards("new-words").size)
            val gameId = "premium-solo-20261115"
            store.setPremiumFolderTitle(gameId, "Tek Kişilik Bulmaca")
            store.attachPremiumWords(gameId, listOf(
                PremiumReviewWord(0, milk, 0, false),
                PremiumReviewWord(1, apple, 1, true),
            ))
            assertEquals(2, store.folderCards(gameId).size)
            assertEquals(1, store.folderCards(gameId, "YANLIS").size)
            assertEquals(1, store.folderCards(gameId, "BILINMIYOR").size)
            assertEquals(1, store.folderCards("new-words").size)
            assertTrue(store.renameFolder(gameId, "2026 / Sınav çalışmam"))
        }
        LearningStore(context, null).use { store ->
            assertEquals("2026 / Sınav çalışmam", store.folders().first { it.id.startsWith("premium-") }.title)
            assertEquals(2, store.folderCards("premium-solo-20261115").size)
        }
    }

    @Test fun practiceRecordsStayInOneProfileAndDoNotCreateNewGameFolder() {
        LearningStore(context, null).use { store ->
            val entry = Entry("SEA", "Deniz")
            val signal = LearningSignal(LearningKind.UNKNOWN, entry, boardUpdatedAt = 1)
            assertTrue(store.record("premium-room-host", signal))
            assertFalse(store.record("premium-room-host", signal))
            val card = store.folderCards("premium-room-host").single()
            assertTrue(store.practiceAny(card, ReviewRating.Good))
            assertEquals(1, store.folders().size)
            assertEquals(1, store.folderCards("premium-room-host").size)
        }
    }

    @Test fun statusMergesWithoutLosingErrorOrUnknown() {
        assertEquals("BILINMIYOR_YANLIS", LearningCollections.mergeStatus("YANLIS", "BILINMIYOR"))
        assertEquals("BILINMIYOR_YANLIS", LearningCollections.mergeStatus("BILINMIYOR", "YANLIS"))
        assertEquals("Yeni Kelimeler", LearningCollections.defaultTitle("new-words", 0L))
        assertNull(LearningCollections.folderFor("personal_review", LearningKind.REVIEW_GOOD))
        assertEquals("name", LearningCollections.safeName("  name  "))
    }
}
