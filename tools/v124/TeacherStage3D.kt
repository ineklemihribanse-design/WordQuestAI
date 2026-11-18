package com.kelimebulmacasi.a1

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Native 3D renderer with 2D recovery if the device GPU cannot start GLES2.
 * We keep the old bitmap-like portrait only as an error fallback, not the main
 * 3D experience. The renderer has NO network calls and NO AI credentials.
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
    val scene = remember { Teacher3DSettings() }
    scene.actor = actor.name
    scene.speaking = speaking
    scene.listening = listening
    scene.thinking = thinking
    scene.speechRate = speechRate.coerceIn(.6f,1.4f)
    var firstFrame by remember { mutableStateOf(false) }
    var renderFailed by remember { mutableStateOf(false) }
    // Some broken EGL drivers never invoke Renderer callbacks: never show a blank room.
    LaunchedEffect(Unit) {
        delay(6000)
        if (!firstFrame) renderFailed = true
    }

    Box(modifier = modifier) {
        if (!firstFrame || renderFailed) {
            TeacherFallback2D(actor, speaking, listening, thinking, speechRate, Modifier.fillMaxSize())
        }
        if (!renderFailed) {
            AndroidView(
                factory = { context ->
                    NativeTeacher3DView(
                        context,
                        scene,
                        onFirstFrame = { firstFrame = true },
                        onError = { renderFailed = true },
                    )
                },
                update = {
                    // Camera state and avatar selection live outside the GL thread.
                    scene.actor = actor.name
                    scene.speaking = speaking
                    scene.listening = listening
                    scene.thinking = thinking
                    scene.speechRate = speechRate.coerceIn(.6f,1.4f)
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { it.onPause() },
            )
        }
        if (firstFrame && !renderFailed) {
            TextButton(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 4.dp),
                onClick = { scene.reset() },
            ) {
                Text("↺ Sıfırla", color = Color(0xFFE9D1A6))
            }
        }
    }
}
