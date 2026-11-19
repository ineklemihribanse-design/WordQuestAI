package com.kelimebulmacasi.a1

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.delay

/**
 * Optional Lena-only GLB player in the crash-isolated :teacher3d process.
 * Does not replace Alex, touch credentials, or claim phoneme-accurate lip sync.
 * Animation mode follows real microphone and TTS playback state from the host.
 */
@Composable
internal fun LenaLive3DStage(
    motion: AvatarMode,
    mouthCue: AvatarMouthFrame,
    speechRate: Float,
    modifier: Modifier = Modifier,
    onRecoverableError: () -> Unit,
) {
    // Important: the original GLB is foot-origin, not head-origin.
    // Default 1 frames the actual face and waist instead of the shoes.
    var cameraPreset by remember { mutableIntStateOf(1) }
    var cameraReset by remember { mutableIntStateOf(0) }
    var observedFrame by remember { mutableStateOf(false) }
    val engine = rememberEngine()
    val loader = rememberModelLoader(engine)
    val node = remember(loader) {
        runCatching {
            ModelNode(
                modelInstance = loader.createModelInstance(
                    "avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
                ),
                autoAnimate = false,
                scaleToUnits = 2.7f,
                centerOrigin = Position(0f, 0f, 0f),
            )
        }.onFailure { Log.e("WQLenaLive", "Cannot load Lena GLB", it) }.getOrNull()
    }
    // A separate lightweight REAL 3D environment; a failed decoration
    // load must never prevent Lena from speaking or appearing.
    val studio = remember(loader) {
        runCatching {
            ModelNode(
                modelInstance = loader.createModelInstance("studio/lena_studio.glb"),
                autoAnimate = false,
            )
        }.onFailure { Log.w("WQLenaStudio", "Studio geometry unavailable", it) }.getOrNull()
    }
    val clips = remember(node) {
        node?.let { model ->
            (0 until model.animationCount).associate { i ->
                (model.animator.getAnimationName(i) ?: "clip_$i") to i
            }
        }.orEmpty()
    }

    LaunchedEffect(node) {
        if (node == null) onRecoverableError()
        else {
            delay(10_000L)
            if (!observedFrame) onRecoverableError()
        }
    }
    LaunchedEffect(node, motion, speechRate, observedFrame) {
        val model = node ?: return@LaunchedEffect
        if (!observedFrame) return@LaunchedEffect
        val clip = LenaAnimationClips.select(motion, clips.keys) ?: return@LaunchedEffect
        val index = clips[clip] ?: return@LaunchedEffect
        runCatching {
            model.playingAnimations.keys.toList().forEach(model::stopAnimation)
            model.playAnimation(index,
                speed = if (motion == AvatarMode.SPEAKING)
                    speechRate.coerceIn(.7f, 1.3f) else 1f,
                loop = motion != AvatarMode.GREETING,
            )
        }.onFailure { Log.e("WQLenaLive", "Animation playback failure", it) }
    }

    Box(modifier.background(Color(0xFF123B30))) {
        if (node != null) {
            key(cameraPreset, cameraReset) {
                val eye = when (cameraPreset) {
                    0 -> Position(0f, 1.36f, 4.35f)
                    2 -> Position(0f, 2.21f, 1.48f)
                    else -> Position(0f, 1.88f, 2.28f)
                }
                val target = when (cameraPreset) {
                    0 -> Position(0f, 1.36f, 0f)
                    2 -> Position(0f, 2.21f, 0f)
                    else -> Position(0f, 1.88f, 0f)
                }
                val camera = rememberCameraNode(engine) {
                    position = eye
                    lookAt(target)
                }
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = loader,
                    cameraNode = camera,
                    cameraManipulator = rememberCameraManipulator(
                        orbitHomePosition = eye,
                        targetPosition = target,
                    ),
                    mainLightNode = rememberMainLightNode(engine) {
                        intensity = 100_000f
                    },
                    childNodes = rememberNodes {
                        if (studio != null) add(studio)
                        add(node)
                    },
                    isOpaque = false,
                    onFrame = { _ ->
                        if (!observedFrame) observedFrame = true
                    },
                )
            }
        } else {
            Text("Lena modeli açılamadı; güvenli 2.5D görünüme geçiliyor.",
                modifier = Modifier.align(Alignment.Center), color = Color.White)
        }
        // Side control never covers Lena's face as the original centered strip did.
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                .background(Color(0xCC14392F), RoundedCornerShape(24.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Tam", "Bel", "Yüz").forEachIndexed { index, label ->
                TextButton(onClick = { cameraPreset = index }) {
                    Text(label, color = if (cameraPreset == index)
                        Color(0xFFFBE2A8) else Color.White)
                }
            }
            TextButton(onClick = { cameraPreset = 1; cameraReset++ }) {
                Text("↺", color = Color(0xFFFBE2A8))
            }
        }
    }
}
