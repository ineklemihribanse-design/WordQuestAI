#!/usr/bin/env python3
"""Static v0.8.1 source check: not an Android APK, emulator, or listening test."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
pkg = root / 'app/src/main/java/com/kelimebulmacasi/a1'
build = (root/'app/build.gradle.kts').read_text()
prefs = (pkg/'WordQuestPreferences.kt').read_text()
app = (pkg/'WordPronunciation.kt').read_text()
native = (pkg/'DeviceAccentPlayer.kt').read_text()
settings = (pkg/'WordQuestSettings.kt').read_text()
conversation = (pkg/'ConversationAudio.kt').read_text()
assert 'versionCode = 14' in build and 'versionName = "0.8.1"' in build
assert 'applicationId = "com.kelimebulmacasi.a1.practice"' in build
assert 'PronunciationMode.Device' in prefs and 'allowOnlineVoice: Boolean = false' in prefs
assert 'pronunciationMode = PronunciationMode.entries.find' in prefs
assert '"$profile.nativeUs"' in prefs and '"$profile.nativeUk"' in prefs
assert 'TextToSpeech' in native and 'KEY_FEATURE_NOT_INSTALLED' in native
assert 'candidate.locale' not in native or 'country' in native
assert 'KEY_FEATURE_NOT_INSTALLED' in conversation
assert 'NativeVoice' not in settings or 'nativeVoiceUs' in settings
assert 'OfflineAccentPlayer(context)' in app
assert 'bundled.play(remaining' in app
assert (pkg/'TeacherStore.kt').exists() and (pkg/'PuzzleScreen.kt').exists()
assets=root/'app/src/main/assets/pronunciation'
manifest=json.loads((assets/'manifest.json').read_text())
ipa=json.loads((assets/'ipa.json').read_text())
assert len(manifest['words']) == 2203
assert len(ipa['entries']) == 2184
assert sum(1 for _ in assets.rglob('*.ogg')) == 4406
print('PASS: v0.8.1 source; per-user pronunciation choice; TTS fallback; 4,406 clips; 2,184 IPA pairs')
print('NOT TESTED: Android compilation, emulator/device playback, natural voice quality, APK signing')
