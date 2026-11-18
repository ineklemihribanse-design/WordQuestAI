#!/usr/bin/env python3
"""1.0.27.1: connect avatar state to real TTS and recognition events.

Works on RESTORED source after v1.0.27 candidate overlay. The stage remains
the licensed GLB SceneView renderer; the prototype does not touch user data,
account APIs, US/UK pronunciation selection, signing, or package ID.
This patch intentionally does not claim phoneme-level lip sync: Android
onRangeStart only gives spoken text spans, and often no timing at all.
"""
from pathlib import Path

root=Path(__file__).resolve().parents[2]
source=root/"source"
pkg=source/"app/src/main/java/com/kelimebulmacasi/a1"
assert pkg.exists(),"Full application sources must be restored through 1.0.27 first"

def once(text,old,new,file):
    count=text.count(old)
    assert count==1,f"{file}: expected one {old[:110]!r}, saw {count}"
    return text.replace(old,new,1)

voice=pkg/"ConversationAudio.kt"
v=voice.read_text(encoding="utf-8")
v=once(v,
    '    private val pronunciationPreferences: () -> WordQuestPreferences = { WordQuestPreferences() },\n',
    '    private val pronunciationPreferences: () -> WordQuestPreferences = { WordQuestPreferences() },\n'
    '    private val onActualSpeechRange: (Long, String) -> Unit = { _, _ -> },\n',
    "ConversationAudio signature")
v=once(v,
    "    private var utterance: String? = null\n",
    "    private var utterance: String? = null\n"
    "    private var activeTurnId: Long? = null\n"
    "    private var utteranceText = \"\"\n",
    "ConversationAudio fields")
v=once(v,
    '        val (rate, volume) = settings()\n',
    '        activeTurnId = turn.id\n'
    '        val (rate, volume) = settings()\n',
    "ConversationAudio play turn")

# Do not display avatar speech during asynchronous TTS initialization.
v=once(v,
    '            pending = turn\n            onPlaying(turn.id)\n',
    '            pending = turn\n',
    "ConversationAudio pending")
v=once(v,
    '        utterance = id\n        if (tts.speak(text.take(TextToSpeech.getMaxSpeechInputLength()),',
    '        utterance = id\n'
    '        utteranceText = text.take(TextToSpeech.getMaxSpeechInputLength())\n'
    '        if (tts.speak(utteranceText,',
    "ConversationAudio TTS submit")
v=once(v,
    '                Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }, id) == TextToSpeech.SUCCESS) {\n            onPlaying(turn.id)\n',
    '                Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }, id) == TextToSpeech.SUCCESS) {\n'
    '            // onStart() reports actual playback; speak() SUCCESS only queues audio.\n',
    "ConversationAudio TTS queued")
v=once(v,
    '        utterance = null\n        offline.stop()\n',
    '        utterance = null\n'
    '        activeTurnId = null\n'
    '        utteranceText = \"\"\n'
    '        offline.stop()\n',
    "ConversationAudio stop")
v=once(v,
    '        override fun onStart(utteranceId: String?) = Unit\n',
    '        override fun onStart(utteranceId: String?) {\n'
    '            main.post {\n'
    '                val id = activeTurnId\n'
    '                if (!closed && utteranceId == utterance && id != null) onPlaying(id)\n'
    '            }\n'
    '        }\n'
    '        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {\n'
    '            // This is WORD/TEXT timing, not actual phoneme/viseme timing.\n'
    '            main.post {\n'
    '                val id = activeTurnId\n'
    '                val text = utteranceText\n'
    '                if (!closed && utteranceId == utterance && id != null &&\n'
    '                    start >= 0 && end > start && end <= text.length) {\n'
    '                    onActualSpeechRange(id, text.substring(start, end))\n'
    '                }\n'
    '            }\n'
    '        }\n',
    "ConversationAudio TTS start callback")
v=once(v,
    '                utterance = null\n                onPlaying(null)\n',
    '                utterance = null\n'
    '                activeTurnId = null\n'
    '                utteranceText = \"\"\n'
    '                onPlaying(null)\n',
    "ConversationAudio finish")
