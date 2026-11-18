package com.kelimebulmacasi.a1

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Lightweight, offline animated illustrations: mouth motion follows TTS playback state. */
internal enum class TeacherAvatar(val label: String, val prompt: String, val hair: Color, val shirt: Color,
                                  val complexion: Color) {
    Elif("Emma", "Friendly, patient and encouraging. One clear question at a time.",
        Color(0xFF332420), Color(0xFF307D6A), Color(0xFFEAC09E)),
    Deniz("Sophia", "Curious conversation partner; use natural, short everyday examples.",
        Color(0xFF554132), Color(0xFF4E78A1), Color(0xFFE8B98D)),
    Mert("Alex", "Calm, precise and supportive tutor; explain grammar gently.",
        Color(0xFF2B302D), Color(0xFF7755A0), Color(0xFFCC9A75)),
}

@Composable
internal fun TalkingTeacherAvatar(character: TeacherAvatar, speaking: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "animated-mouth")
    val mouth by transition.animateFloat(0.12f, 0.95f,
        animationSpec = infiniteRepeatable(tween(230), RepeatMode.Reverse), label = "speaking")
    Canvas(modifier.size(98.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(Color(0xFFEDF3E6), radius = w * .5f, center = Offset(w * .5f, h * .5f))
        drawRoundRect(character.shirt, topLeft = Offset(w*.17f, h*.72f),
            size = Size(w*.66f, h*.29f), cornerRadius = CornerRadius(w*.18f))
        drawOval(character.hair, topLeft = Offset(w*.23f, h*.09f), size = Size(w*.54f,h*.72f))
        drawOval(character.complexion, topLeft = Offset(w*.27f, h*.20f), size = Size(w*.46f,h*.52f))
        drawOval(character.hair, topLeft = Offset(w*.24f, h*.12f), size = Size(w*.52f,h*.20f))
        drawCircle(Color(0xFF2B2624), radius = w*.018f, center = Offset(w*.40f,h*.44f))
        drawCircle(Color(0xFF2B2624), radius = w*.018f, center = Offset(w*.60f,h*.44f))
        drawOval(Color(0xFF9B5A5E), topLeft = Offset(w*.45f,h*.585f),
            size = Size(w*.10f,h*(if (speaking) .035f + mouth*.075f else .025f)))
    }
}

@Composable
internal fun TeacherAvatarPicker(selected: TeacherAvatar, onSelect: (TeacherAvatar) -> Unit,
                                 playing: Boolean, enabled: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TalkingTeacherAvatar(selected, playing)
        Column(Modifier.padding(start = 9.dp)) {
            Text("Öğretmenin: ${selected.label}", fontWeight = FontWeight.SemiBold)
            Row {
                listOf(TeacherAvatar.Mert).forEach { person ->
                    FilterChip(selected = selected == person, onClick = { onSelect(person) },
                        enabled = enabled, label = { Text(person.label) }, modifier = Modifier.padding(end = 3.dp))
                }
            }
        }
    }
}
