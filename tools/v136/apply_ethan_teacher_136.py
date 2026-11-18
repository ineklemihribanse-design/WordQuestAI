#!/usr/bin/env python3
"""v1.0.36: Ethan as real Gemini/AI conversation teacher, launch-safe.

Build after the verified v1.0.35 code60 recovery overlay. The v1.0.34
experimental failure must NOT be reintroduced: this script does NOT install
EthanLive3DStage in the main process, nor write new profile settings while
binding a profile at app start. True human 3D remains in :teacher3d isolated
process. Chat/TTS/mic remain in the existing main conversation screen.

Emma and Sophia assets are later archived by the existing vetted
v134/archive_inactive_teachers_134.py step; never deleted from source recipes.
"""
from pathlib import Path

root=Path(__file__).resolve().parents[2]
app=root/"source/app"
src=app/"src/main/java/com/kelimebulmacasi/a1"
tests=app/"src/test/java/com/kelimebulmacasi/a1"
gradle=app/"build.gradle.kts"

def once(text, old, new, what):
    assert text.count(old)==1, f"{what}: anchor count={text.count(old)}"
    return text.replace(old,new,1)

g=gradle.read_text("utf-8")
assert g.count("versionCode = 60")==1
assert g.count('versionName = "1.0.35-launch-recovery-qa"')==1
assert 'applicationIdSuffix = ".offline101"' in g
assert (src/"Teacher3DProbeActivity.kt").is_file()
assert (app/"src/main/assets/avatars/ethan-mobile.glb").is_file()
assert not (src/"EthanLive3DStage.kt").exists(), "No in-process native 3D"

# AI prompt must be complete, not truncated to the old 250 characters.
conversation=src/"Conversation.kt"
c=conversation.read_text("utf-8")
c=once(c,"avatarPrompt.take(250)","avatarPrompt.take(6_000)","persona cap")
conversation.write_text(c,"utf-8")
(src/"EthanTeacherPersona.kt").write_text(
    (root/"tools/v134/EthanTeacherPersona.kt").read_text("utf-8"),"utf-8")
tests.mkdir(parents=True,exist_ok=True)
(tests/"EthanTeacherPersonaTest.kt").write_text(
    (root/"tools/v134/EthanTeacherPersonaTest.kt").read_text("utf-8"),"utf-8")

ai=src/"AiSession.kt"
s=ai.read_text("utf-8")
assert "ethan_thread_ids" not in s
s=once(s,
    'selectedAvatar.prompt + " " + selectedPersonality.prompt',
    'selectedAvatar.prompt + " " + selectedPersonality.prompt + ' +
    '(if (selectedAvatar == TeacherAvatar.Mert) " " + EthanTeacherPersona.systemPrompt else "")',
    "provider AI persona")
s=once(s,
    'internal var avatar by mutableStateOf(TeacherAvatar.Elif)',
    'internal var avatar by mutableStateOf(TeacherAvatar.Mert)',
    "default teacher")
s=once(s,
    'avatar = TeacherAvatar.entries.firstOrNull { it.name == next.setting("avatar") } ?: TeacherAvatar.Elif',
    '''// Resolve the active avatar IN MEMORY ONLY. Never mutate profile
        // settings while app/session is binding; 1.0.34 may crash on launch.
        val priorTeacherWasEthan = next.setting("avatar") == TeacherAvatar.Mert.name
        avatar = TeacherAvatar.Mert''',
    "profile teacher binding")
s=once(s,
    '''        teacherNotes = next.notes()
        threads = next.threads()
        threads.firstOrNull()?.let { openThread(it.id) }''',
    '''        teacherNotes = next.notes()
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
        threads.firstOrNull()?.let { openThread(it.id) }''',
    "read only bind history isolation")
s=once(s,
    '''internal fun chooseAvatar(value: TeacherAvatar) {
        avatar = value''',
    '''private var ethanThreadIds: MutableSet<String> = mutableSetOf()

    private fun persistEthanThreadIds() {
        // Persist only AFTER a real user turn was successfully saved.
        store?.setSetting("ethan_thread_ids",
            ethanThreadIds.joinToString("|").ifEmpty { "-" })
    }

    internal fun chooseAvatar(value: TeacherAvatar) {
        if (value != TeacherAvatar.Mert) return
        avatar = value''',
    "Ethan active selector")
