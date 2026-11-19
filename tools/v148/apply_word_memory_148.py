#!/usr/bin/env python3
"""Overlay the approved neon Word Memory redesign after v147, without touching learning DB."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/"source"
KOTLIN=SRC/"app/src/main/java/com/kelimebulmacasi/a1"
GRADLE=SRC/"app/build.gradle.kts"
PREVIOUS='versionName = "1.0.47-vocabulary-2p5d-compat"'
CURRENT='versionName = "1.0.48-neon-word-memory"'

def once(text,from_text,to_text,label):
    found=text.count(from_text)
    if found!=1:
        raise RuntimeError(f"Cannot safely patch {label}: expected 1 anchor, got {found}")
    return text.replace(from_text,to_text,1)

def main():
    if not (SRC/"settings.gradle.kts").is_file():
        raise RuntimeError("Base Android sources have not been assembled.")
    version=GRADLE.read_text("utf-8")
    if version.count("versionCode = 72")!=1 or version.count(PREVIOUS)!=1:
        raise RuntimeError("v148 expects v147 compatible APK source, no downgrade allowed.")
    if 'applicationIdSuffix = ".offline101"' not in version:
        raise RuntimeError("App ID changed; refusing to change the installed app.")
    for name in ("VocabularyNeonHome.kt","VocabularyNeonPractice.kt","VocabularyCardImage.kt"):
        source=ROOT/"tools/v148"/name
        destination=KOTLIN/name
        destination.write_text(source.read_text("utf-8"),encoding="utf-8")
    studio=KOTLIN/"LearningStudioScreen.kt"
    text=studio.read_text("utf-8")
    opening='@Composable\ninternal fun LearningScreen(session: AppSession, pronunciation: WordPronunciation, back: () -> Unit) {'
    closing='@Composable\nprivate fun LearningFlashcardStudio('
    if text.count(opening)!=1 or text.count(closing)!=1:
        raise RuntimeError("Unexpected original learning-screen anchors.")
    before,rest=text.split(opening,1)
    unused,after=rest.split(closing,1)
    replacement=(
        '@Composable\n'
        'internal fun LearningScreen(session: AppSession, pronunciation: WordPronunciation, back: () -> Unit) {\n'
        '    VocabularyNeonHome(session, pronunciation, back)\n'
        '}\n\n'
    )
    text=before+replacement+closing+after
    studio.write_text(text,encoding="utf-8")
    # Reuse the existing profile-encrypted AI key; do not log or copy it to plain preferences.
    ai=KOTLIN/"AiSession.kt"
    text=ai.read_text("utf-8")
    anchor='    private suspend fun <T> withProviderKey(input: String, request: suspend (String) -> T): T {'
    method='''    /** On-demand study-card image. Existing encrypted Gemini credentials only. */
    internal suspend fun generateVocabularyVisual(word: String, clue: String): ByteArray {
        if (provider != AiProvider.Gemini) {
            throw IllegalStateException("Görsel üretimi için Ayarlar'dan Gemini seç.")
        }
        if (boundProfile == null) {
            throw IllegalStateException("Önce öğrenci profilini aç.")
        }
        return withProviderKey("") { key ->
            VocabularyGeminiImageService.generate(key, word, clue)
        }
    }

'''
    text=once(text,anchor,method+anchor,"AiSession encrypted key hook")
    ai.write_text(text,encoding="utf-8")
    if "lifecycle-viewmodel-compose:" not in version:
        version=once(version,"dependencies {",
            'dependencies {\n    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")',
            "Compose ViewModel dependency")
    version=once(version,"versionCode = 72","versionCode = 73","Gradle version")
    version=once(version,PREVIOUS,CURRENT,"Gradle versionName")
    GRADLE.write_text(version,encoding="utf-8")
    print("v148 installed: neon Word Memory, per-profile selected robot, safe archives")
    print("v148 AI: opt-in Gemini images via encrypted user key, cache-only local image bytes")
    print("v148 preserved: learning DB/folders/history, package signature, 2.5D teacher, saved credentials")
if __name__=="__main__": main()
