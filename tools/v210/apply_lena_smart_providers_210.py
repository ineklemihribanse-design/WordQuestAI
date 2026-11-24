#!/usr/bin/env python3
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app/src"
PKG=APP/"main/java/com/kelimebulmacasi/a1"
TEST=APP/"test/java/com/kelimebulmacasi/a1"
def once(s,old,new):
    assert s.count(old)==1,(old,s.count(old))
    return s.replace(old,new,1)
p=PKG/"AiProviders.kt"
s=p.read_text("utf-8")
s=once(s,'        val encoded = prefs.getString(field(provider, "key"), null) ?: return ""',
'''        return readLenaEncrypted(field(provider, "key"))
    }

    private fun readLenaEncrypted(storageField: String): String {
        val encoded = prefs.getString(storageField, null) ?: return ""''')
s=once(s,'        val value = validatePersonalApiKey(provider, raw)\n        val cipher',
'''        val value = validatePersonalApiKey(provider, raw)
        writeLenaEncrypted(field(provider, "key"), value)
    }

    private fun writeLenaEncrypted(storageField: String, value: String) {
        val cipher''')
s=once(s,'prefs.edit().putString(field(provider, "key"), Base64.encodeToString(saved, Base64.NO_WRAP)).apply()',
'prefs.edit().putString(storageField, Base64.encodeToString(saved, Base64.NO_WRAP)).apply()')
s=once(s,'    fun removeKey(provider: AiProvider) {','''    fun lenaAuto(): Boolean = if (useMainProcessBridge) mainSnapshot?.getBoolean("lena_auto") == true
        else prefs.getBoolean("$userPrefix.lena_auto", false)
    fun setLenaAuto(enabled: Boolean) {
        check(!useMainProcessBridge)
        prefs.edit().putBoolean("$userPrefix.lena_auto", enabled).apply()
    }
    private fun lenaField(service: Wq210ExtraKey) = "$userPrefix.lena_extra.${service.name}"
    fun lenaExtra(service: Wq210ExtraKey): String = if (useMainProcessBridge)
        mainSnapshot?.getString("lena_extra_${service.name}").orEmpty()
        else readLenaEncrypted(lenaField(service))
    fun lenaGemini(): String = if (useMainProcessBridge) mainSnapshot?.getString("lena_gemini").orEmpty()
        else key(AiProvider.Gemini)
    fun saveLenaExtra(service: Wq210ExtraKey, raw: String) {
        check(!useMainProcessBridge)
        val value = validatePersonalApiKey(AiProvider.OpenAI, raw)
        require(value.startsWith(service.prefix))
        writeLenaEncrypted(lenaField(service), value)
    }
    fun removeLenaExtra(service: Wq210ExtraKey) {
        check(!useMainProcessBridge)
        prefs.edit().remove(lenaField(service)).apply()
    }

    fun removeKey(provider: AiProvider) {''')
p.write_text(s,"utf-8")
p=PKG/"TeacherCredentialBridgeProvider.kt"
s=p.read_text("utf-8")
s=once(s,'        return result','''        result.putBoolean("lena_auto", keys.lenaAuto())
        if (keys.lenaAuto()) {
            result.putString("lena_gemini", keys.lenaGemini())
            for (service in Wq210ExtraKey.entries)
                result.putString("lena_extra_${service.name}", keys.lenaExtra(service))
        }
        return result''')
p.write_text(s,"utf-8")
p=PKG/"AiSession.kt"
s=p.read_text("utf-8")
s=once(s,'    private var credentials: UserAiCredentials? = null','''    private var credentials: UserAiCredentials? = null
    private var lenaRevision by mutableStateOf(0)
    private val lenaRouter = Wq210LenaRouter()
    internal val lenaAutoEnabled get() = lenaRevision.let { credentials?.lenaAuto() == true }
    internal val lenaGeminiAvailable get() = lenaRevision.let { credentials?.lenaGemini()?.isNotBlank() == true }
    internal fun lenaExtraAvailable(service: Wq210ExtraKey) = lenaRevision.let { credentials?.lenaExtra(service)?.isNotBlank() == true }
    internal val lenaHasKey get() = if (lenaAutoEnabled)
        lenaGeminiAvailable || Wq210ExtraKey.entries.any { lenaExtraAvailable(it) } else hasKey
    internal fun setLenaAuto(enabled: Boolean) {
        (credentials ?: error("Profil hazır değil")).setLenaAuto(enabled)
        lenaRevision++; keyChange++; lenaRouter.reset()
    }
    internal fun saveLenaExtra(service: Wq210ExtraKey, raw: String) {
        (credentials ?: error("Profil hazır değil")).saveLenaExtra(service, raw)
        lenaRevision++; keyChange++; lenaRouter.reset()
    }
    internal fun removeLenaExtra(service: Wq210ExtraKey) {
        (credentials ?: error("Profil hazır değil")).removeLenaExtra(service)
        lenaRevision++; keyChange++; lenaRouter.reset()
    }''')
