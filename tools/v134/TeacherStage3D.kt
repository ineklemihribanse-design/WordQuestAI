package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp

/**
 * v1.0.34: Ethan alone is the active AI teacher. All voice/microphone,
 * online Gemini or selected AI provider, local memory and old chats remain in
 * LiveTeacherScreen. This view is strictly a real-human GLB presentation layer.
 *
 * Native Filament SIGABRT can't be caught by Kotlin when running in the same
 * Android process. Real 3D remains explicitly opt-in during device QA; a
 * recoverable 3D initialization failure can switch to the safe conversation
 * view without losing the current turn.
 *
 * Emma/Sophia historical files and conversations are kept in their archive,
 * but are deliberately NOT rendered or advertised as selectable teachers.
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
    var liveEthan3D by remember { mutableStateOf(false) }
    Box(modifier = modifier.background(Color(0xFF102B33))) {
        if (liveEthan3D && actor == TeacherAvatar.Mert) {
            EthanLive3DStage(
                motion = motion,
                mouthCue = mouthCue,
                speechRate = speechRate,
                modifier = Modifier.fillMaxSize(),
                onRecoverableError = { liveEthan3D = false },
            )
        } else {
            TeacherFallback2D(
                TeacherAvatar.Mert,
                speaking, listening, thinking, speechRate,
                Modifier.fillMaxSize()
            )
            Text(
                "Ethan'ın gerçek 3D sohbeti hazır",
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
            )
        }
        if (actor == TeacherAvatar.Mert) {
            TextButton(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                onClick = { liveEthan3D = !liveEthan3D },
            ) {
                Text(
                    if (liveEthan3D) "Güvenli görünüm"
                    else "Ethan ile canlı 3D konuş",
                    color = Color(0xFFF2D9A5)
                )
            }
        }
    }
}