voice.write_text(v,encoding="utf-8")

for file in ("AvatarMotionCore.kt",):
    (pkg/file).write_text((root/"tools/v1271"/file).read_text(encoding="utf-8"),encoding="utf-8")
testdir=source/"app/src/test/java/com/kelimebulmacasi/a1"
testdir.mkdir(parents=True,exist_ok=True)
(testdir/"AvatarMotionCoreTest.kt").write_text((root/"tools/v1271/AvatarMotionCoreTest.kt").read_text(encoding="utf-8"),encoding="utf-8")

ui=pkg/"LiveTeacherScreen.kt"
u=ui.read_text(encoding="utf-8")
u=once(u,'import android.content.Intent\n','import android.content.Intent\nimport android.os.SystemClock\n',"teacher imports")
u=once(u,
    '    var speaking by remember { mutableStateOf<Long?>(null) }\n',
    '    var speaking by remember { mutableStateOf<Long?>(null) }\n'
    '    val avatarMotion = remember { AvatarStateController(ai.avatar.name) }\n'
    '    val lipSync = remember { AvatarLipSyncController() }\n'
    '    var mouthCue by remember { mutableStateOf(AvatarMouthFrame()) }\n',
    "teacher state")
u=once(u,
    '        val playback = ConversationAudio(context, { speaking = it }, { audioError = it },\n',
    '        val playback = ConversationAudio(context, { id ->\n'
    '            speaking = id\n'
    '            if (id == null) {\n'
    '                avatarMotion.audioFinished(null)\n'
    '                lipSync.stop()\n'
    '                mouthCue = AvatarMouthFrame()\n'
    '            } else if (avatarMotion.audioStarted(id)) {\n'
    '                lipSync.start(id)\n'
    '            }\n'
    '        }, { audioError = it },\n',
    "teacher playback")
u=once(u,
    '            pronunciationPreferences = { session.preferences })\n',
    '            pronunciationPreferences = { session.preferences },\n'
    '            onActualSpeechRange = { id, spoken ->\n'
    '                val moment = SystemClock.uptimeMillis()\n'
    '                val viseme = AvatarLipSyncController.fromTextRange(spoken)\n'
    '                if (lipSync.push(id, viseme, moment, LipSyncQuality.TEXT_RANGE_APPROXIMATION)) {\n'
    '                    mouthCue = lipSync.frame(moment)\n'
    '                }\n'
    '            })\n',
    "teacher lip callback")
u=once(u,
    '        sending = true\n        error = \"\"\n',
    '        sending = true\n'
    '        avatarMotion.responsePending()\n'
    '        error = \"\"\n',
    "teacher thinking transition")
u=once(u,
    '                if (ai.autoRead && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) audio?.play(turn)\n',
    '                avatarMotion.responseAvailable()\n'
    '                if (ai.autoRead && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) audio?.play(turn)\n',
    "teacher reply audio")
u=once(u,
    '        listening = false\n        if (result.resultCode == Activity.RESULT_OK) {\n',
    '        listening = false\n'
    '        avatarMotion.micCancelled()\n'
    '        if (result.resultCode == Activity.RESULT_OK) {\n',
    "teacher mic end")
u=once(u,
    '            listening = true\n            recognizer.launch(intent)\n',
    '            recognizer.launch(intent)\n'
    '            listening = true\n'
    '            avatarMotion.micStarted()\n',
    "teacher mic launch")
u=once(u,
    '        if (!canSend || message.isEmpty() || sending) {\n',
    '        if (message.isEmpty() || sending || listening || (!ai.hasKey && ephemeralKey.isBlank())) {\n',
    "teacher recognition send stale-compose guard")
u=once(u,
    '            TeacherStage3D(ai.avatar, speaking != null, listening, sending,\n                session.preferences.speechRate, Modifier.fillMaxSize())',
    '            TeacherStage3D(ai.avatar, speaking != null, listening, sending,\n'
    '                session.preferences.speechRate, Modifier.fillMaxSize(),\n'
    '                mouthCue = mouthCue, motion = avatarMotion.mode)',
    "teacher stage props")
