package com.kelimebulmacasi.a1
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Legacy Compose call-site stays stable; renders mesh-rigged 2.5D instead of a 100-frame atlas. */
@Composable internal fun Teacher2DPortrait(
    motion: AvatarMode,
    mouthCue: AvatarMouthFrame,
    modifier: Modifier = Modifier,
) = Teacher25DPortrait(motion = motion, mouthCue = mouthCue, modifier = modifier)
