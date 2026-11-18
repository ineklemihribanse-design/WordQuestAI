package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.concurrent.CancellationException

internal enum class TeacherPersonality(val label: String, val prompt: String) {
    Kind("Sabırlı", "Be supportive and patient, celebrate small progress and speak gently."),
    Curious("Sohbetçi", "Be warm and curious, keep the exchange conversational and engaging."),
    Structured("Disiplinli", "Keep the lesson organised, give clear corrections and measurable goals."),
}

internal enum class TeacherMode(val label: String, val prompt: String) {
    Practice("Pratik", "Practise English interactively with one natural follow-up question."),
    Grammar("Dilbilgisi", "Explain the relevant grammar rule clearly in Turkish, then give three English examples and one short check question."),
    Lesson("Kişisel ders", "Teach a short, structured CEFR-appropriate lesson using the learner's weak vocabulary, with examples and a short exercise."),
    Correction("Hata düzelt", "Correct the learner's English. Show the corrected version, explain key changes in Turkish and give a similar practice sentence."),
}

class AiSession : ViewModel() {
    private var geminiKey by mutableStateOf("")
    private var credentials: UserAiCredentials? = null
    internal var provider by mutableStateOf(AiProvider.Gemini)
        private set
    internal var providerModel by mutableStateOf(AiProvider.Gemini.defaultModel)
        private set
    internal var avatar by mutableStateOf(TeacherAvatar.Mert)
        private set
    internal var personality by mutableStateOf(TeacherPersonality.Kind)
        private set
    private var keyChange = 0
    private var nextTurn = 0L
    private var store: TeacherStore? = null
    internal var boundProfile by mutableStateOf<String?>(null)
        private set
    internal val hasKey get() = geminiKey.isNotEmpty() || credentials?.hasKey(provider) == true

    internal var turns by mutableStateOf(emptyList<ConversationTurn>())
        private set
    internal var threads by mutableStateOf(emptyList<TeacherThread>())
        private set
    internal var threadId by mutableStateOf<String?>(null)
        private set
    internal var search by mutableStateOf("")
    internal var teacherNotes by mutableStateOf("")
        private set
    internal var mode by mutableStateOf(TeacherMode.Practice)
    internal var draft by mutableStateOf("")
    internal var language by mutableStateOf(ConversationLanguage.English)
    internal var level by mutableStateOf("A2")
    internal var autoRead by mutableStateOf(true)

    internal fun bindProfile(context: Context, profileKey: String) {
        if (boundProfile == profileKey) return
        store?.close()
        store = null
        boundProfile = null
        geminiKey = "" // Never share a transient API key across profiles.
        keyChange++
        credentials = null
        draft = ""
        turns = emptyList()
        threads = emptyList()
        threadId = null
        search = ""
        teacherNotes = ""
        mode = TeacherMode.Practice
        language = ConversationLanguage.English
        level = "A2"
        autoRead = true
        nextTurn = 0
        val next = TeacherStore(context, profileKey)
        val keys = UserAiCredentials(context, profileKey)
        credentials = keys
        provider = keys.selected()
        providerModel = keys.model(provider)
        store = next
        // Resolve the active avatar IN MEMORY ONLY. Never mutate profile
        // settings while app/session is binding; 1.0.34 may crash on launch.
        val priorTeacherWasEthan = next.setting("avatar") == TeacherAvatar.Mert.name
        avatar = TeacherAvatar.Mert
        personality = TeacherPersonality.entries.firstOrNull { it.name == next.setting("personality") }
            ?: TeacherPersonality.Kind
        teacherNotes = next.notes()
        // Existing 1.0.34 marker is read-only during startup. Old Emma/Sophia
        // conversations stay in SQLite but cannot masquerade as Ethan's memory.
        val savedEthanIds = next.setting("ethan_thread_ids").orEmpty()
        ethanThreadIds = when {
            savedEthanIds.isNotBlank() -> savedEthanIds.split('|')
                .filter { it.isNotBlank() && it != "-" }.toMutableSet()
            priorTeacherWasEthan -> next.threads().map { it.id }.toMutableSet()
            else -> mutableSetOf()
        }
        threads = next.threads().filter { it.id in ethanThreadIds }
        threads.firstOrNull()?.let { openThread(it.id) }
        boundProfile = profileKey
    }

    internal fun forgetKey() {
        credentials?.removeKey(provider)
        geminiKey = ""
        keyChange++
    }

    internal fun chooseProvider(value: AiProvider) {
        // Disabled provider cannot be re-selected from an older component.
        if (value == AiProvider.Claude) return
        credentials?.choose(value)
        provider = value
        providerModel = credentials?.model(value) ?: value.defaultModel
        geminiKey = ""
        keyChange++
    }

    internal fun setProviderModel(value: String) {
        credentials?.saveModel(provider, value)
        providerModel = value
    }

    internal fun saveProviderKey(value: String) {
        credentials?.saveKey(provider, value) ?: error("Öğretmen profili henüz yüklenmedi")
        geminiKey = ""
        keyChange++
    }

    private var ethanThreadIds: MutableSet<String> = mutableSetOf()

    private fun persistEthanThreadIds() {
        // Persist only AFTER a real user turn was successfully saved.
        store?.setSetting("ethan_thread_ids",
            ethanThreadIds.joinToString("|").ifEmpty { "-" })
    }

