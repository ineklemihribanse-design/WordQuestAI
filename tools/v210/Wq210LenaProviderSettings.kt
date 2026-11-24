package com.kelimebulmacasi.a1

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun Wq210LenaProviderSettings(ai: AiSession) {
    val context = LocalContext.current
    var status by remember(ai.boundProfile) { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Lena · Akıllı bağlantı")
        Text("Groq → Gemini → OpenRouter (ücretsiz model). Kota sınırsız değildir. Groq ve Gemini hesabın ücretli plandaysa kullanım ücretlendirilebilir.")
        Text("Etkinleştirirsen son sohbet mesajları ve öğretmen bağlamı, yanıt almak için anahtarı kaydedilmiş bu sağlayıcılara sırayla gönderilebilir. API anahtarları yalnız kendi hizmetine gönderilir.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Otomatik sağlayıcı geçişi", modifier = Modifier.weight(1f))
            Switch(checked = ai.lenaAutoEnabled, onCheckedChange = {
                status = runCatching { ai.setLenaAuto(it); "Bağlantı tercihi kaydedildi." }.getOrElse { "Ana uygulamadan ayarları aç." }
            })
        }
        Text("Gemini: " + if (ai.lenaGeminiAvailable) "Anahtar kayıtlı" else "Aşağıdaki normal sağlayıcı ayarından ekle")
        for (service in Wq210ExtraKey.entries) {
            var input by remember(ai.boundProfile, service) { mutableStateOf("") }
            Text(service.label + ": " + if (ai.lenaExtraAvailable(service)) "Anahtar kayıtlı" else "Anahtar eklenmedi")
            OutlinedTextField(input, { input = it.take(4096) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(service.label + " API anahtarı") }, visualTransformation = PasswordVisualTransformation())
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedButton(enabled = input.isNotBlank(), onClick = {
                    status = runCatching { ai.saveLenaExtra(service, input); input = ""; "Anahtar şifreli kaydedildi." }.getOrElse { "Anahtar biçimini veya profilini kontrol et." }
                }) { Text("Kaydet") }
                TextButton(onClick = {
                    status = runCatching { ai.removeLenaExtra(service); input = ""; "Anahtar kaldırıldı." }.getOrElse { "Ana uygulamadan ayarları aç." }
                }) { Text("Sil") }
            }
            TextButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(service.url))) }
            }) { Text(service.label + " anahtar sayfası") }
        }
        if (status.isNotBlank()) Text(status)
        HorizontalDivider()
    }
}
