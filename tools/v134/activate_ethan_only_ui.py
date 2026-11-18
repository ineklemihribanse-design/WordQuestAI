#!/usr/bin/env python3
"""Temporarily show only Ethan in live AI teacher UI; preserve other profiles.

Run after apply_ethan_live_134.py, before archive_inactive_teachers_134.py.
No database deletion/migration, no past conversations or stored teacher
preferences erased. The user's previous Emma/Sophia selection remains in the
profile settings but resolves to Ethan in the current UI.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
PKG=ROOT/"source/app/src/main/java/com/kelimebulmacasi/a1"
AI=PKG/"AiSession.kt"
UI=PKG/"LiveTeacherScreen.kt"

ai=AI.read_text("utf-8")
assert 'EthanTeacherPersona.systemPrompt' in ai
old='internal var avatar by mutableStateOf(TeacherAvatar.Elif)'
assert ai.count(old)==1
ai=ai.replace(old,'internal var avatar by mutableStateOf(TeacherAvatar.Mert)')
old='avatar = TeacherAvatar.entries.firstOrNull { it.name == next.setting("avatar") } ?: TeacherAvatar.Elif'
assert ai.count(old)==1
ai=ai.replace(old,'''// Old Emma/Sophia preferences and their archived conversations are
        // deliberately left in SQLite; the temporary active teacher is Ethan.
        avatar = TeacherAvatar.Mert''')
old='''internal fun chooseAvatar(value: TeacherAvatar) {
        avatar = value'''
assert ai.count(old)==1
ai=ai.replace(old,'''internal fun chooseAvatar(value: TeacherAvatar) {
        // Historical avatars are kept for eventual restoration but cannot
        // trigger native loading while the user focuses on Ethan.
        if (value != TeacherAvatar.Mert) return
        avatar = value''')
# The old local teacher database has not tagged historical threads with an
# avatar ID. Without an allowlist, opening Ethan would mix Emma's latest old
# messages into Ethan's Gemini context. Preserve them, but only reveal threads
# explicitly created under Ethan in this QA mode.
old='''        teacherNotes = next.notes()
        threads = next.threads()
        threads.firstOrNull()?.let { openThread(it.id) }'''
assert ai.count(old)==1, "Teacher thread binding changed: inspect before patch"
ai=ai.replace(old,'''        teacherNotes = next.notes()
        val storedEthanThreads = next.setting("ethan_thread_ids").orEmpty()
        ethanThreadIds = if (storedEthanThreads.isNotBlank()) {
            storedEthanThreads.split('|').filter { it.isNotBlank() && it != "-" }
                .toMutableSet()
        } else {
            mutableSetOf()
        }
        if (storedEthanThreads.isBlank()) {
            // Only an existing Ethan-selected account can bootstrap its
            // most recent historical conversation. Female histories remain
            // in SQLite but are not presented as Ethan's memories.
            if (next.setting("avatar") == TeacherAvatar.Mert.name) {
                next.threads().firstOrNull()?.id?.let { ethanThreadIds.add(it) }
            }
            next.setSetting("ethan_thread_ids",
                ethanThreadIds.joinToString("|").ifEmpty { "-" })
        }
        threads = next.threads().filter { it.id in ethanThreadIds }
        threads.firstOrNull()?.let { openThread(it.id) }''')
old='''internal fun chooseAvatar(value: TeacherAvatar) {'''
assert ai.count(old)==1
ai=ai.replace(old,'''private var ethanThreadIds: MutableSet<String> = mutableSetOf()

    private fun persistEthanThreadIds() {
        store?.setSetting("ethan_thread_ids",
            ethanThreadIds.joinToString("|").ifEmpty { "-" })
    }

    internal fun chooseAvatar(value: TeacherAvatar) {''')
old='''    internal fun openThread(id: String) {
        val history = store ?: return'''
assert ai.count(old)==1
ai=ai.replace(old,'''    internal fun openThread(id: String) {
        // Never pass an archived Emma/Sophia thread to Ethan's AI provider.
        if (id !in ethanThreadIds) return
        val history = store ?: return''')
old='''    internal fun deleteThread(id: String) {
        store?.delete(id)'''
assert ai.count(old)==1
ai=ai.replace(old,'''    internal fun deleteThread(id: String) {
        if (id !in ethanThreadIds) return
        store?.delete(id)
        ethanThreadIds.remove(id)
        persistEthanThreadIds()''')
old='''    private fun refreshThreads() {
        threads = store?.threads(search) ?: emptyList()'''
assert ai.count(old)==1
ai=ai.replace(old,'''    private fun refreshThreads() {
        threads = (store?.threads(search) ?: emptyList())
            .filter { it.id in ethanThreadIds }''')
old='''        val savedId = activeStore.append(activeThread, answer, level)
        nextTurn = answer.id'''
assert ai.count(old)==1, "AI save hook changed"
ai=ai.replace(old,'''        val savedId = activeStore.append(activeThread, answer, level)
        if (ethanThreadIds.add(savedId)) persistEthanThreadIds()
        nextTurn = answer.id''')
AI.write_text(ai,"utf-8")

ui=UI.read_text("utf-8")
marker='TeacherAvatar.entries.forEach'
assert ui.count(marker)==2, "Unexpected avatar selectors; inspect current source before hiding"
ui=ui.replace(marker,'listOf(TeacherAvatar.Mert).forEach')
UI.write_text(ui,"utf-8")
assert 'TeacherAvatar.entries.forEach' not in ui

# Also hide inactive teachers from the older reusable avatar picker,
# preserving the enum and database records for a future restoration.
picker=PKG/"TeacherAvatar.kt"
picker_code=picker.read_text("utf-8")
old="TeacherAvatar.entries.forEach { person ->"
assert picker_code.count(old)==1, "Legacy picker changed unexpectedly"
picker_code=picker_code.replace(old,"listOf(TeacherAvatar.Mert).forEach { person ->")
picker.write_text(picker_code,"utf-8")

stage=(PKG/"TeacherStage3D.kt").read_text("utf-8")
assert 'EthanLive3DStage(' in stage
assert 'Teacher3DProbeActivity::class.java' not in stage
assert "TeacherFallback2D(" in stage

print("Ethan-only AI teacher selection: active=Mert, 2 selectors filtered")
print("Existing Emma/Sophia chat history, profile settings and source code are NOT deleted")
