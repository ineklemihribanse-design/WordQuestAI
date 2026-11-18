package com.kelimebulmacasi.a1

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun AiProviderSettingsSection(ai: AiSession) {
    val context = LocalContext.current
    var entered by remember(ai.provider, ai.boundProfile) { mutableStateOf("") }
    var modelInput by remember(ai.provider, ai.boundProfile) { mutableStateOf(ai.providerModel) }
    var message by remember(ai.provider) { mutableStateOf("") }
    var editing by remember(ai.provider) { mutableStateOf(!ai.hasKey) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Yapay zekâ sağlayıcısı", color = Ink, fontFamily = Serif, fontWeight = FontWeight.Bold)
        Text("Kendi API anahtarınızı isteğe bağlı olarak kaydedin. Sağlayıcının kullanım ücreti veya kotası size aittir.", fontFamily = Sans)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            AiProvider.entries.filter { it != AiProvider.Claude }.forEach { value ->
                FilterChip(selected = ai.provider == value, onClick = {
                    ai.chooseProvider(value)
                    modelInput = ai.providerModel
                    entered = ""; editing = !ai.hasKey; message = ""
                }, label = { Text(value.label, fontFamily = Sans) })
            }
        }
        Text("${ai.provider.label}: " + if (ai.hasKey) "Anahtar cihazda şifreli saklanıyor" else "Anahtar eklenmedi",
            fontFamily = Sans)
        OutlinedTextField(modelInput, { modelInput = it.take(80) }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Model adı") }, singleLine = true,
            supportingText = { Text("Erişiminiz olan model adını sağlayıcı panelinden kontrol edin.") })
        OutlinedButton(onClick = {
            message = runCatching { ai.setProviderModel(modelInput.trim()); "Model kaydedildi." }
                .getOrElse { it.message ?: "Model adı geçersiz." }
        }) { Text("Modeli kaydet") }
        if (editing) OutlinedTextField(entered, { entered = it.take(4096) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = PasswordVisualTransformation(), label = { Text("${ai.provider.label} API anahtarı") })
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (editing) OutlinedButton(onClick = {
                message = runCatching { ai.saveProviderKey(entered); entered = ""; editing = false; "Anahtar güvenli biçimde kaydedildi." }
                    .getOrElse { it.message ?: "Anahtar kaydedilemedi." }
            }, enabled = entered.isNotBlank()) { Text("Anahtarı kaydet") }
            else OutlinedButton(onClick = { editing = true }) { Text("Anahtarı değiştir") }
            if (ai.hasKey) TextButton(onClick = { ai.forgetKey(); entered = ""; editing = true; message = "Anahtar silindi." }) {
                Text("Anahtarı sil")
            }
        }
        TextButton(onClick = {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ai.provider.homepage))) }
                .onFailure { message = "Tarayıcıyı açamadım. Sağlayıcının API paneline gidin." }
        }) { Text("${ai.provider.label} anahtar sayfasını aç") }
        if (message.isNotBlank()) Text(message, fontFamily = Sans)
        Text("Anahtar yalnızca seçili cihazdaki bu hesaba aittir; Firebase'e veya GitHub'a kaydedilmez. " +
            "Bir API anahtarının cihazda bulunması riski tamamen ortadan kaldırmaz. Öğretmene gönderdiğiniz içerik " +
            "seçili sağlayıcıya iletilir. Sohbet ve mikrofon işlemleri veri kullanımına neden olabilir.",
            fontFamily = Sans)
    }
}
