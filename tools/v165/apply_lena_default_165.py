#!/usr/bin/env python3
"""WordQuest 1.0.65: make Lena the ONLY visible live voice teacher.

No Alex in live assistant, fallback, avatar selection or persona. The existing
Alex assets stay unmodified on disk for safe rollback. Do not change user
profiles, keys, chat history, voice accents, Arena, Word Memory or package ID.
Stages improved mouth-movement GLB from v164 original geometry (not v163
fake eyes / mouth).
"""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import sys
import tempfile

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
SRC=APP/"src/main/java/com/kelimebulmacasi/a1"
TESTS=APP/"src/test/java/com/kelimebulmacasi/a1"
GRADLE=APP/"build.gradle.kts"
BASE=ROOT/"assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
GLB=APP/"src/main/assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
MAP=GLB.parent/"animation-map.json"

def once(s,old,new,label):
    n=s.count(old)
    if n!=1:raise AssertionError(f"{label}: one anchor required; found {n}")
    return s.replace(old,new,1)

def main():
    g=GRADLE.read_text("utf-8")
    assert g.count("versionCode = 89")==1
    assert g.count('versionName = "1.0.64-prisma-original-look"')==1
    assert 'applicationIdSuffix = ".fresh"' in g
    assert (SRC/"LenaIsolatedVoiceTeacherActivity.kt").exists()
    assert (SRC/"Teacher2DPortrait.kt").exists()  # archived, never deleted
    assert (SRC/"TeacherCredentialBridgeProvider.kt").exists()
    assert (SRC/"ArenaApprovedHome.kt").exists()
    assert (SRC/"VocabularyNeonHome.kt").exists()
    m=json.loads(MAP.read_text("utf-8"))
    assert hashlib.sha256(GLB.read_bytes()).hexdigest()==m["sha256"]
    assert m["baseAssetSha256"]==hashlib.sha256(BASE.read_bytes()).hexdigest()

    # Rebuild using the original user's actual Prisma material segmentation;
    # keep all 9 surfaces, the 201-bone rig and the approved face sculpt.
    with tempfile.TemporaryDirectory(prefix="wq165-") as folder:
        output=Path(folder)/"Lena_v165.glb"
        subprocess.run([sys.executable,str(ROOT/"tools/v165/build_lena_lip_open_165.py"),
                        str(BASE),str(output),str(MAP)],check=True)
        GLB.write_bytes(output.read_bytes())
    subprocess.run([sys.executable,str(ROOT/"tools/verify_lena_glb.py"),
                    str(GLB)],check=True)
    m=json.loads(MAP.read_text("utf-8"))
    assert m["sha256"]==hashlib.sha256(GLB.read_bytes()).hexdigest()
    assert m["version"]=="1.0.65"

    for f in (SRC/"LenaFaceAnimationClips.kt",TESTS/"LenaFaceAnimationClipsTest.kt"):
        s=f.read_text("utf-8")
        assert "Lena_V164_Face_IdleBlink" in s and "Lena_V164_Face_TalkBlink" in s
        s=s.replace("Lena_V164_Face_","Lena_V165_Face_")
        f.write_text(s,"utf-8")

    # The standalone Lena activity must NEVER show Alex if a native renderer
    # fails; microphone, saved credentials and audio still operate normally.
    f=SRC/"LenaIsolatedVoiceTeacherActivity.kt"
    s=f.read_text("utf-8")
    s=once(s,
        '''            // Renderer failure must not interrupt ongoing AI/chat/audio.
            // Keep the original Alex 2.5D renderer as the safe fallback.
            Teacher2DPortrait(
                motion = mode, mouthCue = mouthCue,
                modifier = Modifier.fillMaxSize(),
            )''',
        '''            // The AI/microphone/TTS remain usable without native graphics.
            // Alex is not displayed in the active teacher experience.
            Text("Lena 3D görünümü kullanılamıyor; sesli sohbet devam ediyor.",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(24.dp))''',
        "remove Alex from Lena fallback")
    assert "Teacher2DPortrait(" not in s
    f.write_text(s,"utf-8")

    # Primary live teacher entry must open Lena directly and hide all Alex
    # branding, screen rendering, avatar chips and legacy Mert AI persona.
    f=SRC/"LiveTeacherScreen.kt"
    s=f.read_text("utf-8")
    assert "val lenaLauncher = rememberLauncherForActivityResult(" in s
    assert "TeacherStage2D(ai.avatar" in s
    assert "Lena ile 3D konuş" in s

    # Saveable flag prevents repeated launching when returning from Lena.
    launcher='''    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->'''
    direct='''    var lenaOpened by rememberSaveable { mutableStateOf(false) }
    val openLena: () -> Unit = {
        audio?.stop()
        val profile = ai.boundProfile
        if (profile.isNullOrBlank()) {
            error = "Lena için önce öğrenci profilini aç."
        } else {
            lenaLauncher.launch(
                Intent(context, LenaIsolatedVoiceTeacherActivity::class.java)
                    .putExtra("teacher_profile", profile)
                    .putExtra("speech_rate", session.preferences.speechRate)
                    .putExtra("speech_volume", session.preferences.speechVolume)
                    .putExtra("voice", session.preferences.voice.name)
            )
        }
    }
    LaunchedEffect(ai.boundProfile) {
        if (!lenaOpened && !ai.boundProfile.isNullOrBlank()) {
            lenaOpened = true
            openLena()
        }
    }

'''
    s=once(s,launcher,direct+launcher,"launch Lena when voice assistant opens")

    # Keep a lightweight landing screen when returning from the GLB activity;
    # no 2D Alex avatars or surface. Other chat, preferences and history UI stay.
    old='''            TeacherStage2D(ai.avatar, speaking != null, listening, sending,
                session.preferences.speechRate, Modifier.fillMaxSize(),
                mouthCue = mouthCue, motion = avatarMotion.mode,
                teacherProfile = ai.boundProfile,
                initialVoice = session.preferences.voice,
                speechVolume = session.preferences.speechVolume,
                onRemoteReturn = { ai.refreshExternalEthanThreads() })'''
    new='''            Column(
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
    s=once(s,old,new,"replace legacy Alex 2D canvas with Lena launch")

    # The old top-right button was a duplicate; keep the obvious central one.
    before='''            TextButton(
                onClick = {
                    audio?.stop()
                    val profile = ai.boundProfile
                    if (profile.isNullOrBlank()) {
                        error = "Lena için önce öğrenci profilini aç."
                    } else {
                        lenaLauncher.launch(
                            Intent(context, LenaIsolatedVoiceTeacherActivity::class.java)
                                .putExtra("teacher_profile", profile)
                                .putExtra("speech_rate", session.preferences.speechRate)
                                .putExtra("speech_volume", session.preferences.speechVolume)
                                .putExtra("voice", session.preferences.voice.name)
                        )
                    }
                },
                enabled = canEdit,
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
            ) { Text("Lena ile 3D konuş", color = LiveGold, fontSize = 13.sp) }'''
    s=once(s,before,'', "remove duplicate Lena launcher")

    # Remove Alex's on-screen avatar picker from the voice assistant ONLY.
    start=s.index('''            if (!keyboardOpen) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp),''')
    end=s.index('''            if (!ai.hasKey && !keyboardOpen) {''',start)
    picker=s[start:end]
    assert 'listOf(TeacherAvatar.Mert).forEach' in picker
    s=s[:start]+s[end:]

    # Remove avatar chooser from this conversation's settings, preserve
    # all other learning style/level/memory settings.
    start=s.index('''                    Text("Karakter", color = Ink, fontWeight = FontWeight.Bold)''')
    end=s.index('''                    Text("Öğretme tarzı", color = Ink, fontWeight = FontWeight.Bold)''',start)
    assert 'TeacherAvatar.Mert' in s[start:end]
    s=s[:start]+s[end:]

    s=s.replace('Text(ai.avatar.label, color = LiveWhite, fontSize = 23.sp', 
                'Text("Lena", color = LiveWhite, fontSize = 23.sp')
    s=s.replace(chr(36)+"{ai.avatar.label}","Lena")

    s=once(s,
        'val turn = ai.reply(key, message, optionalContext, session.profileName)',
        '''val turn = ai.reply(key, message, optionalContext, session.profileName,
                    personaOverride = "You are Lena, a warm and encouraging English tutor. Speak naturally and correct gently.")''',
        "Lena-only AI persona in the main chat")
    assert "TeacherStage2D(" not in s
    assert "listOf(TeacherAvatar.Mert)" not in s
    assert "Alex" not in s
    assert "LenaIsolatedVoiceTeacherActivity::class.java" in s
    assert 'ai.refreshExternalEthanThreads()' in s
    f.write_text(s,"utf-8")

    g=once(g,"versionCode = 89","versionCode = 90","monotonic version code")
    g=once(g,'versionName = "1.0.64-prisma-original-look"',
           'versionName = "1.0.65-lena-speaking-default"',"version name")
    GRADLE.write_text(g,"utf-8")
    print("v1.0.65: original Lena face preserved; lip-open amplitude strengthened; Lena launches by default.")
    print("Alex removed from live screens/fallback/selection, legacy source kept safely.")
    print("Kept user data, original signed package, keys, voice, Arena and Word Memory.")

if __name__=="__main__":main()
