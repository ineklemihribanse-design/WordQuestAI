package com.kelimebulmacasi.a1

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode

/**
 * Opt-in LIVE Ethan: real, proven-on-device human GLB rendered alongside
 * the SAME Gemini, microphone and TTS UI (not in an isolated test activity).
 * Android's actual TTS onStart and onRangeStart events drive mode/mouth cues.
 *
 * Since native Filament SIGABRT can't be caught, parent preserves a safe 2D
 * switch and does not auto-start this experimental in-process renderer.
 */
@Composable
internal fun EthanLive3DStage(
    motion: AvatarMode,
    mouthCue: AvatarMouthFrame,
    speechRate: Float,
    modifier: Modifier = Modifier,
    onRecoverableError: () -> Unit,
) {
    var cameraPreset by remember { mutableIntStateOf(1) }
    var cameraReset by remember { mutableIntStateOf(0) }
    var observedFrame by remember { mutableStateOf(false) }
    val engine = rememberEngine()
    val loader = rememberModelLoader(engine)
    val node = remember(loader) {
        runCatching {
            ModelNode(
                modelInstance = loader.createModelInstance("avatars/ethan-mobile.glb"),
                autoAnimate = false,
                scaleToUnits = 2.7f,
                centerOrigin = Position(0f, 0f, 0f),
            )
        }.onFailure { Log.e("WQEthanLive", "Model load", it) }.getOrNull()
    }
    val names = remember(node) {
        node?.let { m ->
            (0 until m.animationCount).associate { index ->
                (m.animator.getAnimationName(index) ?: "clip_$index") to index
            }
        }.orEmpty()
    }
    val bodyClips = remember(names) { names.filterKeys { !it.startsWith("Teacher_Viseme_") } }
    val faceClips = remember(names) { names.filterKeys { it.startsWith("Teacher_Viseme_") } }
    val facial = remember(node) { AvatarFacialLayer() }
    val gesture = remember(node) { AvatarGestureController() }
    val speechRhythm = remember(node) { EthanPresenceRhythm() }
    val actualMotion = rememberUpdatedState(motion)
    val actualMouth = rememberUpdatedState(mouthCue)
    val actualRate = rememberUpdatedState(speechRate)
    val fpsWindow = remember { longArrayOf(0L, 0L) }
    // The teacher's state is driven by real microphone / AI-turn / TTS events
    // originating from LiveTeacherScreen, not a fabricated chat animation.
    val activityLabel = when (motion) {
        AvatarMode.GREETING -> "Ethan · Merhaba!"
        AvatarMode.LISTENING -> "Ethan · Seni dinliyor"
        AvatarMode.THINKING -> "Ethan · Yanıtını hazırlıyor"
        AvatarMode.SPEAKING -> "Ethan · Konuşuyor"
        AvatarMode.ERROR -> "Ethan · Konuşma bağlantısını kontrol et"
        AvatarMode.IDLE -> "Ethan · Konuşmaya hazır"
    }

    LaunchedEffect(node) {
        if (node == null) onRecoverableError()
        else {
            kotlinx.coroutines.delay(10_000L)
            if (!observedFrame) onRecoverableError()
        }
    }
    LaunchedEffect(node, motion, speechRate, observedFrame) {
        val model = node ?: return@LaunchedEffect
        if (!observedFrame) return@LaunchedEffect
        val priority = when (motion) {
            AvatarMode.GREETING -> listOf("Wave", "Greeting", "Idle")
            AvatarMode.LISTENING -> listOf("Listen", "Idle")
            AvatarMode.THINKING -> listOf("Think", "Idle")
            AvatarMode.SPEAKING -> listOf("Talk", "Idle")
            AvatarMode.ERROR, AvatarMode.IDLE -> listOf("Idle")
        }
        val selected = priority.firstNotNullOfOrNull { clip ->
            bodyClips.entries.firstOrNull {
                it.key.equals(clip, true) || it.key.equals("Teacher_$clip", true)
            }?.value
        } ?: bodyClips.values.firstOrNull()
        if (selected != null) runCatching {
            bodyClips.values.filter { it in model.playingAnimations.keys }
                .forEach(model::stopAnimation)
            model.playAnimation(
                selected,
                speed = if (motion == AvatarMode.SPEAKING)
                    speechRate.coerceIn(.72f, 1.27f) else 1f,
                loop = motion != AvatarMode.GREETING,
            )
        }.onFailure { Log.e("WQEthanLive", "Body clip", it) }
    }

    Box(
        modifier.background(
            Brush.verticalGradient(
                listOf(Color(0xFF254A55), Color(0xFF17343D), Color(0xFF102831))
            )
        )
    ) {
        if (node != null) {
            // Camera presets do not reconstruct Engine or the human mesh.
            key(cameraPreset, cameraReset) {
                val eye = when (cameraPreset) {
                    0 -> Position(0f, 0f, 3.65f)
                    2 -> Position(0f, .80f, 1.32f)
                    else -> Position(0f, .18f, 2.30f)
                }
                val target = when (cameraPreset) {
                    0 -> Position(0f, 0f, 0f)
                    2 -> Position(0f, .80f, 0f)
                    else -> Position(0f, .18f, 0f)
                }
                val camera = rememberCameraNode(engine) {
                    position = eye
                    lookAt(target)
                }
                val orbit = rememberCameraManipulator(
                    orbitHomePosition = eye,
                    targetPosition = target,
                )
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = loader,
                    cameraNode = camera,
                    cameraManipulator = orbit,
                    mainLightNode = rememberMainLightNode(engine) {
                        intensity = 100_000f
                    },
                    childNodes = rememberNodes { add(node) },
                    isOpaque = false,
                    onFrame = { frameNanos ->
                        if (!observedFrame) observedFrame = true
                        val now = SystemClock.uptimeMillis()
                        val mode = actualMotion.value
                        val real = actualMouth.value
                        // Speech animation cadence reflects real TTS word-range callbacks,
                        // never an AI response merely existing or fabricated sentiment.
                        val speechCadence = speechRhythm.speed(mode, real, now)
                        val hasCue = real.cueAtMs > 0L && now >= real.cueAtMs &&
                            now - real.cueAtMs < 155L &&
                            real.quality != LipSyncQuality.NONE && real.weight > 0f
                        // Actual Android TTS text-range callbacks approximate word
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
                        val faceName = facial.frame("Mert", mode, cue, now)
                        faceClips[faceName]?.let { faceIndex ->
                            val previous = faceClips.values.filter {
                                it in node.playingAnimations.keys
                            }
                            if (previous.size != 1 || previous[0] != faceIndex) {
                                previous.forEach(node::stopAnimation)
                                node.playAnimation(faceIndex, speed = 1f, loop = true)
                            }
                        }
                        if (mode == AvatarMode.SPEAKING) {
                            val talking = bodyClips.entries.firstOrNull {
                                it.key.equals("Talk", true)
                            }?.value
                            if (talking != null && talking in node.playingAnimations.keys) {
                                val move = gesture.frame(
                                    mode, frameNanos / 1_000_000L, "Mert"
                                ).gesture
                                val modifierSpeed = when (move) {
                                    AvatarGesture.OPEN_PALM -> 1.06f
                                    AvatarGesture.EXPLAIN -> .95f
                                    else -> 1f
                                }
                                node.setAnimationSpeed(talking,
                                    (actualRate.value.coerceIn(.72f,1.27f) *
                                        modifierSpeed * speechCadence).coerceIn(.65f, 1.4f))
                            }
                        }
                        if (fpsWindow[0] == 0L) fpsWindow[0] = now
                        fpsWindow[1]++
                        val delta = now - fpsWindow[0]
                        if (delta >= 5000L) {
                            Log.i("WQEthanLive", "fps5s=${fpsWindow[1] * 1000f / delta}")
                            fpsWindow[0] = now
                            fpsWindow[1] = 0L
                        }
                    },
                )
            }
        } else {
            Text("Ethan yüklenemedi — 2D güvenli görünüme dönülüyor",
                modifier = Modifier.align(Alignment.Center), color = Color.White)
        }
        // Live status HUD. These are presentation labels for the ACTUAL
        // Audio/AI turn state; Gemini response & user voice stay outside GLB.
        Text(
            activityLabel,
            modifier = Modifier.align(Alignment.BottomStart)
                .padding(12.dp)
                .background(Color(0xDA102B33), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            color = Color(0xFFF2D9A5),
        )
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                .background(Color(0xCC102B33), RoundedCornerShape(12.dp)),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            listOf("Tam Boy", "Bel", "Yüz").forEachIndexed { index, label ->
                TextButton(onClick = { cameraPreset = index }) {
                    Text(label,
                        color = if (cameraPreset == index) Color(0xFFF2D9A5) else Color.White)
                }
            }
            TextButton(onClick = { cameraPreset = 1; cameraReset++ }) {
                Text("↺", color = Color(0xFFF2D9A5))
            }
        }
    }
}