s=once(s,
    '''    internal fun openThread(id: String) {
        val history = store ?: return''',
    '''    internal fun openThread(id: String) {
        if (id !in ethanThreadIds) return
        val history = store ?: return''',
    "protect archived chat")
s=once(s,
    '''    internal fun deleteThread(id: String) {
        store?.delete(id)''',
    '''    internal fun deleteThread(id: String) {
        if (id !in ethanThreadIds) return
        store?.delete(id)
        ethanThreadIds.remove(id)
        persistEthanThreadIds()''',
    "protect archived chat delete")
s=once(s,
    '''    private fun refreshThreads() {
        threads = store?.threads(search) ?: emptyList()''',
    '''    private fun refreshThreads() {
        threads = (store?.threads(search) ?: emptyList())
            .filter { it.id in ethanThreadIds }''',
    "only Ethan listed")
s=once(s,
    '''        val savedId = activeStore.append(activeThread, answer, level)
        nextTurn = answer.id''',
    '''        val savedId = activeStore.append(activeThread, answer, level)
        if (ethanThreadIds.add(savedId)) persistEthanThreadIds()
        nextTurn = answer.id''',
    "persist after successful turn only")
assert 'next.setSetting("ethan_thread_ids"' not in s
ai.write_text(s,"utf-8")

# Keep existing voice recognition/TTS UI and show just Ethan as chosen teacher.
live=src/"LiveTeacherScreen.kt"
t=live.read_text("utf-8")
assert t.count("TeacherAvatar.entries.forEach")==2
t=t.replace("TeacherAvatar.entries.forEach","listOf(TeacherAvatar.Mert).forEach")
live.write_text(t,"utf-8")
picker=src/"TeacherAvatar.kt"
p=picker.read_text("utf-8")
p=once(p,"TeacherAvatar.entries.forEach { person ->",
       "listOf(TeacherAvatar.Mert).forEach { person ->","legacy teacher picker")
picker.write_text(p,"utf-8")

# Strictly NO SceneView in the main process. Keep real 3D Ethan as an opt-in
# separated Android activity; if Filament SIGABRT it will not abort the chat.
stage=src/"TeacherStage3D.kt"
st=(root/"tools/v136/TeacherStage3D.kt").read_text("utf-8")
assert "Teacher3DProbeActivity::class.java" in st
assert "rememberEngine" not in st and "EthanLive3DStage" not in st
stage.write_text(st,"utf-8")

# Force :teacher3d actor to Ethan even if an old/emitted Intent says Emma.
remote=src/"Teacher3DProbeActivity.kt"
q=remote.read_text("utf-8")
q=once(q,
    '''        val actor = when (intent.getStringExtra("actor")) {
            TeacherAvatar.Deniz.name -> TeacherAvatar.Deniz
            TeacherAvatar.Mert.name -> TeacherAvatar.Mert
            else -> TeacherAvatar.Elif
        }
        val withoutMorph = intent.getBooleanExtra("withoutMorph", false)
            && actor != TeacherAvatar.Mert''',
    '''        // Emma/Sophia remain archived. The :teacher3d diagnostic is now
        // Ethan-only, regardless of a stale or forged activity launch extra.
        val actor = TeacherAvatar.Mert
        val withoutMorph = false''',
    "remote actor restriction")
remote.write_text(q,"utf-8")

g=g.replace("versionCode = 60","versionCode = 61",1)
g=g.replace('versionName = "1.0.35-launch-recovery-qa"',
            'versionName = "1.0.36-ethan-safe-ai-teacher-qa"',1)
gradle.write_text(g,"utf-8")
print("QA 1.0.36 code61: AI Ethan persona + real Gemini dialog and TTS, Ethan-only selectors")
print("NO startup profile setting write and NO in-process native Filament renderer")
print("Ethan native GLB remains in Android :teacher3d, not live mouth synced yet")
print("NOT A SIGNED RELEASE; user-device startup/voice/3D verification pending")
