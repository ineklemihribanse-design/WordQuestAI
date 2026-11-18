package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * v1.0.27 candidate avatar renderer.
 * Requires offline licensed VRoid assets validated by tools/v127.
 * Distinct facial morphs and neutral arms are baked into the GLB clips.
 * 2D error fallback remains; model identity/state are never sent to a service.
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
    var resetCounter by remember { mutableIntStateOf(0) }
    var cameraPreset by remember { mutableIntStateOf(0) }
    val asset = when (actor) {
        TeacherAvatar.Elif -> "avatars/emma.glb"
        TeacherAvatar.Deniz -> "avatars/sophia.glb"
        TeacherAvatar.Mert -> "avatars/ethan.glb"
    }
    // A bright studio backdrop; it is no longer an opaque, empty black room.
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF244B55),
                    Color(0xFF17343D),
                    Color(0xFF11262E),
                ),
            ),
        ),
    ) {
        key(asset, resetCounter, cameraPreset) {
            val engine = rememberEngine()
            val loader = rememberModelLoader(engine)

            // Center model at origin, including its head and feet, instead of
            // moving its waist below the viewport (v1.0.26 used -1f).
            val eye = when (cameraPreset) {
                1 -> Position(0f, 0.55f, 2.10f)
                2 -> Position(0f, 1.03f, 1.27f)
                else -> Position(0f, 0.05f, 3.65f)
            }
            val target = when (cameraPreset) {
                1 -> Position(0f, 0.55f, 0f)
                2 -> Position(0f, 1.03f, 0f)
                else -> Position(0f, 0.05f, 0f)
            }
            val camera = rememberCameraNode(engine) {
                position = eye
                lookAt(target)
            }
            val orbit = rememberCameraManipulator(
                orbitHomePosition = eye,
                targetPosition = target,
            )
            val model = remember(loader, asset) {
                runCatching {
                    ModelNode(
                        modelInstance = loader.createModelInstance(asset),
                        autoAnimate = true,
                        scaleToUnits = 2.70f,
                        centerOrigin = Position(0f, 0f, 0f),
                    )
                }.getOrNull()
            }
            if (model == null) {
                TeacherFallback2D(
                    actor, speaking, listening, thinking,
                    speechRate, Modifier.fillMaxSize(),
                )
            } else {
                LaunchedEffect(model, speaking, listening, thinking, speechRate) {
                    val names = (0 until model.animationCount).map { i ->
                        i to (model.animator.getAnimationName(i) ?: "").lowercase()
                    }
                    val terms = when {
                        speaking -> listOf("teacher_talk", "talk")
                        listening -> listOf("teacher_listen", "listen", "idle")
                        thinking -> listOf("teacher_think", "think", "idle")
                        else -> listOf("teacher_idle", "idle")
                    }
                    val clip = terms.firstNotNullOfOrNull { word ->
                        names.firstOrNull { (_, name) -> word in name }?.first
                    } ?: names.firstOrNull()?.first
                    if (clip != null) {
                        model.playingAnimations.keys.toList().forEach(model::stopAnimation)
                        model.playAnimation(
                            clip,
                            speed = if (speaking) speechRate.coerceIn(.7f,1.3f) else 1f,
                            loop = true,
                        )
                    }
                }
                Scene(
                    modifier = Modifier.fillMaxSize(),
                    engine = engine,
                    modelLoader = loader,
                    cameraNode = camera,
                    cameraManipulator = orbit,
                    mainLightNode = rememberMainLightNode(engine) {
                        intensity = 100_000f
                    },
                    childNodes = rememberNodes { add(model) },
                    // Compose gradient must remain visible behind the GL scene.
                    isOpaque = false,
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = 2.dp)
                .background(Color(0xAA0B2630), RoundedCornerShape(12.dp)),
        ) {
            listOf("Tam Boy", "Bel", "Yüz").forEachIndexed { index, caption ->
                TextButton(onClick = { cameraPreset = index; resetCounter++ }) {
                    Text(
                        caption,
                        color = if (cameraPreset == index) Color(0xFFF2D9A5)
                                else Color.White,
                    )
                }
            }
            TextButton(onClick = { cameraPreset = 0; resetCounter++ }) {
                Text("↺", color = Color(0xFFF2D9A5))
            }
        }
    }
}
