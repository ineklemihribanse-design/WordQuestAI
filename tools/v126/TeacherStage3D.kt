package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode

/**
 * Actual licensed rigged GLB people (not hand-coded mannequin primitives).
 * Filament/SceneView 2.3.0 is intentionally pinned for this app's JDK 17 build.
 *
 * Preserves the original TeacherAvatar enum IDs to protect stored conversations.
 * Assets ship offline, and neither AI keys nor message text reach the 3D layer.
 */
@Composable
internal fun TeacherStage3D(
    actor: TeacherAvatar,
    speaking: Boolean,
    listening: Boolean,
    thinking: Boolean,
    speechRate: Float,
    modifier: Modifier = Modifier,
) {
    var cameraReset by remember { mutableIntStateOf(0) }
    val avatarFile = when (actor) {
        TeacherAvatar.Elif -> "avatars/emma.glb"
        TeacherAvatar.Deniz -> "avatars/sophia.glb"
        TeacherAvatar.Mert -> "avatars/ethan.glb"
    }

    Box(modifier = modifier.background(Color(0xFF102B33))) {
        // A separate scene is constructed on avatar switch/reset, so camera
        // state and GPU resources do not leak across different skeletons.
        key(avatarFile, cameraReset) {
            val engine = rememberEngine()
            val loader = rememberModelLoader(engine)
            val camera = rememberCameraNode(engine) {
                position = Position(0f, 1.26f, 4.9f)
            }
            val manipulator = rememberCameraManipulator(
                orbitHomePosition = Position(0f, 1.26f, 4.9f),
                targetPosition = Position(0f, 1.18f, 0f),
            )
            val avatar = remember(loader, avatarFile) {
                runCatching {
                    ModelNode(
                        modelInstance = loader.createModelInstance(avatarFile),
                        autoAnimate = false,
                        scaleToUnits = 2.8f,
                        centerOrigin = Position(0f,-1f,0f),
                    )
                }.getOrNull()
            }
            if (avatar == null) {
                TeacherFallback2D(
                    actor, speaking, listening, thinking,
                    speechRate, Modifier.fillMaxSize(),
                )
            } else {
                // A single clip is played at a time. Multiple full-body clips
                // together would fight over the same humanoid skeleton.
                LaunchedEffect(avatar, speaking, listening, thinking) {
                    val count = avatar.animationCount
                    if (count > 0) {
                        val candidates = (0 until count).map { i ->
                            i to (avatar.animator.getAnimationName(i) ?: "").lowercase()
                        }
                        val patterns = when {
                            speaking -> listOf("talk", "speaking", "gesture", "explain", "wave", "idle")
                            listening -> listOf("listen", "attentive", "idle", "breath")
                            thinking -> listOf("think", "ponder", "idle")
                            else -> listOf("idle", "breath", "stand")
                        }
                        val clip = patterns.firstNotNullOfOrNull { pattern ->
                            candidates.firstOrNull { (_, name) -> pattern in name }?.first
                        } ?: 0
                        avatar.playingAnimations.keys.toList().forEach(avatar::stopAnimation)
                        avatar.playAnimation(
                            clip, speed = if (speaking) speechRate.coerceIn(.7f,1.3f) else 1f,
                            loop = true,
                        )
                    }
                }
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = loader,
                    cameraNode = camera,
                    cameraManipulator = manipulator,
                    childNodes = rememberNodes { add(avatar) },
                    isOpaque = true,
                )
            }
        }

        TextButton(
            onClick = { cameraReset++ },
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Text("↺ Sıfırla", color = Color(0xFFE7CDA2))
        }
    }
}
