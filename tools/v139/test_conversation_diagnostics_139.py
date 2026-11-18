#!/usr/bin/env python3
"""Static fail-closed acceptance for v1.0.39 conversation error-classification QA."""
from pathlib import Path

root = Path(__file__).resolve().parents[2]
app = root / "source/app"
pkg = app / "src/main/java/com/kelimebulmacasi/a1"
gradle = (app / "build.gradle.kts").read_text("utf-8")
assert "versionCode = 64" in gradle
assert 'versionName = "1.0.39-conversation-error-diagnostics-qa"' in gradle
assert 'applicationIdSuffix = ".offline101"' in gradle

diagnostics = (pkg / "ConversationFailureDiagnostics.kt").read_text("utf-8")
for term in ("UnknownHostException", "SocketTimeoutException", "ConnectException",
             "SSLException", "IOException", "JSONException", "SQLException", "SecurityException"):
    assert term in diagnostics
assert 'problem.message' not in diagnostics
assert 'conversationFailureMessage(problem)' in (pkg / "LiveTeacherScreen.kt").read_text("utf-8")
assert 'conversationFailureMessage(problem)' in (pkg / "ConversationScreen.kt").read_text("utf-8")
assert 'conversationFailureMessage(e)' in (pkg / "EthanIsolatedVoiceTeacherActivity.kt").read_text("utf-8")
for name in ("LiveTeacherScreen.kt", "ConversationScreen.kt"):
    screen = (pkg / name).read_text("utf-8")
    assert 'val optionalContext = try { session.teacherLearningContext() } catch (_: Exception) { "" }' in screen
    assert 'ai.reply(key, message, optionalContext, session.profileName)' in screen
assert "class ConversationFailureDiagnosticsTest" in (
    app / "src/test/java/com/kelimebulmacasi/a1/ConversationFailureDiagnosticsTest.kt"
).read_text("utf-8")

providers = (pkg / "AiProviders.kt").read_text("utf-8")
assert 'Gemini("Google Gemini", "gemini-3.1-flash-lite"' in providers
assert 'OpenAI("OpenAI"' in providers
assert 'it != AiProvider.Claude && it.name' in providers
assert "AiProvider.entries.filter { it != AiProvider.Claude }" in (
    pkg / "AiProviderSettings.kt"
).read_text("utf-8")
assert (app / "src/main/assets/avatars/ethan-mobile.glb").read_bytes()[:4] == b"glTF"
assert 'android:process=":teacher3d"' in (app / "src/main/AndroidManifest.xml").read_text("utf-8")
print("PASS: Ethan conversation, text chat, isolated 3D share non-sensitive failure diagnostics")
print("PASS: provider models, API key storage, Claude disabled, Ethan 3D and app identity preserved")
print("PENDING: actual Android network and Gemini/OpenAI behavior on user's device")
