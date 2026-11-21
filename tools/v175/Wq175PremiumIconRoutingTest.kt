package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Test

/** Visual icon contract: real room names route to the original art assets. */
class Wq175PremiumIconRoutingTest {
    @Test fun secondaryRoomsUseDedicatedIconsInsteadOfGenericCards() {
        val expected=mapOf(
            "PDF'den kelime ekle" to "pdf",
            "Kamera ile tara" to "camera",
            "Fotoğraf ve tarama" to "scan",
            "Paragraf Çalışması" to "paragraph",
            "Çeviri" to "translate",
            "Yakın Anlam" to "synonym",
            "Cümle Tamamlama" to "completion",
            "Mini Deneme" to "exam",
            "Yanlış Analizi" to "review",
            "Kendi Defterim" to "notebook",
            "Süreli Oyun" to "timer",
            "Başarı Sonuçları" to "success",
            "Kişisel AI öğretmenim" to "ai_teacher",
            "Yanlışlarımdan bulmaca" to "mistakes"
        )
        expected.forEach { (title,icon) ->
            assertEquals(title, icon, Wq175IconNames.menu(title))
        }
    }
}
