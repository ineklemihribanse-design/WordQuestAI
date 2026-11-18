package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The active teacher presentation is strictly native Compose 2D.
 * It never launches an Activity, touches the native 3D renderer, reads keys,
 * writes a profile setting or asks for additional permissions.
 */
@Composable
internal fun TeacherStage2D(
    actor: TeacherAvatar,
    speaking: Boolean,
    listening: Boolean,
    thinking: Boolean,
    speechRate: Float,
    modifier: Modifier = Modifier,
    mouthCue: AvatarMouthFrame = AvatarMouthFrame(),
    motion: AvatarMode = AvatarMode.IDLE,
    teacherProfile: String? = null,
    initialVoice: EnglishVoice = EnglishVoice.American,
    speechVolume: Float = 1f,
    onRemoteReturn: () -> Unit = {},
) {
    val actualState = when {
        speaking || motion == AvatarMode.SPEAKING -> AvatarMode.SPEAKING
        listening || motion == AvatarMode.LISTENING -> AvatarMode.LISTENING
        thinking || motion == AvatarMode.THINKING -> AvatarMode.THINKING
        else -> motion
    }
    Box(modifier.background(Color(0xFF173A43))) {
        Teacher2DPortrait(
            motion = actualState,
            mouthCue = mouthCue,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("Alex · Canlı İngilizce öğretmeni", color = Color(0xFFF2DCB5))
            val subtitle = when(actualState) {
                AvatarMode.SPEAKING -> "Konuşuyor"
                AvatarMode.LISTENING -> "Seni dinliyor"
                AvatarMode.THINKING -> "Yanıtını düşünüyor"
                AvatarMode.GREETING -> "Merhaba!"
                AvatarMode.ERROR -> "Bağlantıyı kontrol et"
                AvatarMode.IDLE -> "Konuşmaya hazır"
            }
            Text(subtitle, color = Color(0xFFF6F7F4))
        }
    }
}

/**
 * Source compatibility for historical call sites only. Despite this legacy
 * name there is NO SceneView/Filament or 3D surface in the shipping APK.
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
    teacherProfile: String? = null,
    initialVoice: EnglishVoice = EnglishVoice.American,
    speechVolume: Float = 1f,
    onRemoteReturn: () -> Unit = {},
) = TeacherStage2D(
    actor = actor,
    speaking = speaking,
    listening = listening,
    thinking = thinking,
    speechRate = speechRate,
    modifier = modifier,
    mouthCue = mouthCue,
    motion = motion,
    teacherProfile = teacherProfile,
    initialVoice = initialVoice,
    speechVolume = speechVolume,
    onRemoteReturn = onRemoteReturn,
)
