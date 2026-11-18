#!/usr/bin/env python3
"""Repeatable source and legacy-asset comparison; NOT an Android compilation/device test."""
import hashlib
from pathlib import Path
import sys

if len(sys.argv)!=3:sys.exit('usage: python tools/static_upgrade_audit.py /path/to/v075_source_root /path/to/v080_source_root')
old,new=map(Path,sys.argv[1:])
assert old.is_dir() and new.is_dir()
legacy_assets=[p.relative_to(old) for p in (old/'app/src/main/assets').rglob('*') if p.is_file()]
assert legacy_assets
for path in legacy_assets:
    assert hashlib.sha256((old/path).read_bytes()).digest()==hashlib.sha256((new/path).read_bytes()).digest(),path
for path_in_old in (old/'app/src/main/res').rglob('*'):
    if path_in_old.is_file():
        path=path_in_old.relative_to(old)
        assert path_in_old.read_bytes()==(new/path).read_bytes(),path
for path_in_old in (old/'app/src/test').rglob('*'):
    if path_in_old.is_file() and path_in_old.name!='WordPronunciationTest.kt':
        path=path_in_old.relative_to(old)
        assert path_in_old.read_bytes()==(new/path).read_bytes(),path
build=(new/'app/build.gradle.kts').read_text()
assert 'versionCode = 13' in build and 'versionName = "0.8.0"' in build
assert 'applicationId = "com.kelimebulmacasi.a1.practice"' in build
assert 'noCompress += setOf("ogg")' in build
v=(new/'app/src/main/java/com/kelimebulmacasi/a1/WordPronunciation.kt').read_text()
a=(new/'app/src/main/java/com/kelimebulmacasi/a1/OfflineAccentPlayer.kt').read_text()
assert 'OfflineAccentPlayer(context)' in v
for term in ['pronunciation/manifest.json','assets.openFd','MediaPlayer','prepareAsync','Başka aksan kullanılmadı']:
    assert term in a,term
assert 'import android.speech.tts.TextToSpeech' not in v and 'import android.speech.tts.TextToSpeech' not in a
assert 'private val store = context.getSharedPreferences("wordquest_preferences"' in (new/'app/src/main/java/com/kelimebulmacasi/a1/WordQuestPreferences.kt').read_text()
assert (new/'app/src/main/java/com/kelimebulmacasi/a1/TeacherStore.kt').exists()
print(f'PASS static: preserved {len(legacy_assets)} original puzzle, vocabulary and license assets + resources')
print('PASS static: separate bundled audio engine and existing v0.8.0 teacher/history source files')
print('NOT TESTED: Kotlin/Gradle compilation, app launch, device audio, AI service or installation')
