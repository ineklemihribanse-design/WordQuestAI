package com.kelimebulmacasi.a1

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Safe voice-conversation host for native GLB diagnostics.
 * Neither the primary chat activity nor this composable constructs Filament:
 * the GLB is loaded in the isolated :teacher3d process by the explicit button.
 *
 * The Android ActivityResult callback resumes the teacher with microphone,
 * Gemini history and US/UK voices intact after a successful exit OR a
 * renderer-process crash. This diagnostic screen is not a finished 3D tutor.
 */
@Composable
internal fun TeacherStage3D(
    actor: TeacherAvatar,
    speaking: Boolean,
    listening: Boolean,
    thinking: Boolean,
    speechRate: Float,
    modifier: Modifier = Modifier,
    mouthCue: AvatarMouthFrame = AvatarMouthFrame(),
    motion: AvatarMode = AvatarMode.IDLE,
) {
    val context = LocalContext.current
    var diagnostic by remember { mutableStateOf("") }
    val openViewer = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        diagnostic = Teacher3DHealth.lastResult(context)
    }
    Box(modifier = modifier.background(Color(0xFF102B33))) {
        TeacherFallback2D(
            actor, speaking, listening, thinking, speechRate,
            Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
        ) {
            TextButton(
                onClick = {
                    Teacher3DHealth.mark(context, "requested_${actor.name}")
                    openViewer.launch(
                        Intent(context, Teacher3DProbeActivity::class.java)
                            .putExtra("actor", actor.name)
                    )
                },
            ) {
                Text("Gerçek 3D modeli dene", color = Color(0xFFF2D9A5))
            }
            if (diagnostic.isNotEmpty()) {
                Text(diagnostic, color = Color.White)
            }
        }
    }
}
