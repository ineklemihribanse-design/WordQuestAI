#!/usr/bin/env python3
"""Repair saved API-key access for Lena's separate :teacher3d Android process.

The encrypted preferences remain owned by the primary process. A nonexported
same-UID provider serves only the selected user's provider/model/key on demand.
Existing profile data, Alex, Arena, Word Memory and application ID are untouched.
"""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "source/app"
SRC = APP / "src/main/java/com/kelimebulmacasi/a1"
TEST = APP / "src/test/java/com/kelimebulmacasi/a1"
GRADLE = APP / "build.gradle.kts"
MANIFEST = APP / "src/main/AndroidManifest.xml"
PATCH = ROOT / "tools/v159"

def once(text, before, after, label):
    count = text.count(before)
    if count != 1:
        raise AssertionError(f"{label}: expected exactly 1 match, got {count}")
    return text.replace(before, after, 1)

def main():
    gradle = GRADLE.read_text("utf-8")
    assert gradle.count("versionCode = 83") == 1
    assert gradle.count('versionName = "1.0.58-lena-voice"') == 1
    assert 'applicationIdSuffix = ".fresh"' in gradle
    assert (SRC / "LenaIsolatedVoiceTeacherActivity.kt").is_file()
    for filename in ("Teacher2DPortrait.kt", "ArenaApprovedHome.kt", "VocabularyNeonHome.kt"):
        assert (SRC / filename).is_file(), filename

    shutil.copyfile(PATCH / "TeacherCredentialBridgeProvider.kt",
                    SRC / "TeacherCredentialBridgeProvider.kt")
    TEST.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(PATCH / "TeacherCredentialBridgeRulesTest.kt",
                    TEST / "TeacherCredentialBridgeRulesTest.kt")

    file = SRC / "AiProviders.kt"
    text = file.read_text("utf-8")
    text = once(text,
        "internal class UserAiCredentials(context: Context, profile: String) {",
        """internal class UserAiCredentials(
    context: Context, profile: String, private val useMainProcessBridge: Boolean = false,
) {
    private val appContext = context.applicationContext
    private val bridgeProfile = profile
    // Cross-process preferences are unsafe: take a single, read-only IPC snapshot.
    private val mainSnapshot: android.os.Bundle? by lazy {
        if (useMainProcessBridge)
            TeacherCredentialBridgeProvider.snapshot(appContext, bridgeProfile)
        else null
    }""", "credential constructor and bridge snapshot")
    text = once(text,
        """    fun selected(): AiProvider = AiProvider.entries.firstOrNull {
        // Historical Claude choices are left encrypted on disk, never made active.
        it != AiProvider.Claude && it.name == prefs.getString("$userPrefix.selected", null)
    } ?: AiProvider.Gemini""",
        """    fun selected(): AiProvider {
        if (useMainProcessBridge)
            return TeacherCredentialBridgeRules.selected(mainSnapshot?.getString("provider"))
        return AiProvider.entries.firstOrNull {
            // Historical Claude choices are left encrypted on disk, never made active.
            it != AiProvider.Claude && it.name == prefs.getString("$userPrefix.selected", null)
        } ?: AiProvider.Gemini
    }""", "provider in main process")
    text = once(text,
        """    fun model(provider: AiProvider): String = prefs.getString(field(provider, "model"), null)
        ?.takeIf { aiModelPattern.matches(it) } ?: provider.defaultModel""",
        """    fun model(provider: AiProvider): String {
        if (useMainProcessBridge) {
            val value = if (mainSnapshot?.getString("provider") == provider.name)
                mainSnapshot?.getString("model") else null
            return TeacherCredentialBridgeRules.model(value, provider)
        }
        return prefs.getString(field(provider, "model"), null)
            ?.takeIf { aiModelPattern.matches(it) } ?: provider.defaultModel
    }""", "provider model in main process")
    text = once(text,
        '    fun key(provider: AiProvider): String {\n',
        """    fun key(provider: AiProvider): String {
        if (useMainProcessBridge) return TeacherCredentialBridgeRules.key(
            mainSnapshot?.getString("provider"), provider, mainSnapshot?.getString("key"))
""", "provider key in main process")
    for name, args in (("choose", "provider: AiProvider"),
                       ("saveModel", "provider: AiProvider, model: String"),
                       ("saveKey", "provider: AiProvider, raw: String"),
                       ("removeKey", "provider: AiProvider")):
        text = once(text, f"    fun {name}({args}) {{",
                    f'    fun {name}({args}) {{\n'
                    f'        check(!useMainProcessBridge) {{ "Credential changes require primary process" }}',
                    "disable writes from Lena: " + name)
    file.write_text(text, "utf-8")

    file = SRC / "AiSession.kt"
    text = file.read_text("utf-8")
    text = once(text,
        "internal fun bindProfile(context: Context, profileKey: String) {",
        """internal fun bindProfile(
        context: Context, profileKey: String, useMainProcessBridge: Boolean = false
    ) {""", "bind secondary-process profile")
    text = once(text,
        "val keys = UserAiCredentials(context, profileKey)",
        "val keys = UserAiCredentials(context, profileKey, useMainProcessBridge)",
        "use shared provider/model/key snapshot")
    file.write_text(text, "utf-8")

    file = SRC / "LenaIsolatedVoiceTeacherActivity.kt"
    text = file.read_text("utf-8")
    text = once(text,
        "ai.bindProfile(applicationContext, profile)",
        "ai.bindProfile(applicationContext, profile, useMainProcessBridge = true)",
        "Lena chooses the primary-process credential bridge")
    text = once(text,
        'error = "AI anahtarını ana uygulamanın Ayarlar bölümünden kaydedin."',
        'error = "Kayıtlı AI anahtarına ulaşılamadı. Lena penceresini yeniden aç veya ana uygulamadaki sağlayıcıyı kontrol et."',
        "show accurate saved-credential status")
    file.write_text(text, "utf-8")

    text = MANIFEST.read_text("utf-8")
    assert "teacher_credentials" not in text
    text = once(text, "</application>",
        '''    <!-- Internal main-process provider; not accessible from other apps. -->
        <provider
            android:name=".TeacherCredentialBridgeProvider"
            android:authorities="${applicationId}.teacher_credentials"
            android:exported="false" />
    </application>''', "nonexported same-app provider")
    MANIFEST.write_text(text, "utf-8")

    gradle = once(gradle, "versionCode = 83", "versionCode = 84",
                  "increment version code")
    gradle = once(gradle, 'versionName = "1.0.58-lena-voice"',
                  'versionName = "1.0.59-lena-saved-key"', "upgrade version name")
    GRADLE.write_text(gradle, "utf-8")

    assert 'applicationIdSuffix = ".fresh"' in gradle
    assert 'android:exported="false"' in MANIFEST.read_text()
    assert 'useMainProcessBridge = true' in (SRC / "LenaIsolatedVoiceTeacherActivity.kt").read_text()
    print("v1.0.59: saved Gemini/OpenAI key and selected provider available to Lena via internal IPC.")
    print("Alex/Arena/Word Memory/data/signing preserved; no key in Intents, logs or GitHub.")

if __name__ == "__main__":
    main()