s=once(s,'        credentials = null','        credentials = null\n        lenaRevision++; lenaRouter.reset()')
s=once(s,'        credentials = keys','        credentials = keys\n        lenaRevision++')
s=once(s,'        credentials?.removeKey(provider)','        credentials?.removeKey(provider)\n        lenaRevision++; lenaRouter.reset()')
s=once(s,'        credentials?.saveKey(provider, value) ?: error("Öğretmen profili henüz yüklenmedi")','        credentials?.saveKey(provider, value) ?: error("Öğretmen profili henüz yüklenmedi")\n        lenaRevision++; lenaRouter.reset()')
s=once(s,'        val answer = withProviderKey(key) {','''        val lenaKeyVersion = keyChange
        val answer = if (personaOverride?.contains("Lena", ignoreCase = true) == true && lenaAutoEnabled) {
            val keys = credentials ?: error("Profil hazır değil")
            val routes = mapOf(
                Wq210LenaService.Groq to keys.lenaExtra(Wq210ExtraKey.Groq),
                Wq210LenaService.Gemini to (if (selectedProvider == AiProvider.Gemini && key.isNotBlank()) key else keys.lenaGemini()),
                Wq210LenaService.OpenRouter to keys.lenaExtra(Wq210ExtraKey.OpenRouter)
            )
            val id = nextTurn + 1
            val lang = language
            val request = wq209LenaTextHistory(conversationRequest(turns, message, lang, level, mode,
                teacherNotes, learningContext, learnerName, personaOverride + " " + selectedPersonality.prompt), turns)
            lenaRouter.run(routes) { service, secret ->
                val response = wq207LenaRequest(6_500L) { wq210LenaHttp(service, secret, request) }
                readConversationReply(response, id, message, lang)
            }
        } else withProviderKey(key) {''')
s=once(s,'        if (owner != boundProfile || activeStore !== store || activeThread != threadId) {','        if (lenaKeyVersion != keyChange || owner != boundProfile || activeStore !== store || activeThread != threadId) {')
p.write_text(s,"utf-8")
p=PKG/"AiProviderSettings.kt"
s=p.read_text("utf-8")
s=once(s,'        Text("Yapay zekâ sağlayıcısı",','        Wq210LenaProviderSettings(ai)\n        Text("Yapay zekâ sağlayıcısı",')
p.write_text(s,"utf-8")
for name in ("ConversationScreen.kt","LenaIsolatedVoiceTeacherActivity.kt"):
    p=PKG/name
    s=p.read_text("utf-8")
    assert "ai.hasKey" in s,name
    s=s.replace("ai.hasKey","ai.lenaHasKey")
    p.write_text(s,"utf-8")
# Only change LiveTeacherScreen if it contains actual Lena persona; legacy Alex screens remain unchanged.
p=PKG/"LiveTeacherScreen.kt"
s=p.read_text("utf-8")
if 'personaOverride = "You are Lena' in s:
    s=s.replace("ai.hasKey","ai.lenaHasKey")
    p.write_text(s,"utf-8")
for name in ("Wq210LenaRouter.kt","Wq210LenaProviderSettings.kt"):
    shutil.copyfile(ROOT/"tools/v210"/name,PKG/name)
shutil.copyfile(ROOT/"tools/v210/Wq210LenaRouterTest.kt",TEST/"Wq210LenaRouterTest.kt")
print("PASS: optional Lena-only provider routing, encrypted separate keys, same-UID bridge, no paid OpenAI auto routing")
