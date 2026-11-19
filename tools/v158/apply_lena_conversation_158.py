#!/usr/bin/env python3
"""v1.0.58: add optional real Lena GLB conversation without modifying Alex.

Runs AFTER the v1.0.57 source assembly, before audio generation/Gradle.
Never copies the old archived Ethan GLB, changes the installed package,
moves API secrets, or deletes user files. A native crash stays in :teacher3d.
"""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "source/app"
PKG = APP / "src/main/java/com/kelimebulmacasi/a1"
TEST = APP / "src/test/java/com/kelimebulmacasi/a1"
GRADLE = APP / "build.gradle.kts"
MANIFEST = APP / "src/main/AndroidManifest.xml"
SRC = ROOT / "tools/v158"
LENA = ROOT / "assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
MAPPING = ROOT / "assets/avatars/lena/animation-map.json"

def once(s: str, before: str, after: str, label: str) -> str:
    amount = s.count(before)
    if amount != 1:
        raise AssertionError(f"{label}: expected exactly one match, found {amount}")
    return s.replace(before, after, 1)

def main():
    assert PKG.is_dir(), "1.0.57 Android app not staged"
    g = GRADLE.read_text("utf-8")
    assert g.count("versionCode = 82") == 1, "Unexpected base version code"
    assert g.count('versionName = "1.0.57-arena-memory"') == 1
    assert 'applicationIdSuffix = ".fresh"' in g, "Do not change installed app identity"
    assert (PKG / "TeacherStage2D.kt").is_file(), "Alex 2.5D baseline missing"
    assert (PKG / "Teacher2DPortrait.kt").is_file(), "Do not remove Alex"
    assert (PKG / "LiveTeacherScreen.kt").is_file(), "Conversation UI missing"
    assert (PKG / "AiSession.kt").is_file(), "AI profile must be preserved"

    assert LENA.stat().st_size > 1_000_000, "Lena GLB absent or incomplete"
    clip_map = json.loads(MAPPING.read_text("utf-8"))
    assert clip_map["characterId"] == "lena"
    assert clip_map["sha256"] == hashlib.sha256(LENA.read_bytes()).hexdigest(), "Lena GLB hash mismatch"
    subprocess.run([sys.executable, str(ROOT / "tools/verify_lena_glb.py"), str(LENA)], check=True)
    target = APP / "src/main/assets/avatars/lena"
    target.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(LENA, target / LENA.name)
    shutil.copyfile(MAPPING, target / MAPPING.name)

    TEST.mkdir(parents=True, exist_ok=True)
    for name in ("LenaAnimationClips.kt", "LenaLive3DStage.kt"):
        shutil.copyfile(SRC / name, PKG / name)
    shutil.copyfile(SRC / "LenaAnimationClipsTest.kt", TEST / "LenaAnimationClipsTest.kt")

    # Reuse the already-tested microphone/Gemini/TTS activity, but give Lena a
    # separate identity and independent real GLB stage. This activity was
    # archived when Alex 2.5D replaced native Ethan; no Ethan code is restored.
    previous = (ROOT / "tools/v137/EthanIsolatedVoiceTeacherActivity.kt").read_text("utf-8")
    previous = previous.replace("EthanIsolatedVoiceTeacherActivity", "LenaIsolatedVoiceTeacherActivity")
    previous = previous.replace("Ethan3DConversation", "Lena3DConversation")
    previous = previous.replace("EthanLive3DStage", "LenaLive3DStage")
    previous = previous.replace("Ethan", "Lena")
    previous = previous.replace("ethan", "lena")
    previous = once(
        previous,
        """            TeacherFallback2D(
                TeacherAvatar.Mert, playing != null, listening, thinking,
                rate, Modifier.fillMaxSize(),
            )""",
        """            // Renderer failure must not interrupt ongoing AI/chat/audio.
            // Keep the original Alex 2.5D renderer as the safe fallback.
            Teacher2DPortrait(
                motion = mode, mouthCue = mouthCue,
                modifier = Modifier.fillMaxSize(),
            )""",
        "Lena non-native fallback",
    )
    previous = once(
        previous, 'val reply = ai.reply("", message)',
        'val reply = ai.reply("", message, personaOverride = "You are Lena, a warm and encouraging English conversation tutor. Speak naturally, correct gently, and ask one clear question at a time.")',
        "Lena AI identity",
    )
    assert "LenaLive3DStage(" in previous and "TeacherFallback2D(" not in previous
    (PKG / "LenaIsolatedVoiceTeacherActivity.kt").write_text(previous, "utf-8")

    # Preserve old Alex prompts for its existing chat; only the dedicated
    # Lena activity supplies a persona override.
    ai_file = PKG / "AiSession.kt"
    ai = ai_file.read_text("utf-8")
    ai = once(
        ai,
        'suspend fun reply(key: String, message: String, learningContext: String = "", learnerName: String = ""): ConversationTurn {',
        'suspend fun reply(key: String, message: String, learningContext: String = "", learnerName: String = "", personaOverride: String? = null): ConversationTurn {',
        "optional AI persona without changing legacy calls",
    )
    ai = once(
        ai,
        'selectedAvatar.prompt + " " + selectedPersonality.prompt + (if (selectedAvatar == TeacherAvatar.Mert) " " + Teacher2DPersona.systemPrompt else "")',
        '(personaOverride ?: selectedAvatar.prompt) + " " + selectedPersonality.prompt + (if (personaOverride == null && selectedAvatar == TeacherAvatar.Mert) " " + Teacher2DPersona.systemPrompt else "")',
        "Lena-only persona override; Alex preserved",
    )
    ai_file.write_text(ai, "utf-8")

    # A prominent Lena action lives on the existing voice assistant screen.
    # Opening it never passes the API key to an Intent and never touches Alex.
    ui_file = PKG / "LiveTeacherScreen.kt"
    ui = ui_file.read_text("utf-8")
    ui = once(
        ui,
        "    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->",
        """    val lenaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> ai.refreshExternalEthanThreads() }

    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->""",
        "dedicated nonexported Lena activity result",
    )
    ui = once(
        ui,
        '''            Text("3D · Döndür · İki parmakla yakınlaş", color = LiveWhite.copy(alpha = .73f), fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))''',
        '''            TextButton(
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
            ) { Text("Lena ile 3D konuş", color = LiveGold, fontSize = 13.sp) }''',
        "Lena button on conversation stage",
    )
    ui_file.write_text(ui, "utf-8")

    xml = MANIFEST.read_text("utf-8")
    assert "LenaIsolatedVoiceTeacherActivity" not in xml
    xml = once(
        xml, "</application>",
        '''    <!-- Lena's native renderer is isolated; main Alex chat remains unaffected. -->
        <activity
            android:name=".LenaIsolatedVoiceTeacherActivity"
            android:exported="false"
            android:process=":teacher3d" />
    </application>''',
        "Lena isolated activity manifest entry",
    )
    MANIFEST.write_text(xml, "utf-8")
    g = once(
        g, 'implementation("com.squareup.okhttp3:okhttp:4.12.0")',
        'implementation("com.squareup.okhttp3:okhttp:4.12.0")\n    implementation("io.github.sceneview:sceneview:2.3.0")',
        "SceneView dependency in the isolated Lena activity",
    )
    g = once(g, "versionCode = 82", "versionCode = 83", "monotonic versionCode")
    g = once(
        g, 'versionName = "1.0.57-arena-memory"',
        'versionName = "1.0.58-lena-voice"',
        "1.0.58 versionName",
    )
    GRADLE.write_text(g, "utf-8")

    assert 'applicationIdSuffix = ".fresh"' in g
    assert (PKG / "ArenaApprovedHome.kt").is_file(), "Arena UI must remain"
    assert (PKG / "VocabularyNeonHome.kt").is_file(), "Word Memory must remain"
    assert (PKG / "Teacher2DPortrait.kt").is_file(), "Alex must remain"
    print("Lena 1.0.58: independent GLB teacher with real AI, mic, TTS, named animation modes.")
    print("No silent native renderer in main process; original Alex, Arena, Word Memory preserved.")
    print("Lip sync is NOT phoneme-perfect; device graphics and visual rig QA still required.")

if __name__ == "__main__":
    main()
