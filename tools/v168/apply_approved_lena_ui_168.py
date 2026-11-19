#!/usr/bin/env python3
"""Apply approved first-reference Lena messenger UI after the verified v1.0.67 stage.

This is an incremental source overlay, NOT a replacement for a newer app.
Keep the actual Lena GLB portrait drawable, 3D engine, chat persistence,
AI key handling, learner photo storage, Arena and Word Memory untouched.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
PKG = ROOT / "source/app/src/main/java/com/kelimebulmacasi/a1"
GRADLE = ROOT / "source/app/build.gradle.kts"
BRAND = ROOT / "tools/v168/LenaMessengerBrand.kt"

def once(data: str, before: str, after: str, name: str) -> str:
    found = data.count(before)
    if found != 1:
        raise AssertionError(f"{name}: expected one matching anchor, found {found}")
    return data.replace(before, after, 1)

def main() -> None:
    g = GRADLE.read_text(encoding="utf-8")
    assert g.count("versionCode = 92") == 1
    assert g.count('versionName = "1.0.67-lena-real-profile-icons"') == 1
    assert 'applicationIdSuffix = ".fresh"' in g
    assert (PKG / "LenaMessengerBrand.kt").exists()
    assert (PKG / "LenaLive3DStage.kt").exists()
    assert (PKG / "ArenaApprovedHome.kt").exists()
    assert (PKG / "VocabularyNeonHome.kt").exists()
    assert (ROOT / "source/app/src/main/res/drawable-nodpi/wq167_lena_portrait.png").exists()
    # Only restyle composables: the original portrait resource and bytes are
    # never overwritten, regenerated or changed in this step.
    shutil.copyfile(BRAND, PKG / "LenaMessengerBrand.kt")

    file = PKG / "ConversationScreen.kt"
    s = file.read_text(encoding="utf-8")
    for contract in ('ConversationBubble(turn.answer, "Lena", userPortrait.value)',
                     "rememberLenaUserPortrait(session.profileKey)",
                     "audio?.play(turn)", "ai.searchThreads", "ai.newConversation()"):
        assert contract in s, contract

    s = once(s, "import androidx.compose.foundation.background\n",
        "import androidx.compose.foundation.background\n"
        "import androidx.compose.ui.graphics.Brush\n", "gradient import")
    s = once(s, "import androidx.compose.material3.TextButton\n",
        "import androidx.compose.material3.TextButton\n"
        "import androidx.compose.material3.Slider\n", "slider import")
    s = once(s, "    var speaking by remember { mutableStateOf<Long?>(null) }",
        "    var speaking by remember { mutableStateOf<Long?>(null) }\n"
        "    var voiceSettingsOpen by rememberSaveable { mutableStateOf(false) }",
        "voice controls state")
    s = once(s,
        "Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),",
        "Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()"
        ".background(Brush.verticalGradient(listOf(Color(0xFFFAFAF7), Color.White, Color(0xFFF0F8F2)))),",
        "light soft-mint background")
    s = once(s,
        "Modifier.widthIn(max = 760.dp).fillMaxSize().padding(horizontal = 15.dp),",
        "Modifier.widthIn(max = 1080.dp).fillMaxSize().padding(horizontal = 16.dp),",
        "responsive tablet width")
    s = once(s,
        "Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {",
        "Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),"
        " verticalAlignment = Alignment.CenterVertically,"
        " horizontalArrangement = Arrangement.spacedBy(7.dp)) {",
        "topbar spacing")
    s = once(s,
        "TextButton(onClick = back, contentPadding = PaddingValues(horizontal = 6.dp)) {",
        "TextButton(onClick = back, modifier = Modifier.clip(RoundedCornerShape(50))"
        ".background(Color.White).border(1.dp, Color(0xFFD4E7DA), RoundedCornerShape(50)),"
        " contentPadding = PaddingValues(horizontal = 9.dp)) {",
        "pill back")
    s = once(s,
        'Text("Lena · Mesajlar", modifier = Modifier.weight(1f), color = Ink, fontFamily = Serif, fontSize = 20.sp, lineHeight = 23.sp)',
        'Text("Lena · Mesajlar", modifier = Modifier.weight(1f), color = Color(0xFF123D2F),'
        ' fontFamily = Serif, fontSize = 22.sp, lineHeight = 26.sp)',
        "approved title")
    s = once(s,
        "TextButton(onClick = { audio?.stop(); scene3d = true }, contentPadding = PaddingValues(horizontal = 5.dp)) {",
        "TextButton(onClick = { audio?.stop(); scene3d = true },"
        " modifier = Modifier.clip(RoundedCornerShape(50))"
        ".background(Color(0xFFE9F9E7)).border(1.dp, Color(0xFF46B867), RoundedCornerShape(50)),"
        " contentPadding = PaddingValues(horizontal = 9.dp)) {",
        "real 3d action")
    s = once(s,
        "}, enabled = canEdit && ai.turns.isNotEmpty(), contentPadding = PaddingValues(horizontal = 6.dp)) {",
        "}, enabled = canEdit && ai.turns.isNotEmpty(),"
        " modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White)"
        ".border(1.dp, Color(0xFFD4E7DA), RoundedCornerShape(50)),"
        " contentPadding = PaddingValues(horizontal = 9.dp)) {",
        "new conversation action")
    s = once(s,
        "if (!keyboardVisible) Row(\n                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),\n"
        "                verticalAlignment = Alignment.CenterVertically,\n            ) {",
        "if (!keyboardVisible) Row(\n"
        "                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),\n"
        "                horizontalArrangement = Arrangement.spacedBy(8.dp),\n"
        "                verticalAlignment = Alignment.CenterVertically,\n            ) {\n"
        '                Text("Ders araçları", color = Color(0xFF16583B),\n'
        "                    fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 12.sp)\n",
        "scrollable lesson toolbar")
    s = once(s,
        "verticalArrangement = Arrangement.spacedBy(10.dp),",
        "verticalArrangement = Arrangement.spacedBy(12.dp),",
        "bubble spacing")
    s = once(s,
        ".background(if (mine) Color(0xFFEAF7D8) else Color.White, shape)",
        ".background(if (mine) Brush.horizontalGradient(listOf(Color(0xFFF2FBDD),"
        " Color(0xFFDFF2C6))) else Brush.horizontalGradient(listOf(Color.White,"
        " Color(0xFFFEFFFC))), shape)",
        "approved left white / right lime bubbles")
    s = once(s,
        ".border(1.dp, Color(0xFFD8E8D6), shape)",
        ".border(1.dp, Color(0xFFD1E3D3), shape)",
        "bubble border")
    s = once(s,
        "}, enabled = !recording && !sending) {",
        "}, enabled = !recording && !sending,"
        " modifier = Modifier.clip(RoundedCornerShape(50))"
        ".background(Color(0xFFE8F7E6)).border(1.dp, Color(0xFFC5E5C8), RoundedCornerShape(50))) {",
        "message audio pill")
    s = once(s,
        'label = { Text("Lena’ya mesaj yaz", fontFamily = Sans) },',
        'placeholder = { Text("Lena’ya mesaj yaz…", fontFamily = Sans) },\n'
        '                shape = RoundedCornerShape(25.dp),',
        "large rounded composer")

    begin = s.index(
        "            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n"
        "                OutlinedButton(onClick = ::listen")
    end = s.index(
        "            if (!keyboardVisible) Text(\"Son 8 sohbet turu", begin)
    # All actions remain real, including replaying the latest recorded AI reply.
    # There are no fake attachment, translation or playback progress controls.
    s = s[:begin] + '''            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = ::listen, enabled = canEdit,
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF269A4F), contentColor = Color.White)) {
                    Image(painterResource(R.drawable.wq167_mic),
                        contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (recording) "Dinleniyor" else "Konuş", fontFamily = Sans,
                        fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        audioError = ""
                        if (speaking != null) audio?.stop()
                        else ai.turns.lastOrNull()?.let { audio?.play(it) }
                    },
                    enabled = canEdit && ai.turns.isNotEmpty(),
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 11.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White, contentColor = Color(0xFF17543A)),
                ) {
                    Image(painterResource(if (speaking == null) R.drawable.wq167_listen
                        else R.drawable.wq167_pause),
                        contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (speaking == null) "Dinle" else "Durdur",
                        fontFamily = Sans, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = ::send, enabled = canSend,
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF269A4F), contentColor = Color.White)) {
                    Image(painterResource(R.drawable.wq167_send),
                        contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (sending) "Bekle" else "Gönder", fontFamily = Sans,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (!keyboardVisible) Row(
                Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LenaVoiceWaveform(speaking != null)
                OutlinedButton(
                    onClick = { voiceSettingsOpen = true },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White, contentColor = Color(0xFF16543C)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text("Ses Ayarları ▾", fontFamily = Sans, fontSize = 12.sp)
                }
            }
''' + s[end:]
    # Keep the transparent data disclosure accessible in the expanded tools.
    s = once(s,
        'if (!keyboardVisible) Text("Son 8 sohbet turu',
        'if (learningToolsOpen && !keyboardVisible) Text("Son 8 sohbet turu',
        "compact chat disclosure")

    s = once(s,
        "        deleteCandidate?.let { thread ->",
        '''        if (voiceSettingsOpen) {
            AlertDialog(
                onDismissRequest = { voiceSettingsOpen = false },
                title = { Text("Ses Ayarları") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Konuşma hızı: " +
                            String.format(java.util.Locale.ROOT, "%.1fx", session.preferences.speechRate))
                        Slider(value = session.preferences.speechRate,
                            onValueChange = {
                                session.changePreferences(session.preferences.copy(
                                    speechRate = it.coerceIn(.5f, 1.5f)))
                            }, valueRange = .5f..1.5f)
                        Text("Ses düzeyi: " +
                            (session.preferences.speechVolume * 100).toInt() + "%")
                        Slider(value = session.preferences.speechVolume,
                            onValueChange = {
                                session.changePreferences(session.preferences.copy(
                                    speechVolume = it.coerceIn(0f, 1f)))
                            }, valueRange = 0f..1f)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { voiceSettingsOpen = false }) {
                        Text("Tamam")
                    }
                },
            )
        }
        deleteCandidate?.let { thread ->''',
        "real saved voice settings")

    assert "LenaActualAvatar(37.dp)" in s
    assert "LenaUserAvatar(userAvatar, 37.dp)" in s
    assert "ai.newConversation()" in s
    assert "audio?.play(turn)" in s
    assert "session.changePreferences" in s
    assert 'R.drawable.wq167_lena_portrait' not in s, "Keep single original avatar owner"
    file.write_text(s, encoding="utf-8")

    g = once(g, "versionCode = 92", "versionCode = 93", "monotonic android version")
    g = once(g, 'versionName = "1.0.67-lena-real-profile-icons"',
        'versionName = "1.0.68-lena-approved-messenger-ui"', "versionName")
    GRADLE.write_text(g, encoding="utf-8")
    print("WordQuest 1.0.68: approved mint UI, responsive header, real audio buttons and settings.")
    print("Existing Lena drawable, 201 bone GLB, AI messages, history, all old learning screens untouched.")

if __name__ == "__main__":
    main()
