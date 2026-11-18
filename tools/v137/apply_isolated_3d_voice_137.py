#!/usr/bin/env python3
"""After QA code61, wire fully interactive real GLB Ethan inside :teacher3d.

Unlike the 1.0.34 main-process Filament experiment, ALL SceneView and
voice-driven visemes run in the crash-isolated :teacher3d Android process.
It opens via explicit nonexported Activity with profile id only; Gemini keys
are retrieved by existing per-profile credentials, never Intent extras.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[2]
app=root/"source/app"
pkg=app/"src/main/java/com/kelimebulmacasi/a1"
tests=app/"src/test/java/com/kelimebulmacasi/a1"
manifest=app/"src/main/AndroidManifest.xml"
gradle=app/"build.gradle.kts"

def once(data,old,new,description):
    assert data.count(old)==1, (description,data.count(old))
    return data.replace(old,new,1)

g=gradle.read_text("utf-8")
assert g.count("versionCode = 61")==1
assert g.count('versionName = "1.0.36-ethan-safe-ai-teacher-qa"')==1
assert 'applicationIdSuffix = ".offline101"' in g
stage=(pkg/"TeacherStage3D.kt").read_text("utf-8")
assert "Teacher3DProbeActivity::class.java" in stage
assert "EthanLive3DStage" not in stage
assert not (pkg/"EthanLive3DStage.kt").exists()
assert (app/"src/main/assets/avatars/ethan-mobile.glb").is_file()

for name,folder in (
    ("EthanLive3DStage.kt","v134"),
    ("EthanPresenceRhythm.kt","v134"),
    ("EthanIsolatedVoiceTeacherActivity.kt","v137"),
    ("TeacherStage3D.kt","v137"),
):
    (pkg/name).write_text((root/"tools"/folder/name).read_text("utf-8"),"utf-8")
tests.mkdir(parents=True,exist_ok=True)
(tests/"EthanPresenceRhythmTest.kt").write_text(
    (root/"tools/v134/EthanPresenceRhythmTest.kt").read_text("utf-8"),"utf-8")

ui=pkg/"LiveTeacherScreen.kt"
c=ui.read_text("utf-8")
c=once(c,
    "mouthCue = mouthCue, motion = avatarMotion.mode)",
    """mouthCue = mouthCue, motion = avatarMotion.mode,
                teacherProfile = ai.boundProfile,
                initialVoice = session.preferences.voice,
                speechVolume = session.preferences.speechVolume,
                onRemoteReturn = { ai.refreshExternalEthanThreads() })""",
    "teacher callsite")
ui.write_text(c,"utf-8")

ai=pkg/"AiSession.kt"
s=ai.read_text("utf-8")
old='''    internal fun chooseAvatar(value: TeacherAvatar) {'''
new='''    /** Refresh main UI after the isolated 3D tutor saved a real AI turn.
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

    internal fun chooseAvatar(value: TeacherAvatar) {'''
s=once(s,old,new,"main UI refresh callback")
ai.write_text(s,"utf-8")

xml=manifest.read_text("utf-8")
assert xml.count("</application>")==1
assert 'android:name=".Teacher3DProbeActivity"' in xml
assert "EthanIsolatedVoiceTeacherActivity" not in xml
activity='''        <!-- Full human 3D + AI/microphone/TTS, crash-isolated from main app. -->
        <activity
            android:name=".EthanIsolatedVoiceTeacherActivity"
            android:exported="false"
            android:process=":teacher3d" />
'''
xml=xml.replace("</application>",activity+"    </application>",1)
manifest.write_text(xml,"utf-8")

g=g.replace("versionCode = 61","versionCode = 62",1)
g=g.replace('versionName = "1.0.36-ethan-safe-ai-teacher-qa"',
            'versionName = "1.0.37-ethan-isolated-3d-voice-qa"',1)
gradle.write_text(g,"utf-8")
print("QA 1.0.37 code62 — real 3D human Ethan plus mic -> Gemini -> TTS -> visemes")
print("Filament remains entirely inside crash-isolated :teacher3d; main launcher unaffected")
print("NO API secrets in Intents; same per-profile credentials, explicit user speech only")
print("NOT A DEVICE VERIFIED RELEASE: phonemes approximate Android TTS text ranges")