# Stop old actor's sound and neutralize lips on character changes.
anchor='    val currentTurn = ai.turns.lastOrNull()\n'
u=once(u,anchor,anchor+
    '\n    LaunchedEffect(ai.avatar) {\n'
    '        if (avatarMotion.actorId != ai.avatar.name) {\n'
    '            audio?.stop()\n'
    '            avatarMotion.selectActor(ai.avatar.name)\n'
    '            lipSync.stop()\n'
    '            mouthCue = AvatarMouthFrame()\n'
    '        }\n'
    '        if (avatarMotion.mode == AvatarMode.IDLE) {\n'
    '            // A single complete wave on entry, not a 5.5-second loop.\n'
    '            avatarMotion.greeting()\n'
    '            kotlinx.coroutines.delay(2550L)\n'
    '            avatarMotion.greetingFinished()\n'
    '        }\n'
    '    }\n',
    "teacher actor switch")
ui.write_text(u,encoding="utf-8")

stage=pkg/"TeacherStage3D.kt"
a=stage.read_text(encoding="utf-8")
a=once(a,
    '    modifier: Modifier = Modifier,\n) {',
    '    modifier: Modifier = Modifier,\n'
    '    mouthCue: AvatarMouthFrame = AvatarMouthFrame(),\n'
    '    motion: AvatarMode = AvatarMode.IDLE,\n'
    ') {',
    "stage extra state")
# Explicit gesture-aware clip selection while keeping GLB, camera and visual QA unchanged.
a=once(a,
    '                        autoAnimate = true,',
    '                        autoAnimate = false,',
    "stage disable all-clips playback")
a=once(a,
    'import androidx.compose.runtime.LaunchedEffect\n',
    'import android.os.SystemClock\nimport androidx.compose.runtime.LaunchedEffect\n',
    "stage monotonic TTS cue clock")
a=once(a,
    '                LaunchedEffect(model, speaking, listening, thinking, speechRate) {',
    '                LaunchedEffect(model, speaking, listening, thinking, speechRate, motion) {',
    "stage clip effect")
a=once(a,
    '                    val terms = when {\n                        speaking -> listOf("teacher_talk", "talk")\n                        listening -> listOf("teacher_listen", "listen", "idle")\n                        thinking -> listOf("teacher_think", "think", "idle")\n                        else -> listOf("teacher_idle", "idle")\n                    }',
    '                    val terms = when (motion) {\n'
    '                        AvatarMode.SPEAKING -> listOf("teacher_talk", "talk", "gesture")\n'
    '                        AvatarMode.LISTENING -> listOf("teacher_listen", "listen", "idle")\n'
    '                        AvatarMode.THINKING -> listOf("teacher_think", "think", "idle")\n'
    '                        AvatarMode.GREETING -> listOf("teacher_greet", "greet", "wave", "idle")\n'
    '                        else -> listOf("teacher_idle", "idle")\n'
    '                    }',
    "stage animation priority")

a=once(a,
    '                            speed = if (speaking) speechRate.coerceIn(.7f,1.3f) else 1f,\n'
    '                            loop = true,',
    '                            speed = when(motion) {\n'
    '                                AvatarMode.GREETING -> 2.3f\n'
    '                                AvatarMode.SPEAKING -> speechRate.coerceIn(.7f,1.3f)\n'
    '                                else -> 1f\n'
    '                            },\n'
    '                            loop = motion != AvatarMode.GREETING,',
    "stage complete wave only once")

