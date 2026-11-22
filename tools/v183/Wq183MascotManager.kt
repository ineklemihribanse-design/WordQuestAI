package com.kelimebulmacasi.a1

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Profile-owned optional ZIP importer. Built-in avatars remain unchanged. */
@Composable
internal fun Wq183MascotManager(
    profileKey: String,
    selected: String,
    onSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var shown by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<String?>(null) }
    var items by remember(profileKey) {
        mutableStateOf(Wq183MascotStore.list(context, profileKey))
    }
    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { Wq183MascotStore.importPack(context, uri, profileKey) }
                }
                result.onSuccess {
                    items = Wq183MascotStore.list(context, profileKey)
                    pending = it.id
                    info = "Maskot kaydedildi. Kullanmak için 'Seç' düğmesine bas."
                }.onFailure {
                    info = "Maskot ZIP'i kabul edilmedi: " + (it.message ?: "Geçersiz paket")
                }
                busy = false
            }
        }
    }
    OutlinedButton(onClick = { shown = true }, modifier = Modifier.fillMaxWidth()) {
        Text("+ Kendi Maskotumu Ekle")
    }
    if (shown) AlertDialog(
        onDismissRequest = { if (!busy) shown = false },
        title = { Text("Kişisel Maskotlarım") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Yalnızca WordQuest Mascot Pack v1 ZIP yüklenir. Dosyalar cihazında kalır.")
                Button(
                    onClick = { chooser.launch(arrayOf("application/zip", "application/octet-stream")) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) "Kontrol ediliyor…" else "ZIP dosyası seç") }
                if (info.isNotBlank()) Text(info)
                items.forEach { mascot ->
                    var draftName by remember(mascot.id) { mutableStateOf(mascot.name) }
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Wq2DPortrait(mascot.id, Modifier.size(64.dp))
                            Column(Modifier.weight(1f)) {
                                Text(mascot.name, maxLines = 2)
                                Text(if (mascot.id == selected) "Seçili maskot"
                                    else "Kişisel maskot")
                            }
                        }
                        OutlinedTextField(
                            value = draftName,
                            onValueChange = { draftName = it.take(40) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Maskot adı") },
                            singleLine = true
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    if (Wq183MascotStore.rename(
                                            context, profileKey, mascot.id, draftName)) {
                                        items = Wq183MascotStore.list(context, profileKey)
                                        info = "Maskot adı kaydedildi."
                                    } else info = "Geçersiz maskot adı."
                                },
                                enabled = draftName.trim().isNotBlank() &&
                                    draftName.trim() != mascot.name
                            ) { Text("Adı kaydet") }
                            TextButton(onClick = {
                                Wq183MascotStore.activate(context, profileKey)
                                onSelected(mascot.id)
                                pending = null
                                info = "Maskot seçildi."
                            }) { Text(if (pending == mascot.id) "Seç" else "Kullan") }
                            TextButton(onClick = {
                                if (mascot.id == selected) onSelected("piko")
                                Wq183MascotStore.delete(context, profileKey, mascot.id)
                                items = Wq183MascotStore.list(context, profileKey)
                                pending = null
                            }) { Text("Sil") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (!busy) shown = false }) { Text("Kapat") } }
    )
}
