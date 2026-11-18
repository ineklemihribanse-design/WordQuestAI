package com.kelimebulmacasi.a1

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDate

/** Unified offline-first view; online AI or account sync is never silently claimed as active. */
@Composable
internal fun LearningDashboardScreen(
    session: AppSession,
    ai: AiSession,
    back: () -> Unit,
    openYds: () -> Unit,
    openLearning: () -> Unit,
    openPractice: () -> Unit,
    openTeacher: (String) -> Unit,
    openAccount: () -> Unit,
    openBackup: () -> Unit,
) {
    val context = LocalContext.current
    val yds = remember(context, session.profileKey) { YdsProgressStore(context, session.profileKey) }
    val daily = remember(context, session.profileKey) { DailyPlanStore(context, session.profileKey) }
    val due = session.learningDueCount()
    val total = session.searchLearning("", 0, 50_000).size
    val mistakes = session.searchLearning("", 0, 500).filter { it.wrong > 0 || it.forgotten > 0 }
        .sortedByDescending { it.wrong + it.forgotten }.take(5)
    val weak = yds.weakestTopic()
    val recommendation = yds.levelRecommendation()
    val name = session.learningProfileName

    ScreenPage("Öğrenme Merkezim", "Oyun, YDS, kelime hafızası ve öğretmen tek yerde", back) {
        Text("$name için ilerleme", color = Ink, fontFamily = Serif)
        BodyText("${session.totalPoints} bulmaca puanı · ${session.finishedCount} tamamlanan bulmaca")
        BodyText("$total öğrenme kartı · $due tekrar bekliyor")
        BodyText("YDS: ${yds.totalCorrect()}/${yds.totalAttempts()} doğru · %${yds.percent()} başarı")
        BodyText("Bugün: ${daily.completed().size} alıştırma · ${(daily.seconds / 60)} dakika pratik ekranında")
        Text("Bana uygun çalışma", color = Ink, fontFamily = Serif)
        BodyText("Önerilen YDS seviyesi: ${recommendation.label}. " +
            if (weak == null) "İlk YDS denemesiyle seviyeni belirle."
            else "Geliştirilecek konu: ${weak.label}. Yanlışlarını tekrar çöz.")
        if (mistakes.isNotEmpty()) {
            BodyText("Öne çıkan zor kelimeler: " + mistakes.joinToString(", ") { it.answer.take(22) })
        }
        PrimaryAction("Önerilen YDS çalışmasını aç", openYds)
        OutlinedButton(onClick = openLearning, modifier = Modifier.fillMaxWidth()) {
            Text("Akıllı kelime tekrarını aç ($due)", fontFamily = Sans)
        }
        OutlinedButton(onClick = openPractice, modifier = Modifier.fillMaxWidth()) {
            Text("15 / 30 / 60 dakikalık günlük plan", fontFamily = Sans)
        }
        Text("AI öğretmenim", color = Ink, fontFamily = Serif)
        BodyText(if (ai.hasKey) "AI anahtarı bu profilde etkin; ders isteyebilirsin."
            else "AI anahtarı henüz girilmemiş. Çevrimdışı oyunlar ücretsiz çalışır; çevrimiçi öğretmen için ayrı sağlayıcı erişimi gerekir.")
        OutlinedButton(onClick = {
            val topic = weak?.label ?: "YDS kelime ve dilbilgisi"
            openTeacher("$name için Türkçe açıklamalı kişisel İngilizce çalışma hazırla. " +
                "Öncelikli konu: $topic; önerilen YDS seviyesi: ${recommendation.label}. " +
                "${session.teacherLearningContext()} Yanlışlarım için beş özgün alıştırma sorusu oluştur.")
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Yanlışlarıma göre kişisel AI dersi", fontFamily = Sans)
        }
        Text("Hesap ve yedekleme", color = Ink, fontFamily = Serif)
        BodyText(if (session.accountName != null) "Hesap: ${session.profileName}. ${session.accountStatus}"
            else "Yerel profiller birbirinden ayrıdır. Otomatik cihazlar arası eşitleme ancak yapılandırılmış hesap ve sunucu varsa kullanılabilir.")
        OutlinedButton(onClick = openAccount, modifier = Modifier.fillMaxWidth()) {
            Text("Hesap / eşitleme durumunu gör", fontFamily = Sans)
        }
        OutlinedButton(onClick = openBackup, modifier = Modifier.fillMaxWidth()) {
            Text("Profil yedeği oluştur veya içe aktar", fontFamily = Sans)
        }
        BodyText("Çevrimdışı bulmacalar, YDS testleri, öğrenme defteri ve günlük planlar internet gerektirmez. " +
            "Ücretsiz AI sağlayıcıları arasında otomatik geçiş, geçerli hesaplar/izinler olmadan etkinleştirilmez.")
    }
}