    /** Refresh main UI after the isolated 3D tutor saved a real AI turn.
     * Never write a profile setting while main activity resumes. */
    internal fun refreshExternalEthanThreads() {
        val current = store ?: return
        val saved = current.setting("ethan_thread_ids").orEmpty()
        if (saved.isNotBlank()) {
            ethanThreadIds = saved.split('|')
                .filter { it.isNotBlank() && it != "-" }.toMutableSet()
        }
        threads = current.threads(search).filter { it.id in ethanThreadIds }
        if (threadId == null && threads.isNotEmpty()) {
            openThread(threads.first().id)
        } else if (threadId != null && threadId in ethanThreadIds) {
            openThread(threadId!!)
        }
    }

    internal fun chooseAvatar(value: TeacherAvatar) {
        if (value != TeacherAvatar.Mert) return
        avatar = value
        store?.setSetting("avatar", value.name)
    }

    internal fun choosePersonality(value: TeacherPersonality) {
        personality = value
        store?.setSetting("personality", value.name)
    }

    internal fun rememberUserPreference(message: String) {
        val note = Regex("""^(?:bunu hatırla|hatırla|remember)\s*[:：]\s*(.{3,260})$""", RegexOption.IGNORE_CASE)
            .matchEntire(message.trim())?.groupValues?.getOrNull(1) ?: return
        val updated = (teacherNotes + "\n" + note).trim().takeLast(1500)
        saveTeacherNotes(updated)
    }

    internal fun newConversation() {
        threadId = null
        turns = emptyList()
        draft = ""
    }

    internal fun openThread(id: String) {
        if (id !in ethanThreadIds) return
        val history = store ?: return
        val metadata = history.threads().firstOrNull { it.id == id } ?: return
        val loaded = history.turns(id)
        language = ConversationLanguage.entries.firstOrNull { it.name == metadata.language }
            ?: ConversationLanguage.English
        level = metadata.level.takeIf { it in conversationLevels } ?: "A2"
        threadId = id
        turns = loaded
        nextTurn = maxOf(nextTurn, loaded.maxOfOrNull { it.id } ?: 0L)
        draft = ""
    }

    internal fun deleteThread(id: String) {
        if (id !in ethanThreadIds) return
        store?.delete(id)
        ethanThreadIds.remove(id)
        persistEthanThreadIds()
        if (threadId == id) newConversation()
        refreshThreads()
    }

    internal fun searchThreads(term: String) {
        search = term.take(120)
        refreshThreads()
    }

    internal fun saveTeacherNotes(text: String) {
        teacherNotes = text.take(1_500)
        store?.setNotes(teacherNotes)
    }

    private fun refreshThreads() {
        threads = (store?.threads(search) ?: emptyList())
            .filter { it.id in ethanThreadIds }
    }

    internal suspend fun suggestWords(key: String, topic: String, level: String, count: Int, used: Set<String>): List<WordPair> =
        withProviderKey(key) { requestOnlineWords(it, topic, level, count, used, provider, providerModel) }

    internal suspend fun reply(key: String, message: String, learningContext: String = "", learnerName: String = ""): ConversationTurn {
        val owner = boundProfile ?: error("Öğretmen profili hazır değil")
        val activeStore = store ?: error("Öğretmen geçmişi açılamadı")
        val activeThread = threadId
        val selectedAvatar = avatar
        val selectedPersonality = personality
        val selectedProvider = provider
        val selectedModel = providerModel
        val answer = withProviderKey(key) {
            requestConversationReply(it, nextTurn + 1, turns, message, language, level, mode, teacherNotes, learningContext,
                learnerName, selectedProvider, selectedModel, selectedAvatar.prompt + " " + selectedPersonality.prompt + (if (selectedAvatar == TeacherAvatar.Mert) " " + Teacher2DPersona.systemPrompt else ""))
        }
        if (owner != boundProfile || activeStore !== store || activeThread != threadId) {
            throw CancellationException("Öğretmen profili veya sohbet değişti")
        }
        // Persist before updating the visible conversation: no silent in-memory-only success.
        val savedId = activeStore.append(activeThread, answer, level)
        if (ethanThreadIds.add(savedId)) persistEthanThreadIds()
        nextTurn = answer.id
        threadId = savedId
        turns = turns + answer
        rememberUserPreference(message)
        refreshThreads()
        return answer
    }

    /** On-demand study-card image. Existing encrypted Gemini credentials only. */
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

    private suspend fun <T> withProviderKey(input: String, request: suspend (String) -> T): T {
        val activeKey = validatePersonalApiKey(provider, input.ifBlank { credentials?.key(provider) ?: geminiKey })
        val keyVersion = keyChange
        val response = try {
            request(activeKey)
        } catch (problem: WordRequestException) {
            // Keep the stored key until the user explicitly changes or removes it.
            if (problem.invalidKey && keyVersion == keyChange) geminiKey = ""
            throw problem
        }
        // One-time keys entered in a chat are not silently persisted.
        if (keyVersion != keyChange) throw CancellationException("AI sağlayıcısı veya profil değişti")
        return response
    }

    override fun onCleared() {
        store?.close()
        store = null
        super.onCleared()
    }
}
