#!/usr/bin/env python3
"""v1.0.69: actual Lena (untouched GLB) in a 3D studio + premium compact chat.

Runs *after* all 1.0.68 layers. Replaces no learner history, AI keys, Arena,
Word Memory, original avatar geometry, or signed app package identity.
"""
from pathlib import Path
import shutil, subprocess, sys

ROOT=Path(__file__).resolve().parents[2]
PKG=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
APP=ROOT/"source/app"
SRC=ROOT/"tools/v169"
GRADLE=APP/"build.gradle.kts"

def once(text, old, new, label):
    n=text.count(old)
    if n!=1:
        raise AssertionError(f"{label}: expected exactly 1 replacement, got {n}")
    return text.replace(old,new,1)

def main():
    g=GRADLE.read_text("utf-8")
    assert g.count("versionCode = 93")==1
    assert g.count('versionName = "1.0.68-lena-approved-messenger-ui"')==1
    assert 'applicationIdSuffix = ".fresh"' in g
    portrait=APP/"src/main/res/drawable-nodpi/wq167_lena_portrait.png"
    original=APP/"src/main/assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
    assert portrait.exists() and original.exists()
    portrait_before=portrait.read_bytes()
    lena_size=original.stat().st_size
    assert lena_size>1_000_000
    assert (PKG/"ArenaApprovedHome.kt").exists()
    assert (PKG/"VocabularyNeonHome.kt").exists()

    # Build offline, low-poly studio glTF in 3D, not a frozen screenshot.
    studio=APP/"src/main/assets/studio/lena_studio.glb"
    subprocess.run([sys.executable,str(SRC/"build_lena_studio.py"),str(studio)],check=True)
    assert studio.stat().st_size>10_000
    shutil.copyfile(SRC/"LenaLive3DStage.kt",PKG/"LenaLive3DStage.kt")
    shutil.copyfile(SRC/"LenaMessengerBrand.kt",PKG/"LenaMessengerBrand.kt")
    icon_target=APP/"src/main/res/drawable/wq169_attach.xml"
    icon_target.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(SRC/"wq169_attach.xml",icon_target)

    chat=PKG/"ConversationScreen.kt"
    s=chat.read_text("utf8")
    for dep in ("LenaMessengerProfileHeader(","LenaVoiceWaveform(",
                "ai.newConversation()","rememberLenaUserPortrait(session.profileKey)"):
        assert dep in s,dep
    s=once(s,"import androidx.compose.material3.TextButton\n",
        "import androidx.compose.material3.TextButton\n"
        "import androidx.compose.material3.Surface\n", "Surface import")
    s=once(s,"import androidx.compose.foundation.layout.Box\n",
        "import androidx.compose.foundation.layout.Box\n"
        "import androidx.compose.foundation.layout.BoxWithConstraints\n"
        "import androidx.compose.foundation.layout.height\n", "responsive composer and height")
    s=once(s,"import androidx.compose.ui.graphics.Color\n",
        "import androidx.compose.ui.graphics.Color\n"
        "import androidx.compose.ui.graphics.ColorFilter\n", "icon tint")
    s=once(s,"import androidx.compose.ui.platform.LocalFocusManager\n",
        "import androidx.compose.ui.platform.LocalFocusManager\n"
        "import androidx.compose.ui.window.Dialog\n"
        "import androidx.compose.ui.draw.shadow\n", "dialog imports")
    # Only allow user-chosen plain text attachments in the text-only AI.
    # Read at most 1200 characters, never upload unselected local files.
    before="    fun listen() {\n"
    attachment='''    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { selectedUri ->
        if (selectedUri != null && canEdit) {
            runCatching {
                context.contentResolver.openInputStream(selectedUri)?.bufferedReader()?.use { stream ->
                    val buffer = CharArray(1200)
                    val read = stream.read(buffer)
                    if (read > 0) {
                        ai.draft = (ai.draft.trim() + "\\n" + String(buffer, 0, read))
                            .trim().take(maxMessageLength)
                    }
                } ?: throw IllegalArgumentException("Metin dosyası açılamadı")
            }.onFailure { error = "Dosya okunamadı. Lütfen bir metin dosyası seç." }
        }
    }

'''
    s=once(s,before,attachment+before,"bounded text attachment")

    # Remove the full-height duplicate IPA/mode/provider forms from chat.
    # Keep them operational inside a scrollable modal, not above messages.
    start=s.index("            if (!keyboardVisible) TextButton(",
                  s.index("LenaMessengerModeBar("))
    stop=s.index("            LazyColumn(",start)
    old_tools=s[start:stop]
    tool_begin=old_tools.index("            if (!keyboardVisible) Row(")
    tool_body=old_tools[tool_begin:]
    tool_body=tool_body.replace("if (!keyboardVisible) Row(","Row(",1)
    tool_body=tool_body.replace("if (historyOpen && !keyboardVisible)","if (historyOpen)")
    tool_body=tool_body.replace("if (notesOpen && !keyboardVisible)","if (notesOpen)")
    tool_body=tool_body.replace("if (learningToolsOpen && !keyboardVisible)","if (learningToolsOpen)")
    # Dialog does not hide itself merely because a learner opens the keyboard.
    compact='''            if (!keyboardVisible) Row(
                Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,
            ) {
                TextButton(onClick = { learningToolsOpen = true }) {
                    Text("Ders araçları  ⚙", color=Color(0xFF276446),
                        fontSize=12.sp, fontWeight=FontWeight.SemiBold)
                }
            }
            if (learningToolsOpen) {
                Dialog(onDismissRequest={ learningToolsOpen=false }) {
                    Surface(shape=RoundedCornerShape(26.dp),color=Color(0xFFF9FCF7)) {
                        Column(Modifier.fillMaxWidth().heightIn(max=620.dp)
                            .verticalScroll(rememberScrollState()).padding(18.dp),
                            verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                                verticalAlignment=Alignment.CenterVertically) {
                                Text("Ders araçları",color=Color(0xFF185238),
                                    fontFamily=Serif,fontSize=22.sp,fontWeight=FontWeight.Bold)
                                TextButton(onClick={ learningToolsOpen=false }) { Text("Kapat") }
                            }
'''+tool_body+'''                            Text(
                                "Sohbet geçmişi ve öğrenme notları cihazında saklanır. " +
                                "Yanıt oluşturulurken son konuşma turları seçtiğin AI sağlayıcısına gönderilir.",
                                color=Color(0xFF657B6B),fontSize=11.sp,lineHeight=15.sp)
                        }
                    }
                }
            }
'''
    s=s[:start]+compact+s[stop:]
    # Chat can now occupy the main scrollable area. Never invent per-turn dates.
    s=once(s,"            LazyColumn(\n",
        '''            if (!keyboardVisible) Row(Modifier.fillMaxWidth().padding(vertical=3.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFD4E5D9)))
                Text("Sohbet",color=Color(0xFF2C7950),fontSize=12.sp,
                    fontWeight=FontWeight.SemiBold)
                Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFD4E5D9)))
            }
            LazyColumn(
''',"conversation separator")
    s=once(s,".fillMaxWidth(if (mine) .78f else .84f)",
        ".fillMaxWidth(if (mine) .82f else .94f)","balanced bubble widths")
    s=once(s,"if (action != null) Box(Modifier.align(Alignment.End)) { action() }",
        "if (action != null) Box(Modifier.fillMaxWidth()) { action() }",
        "full message player width")
    s=once(s,
        ".background(if (mine) Brush.horizontalGradient(",
        ".shadow(3.dp, shape).background(if (mine) Brush.horizontalGradient(",
        "subtle bubble elevation")

    # All message players are real Android TTS callbacks.
    # Do not show imaginary times/durations: TTS APIs do not expose those.
    beg=s.index('                        ConversationBubble(turn.answer, "Lena", userPortrait.value) {')
    end=s.index('                    }\n                }\n                if (sending) item',beg)
    reply='''                        ConversationBubble(turn.answer, "Lena", userPortrait.value) {
                            Column(Modifier.fillMaxWidth(),
                                verticalArrangement=Arrangement.spacedBy(2.dp)) {
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement=Arrangement.End) {
                                    LenaMessageBookmark(session.profileKey,turn.id)
                                }
                                LenaMessageAudioBar(
                                    playing=speaking==turn.id,
                                    speechRate=session.preferences.speechRate,
                                    onToggle={
                                        audioError=""
                                        if (speaking==turn.id) audio?.stop()
                                        else audio?.play(turn)
                                    },
                                    onRateChange={
                                        val steps=listOf(.75f,1f,1.25f,1.5f)
                                        val next=steps.firstOrNull {
                                            it>session.preferences.speechRate+.02f
                                        } ?:steps.first()
                                        session.changePreferences(
                                            session.preferences.copy(speechRate=next))
                                    },
                                )
                            }
                        }
'''
    s=s[:beg]+reply+s[end:]

    # One compact composer: send is on the message line as in the reference.
    begin=s.index('            OutlinedTextField(\n                ai.draft, { ai.draft = it.take(maxMessageLength) }')
    finish=s.index('            if (learningToolsOpen && !keyboardVisible) Text("Son 8 sohbet turu',begin)
    composer='''            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val wide=maxWidth>=650.dp
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(31.dp))
                        .background(Color.White)
                        .border(1.dp,Color(0xFFD4E7D9),RoundedCornerShape(31.dp))
                        .padding(horizontal=5.dp,vertical=2.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    if (wide) TextButton(onClick={ attachmentPicker.launch("text/plain") },
                        enabled=canEdit,contentPadding=PaddingValues(horizontal=4.dp)) {
                        Image(painterResource(R.drawable.wq169_attach),
                            contentDescription="Metin dosyası ekle",
                            modifier=Modifier.size(21.dp))
                    }
                    OutlinedTextField(
                        ai.draft,{ ai.draft=it.take(maxMessageLength) },
                        modifier=Modifier.weight(1f),enabled=canEdit,
                        minLines=1,maxLines=3,shape=RoundedCornerShape(27.dp),
                        placeholder={ Text("Lena’ya mesaj yaz…",fontFamily=Sans) },
                        keyboardOptions=KeyboardOptions(
                            capitalization=KeyboardCapitalization.Sentences),
                    )
                    if (wide) {
                        TextButton(onClick={
                            ai.draft=(ai.draft+"🙂").take(maxMessageLength)
                        },enabled=canEdit,contentPadding=PaddingValues(horizontal=3.dp)) {
                            Text("☺",color=Color(0xFF266546),fontSize=19.sp)
                        }
                        TextButton(onClick={
                            ai.language=if (ai.language==ConversationLanguage.English)
                                ConversationLanguage.Turkish else ConversationLanguage.English
                            audio?.stop()
                        },enabled=canEdit,contentPadding=PaddingValues(horizontal=3.dp)) {
                            Text("文A",fontSize=14.sp,color=Color(0xFF266546))
                        }
                    }
                    Button(onClick=::send,enabled=canSend,shape=RoundedCornerShape(50),
                        modifier=Modifier.size(46.dp),
                        contentPadding=PaddingValues(9.dp),
                        colors=ButtonDefaults.buttonColors(
                            containerColor=Color(0xFF239B4E),contentColor=Color.White)) {
                        Image(painterResource(R.drawable.wq167_send),
                            contentDescription="Gönder",
                            colorFilter=ColorFilter.tint(Color.White),
                            modifier=Modifier.size(23.dp))
                    }
                }
            }
            if (!keyboardVisible) BoxWithConstraints(Modifier.fillMaxWidth()) {
                val wide=maxWidth>=650.dp
                Row(Modifier.fillMaxWidth().padding(vertical=5.dp),
                    horizontalArrangement=Arrangement.spacedBy(7.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Button(onClick=::listen,enabled=canEdit,
                        modifier=Modifier.weight(1f),shape=RoundedCornerShape(50),
                        colors=ButtonDefaults.buttonColors(
                            containerColor=Color(0xFF229B50),contentColor=Color.White)) {
                        Image(painterResource(R.drawable.wq167_mic),
                            contentDescription=null,
                            colorFilter=ColorFilter.tint(Color.White),
                            modifier=Modifier.size(20.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if(recording) "Dinleniyor" else "Konuş",
                            fontSize=12.sp,fontWeight=FontWeight.Bold)
                    }
                    OutlinedButton(onClick={
                        audioError=""
                        if(speaking!=null)audio?.stop()
                        else ai.turns.lastOrNull()?.let { audio?.play(it) }
                    }, enabled=canEdit && ai.turns.isNotEmpty(),
                        modifier=Modifier.weight(if(wide).75f else 1f),
                        shape=RoundedCornerShape(50),
                        colors=ButtonDefaults.outlinedButtonColors(
                            containerColor=Color.White,contentColor=Color(0xFF185839))) {
                        Image(painterResource(if(speaking==null) R.drawable.wq167_listen
                            else R.drawable.wq167_pause),
                            contentDescription=null,modifier=Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if(speaking==null)"Dinle" else "Durdur",
                            fontWeight=FontWeight.SemiBold,fontSize=12.sp)
                    }
                    if(wide)LenaVoiceWaveform(speaking!=null)
                    OutlinedButton(onClick={voiceSettingsOpen=true},
                        modifier=Modifier.weight(if(wide)1f else 1.18f),
                        shape=RoundedCornerShape(50),
                        colors=ButtonDefaults.outlinedButtonColors(
                            containerColor=Color.White,contentColor=Color(0xFF185839))) {
                        Text("Ses Ayarları ▾",fontSize=12.sp,maxLines=1)
                    }
                }
            }
'''
    s=s[:begin]+composer+s[finish:]
    # The privacy disclosure is now in the tools modal instead of pushing the
    # last actual message out of the viewport.
    privacy=s.index('            if (learningToolsOpen && !keyboardVisible) Text("Son 8 sohbet turu')
    stop=s.index('\n        }\n        deleteCandidate?.let',privacy) if '\n        }\n        deleteCandidate?.let' in s[privacy:] else -1
    if stop==-1:
        stop=s.index('\n        }\n        if (voiceSettingsOpen)',privacy)
    s=s[:privacy]+s[stop:]
    assert 'LenaMessageAudioBar(' in s and 'LenaMessageBookmark(' in s
    assert 'attachmentPicker.launch("text/plain")' in s
    assert 'ai.searchThreads' in s and 'ai.newConversation()' in s
    assert 'audio?.play(turn)' in s and 'session.changePreferences' in s
    chat.write_text(s,"utf8")

    live=PKG/"LiveTeacherScreen.kt"
    ui=live.read_text("utf8")
    assert 'LenaLive3DStage(' in ui
    assert 'openTextChat()' in ui
    ui=once(ui,
        "import androidx.compose.foundation.layout.width\n",
        "import androidx.compose.foundation.layout.width\n"
        "import androidx.compose.foundation.layout.size\n", "size import")
    ui=once(ui,
        "import androidx.compose.foundation.shape.RoundedCornerShape\n",
        "import androidx.compose.foundation.shape.RoundedCornerShape\n"
        "import androidx.compose.ui.draw.clip\n", "clip import")
    bar_start=ui.index('        Row(\n            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),')
    bar_end=ui.index("        // Large, independently rendered 3D scene",bar_start)
    topbar='''        Row(
            Modifier.fillMaxWidth().background(Color(0xFF164433))
                .padding(horizontal=12.dp,vertical=9.dp),
            horizontalArrangement=Arrangement.spacedBy(9.dp),
            verticalAlignment=Alignment.CenterVertically,
        ) {
            TextButton(onClick={audio?.stop();back()},
                modifier=Modifier.clip(RoundedCornerShape(50))
                    .background(Color(0xFFF2F9EF)),
                contentPadding=PaddingValues(horizontal=12.dp)) {
                Text("‹ Geri",color=Color(0xFF18543A),fontWeight=FontWeight.Bold)
            }
            LenaActualAvatar(42.dp)
            Column(Modifier.weight(1f)) {
                Text("CANLI AI ÖĞRETMEN",fontSize=10.sp,color=Color(0xFFF0D8AA),
                    fontWeight=FontWeight.SemiBold,letterSpacing=1.sp)
                Text("Lena",fontSize=24.sp,color=Color.White,fontFamily=Serif,
                    fontWeight=FontWeight.Bold)
            }
            TextButton(onClick={audio?.stop();threadsOpen=true;ai.searchThreads("")},
                enabled=canEdit) { Text("Sohbetler",color=Color.White,fontSize=12.sp) }
            TextButton(onClick={audio?.stop();settingsOpen=true},enabled=canEdit) {
                Text("Ayarlar",color=Color(0xFFFFE1A6),fontSize=12.sp)
            }
            TextButton(onClick={audio?.stop();openTextChat()},
                modifier=Modifier.clip(RoundedCornerShape(50))
                    .background(Color(0xFFF4FAF0))) {
                Text("Mesajlaşma",color=Color(0xFF17543A),
                    fontSize=12.sp,fontWeight=FontWeight.Bold)
            }
        }

'''
    ui=ui[:bar_start]+topbar+ui[bar_end:]
    # Remove obsolete duplicate floating chat switch that covered the studio.
    old='''            TextButton(
                onClick = { audio?.stop(); openTextChat() },
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            ) { Text("Mesajlaşma", color = LiveGold, fontSize = 13.sp) }'''
    ui=once(ui,old,"","duplicate stage chat")
    # Stage status and subtitle sit in translucent foreground as requested.
    status_start=ui.index('            Column(\n                Modifier.fillMaxWidth().align(Alignment.BottomCenter)')
    status_end=ui.index('        }\n\n        Column(\n            Modifier.fillMaxWidth().background(LiveDark',status_start)
    status='''            Column(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(
                        Color.Transparent,Color(0xCC123F33),Color(0xED14392F))))
                    .padding(start=20.dp,end=20.dp,top=40.dp,bottom=12.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
            ) {
                Row(Modifier.clip(RoundedCornerShape(50))
                    .background(Color(0x991D4738))
                    .border(1.dp,Color(0x88B7D7C0),RoundedCornerShape(50))
                    .padding(horizontal=14.dp,vertical=7.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(9.dp).clip(CircleShape)
                        .background(Color(0xFF56D978)))
                    Text(when {
                        listening->"Lena seni dinliyor…"
                        sending->"Lena yanıtını hazırlıyor…"
                        speaking!=null->"Lena konuşuyor"
                        else->"Lena hazır · Dilediğin zaman konuşabilirsin"
                    },color=Color(0xFFFFE5BB),fontSize=12.sp,
                    fontWeight=FontWeight.SemiBold)
                }
                if(currentTurn!=null && !keyboardOpen) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp))
                        .background(Color(0xBB194C3E))
                        .border(1.dp,Color(0x77D0E4D5),RoundedCornerShape(25.dp))
                        .padding(horizontal=13.dp,vertical=9.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(11.dp)) {
                        LenaVoiceWaveform(speaking!=null)
                        Text(currentTurn.answer.replace(Regex("\\\\s+")," ").take(175),
                            modifier=Modifier.weight(1f),
                            color=Color.White,fontSize=12.sp,lineHeight=16.sp,
                            maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(String.format(java.util.Locale.ROOT,"%.1f×",
                            session.preferences.speechRate),
                            color=Color(0xFFFFE2AC),fontSize=11.sp)
                        TextButton(onClick={
                            if(speaking==currentTurn.id)audio?.stop()
                            else audio?.play(currentTurn)
                        }) { Text(if(speaking!=null)"■" else "▶",color=Color.White) }
                    }
                }
            }
'''
    ui=ui[:status_start]+status+ui[status_end:]
    ui=once(ui,
        'Modifier.fillMaxWidth().background(LiveDark, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))',
        'Modifier.fillMaxWidth().background(Color(0xFFF7FBF5), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))',
        'cream composer')
    ui=once(ui,
        'Text("Öğretmenine bir şey sor…", color = LiveWhite.copy(alpha = .63f))',
        'Text("Lena’ya mesaj yaz…", color = Color(0xFF728279))',
        'chat placeholder')
    ui=once(ui,
        'containerColor = Color(0xFF387B74), contentColor = LiveWhite',
        'containerColor = Color(0xFF239B4F), contentColor = Color.White',
        'mic green')
    ui=once(ui,
        'Text(if (speaking != null) "■ Durdur" else "▶ Dinle", color = LiveWhite, fontSize = 14.sp)',
        'Text(if (speaking != null) "■ Durdur" else "▶ Dinle", color = Color(0xFF194E38), fontSize = 14.sp)',
        'listen foreground')
    # Light message field instead of hard-to-read white text over cream.
    field=ui.index("@Composable\nprivate fun darkFieldColors()")
    ui=ui[:field]+'''@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF173D2D),
    unfocusedTextColor = Color(0xFF173D2D),
    disabledTextColor = Color(0xFF677E70),
    cursorColor = Color(0xFF279A50),
    focusedBorderColor = Color(0xFF60B67A),
    unfocusedBorderColor = Color(0xFFCBDDD0),
    focusedLabelColor = Color(0xFF21603D),
    unfocusedLabelColor = Color(0xFF21603D),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
)
'''
    assert "LenaLive3DStage(" in ui and 'LenaVoiceWaveform(' in ui
    assert "openTextChat()" in ui and "ai.reply(" in ui
    live.write_text(ui,"utf8")

    g=once(g,"versionCode = 93","versionCode = 94","next code")
    g=once(g,'versionName = "1.0.68-lena-approved-messenger-ui"',
        'versionName = "1.0.69-lena-premium-3d-studio"',"next version")
    GRADLE.write_text(g,"utf8")
    assert portrait_before==portrait.read_bytes()
    assert original.stat().st_size==lena_size
    print("WordQuest 1.0.69: actual 3D studio, waist-up entry camera, true Lena portrait.")
    print("Premium messages, bounded text attachment, real TTS, per-learner bookmarks,")
    print("kept AI, profile history, Arena, Word Memory and signed package identity.")

if __name__=="__main__":
    main()
