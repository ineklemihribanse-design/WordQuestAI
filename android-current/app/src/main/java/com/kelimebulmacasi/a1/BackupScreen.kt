package com.kelimebulmacasi.a1

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun BackupScreen(session: AppSession, back: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val backup = session.backup()
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri) ?: error("open")
                    stream.bufferedWriter().use { it.write(backup.encode()) }
                }
                status = "Yedek kaydedildi."
                failed = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                status = "Yedek kaydedilemedi."
                failed = true
            } finally {
                busy = false
            }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val backup = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri) ?: error("open")
                    val contents = ByteArrayOutputStream()
                    stream.use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count == -1) break
                            require(contents.size() + count <= 32_000_000)
                            contents.write(buffer, 0, count)
                        }
                    }
                    decodeBackupFile(contents.toByteArray())
                }
                if (backup.games.isEmpty() && backup.progress.isEmpty() && backup.learning?.cards.isNullOrEmpty() && backup.yds.isEmpty()) {
                    status = "Bu dosyada aktarılacak bulmaca veya ilerleme yok. Eski uygulamada hesapla oynadıysan o hesabın kayıt dosyasını seç."
                } else {
                    session.restore(backup)
                    status = "Kayıtlar aktarıldı · ${backup.games.size} kişisel bulmaca · ${backup.progress.size} ilerleme · ${backup.learning?.cards?.size ?: 0} kelime · ${backup.yds.size} YDS istatistiği"
                }
                failed = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                status = "Dosya okunamadı. Kelime Bulmacası’nın yedek JSON’unu veya eski kayıt XML’ini seç."
                failed = true
            } finally {
                busy = false
            }
        }
    }
    ScreenPage("Yedekle ve aktar", "Bulmacaların, ilerlemen, kelime defterin ve tekrar geçmişin", back) {
        BodyText("Etkin profil · ${session.learningProfileName}")
        PrimaryAction("Yedek dosyası oluştur", { export.launch("Kelime_Bulmacasi_Yedek.json") }, !busy)
        OutlinedButton(onClick = { restore.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
            Text("Kayıt dosyasını içe aktar", fontFamily = Sans, fontWeight = FontWeight.Bold)
        }
        BodyText("Yedek JSON veya önceki Kelime Bulmacası’ndan alınan XML dosyasını seçebilirsin.")
        if (status.isNotEmpty()) BodyText(status, if (failed) ErrorInk else Forest)
    }
}
