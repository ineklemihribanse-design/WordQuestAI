#!/usr/bin/env python3
"""1.0.40 QA: TTS range-gated mouth cues and a calmer Ethan Talk animation.

This runs AFTER v139. The existing 3D renderer remains isolated in :teacher3d,
so no Filament Engine is ever created in the launcher. Does not mutate users,
API keys, chat records or US/UK pronunciation configuration.
"""
from pathlib import Path
root = Path(__file__).resolve().parents[2]
app = root / "source/app"
pkg = app / "src/main/java/com/kelimebulmacasi/a1"
gradle = app / "build.gradle.kts"

def one(text, before, after, name):
    count=text.count(before)
    assert count == 1, (name,count)
    return text.replace(before,after,1)

g=gradle.read_text("utf-8")
assert g.count("versionCode = 64") == 1
assert g.count('versionName = "1.0.39-conversation-error-diagnostics-qa"') == 1
assert 'applicationIdSuffix = ".offline101"' in g

p=pkg/"EthanIsolatedVoiceTeacherActivity.kt"
s=p.read_text("utf-8")
s=one(s,
    '    var mouthCue by remember { mutableStateOf(AvatarMouthFrame()) }',
    '''    var mouthCue by remember { mutableStateOf(AvatarMouthFrame()) }
    // Per-turn cue sequences let the renderer distinguish successive words.
    val lipSync = remember { AvatarLipSyncController() }''',
    "Per-utterance TTS cue lifecycle")
s=one(s,
    '''            onPlaying = { id -> main.post {
                playing = id
                if (id == null) mouthCue = AvatarMouthFrame()
            }},''',
    '''            onPlaying = { id -> main.post {
                playing = id
                if (id == null) lipSync.stop() else lipSync.start(id)
                // Old viseme must never bleed into the next reply or pause.
                mouthCue = AvatarMouthFrame()
            }},''',
    "Audio lifecycle clears previous cue")
s=one(s,
    '''            onActualSpeechRange = { _, spoken -> main.post {
                mouthCue = AvatarMouthFrame(
                    viseme = AvatarLipSyncController.fromTextRange(spoken),
                    weight = .68f,
                    quality = LipSyncQuality.TEXT_RANGE_APPROXIMATION,
                    cueAtMs = SystemClock.uptimeMillis(),
                )
            }},''',
    '''            onActualSpeechRange = { id, spoken -> main.post {
                if (playing == id) {
                    val at = SystemClock.uptimeMillis()
                    if (lipSync.push(
                            id, AvatarLipSyncController.fromTextRange(spoken),
                            at, LipSyncQuality.TEXT_RANGE_APPROXIMATION, .55f
                        )) {
                        mouthCue = lipSync.frame(at)
                    }
                }
            }},''',
    "Bind word-range to the active utterance and increment cue sequence")
p.write_text(s,"utf-8")

p=pkg/"EthanLive3DStage.kt"
s=p.read_text("utf-8")
s=one(s,
    '    val gesture = remember(node) { AvatarGestureController() }\n',
    '',
    "Stop periodic wall-clock gestures that do not follow words")
s=one(s,
    '    val fpsWindow = remember { longArrayOf(0L, 0L) }',
    '''    val fpsWindow = remember { longArrayOf(0L, 0L) }
    val lastFaceCue = remember(node) { longArrayOf(-1L) }''',
    "Track successive real word cues for a repeated mouth shape")
s=one(s,
    '''                        // Actual Android TTS text-range callbacks approximate word
                        // timing. When absent, a gentle pulse runs ONLY while the
                        // audio onStart signal says Ethan is really speaking.
                        val cue = if (!hasCue && mode == AvatarMode.SPEAKING) {
                            val active = now % 320L < 175L
                            AvatarMouthFrame(
                                viseme = if (active) AvatarViseme.A else AvatarViseme.NEUTRAL,
                                weight = if (active) .42f else 0f,
                                quality = LipSyncQuality.TEXT_RANGE_APPROXIMATION,
                                cueAtMs = now,
                            )
                        } else real
                        val faceName = facial.frame("Mert", mode, cue, now)''',
    '''                        // No invented 320-ms vowel pulses. If the TTS engine has
                        // no genuine onRangeStart, keep Ethan's mouth at rest.
                        val cue = if (hasCue) real else AvatarMouthFrame()
                        if (mode != AvatarMode.SPEAKING) lastFaceCue[0] = -1L
                        val newWord = mode == AvatarMode.SPEAKING && hasCue &&
                            real.cueSequence > 0L && real.cueSequence != lastFaceCue[0]
                        if (newWord) lastFaceCue[0] = real.cueSequence
                        val faceName = facial.frame("Mert", mode, cue, now)''',
    "Only real TTS callbacks animate the mouth")
s=one(s,
    '''                            if (previous.size != 1 || previous[0] != faceIndex) {
                                previous.forEach(node::stopAnimation)
                                node.playAnimation(faceIndex, speed = 1f, loop = true)
                            }''',
    '''                            if (previous.size != 1 || previous[0] != faceIndex || newWord) {
                                previous.forEach(node::stopAnimation)
                                // Never loop the short blink clip many times per blink.
                                node.playAnimation(faceIndex, speed = 1f, loop = false)
                            }''',
    "One-shot face animation and repeated same-vowel word cues")
s=one(s,
    '''                                val move = gesture.frame(
                                    mode, frameNanos / 1_000_000L, "Mert"
                                ).gesture
                                val modifierSpeed = when (move) {
                                    AvatarGesture.OPEN_PALM -> 1.06f
                                    AvatarGesture.EXPLAIN -> .95f
                                    else -> 1f
                                }
                                node.setAnimationSpeed(talking,
                                    (actualRate.value.coerceIn(.72f,1.27f) *
                                        modifierSpeed * speechCadence).coerceIn(.65f, 1.4f))''',
    '''                                // The baked Talk upper arms/forearms are blended
                                // toward Idle; no periodic jerk-inducing speed changes.
                                node.setAnimationSpeed(talking,
                                    (actualRate.value.coerceIn(.80f, 1.20f) *
                                        speechCadence).coerceIn(.78f, 1.30f))''',
    "Remove arbitrary gesture scheduler from Talk clip speed")
p.write_text(s,"utf-8")

g=g.replace("versionCode = 64","versionCode = 65",1).replace(
    'versionName = "1.0.39-conversation-error-diagnostics-qa"',
    'versionName = "1.0.40-ethan-natural-mouth-gestures-qa"',1)
gradle.write_text(g,"utf-8")
print("1.0.40 QA: real active TTS range cues, no synthetic lip pulses")
print("1.0.40 QA: no periodic arm-speed gesture jerk; Ethan Talk GLB is softly retimed")
print("No change to original 3D engine isolation, provider API keys, Emma/Sophia archives")
print("NOT DEVICE VERIFIED: Android TTS word range is not phoneme-perfect")
