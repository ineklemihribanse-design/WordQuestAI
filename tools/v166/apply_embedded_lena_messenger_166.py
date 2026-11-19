#!/usr/bin/env python3
"""v166: Lena in the big live screen; actual Lena text messenger fallback.
Run after v165 overlay; keep approved GLB face, Alex legacy files and data.
"""
from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
GRADLE=ROOT/"source/app/build.gradle.kts"

def once(s,old,new,label):
    n=s.count(old)
    if n!=1:raise AssertionError(f"{label}: expected 1 occurrence, found {n}")
    return s.replace(old,new,1)

def main():
    g=GRADLE.read_text("utf-8")
    assert g.count("versionCode = 90")==1 and g.count('versionName = "1.0.65-lena-speaking-default"')==1
    assert 'applicationIdSuffix = ".fresh"' in g
    for name in ("LenaLive3DStage.kt","LenaFaceAnimationClips.kt","TeacherCredentialBridgeProvider.kt",
                 "ArenaApprovedHome.kt","VocabularyNeonHome.kt","Teacher2DPortrait.kt"):
        assert (SRC/name).is_file(),name

    f=SRC/"LiveTeacherScreen.kt"
    s=f.read_text("utf-8")
    assert "Lena ile yüz yüze sesli konuşma" in s
    assert "var lenaOpened by rememberSaveable" in s
    # Remove the second isolated screen launch. Audio / TTS already comes
    # from LiveTeacherScreen; it now drives an embedded LenaLive3DStage.
    start=s.index("    val lenaLauncher = rememberLauncherForActivityResult(")
    end=s.index("    val recognizer = rememberLauncherForActivityResult(",start)
    s=s[:start]+'''    var lena3dFailed by remember { mutableStateOf(false) }

'''+s[end:]
    old='''            Column(
                modifier = Modifier.fillMaxSize().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Lena · Canlı 3D İngilizce öğretmeni",
                    color = LiveGold, fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text("Lena ile yüz yüze sesli konuşma",
                    color = LiveWhite, fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                Button(onClick = openLena, enabled = canEdit) {
                    Text("Lena ile konuş")
                }
            }'''
    new='''            // Real Lena GLB occupies the stage in THIS screen, and local
            // TTS speaking state drives mouth morphs. On a recoverable load
            // error the actual text messenger is the safe fallback.
            if (!lena3dFailed) {
                LenaLive3DStage(
                    motion = avatarMotion.mode,
                    mouthCue = mouthCue,
                    speechRate = session.preferences.speechRate,
                    modifier = Modifier.fillMaxSize(),
                    onRecoverableError = {
                        lena3dFailed = true
                        audio?.stop()
                        openTextChat()
                    },
                )
            } else {
                Column(
                    Modifier.fillMaxSize().padding(22.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Lena 3D kullanılamıyor.",
                        color = LiveWhite, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { audio?.stop(); openTextChat() }) {
                        Text("Mesajlaşarak devam et")
                    }
                }
            }
            // A working alternative to 3D is always directly accessible.
            TextButton(
                onClick = { audio?.stop(); openTextChat() },
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            ) { Text("Mesajlaşma", color = LiveGold, fontSize = 13.sp) }'''
    s=once(s,old,new,"replace empty Lena launch placeholder with embedded model")
    # v165 has already removed the obsolete camera text overlay.
    assert "openLena" not in s and "lenaLauncher" not in s
    assert "LenaLive3DStage(" in s and "TeacherStage2D(" not in s
    assert "openTextChat()" in s
    f.write_text(s,"utf-8")
    print("Lena directly rendered in the live teacher stage; on-error + manual text switch.")

    f=SRC/"ConversationScreen.kt"
    s=f.read_text("utf-8")
    assert "TeacherAvatarPicker(ai.avatar, ai::chooseAvatar" in s
    assert 'Text("Kişisel AI Öğretmen"' in s
    # Start on the independent messenger rather than initializing the native
    # 3D engine. A separate 3D Görüşme action enters Lena, so text chat also
    # remains usable after an unrecoverable native GLB failure.
    s=once(s,
        "    var scene3d by rememberSaveable { mutableStateOf(true) }",
        "    var scene3d by remember { mutableStateOf(false) }",
        "safe messenger default for independent fallback")
    s=once(s,
        "    var historyOpen by rememberSaveable { mutableStateOf(false) }",
        "    var historyOpen by rememberSaveable { mutableStateOf(false) }\n    var learningToolsOpen by rememberSaveable { mutableStateOf(false) }",
        "collapsible tools state")
    s=once(s,
        'val turn = ai.reply(key, message, optionalContext, session.profileName)',
        'val turn = ai.reply(key, message, optionalContext, session.profileName,\n                    personaOverride = "You are Lena, a patient English teacher. Respond warmly, correct gently and clearly.")',
        "Lena text AI identity")
    s=once(s,
        'Text("Kişisel AI Öğretmen", modifier = Modifier.weight(1f), color = Ink, fontFamily = Serif, fontSize = 20.sp, lineHeight = 23.sp)',
        'Text("Lena · Mesajlar", modifier = Modifier.weight(1f), color = Ink, fontFamily = Serif, fontSize = 20.sp, lineHeight = 23.sp)',
        "messenger header")
    s=once(s,'Text("3D Ders", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)',
        'Text("3D Görüşme", color = Forest, fontFamily = Sans, fontWeight = FontWeight.Bold)',
        "return to embedded 3D")
    begin=s.index('            BodyText("Sohbet profili: ')
    end=s.index('            if (!keyboardVisible) Row(',begin)
    segment=s[begin:end]
    assert "TeacherAvatarPicker(" in segment
    s=s[:begin]+'''            Text("Lena ile yazış · 3D ya da ses sorunu olursa buradan devam et",
                color = Muted, fontFamily = Sans, fontSize = 12.sp)
            if (!keyboardVisible) TextButton(
                onClick = { learningToolsOpen = !learningToolsOpen },
                contentPadding = PaddingValues(horizontal = 6.dp),
            ) { Text(if (learningToolsOpen) "Ders araçlarını kapat" else "Ders araçları",
                color = Forest, fontWeight = FontWeight.Bold) }
'''+s[end:]
    # Keep history and learning-memory buttons, but collapse IPA, language,
    # modes, difficulty, provider info to reclaim the chat viewport.
    start=s.index("            if (!keyboardVisible && ai.language == ConversationLanguage.English) {")
    end=s.index("            LazyColumn(",start)
    settings=s[start:end]
    assert "ipaSpeech" in settings and "TeacherMode.entries" in settings
    s=s[:start]+'''            if (learningToolsOpen && !keyboardVisible) {
'''+settings+'''            }
'''+s[end:]
    s=once(s,'ConversationBubble(turn.answer, "Asistan")',
           'ConversationBubble(turn.answer, "Lena")',"Lena bubble title")
    s=once(s,'label = { Text("Mesajın", fontFamily = Sans) }',
           'label = { Text("Lena’ya mesaj yaz", fontFamily = Sans) }',"chat composer")
    assert "TeacherAvatarPicker(" not in s and "Alex" not in s
    assert "ConversationBubble(turn.answer, \"Lena\")" in s
    assert "ai.searchThreads" in s and "ai.newConversation()" in s
    assert "TeacherMode.entries" in s and "ipaSpeech" in s
    f.write_text(s,"utf-8")
    print("Lena messenger staged with history, message bubbles, saved keys, Gemini, learning tools.")

    g=once(g,"versionCode = 90","versionCode = 91","versionCode")
    g=once(g,'versionName = "1.0.65-lena-speaking-default"',
           'versionName = "1.0.66-lena-inline-messenger"',"versionName")
    GRADLE.write_text(g,"utf-8")
    print("v166: preserving original Lena face/rig and all app/user data.")

if __name__=="__main__":
    main()
