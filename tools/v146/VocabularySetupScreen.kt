package com.kelimebulmacasi.a1

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The library stays separate from the actual word-study page. */
@Composable
internal fun VocabularySetupScreen(
    session: AppSession,
    folders: List<LearningFolder>,
    initialFolder: String?,
    initialFilter: String,
    onStart: (VocabularyStudyConfig) -> Unit,
) {
    var folderId by rememberSaveable(session.profileKey, initialFolder) {
        mutableStateOf(initialFolder ?: "all")
    }
    var filter by rememberSaveable(session.profileKey, initialFilter) { mutableStateOf(initialFilter) }
    var mode by rememberSaveable(session.profileKey) { mutableStateOf("card") }
    var direction by rememberSaveable(session.profileKey) { mutableStateOf("tr-en") }
    var count by rememberSaveable(session.profileKey) { mutableIntStateOf(20) }
    var folderMenu by remember { mutableStateOf(false) }

    val available = vocabularyStudyCards(session, folderId, filter)
    val studyReady = if (mode == "type") available.filter { VocabularyWordTypes.get(it.answer) != null }
        else available
    LearningPanel {
        Text("Yeni kelime çalışması", color = Forest, fontFamily = Sans,
            fontWeight = FontWeight.Bold, fontSize = 20.sp)
        BodyText("Çalışma ayarlarını seç. Sorular ayrı bir sayfada açılacak.")
        Text("Klasör", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(onClick = { folderMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(folders.firstOrNull { it.id == folderId }?.title ?: "Bütün klasörler", fontFamily = Sans)
                Text(" ▾")
            }
            DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                DropdownMenuItem(text = { Text("Bütün klasörler") }, onClick = {
                    folderId = "all"; folderMenu = false
                })
                for (folder in folders) {
                    DropdownMenuItem(text = { Text(folder.title) }, onClick = {
                        folderId = folder.id; folderMenu = false
                    })
                }
            }
        }
        Text("Kelime havuzu", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((value, label) in listOf("" to "Hepsi", "BILINMIYOR" to "Bilinmeyen",
                "YANLIS" to "Yanlış", "YENI" to "Yeni")) {
                FilterChip(selected = filter == value, onClick = { filter = value },
                    label = { Text(label, fontFamily = Sans) })
            }
        }
        Text("Çalışma yöntemi", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((value, label) in listOf("card" to "Kartı çevir",
                "write" to "Yazarak cevapla", "type" to "Kelime türünü eşleştir",
                "mixed" to "Karma çalışma")) {
                FilterChip(selected = mode == value, onClick = { mode = value },
                    label = { Text(label, fontFamily = Sans) },
                    modifier = Modifier.fillMaxWidth())
            }
        }
        Text("Çeviri yönü", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            for ((value, label) in listOf("tr-en" to "TR → EN", "en-tr" to "EN → TR",
                "mixed" to "Karışık")) {
                FilterChip(selected = direction == value, onClick = { direction = value },
                    label = { Text(label, fontFamily = Sans) })
            }
        }
        if (mode == "type") BodyText("Kelime türü yalnızca güvenilir biçimde sınıflandırılmış sözcüklerde sorulur.")
        Text("Kaç kelime?", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (number in listOf(10, 20, 30, 50)) {
                FilterChip(selected = count == number, onClick = { count = number },
                    label = { Text(number.toString(), fontFamily = Sans) },
                    modifier = Modifier.weight(1f))
            }
        }
        BodyText("Seçime uygun " + studyReady.size + " kelime")
        Button(
            enabled = studyReady.isNotEmpty(),
            onClick = {
                onStart(VocabularyStudyConfig(folderId, filter, mode, direction,
                    count, System.currentTimeMillis().toInt()))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Çalışmaya başla", fontFamily = Sans) }
        if (studyReady.isEmpty()) BodyText("Bu seçimde çalışılacak kelime yok. Başka bir klasör veya yöntem seç.")
    }
}
