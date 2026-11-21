package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            "Yanlışlarımdan bulmaca" to "mistakes",
            "İlerlemem" to "analytics",
            "Öğrenme Merkezim" to "insights",
            "Arkadaşlarım" to "friends",
            "Hesabım" to "account",
            "Ayarlar" to "settings",
            "Yedekleme" to "backup",
            "Oyun Geçmişim" to "history",
            "Sözlük" to "library",
            "Aktif Gün Serisi" to "streak",
            "Mesajlarım" to "messages"
        )
        expected.forEach { (title,icon) ->
            assertEquals(title, icon, Wq175IconNames.menu(title))
        }
    }
    @Test fun approvedVisualAssetsTakePriorityInPrimaryRooms() {
        val approved = mapOf(
            "trophy" to R.drawable.wq175_arena_icon,
            "yds" to R.drawable.wq175_yds_akademi_icon,
            "cards" to R.drawable.wq175_kelime_hafizam_icon,
            "synonym" to R.drawable.wq175_eslestirmelerim_icon,
            "plan" to R.drawable.wq175_gunluk_plan_icon,
            "grammar" to R.drawable.wq175_dilbilgisi_icon,
            "puzzle" to R.drawable.wq175_bulmacalarim_icon,
            "listen" to R.drawable.wq175_dinle_ve_yaz_icon
        )
        approved.forEach { (name, resource) ->
            assertEquals(name, resource, Wq175IconNames.approvedResource(name))
        }
        // Duels retain their special sword/arena design, not a generic trophy.
        assertNull(Wq175IconNames.approvedResource("arena_duel"))
        assertNull(Wq175IconNames.approvedResource("arena_team"))
        assertNull(Wq175IconNames.approvedResource("arena_quiz"))
    }
}
