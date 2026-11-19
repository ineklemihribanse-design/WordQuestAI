package com.kelimebulmacasi.a1

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Selected image/PDF/DOCX/TXT content is recognized on device; the chat API is text-only. */
internal data class LenaPreparedAttachment(
    val name: String, val category: String, val extractedText: String,
) {
    fun promptText(): String = "[" + category + ": " + name + "]\n" +
        "Ekteki okunabilir metin:\n" + extractedText.take(5500)
}

private val lenaEmojis = listOf(
    "😊","😂","😍","🥰","😄","😁","😎","🤔",
    "😮","😅","😭","🥹","😢","😡","👍","👎",
    "👏","🙌","🙏","👋","💪","❤️","💚","💙",
    "💜","✨","🔥","🎉","🎊","⭐","🌸","🌺",
    "🌿","📚","🎓","💡","✅","❓","💬","🇬🇧",
    "🇺🇸","🇹🇷","☕","🌍","📝","💯","🤗","🙂",
)

@Composable
internal fun LenaRichComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    enabled: Boolean,
    canSend: Boolean,
    onSend: () -> Unit,
    maxLength: Int,
    attachment: LenaPreparedAttachment?,
    attachmentLoading: Boolean,
    onAttachmentSelected: (Uri, Boolean) -> Unit,
    onRemoveAttachment: () -> Unit,
    onLanguageToggle: () -> Unit,
) {
    var attachMenu by remember { mutableStateOf(false) }
    var emojiMenu by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onAttachmentSelected(uri, true)
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onAttachmentSelected(uri, false)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (attachment != null || attachmentLoading) {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFFE8F5E8), RoundedCornerShape(15.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(if (attachmentLoading) "⏳" else "📎", fontSize = 17.sp)
                Text(
                    if (attachmentLoading) "Dosya cihazda okunuyor…" else
                        (attachment?.name.orEmpty() + " · " + (attachment?.extractedText?.length ?: 0) + " karakter"),
                    modifier = Modifier.weight(1f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, fontSize = 12.sp,
                    color = Color(0xFF20563B),
                )
                if (attachment != null) TextButton(onClick = onRemoveAttachment) {
                    Text("Kaldır", color = Color(0xFF20563B), fontSize = 12.sp)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(Color.White, RoundedCornerShape(30.dp))
                .border(1.dp, Color(0xFFCBDFD0), RoundedCornerShape(30.dp))
                .padding(horizontal = 5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box {
                IconButton(onClick = { attachMenu = true }, enabled = enabled,
                    modifier = Modifier.size(44.dp)) {
                    Image(painterResource(R.drawable.wq169_attach),
                        contentDescription = "Görsel veya belge ekle",
                        modifier = Modifier.size(21.dp))
                }
                DropdownMenu(expanded = attachMenu, onDismissRequest = { attachMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("🖼  Görsel ekle", fontSize = 14.sp) },
                        onClick = { attachMenu = false; imagePicker.launch("image/*") },
                    )
                    DropdownMenuItem(
                        text = { Text("📄  Belge ekle · PDF, TXT, DOCX", fontSize = 14.sp) },
                        onClick = {
                            attachMenu = false
                            documentPicker.launch(arrayOf(
                                "application/pdf", "text/plain",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            ))
                        },
                    )
                }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { onDraftChange(it.take(maxLength)) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Lena’ya mesaj yaz…", fontSize = 14.sp, maxLines = 1) },
                shape = RoundedCornerShape(24.dp),
                minLines = 1,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF1E4634),
                    unfocusedTextColor = Color(0xFF1E4634),
                    focusedBorderColor = Color(0xFF91CA9E),
                    unfocusedBorderColor = Color(0xFFE1EAE2),
                    focusedContainerColor = Color(0xFFFDFEFC),
                    unfocusedContainerColor = Color(0xFFFDFEFC),
                ),
            )
            Box {
                IconButton(onClick = { emojiMenu = true }, enabled = enabled,
                    modifier = Modifier.size(42.dp)) {
                    Text("☺", fontSize = 25.sp, color = Color(0xFF216C46))
                }
                DropdownMenu(
                    expanded = emojiMenu, onDismissRequest = { emojiMenu = false },
                    modifier = Modifier.background(Color(0xFFF9FEF8)),
                ) {
                    Text("Emoji seç", modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
                        color = Color(0xFF275F40), fontWeight = FontWeight.Bold)
                    lenaEmojis.chunked(6).forEach { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            group.forEach { emoji ->
                                IconButton(onClick = {
                                    onDraftChange((draft + emoji).take(maxLength))
                                    emojiMenu = false
                                }, modifier = Modifier.size(42.dp)) {
                                    Text(emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }
            }
            IconButton(onClick = onLanguageToggle, enabled = enabled,
                modifier = Modifier.size(40.dp)) {
                Text("文A", fontSize = 14.sp, color = Color(0xFF216C46))
            }
            Button(
                onClick = onSend,
                enabled = canSend && !attachmentLoading,
                shape = RoundedCornerShape(50),
                modifier = Modifier.size(46.dp),
                contentPadding = PaddingValues(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF239B4E), contentColor = Color.White,
                ),
            ) {
                Image(painterResource(R.drawable.wq167_send),
                    contentDescription = "Gönder",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(23.dp))
            }
        }
    }
}
