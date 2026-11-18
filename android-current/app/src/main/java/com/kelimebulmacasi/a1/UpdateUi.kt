package com.kelimebulmacasi.a1

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LocalAppUpdater = staticCompositionLocalOf<AppUpdater> { error("AppUpdater not supplied") }

@Composable
internal fun UpdateSettingsSection() {
    val updater = LocalAppUpdater.current
    val context = LocalContext.current
    HorizontalDivider()
    Text("Güncellemeler", color = Ink, fontFamily = Serif, fontSize = 25.sp)
    Text("Mevcut sürüm: ${updater.installedVersion()}", color = Ink, fontFamily = Sans)
    Text("Son kontrol: ${updater.lastCheckLabel()}", color = Muted, fontSize = 13.sp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Otomatik güncelleme kontrolü", Modifier.weight(1f), color = Ink, fontFamily = Sans)
        Switch(checked = updater.automatic, onCheckedChange = updater::changeAutomatic)
    }
    OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !updater.checking && !updater.downloading,
        onClick = { updater.check(force = true) }) {
        Text(if (updater.checking) "Kontrol ediliyor…" else "Güncellemeleri Kontrol Et", fontFamily = Sans)
    }
    Text(updater.status, color = Ink, fontFamily = Sans, fontSize = 13.sp)
    updater.available?.let { offer ->
        Text("Yeni sürüm: ${offer.versionName} · ${offer.sizeLabel}", color = Forest, fontWeight = FontWeight.Bold)
        Text("Değişiklikler: ${offer.notes.ifBlank { "Sürüm notu eklenmedi." }}", color = Ink, fontFamily = Sans)
        if (!updater.downloading) {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                (context as? Activity)?.let(updater::updateNow)
            }) { Text("Şimdi Güncelle") }
        }
    }
    if (updater.downloading) {
        LinearProgressIndicator(progress = { updater.progress / 100f }, modifier = Modifier.fillMaxWidth())
        Text("APK indiriliyor: %${updater.progress}", color = Ink)
        OutlinedButton(onClick = updater::cancelDownload) { Text("İndirmeyi İptal Et") }
    }
    Text("İndirme HTTPS ile yapılır. APK'nın SHA-256 değeri ve uygulama imzası " +
        "kontrol edilir. Kurulum için Android onayı gerekir.", color = Muted, fontSize = 12.sp)
}

@Composable
internal fun UpdateOverlayDialogs() {
    val updater = LocalAppUpdater.current
    val activity = LocalContext.current as? Activity
    val offer = updater.available
    if (updater.showPrompt && offer != null) {
        AlertDialog(
            onDismissRequest = updater::dismissOffer,
            title = { Text("Yeni WordQuest AI sürümü") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Mevcut sürüm: ${updater.installedVersion()}")
                    Text("Yeni sürüm: ${offer.versionName}")
                    Text("İndirme boyutu: ${offer.sizeLabel}")
                    Text("Neler değişti?\n${offer.notes.ifBlank { "Yeni iyileştirmeler ve hata düzeltmeleri." }}")
                }
            },
            confirmButton = {
                TextButton(onClick = { if (activity != null) updater.updateNow(activity) }, enabled = activity != null) {
                    Text("Şimdi Güncelle")
                }
            },
            dismissButton = { TextButton(onClick = updater::dismissOffer) { Text("Daha Sonra") } },
        )
    } else if (updater.downloading) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("WordQuest AI indiriliyor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${updater.progress}% · ${offer?.sizeLabel.orEmpty()}")
                    LinearProgressIndicator(progress = { updater.progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("İndirme tamamlandıktan sonra Android kurulum onayı isteyecek.")
                }
            },
            confirmButton = { TextButton(onClick = updater::cancelDownload) { Text("İptal") } },
        )
    }
}
