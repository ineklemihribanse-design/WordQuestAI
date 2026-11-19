#!/usr/bin/env python3
"""v1.0.71: working files/emoji on both Lena screens, foreground subtitles, active GLB clips.

Apply strictly after v1.0.70. Existing GLB, chairs, signing, keys,
conversation history, Arena and Word Memory are byte-for-byte untouched.
"""
from pathlib import Path
import hashlib, shutil

ROOT=Path(__file__).resolve().parents[2]
PKG=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
APP=ROOT/"source/app"
BASE=ROOT/"tools/v171"
GRADLE=APP/"build.gradle.kts"

def once(s,old,new,label):
    count=s.count(old)
    if count!=1: raise AssertionError(f"{label}: expected one match, got {count}")
    return s.replace(old,new,1)

def main():
    gradle=GRADLE.read_text("utf8")
    assert gradle.count("versionCode = 95")==1
    assert gradle.count('versionName = "1.0.70-user-chair-safe-greeting"')==1
    assert 'applicationIdSuffix = ".fresh"' in gradle
    lena=APP/"src/main/assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
    chair=APP/"src/main/assets/studio/chairs_and_window_set_-_game_ready.glb"
    portrait=APP/"src/main/res/drawable-nodpi/wq167_lena_portrait.png"
    targets=[lena,chair,portrait]
    assert all(p.is_file() for p in targets)
    before={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in targets}
    assert (PKG/"ArenaApprovedHome.kt").exists()
    assert (PKG/"VocabularyNeonHome.kt").exists()

    for filename in ("LenaRichComposer.kt","LenaAttachmentReader.kt","LenaLive3DStage.kt"):
        shutil.copyfile(BASE/filename,PKG/filename)

    # 1) Real composer replaces v1.0.69's 650dp-only paperclip and one-emoji shortcut.
    chat=PKG/"ConversationScreen.kt"
    s=chat.read_text("utf8")
    assert 'attachmentPicker.launch("text/plain")' in s
    assert 'LenaMessageAudioBar(' in s
    s=once(s,
        "    val canEdit = !sending && !recording",
        """    var selectedAttachment by remember { mutableStateOf<LenaPreparedAttachment?>(null) }
    var attachmentLoading by remember { mutableStateOf(false) }
    val canEdit = !sending && !recording""","chat attachment state")
    s=once(s,
        "    val canSend = canEdit && (ai.hasKey || apiKey.isNotBlank()) && ai.draft.isNotBlank()",
        """    val canSend = canEdit && !attachmentLoading &&
        (ai.hasKey || apiKey.isNotBlank()) &&
        (ai.draft.isNotBlank() || selectedAttachment != null)""","chat send state")
    start=s.index("    val attachmentPicker = rememberLauncherForActivityResult(")
    end=s.index("    fun listen() {",start)
    handler="""    fun importAttachment(uri: android.net.Uri, isImage: Boolean) {
        if (!canEdit || attachmentLoading) return
        attachmentLoading = true
        scope.launch {
            try {
                selectedAttachment = readLenaSelectedAttachment(context, uri, isImage)
                error = ""
            } catch (exception: Exception) {
                error = exception.message ?: "Dosya okunamadı."
                selectedAttachment = null
            } finally {
                attachmentLoading = false
            }
        }
    }

"""
    s=s[:start]+handler+s[end:]
    s=once(s,"        val message = ai.draft.trim()",
        """        val message = listOfNotNull(ai.draft.trim().takeIf(String::isNotBlank),
            selectedAttachment?.promptText()).joinToString("\\n\\n")
            .take(maxMessageLength)""","chat attachment input")
    s=once(s,'                ai.draft = ""\n                apiKey = ""',
        '                ai.draft = ""\n                selectedAttachment = null\n                apiKey = ""',
        "clear sent attachment")
    start=s.index('            BoxWithConstraints(Modifier.fillMaxWidth()) {\n                val wide=maxWidth>=650.dp\n                Row(Modifier.fillMaxWidth().clip')
    end=s.index('            if (!keyboardVisible) BoxWithConstraints(Modifier.fillMaxWidth()) {',start)
    composer="""            LenaRichComposer(
                draft = ai.draft,
                onDraftChange = { ai.draft = it.take(maxMessageLength) },
                enabled = canEdit,
                canSend = canSend,
                onSend = ::send,
                maxLength = maxMessageLength,
                attachment = selectedAttachment,
                attachmentLoading = attachmentLoading,
                onAttachmentSelected = ::importAttachment,
                onRemoveAttachment = { selectedAttachment = null },
                onLanguageToggle = {
                    ai.language = if (ai.language == ConversationLanguage.English)
                        ConversationLanguage.Turkish else ConversationLanguage.English
                    audio?.stop()
                },
            )
"""
    s=s[:start]+composer+s[end:]
    assert "LenaRichComposer(" in s and 'attachmentPicker.launch("text/plain")' not in s
    assert 'ai.newConversation()' in s and 'audio?.play(turn)' in s
    chat.write_text(s,"utf8")

    # 2) Same image/document and emoji UI in the 3D teacher; preserve speech-recognizer flow.
    file=PKG/"LiveTeacherScreen.kt"
    ui=file.read_text("utf8")
    assert "LenaLive3DStage(" in ui
    assert "fun send(text: String)" in ui
    ui=once(ui,
        '    var audioError by remember { mutableStateOf("") }',
        '''    var audioError by remember { mutableStateOf("") }
    var selectedAttachment by remember { mutableStateOf<LenaPreparedAttachment?>(null) }
    var attachmentLoading by remember { mutableStateOf(false) }''',
        "3d attachment state")
    ui=once(ui,
        "    fun send(text: String) {",
        """    fun importAttachment(uri: android.net.Uri, isImage: Boolean) {
        if (!canEdit || attachmentLoading) return
        attachmentLoading = true
        scope.launch {
            try {
                selectedAttachment = readLenaSelectedAttachment(context, uri, isImage)
                error = ""
            } catch (exception: Exception) {
                error = exception.message ?: "Dosya okunamadı."
                selectedAttachment = null
            } finally {
                attachmentLoading = false
            }
        }
    }

    fun send(text: String) {""",
        "3d attachment handler")
    ui=once(ui,
        "        val message = text.trim()",
        """        // Speech-recognized text remains text; composer may add extracted
        // image/document writing, but raw image bytes are never misrepresented.
        val message = listOfNotNull(text.trim().takeIf(String::isNotBlank),
            selectedAttachment?.promptText()).joinToString("\\n\\n")
            .take(maxMessageLength)""",
        "3d attachment send")
    ui=once(ui,
        '                ephemeralKey = ""\n                avatarMotion.responseAvailable()',
        '                ephemeralKey = ""\n                selectedAttachment = null\n                avatarMotion.responseAvailable()',
        "3d clear selected file")
    start=ui.index('            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),\n                verticalAlignment = Alignment.CenterVertically) {\n                OutlinedTextField(\n                    value = ai.draft')
    end=ui.index('            if (!keyboardOpen) {',start)
    ui=ui[:start]+"""            LenaRichComposer(
                draft = ai.draft,
                onDraftChange = { ai.draft = it.take(maxMessageLength) },
                enabled = canEdit,
                canSend = canSend && (ai.draft.isNotBlank() || selectedAttachment != null),
                onSend = { send(ai.draft) },
                maxLength = maxMessageLength,
                attachment = selectedAttachment,
                attachmentLoading = attachmentLoading,
                onAttachmentSelected = ::importAttachment,
                onRemoveAttachment = { selectedAttachment = null },
                onLanguageToggle = {
                    ai.language = if (ai.language == ConversationLanguage.English)
                        ConversationLanguage.Turkish else ConversationLanguage.English
                    audio?.stop()
                },
            )
"""+ui[end:]

    # 3) Remove stage-bottom overlay completely: the transcript belongs to a
    # separate high-contrast composable BETWEEN the 3D viewport and composer.
    stage_begin=ui.index('            Column(\n                Modifier.fillMaxWidth().align(Alignment.BottomCenter)')
    stage_end=ui.index('        }\n\n        Column(\n            Modifier.fillMaxWidth().background(Color(0xFFF7FBF5)',stage_begin)
    ui=ui[:stage_begin]+ui[stage_end:]
    anchor='        }\n\n        Column(\n            Modifier.fillMaxWidth().background(Color(0xFFF7FBF5)'
    subtitle=r'''        }

        // Screen-space text panel, outside the SceneView surface.
        // The model and scene cannot paint over this native Compose control.
        Column(
            Modifier.fillMaxWidth().background(Color(0xFF174B3C))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("●", color = Color(0xFF4FDC78), fontSize = 12.sp)
                Text(when {
                    listening -> "Lena seni dinliyor…"
                    sending -> "Lena yanıtını hazırlıyor…"
                    speaking != null -> "Lena konuşuyor"
                    else -> "Lena hazır · Dilediğin zaman konuşabilirsin"
                }, color = Color(0xFFF6E8CB), fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
            }
            if (currentTurn != null && !keyboardOpen) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(Color(0xFF245F4C), RoundedCornerShape(15.dp))
                        .padding(start = 12.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(currentTurn.answer.replace(Regex("\\s+"), " ").take(300),
                        modifier = Modifier.weight(1f), color = Color.White,
                        fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = {
                        audioError = ""
                        if (speaking == currentTurn.id) audio?.stop()
                        else audio?.play(currentTurn)
                    }) {
                        Text(if (speaking == currentTurn.id) "■" else "▶",
                            color = Color.White, fontSize = 18.sp)
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().background(Color(0xFFF7FBF5)'''
    ui=once(ui,anchor,subtitle,"move subtitle in front of 3d")
    # Keep error text readable against the light composer.
    ui=ui.replace('Text(error, color = Color(0xFFFFB6A3)',
                  'Text(error, color = Color(0xFFAE4335)')
    ui=ui.replace('Text(audioError, color = Color(0xFFFFD5A1)',
                  'Text(audioError, color = Color(0xFFAE4335)')
    assert "LenaRichComposer(" in ui and "onAttachmentSelected = ::importAttachment" in ui
    assert "LenaLive3DStage(" in ui and "onRecoverableError" in ui
    assert "openTextChat()" in ui and "ai.reply(" in ui
    file.write_text(ui,"utf8")

    for path in targets:
        assert before[str(path)] == hashlib.sha256(path.read_bytes()).hexdigest(), path
    gradle=once(gradle,"versionCode = 95","versionCode = 96","monotonic version code")
    gradle=once(gradle,'versionName = "1.0.70-user-chair-safe-greeting"',
        'versionName = "1.0.71-lena-animation-chat-tools"',"monotonic version")
    GRADLE.write_text(gradle,"utf8")
    print("1.0.71: always-visible photo/document menu, 48 emoji choices on both screens.")
    print("Photo/PDF OCR and document extraction stay on device; AI is text-only.")
    print("3D subtitle moved to an independent foreground panel, animation effect ungated.")
    print("Original Lena GLB, chair binary, portraits, AI keys, history and Arena untouched.")

if __name__ == "__main__": main()
