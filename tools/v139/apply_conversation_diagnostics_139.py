#!/usr/bin/env python3
"""v1.0.39 QA: diagnose conversation failures rather than masking every failure as internet loss.

Apply AFTER v1.0.38, keeping all Gemini/OpenAI keys/models and Ethan renderer
unchanged. No API credentials or request/response bodies are written to logs.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[2]
app = root / "source/app"
pkg = app / "src/main/java/com/kelimebulmacasi/a1"
tests = app / "src/test/java/com/kelimebulmacasi/a1"

def once(contents, before, after, label):
    count = contents.count(before)
    assert count == 1, f"{label}: expected 1 exact anchor, found {count}"
    return contents.replace(before, after, 1)

gradle = app / "build.gradle.kts"
g = gradle.read_text("utf-8")
assert g.count("versionCode = 63") == 1
assert g.count('versionName = "1.0.38-gemini-openai-only-qa"') == 1
assert 'applicationIdSuffix = ".offline101"' in g

diagnostics = pkg / "ConversationFailureDiagnostics.kt"
assert not diagnostics.exists(), "Diagnostic module already exists"
diagnostics.write_text("""package com.kelimebulmacasi.a1

/** Safe, actionable errors; never interpolate exception messages that may contain private data.
 * WordRequestException is already shown verbatim in the callers, so this handles unexpected
 * failures instead of pretending every exception is a broken internet connection.
 */
internal fun conversationFailureMessage(problem: Throwable): String = when (problem) {
    is java.net.UnknownHostException ->
        "AI sunucusunun adresi çözümlenemedi (DNS). Bağlantı ve DNS ayarlarını kontrol et."
    is java.net.SocketTimeoutException ->
        "AI yanıtı zaman aşımına uğradı. Biraz sonra yeniden dene."
    is java.net.ConnectException ->
        "AI sunucusuna bağlantı açılamadı. Ağ erişimini kontrol et."
    is javax.net.ssl.SSLException ->
        "AI sunucusuyla güvenli TLS bağlantısı kurulamadı. Cihazın tarihini ve ağını kontrol et."
    is java.io.IOException ->
        "AI ağ isteği tamamlanamadı (" + problem.javaClass.simpleName + "). Daha sonra yeniden dene."
    is org.json.JSONException ->
        "AI yanıtı okunamadı (JSON). Sağlayıcı yanıt biçimi kontrol edilmeli."
    is android.database.SQLException ->
        "Sohbet veritabanı işlemi başarısız oldu. Verileri silmeden uygulama hatasını bildir."
    is SecurityException ->
        "AI bağlantısı için bir cihaz veya ağ izni engellendi."
    is IllegalArgumentException ->
        "Konuşma isteği hazırlanırken geçersiz bir değer bulundu. Model ve sohbet ayarlarını kontrol et."
    else ->
        "Konuşma Asistanı hatası (" + problem.javaClass.simpleName + "). Hata türünü bildir; API anahtarını paylaşma."
}
""", encoding="utf-8")

live = pkg / "LiveTeacherScreen.kt"
t = live.read_text("utf-8")
t = once(t, '''            } catch (_: Exception) {
                ai.draft = message
                error = "Bağlantı kurulamadı. İnternetini ve seçili AI sağlayıcısını kontrol et."
''',
'''            } catch (problem: Exception) {
                ai.draft = message
                error = conversationFailureMessage(problem)
''', "Live Ethan generic error")
# Learning evidence is optional: errors retrieving it should not block basic conversation.
t = once(t, '''                val turn = ai.reply(key, message, session.teacherLearningContext(), session.profileName)''', '''                val optionalContext = try { session.teacherLearningContext() } catch (_: Exception) { "" }
                val turn = ai.reply(key, message, optionalContext, session.profileName)''', "Live optional lesson context")
live.write_text(t, encoding="utf-8")

conversation = pkg / "ConversationScreen.kt"
c = conversation.read_text("utf-8")
c = once(c, '''            } catch (_: Exception) {
                error = "Yanıt alınamadı. İnternet bağlantını kontrol edip tekrar gönder."
''',
'''            } catch (problem: Exception) {
                error = conversationFailureMessage(problem)
''', "Text conversation generic error")
c = once(c, '''                val turn = ai.reply(key, message, session.teacherLearningContext(), session.profileName)''', '''                val optionalContext = try { session.teacherLearningContext() } catch (_: Exception) { "" }
                val turn = ai.reply(key, message, optionalContext, session.profileName)''', "Text chat optional lesson context")
conversation.write_text(c, encoding="utf-8")

remote = pkg / "EthanIsolatedVoiceTeacherActivity.kt"
r = remote.read_text("utf-8")
r = once(r, '''                } catch (e: Exception) {
                    error = "AI yanıtı alınamadı (" + e.javaClass.simpleName + ")."
''',
'''                } catch (e: Exception) {
                    error = conversationFailureMessage(e)
''', "Isolated 3D Ethan generic error")
remote.write_text(r, encoding="utf-8")

tests.mkdir(parents=True, exist_ok=True)
test_file = tests / "ConversationFailureDiagnosticsTest.kt"
assert not test_file.exists(), "Tests already exist"
test_file.write_text("""package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class ConversationFailureDiagnosticsTest {
    @Test fun distinguishDnsFromOtherConnectionFailures() {
        val msg = conversationFailureMessage(java.net.UnknownHostException("private-token-marker"))
        assertTrue(msg.contains("DNS"))
        assertFalse(msg.contains("private-token-marker"))
        assertTrue(conversationFailureMessage(java.net.ConnectException()).contains("sunucusuna"))
    }

    @Test fun distinguishTransportAndUnexpectedErrorsWithoutSecrets() {
        assertTrue(conversationFailureMessage(java.net.SocketTimeoutException()).contains("zaman aşımı"))
        val failure = conversationFailureMessage(IllegalStateException("MY-SECRET-API-KEY"))
        assertTrue(failure.contains("IllegalStateException"))
        assertFalse(failure.contains("MY-SECRET-API-KEY"))
    }
}
""", encoding="utf-8")

g = g.replace("versionCode = 63", "versionCode = 64", 1).replace(
    'versionName = "1.0.38-gemini-openai-only-qa"',
    'versionName = "1.0.39-conversation-error-diagnostics-qa"', 1)
gradle.write_text(g, encoding="utf-8")
print("1.0.39 QA: masked conversation errors now reveal sanitized failure categories")
print("Gemini/OpenAI credentials, model selection, provider request payloads and signing unchanged")
print("No proven live API fix: end-user device diagnostic and CI test still mandatory")
