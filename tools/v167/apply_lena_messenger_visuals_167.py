#!/usr/bin/env python3
"""Build Lena's bright messenger from the actual WordQuest 3D teacher.

Run after v166's already-tested messenger/3D-screen overlay. It preserves
all chat/network/TTS/history behavior and all previous app functionality.
Only UI assets/profile pictures/icons/bubble style are changed. User gallery
photo is private, persistent per profile and can be removed. No generic AI
woman portrait, no icon font packages, no new app permissions.
"""
from pathlib import Path
import shutil
import subprocess
import sys
import struct

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
SRC=APP/"src/main/java/com/kelimebulmacasi/a1"
RES=APP/"src/main/res"
GRADLE=APP/"build.gradle.kts"
V167=ROOT/"tools/v167"
MODEL=APP/"src/main/assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"

def once(s,old,new,label):
    n=s.count(old)
    if n!=1:raise AssertionError(f"{label}: expected one occurrence, got {n}")
    return s.replace(old,new,1)

def main():
    g=GRADLE.read_text("utf-8")
    assert g.count("versionCode = 91")==1
    assert g.count('versionName = "1.0.66-lena-inline-messenger"')==1
    assert 'applicationIdSuffix = ".fresh"' in g
    assert MODEL.is_file()
    assert (SRC/"ConversationScreen.kt").is_file()
    for important in ("LenaLive3DStage.kt","TeacherCredentialBridgeProvider.kt",
                      "ArenaApprovedHome.kt","VocabularyNeonHome.kt","Teacher2DPortrait.kt"):
        assert (SRC/important).is_file(),important
    # Produce a PORTRAIT FROM THE SAME GLB THAT IS PACKAGED IN THIS APK.
    # Rendering this staged GLB after v165/v166 ensures the hair/eyes/face
    # match the live teacher, not another synthetic teacher portrait.
    portrait=RES/"drawable-nodpi/wq167_lena_portrait.png"
    subprocess.run([sys.executable,str(V167/"render_lena_real_portrait.py"),
        str(MODEL),str(portrait)],check=True)
    png=portrait.read_bytes()
    assert png.startswith(b"\x89PNG\r\n\x1a\n") and struct.unpack(">II",png[16:24])==(448,448)

    icons=RES/"drawable"
    subprocess.run([sys.executable,str(V167/"generate_messenger_icons.py"),
                    str(icons)],check=True)
    assert len(list(icons.glob("wq167_*.xml")))>=15
    shutil.copyfile(V167/"LenaMessengerBrand.kt",SRC/"LenaMessengerBrand.kt")

    file=SRC/"ConversationScreen.kt"
    s=file.read_text("utf-8")
    assert 'Text("Lena · Mesajlar"' in s
    assert "learningToolsOpen" in s
    assert "TeacherAvatarPicker(" not in s
    assert "scene3d by remember { mutableStateOf(false) }" in s
    assert "ConversationBubble(turn.answer, \"Lena\")" in s
    assert "ai.searchThreads" in s and "ai.newConversation()" in s
    assert "audio?.play(turn)" in s
    # Imports for image, avatars, custom icons and pastel bubble palette.
    imports=[
        "import androidx.compose.foundation.Image",
        "import androidx.compose.ui.graphics.Color",
        "import androidx.compose.ui.graphics.ImageBitmap",
        "import androidx.compose.ui.res.painterResource",
        "import androidx.compose.foundation.layout.Spacer",
        "import androidx.compose.foundation.layout.size",
        "import androidx.compose.foundation.layout.width",
        "import androidx.compose.foundation.shape.CircleShape",
        "import androidx.compose.ui.draw.clip",
    ]
    anchor="import androidx.compose.foundation.background\n"
    assert anchor in s
    s=s.replace(anchor,anchor+"".join(x+"\n" for x in imports if x not in s),1)
    state="    var learningToolsOpen by rememberSaveable { mutableStateOf(false) }"
    s=once(s,state,
        state+"\n    val userPortrait = rememberLenaUserPortrait(session.profileKey)",
        "live private-profile avatar state")

    old='''            Text("Lena ile yazış · 3D ya da ses sorunu olursa buradan devam et",
                color = Muted, fontFamily = Sans, fontSize = 12.sp)'''
    new='''            if (!keyboardVisible) {
                LenaMessengerProfileHeader(
                    profileName = session.profileName,
                    profileKey = session.profileKey,
                    aiReady = ai.hasKey,
                    userAvatar = userPortrait,
                )
                LenaMessengerModeBar(
                    selected = ai.mode,
                    onSelect = { ai.mode = it },
                    onVocabulary = { learningToolsOpen = true },
                )
            }'''
    s=once(s,old,new,"actual Lena profile and original custom icon tabs")

    # Reuse original assets for every displayed teacher/user message.
    s=once(s,'ConversationBubble(turn.message, "Sen")',
        'ConversationBubble(turn.message, "Sen", userPortrait.value)',
        "user bubble profile photo")
    s=once(s,'ConversationBubble(turn.answer, "Lena") {',
        'ConversationBubble(turn.answer, "Lena", userPortrait.value) {',
        "Lena bubble real-model portrait")
    s=once(s,'ConversationBubble(pending, "Sen")',
        'ConversationBubble(pending, "Sen", userPortrait.value)',
        "pending bubble user profile")
    # The previous TTS control already works. Preserve the click action
    # exactly and only add our original icons next to its stateful text.
    s=once(s,
        '''                                Text(if (speaking == turn.id) "Durdur" else "Dinle", fontFamily = Sans, fontWeight = FontWeight.Bold)''',
        '''                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(
                                            if (speaking == turn.id) R.drawable.wq167_pause else R.drawable.wq167_audio),
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (speaking == turn.id) "Durdur" else "Dinle",
                                        color = Forest, fontFamily = Sans,
                                        fontWeight = FontWeight.Bold)
                                }''',
        "original per-message TTS audio controls with vector icon")
    # Original header navigation stays functional; its two relevant
    # actions acquire original WordQuest UI vector illustrations.
    s=once(s,
        '''                    Text("3D Görüşme", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)''',
        '''                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.wq167_cube), contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("3D Görüşme", color = Forest, fontFamily = Sans,
                            fontWeight = FontWeight.Bold)
                    }''',
        "3d icon")
    s=once(s,
        '''                    Text("Yeni sohbet", fontFamily = Sans)''',
        '''                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.wq167_new), contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Yeni sohbet", fontFamily = Sans)
                    }''',
        "new chat icon")
    s=once(s,
        '''enabled = canEdit) { Text(if (historyOpen) "Geçmişi kapat" else "Sohbet geçmişi") }''',
        '''enabled = canEdit) {
                    Image(painterResource(R.drawable.wq167_history), contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (historyOpen) "Geçmişi kapat" else "Sohbet geçmişi")
                }''',
        "conversation history icon")
    s=once(s,
        '''                    Text(if (notesOpen) "Notları kapat" else "Öğrenme hafızam")''',
        '''                    Image(painterResource(R.drawable.wq167_grammar), contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (notesOpen) "Notları kapat" else "Öğrenme hafızam")''',
        "learning memory icon")
    s=once(s,
        '''                    Text(if (recording) "Dinleniyor…" else "Mikrofon", fontFamily = Sans, fontWeight = FontWeight.Bold)''',
        '''                    Image(painterResource(R.drawable.wq167_mic), contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (recording) "Dinleniyor…" else "Mikrofon", fontFamily = Sans,
                        fontWeight = FontWeight.Bold)''',
        "microphone icon")
    s=once(s,
        '''                    Text(if (sending) "Bekle…" else "Gönder", fontFamily = Sans, fontWeight = FontWeight.Bold)''',
        '''                    Image(painterResource(R.drawable.wq167_send), contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (sending) "Bekle…" else "Gönder", fontFamily = Sans,
                        fontWeight = FontWeight.Bold)''',
        "send action icon")

    # Rebuild the single bubble component with the real portrait and the
    # optional user-uploaded profile photo, still selectable/copyable text.
    func=s.index("@Composable\nprivate fun ConversationBubble(")
    assert s.count("@Composable\nprivate fun ConversationBubble(")==1
    replacement='''@Composable
private fun ConversationBubble(
    message: String,
    label: String,
    userAvatar: ImageBitmap? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val mine = label == "Sen"
    val shape = RoundedCornerShape(19.dp)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!mine) {
            LenaActualAvatar(37.dp)
            Spacer(Modifier.width(7.dp))
        }
        Column(
            Modifier.fillMaxWidth(if (mine) .78f else .84f)
                .background(if (mine) Color(0xFFEAF7D8) else Color.White, shape)
                .border(1.dp, Color(0xFFD8E8D6), shape)
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(label, color = Forest, fontFamily = Sans, fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
            SelectionContainer { BodyText(message, Ink) }
            if (action != null) Box(Modifier.align(Alignment.End)) { action() }
        }
        if (mine) {
            Spacer(Modifier.width(7.dp))
            LenaUserAvatar(userAvatar, 37.dp)
        }
    }
}
'''
    s=s[:func]+replacement
    assert 'LenaActualAvatar(37.dp)' in s
    assert 'rememberLenaUserPortrait(session.profileKey)' in s
    assert "TeacherAvatarPicker(" not in s
    assert "ai.newConversation()" in s and "audio?.play(turn)" in s
    file.write_text(s,"utf-8")

    g=once(g,"versionCode = 91","versionCode = 92","monotonic version")
    g=once(g,'versionName = "1.0.66-lena-inline-messenger"',
           'versionName = "1.0.67-lena-real-profile-icons"',"version string")
    GRADLE.write_text(g,"utf-8")
    print("v167 staged real Lena model portrait, 17 unique icons, selectable persistent user photo and light bubbles.")
    print("Preserved 3D lesson, message TTS, writing, user history, Alex archive, all app modules and signing.")

if __name__=="__main__":
    main()