a=once(a,
    '                Scene(\n',
    '                val gestureEngine = remember(actor) { AvatarGestureController() }\n'
    '                val facialEngine = remember(model) { AvatarFacialLayer() }\n'
    '                val faceClipIndices = remember(model) {\n'
    '                    (0 until model.animationCount).mapNotNull { idx ->\n'
    '                        val name = model.animator.getAnimationName(idx)\n'
    '                        if (name?.startsWith("Teacher_Viseme_") == true)\n'
    '                            name to idx else null\n'
    '                    }.toMap()\n'
    '                }\n'
    '                val talkAnimationIndex = remember(model) {\n'
    '                    (0 until model.animationCount).firstOrNull { i ->\n'
    '                        val name = model.animator.getAnimationName(i)?.lowercase() ?: ""\n'
    '                        name.contains("teacher_talk") || name == "talk"\n'
    '                    }\n'
    '                }\n'
    '                Scene(\n',
    "stage gesture controller binding")
a=once(a,
    '                    isOpaque = false,\n                )',
    '                    isOpaque = false,\n'
    '                    onFrame = { frameTimeNanos ->\n'
    '                        // One owner of all facial morph weights per frame:\n'
    '                        // separate eye and mouth clips overwrite each other.\n'
    '                        // Use a precomposed clip and expire a TTS word cue\n'
    '                        // after 170 ms to prevent a stuck-open mouth.\n'
    '                        val clipName = facialEngine.frame(actor.name, motion,\n'
    '                            mouthCue, SystemClock.uptimeMillis())\n'
    '                        val faceIndex = faceClipIndices[clipName]\n'
    '                        if (faceIndex != null) {\n'
    '                            val playingFaces = faceClipIndices.values.filter {\n'
    '                                model.playingAnimations.containsKey(it)\n'
    '                            }\n'
    '                            if (playingFaces.size != 1 || playingFaces[0] != faceIndex) {\n'
    '                                playingFaces.forEach(model::stopAnimation)\n'
    '                                model.playAnimation(faceIndex, speed = 1f, loop = true)\n'
    '                            }\n'
    '                        }\n'
    '                        // Source GLB includes small full-body/arm gestures. Vary\n'
    '                        // playback subtly with the actor-specific speaking rhythm.\n'
    '                        val index = talkAnimationIndex\n'
    '                        if (motion == AvatarMode.SPEAKING && index != null &&\n'
    '                            model.playingAnimations.containsKey(index)) {\n'
    '                            val gesture = gestureEngine.frame(motion,\n'
    '                                frameTimeNanos / 1_000_000L, actor.name).gesture\n'
    '                            val speedMultiplier = when (gesture) {\n'
    '                                AvatarGesture.OPEN_PALM -> 1.07f\n'
    '                                AvatarGesture.EXPLAIN -> .94f\n'
    '                                else -> 1f\n'
    '                            }\n'
    '                            model.setAnimationSpeed(index,\n'
    '                                speechRate.coerceIn(.7f, 1.3f) * speedMultiplier)\n'
    '                        }\n'
    '                    },\n'
    '                )',
    "stage live gesture clock")
for file in ("AvatarFacialLayer.kt",):
    (pkg/file).write_text((root/"tools/v1271"/file).read_text(encoding="utf-8"),encoding="utf-8")
(testdir/"AvatarFacialLayerTest.kt").write_text((root/"tools/v1271/AvatarFacialLayerTest.kt").read_text(encoding="utf-8"),encoding="utf-8")
stage.write_text(a,encoding="utf-8")
# V1.0.27.1 stays in a draft branch, never changes the signed release pipeline
gradle=source/"app/build.gradle.kts"
g=gradle.read_text(encoding="utf-8")
assert g.count("versionCode = 50")==1 and g.count('versionName = "1.0.27-premium-human-avatar"')==1
assert 'applicationIdSuffix = ".offline101"' in g
g=g.replace("versionCode = 50","versionCode = 51")
g=g.replace('versionName = "1.0.27-premium-human-avatar"','versionName = "1.0.27.1-animated-lipsync-avatar"')
gradle.write_text(g,encoding="utf-8")
print("1.0.27.1: genuine TTS onStart/range/done, recognition transitions, voice cue engine + tests installed")
print("NOTE: Android TTS text-range callbacks do NOT provide phoneme timings.")
print("NOTE: 3D morph binding requires model-specific metadata and further device QA. Do not release.")
