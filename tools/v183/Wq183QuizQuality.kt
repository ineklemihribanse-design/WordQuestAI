package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fail closed: malformed questions never enable a score-producing answer. */
internal fun Wq183QuizIsValid(prompt: String, options: Iterable<String>, correct: Int): Boolean {
    val answers = options.toList()
    return prompt.isNotBlank() && answers.size == 5 && correct in 0..4 &&
        answers.all { it.isNotBlank() } &&
        answers.map { it.trim().lowercase() }.toSet().size == 5
}

/** Explicit dark ink on a light, size-flexible question card; never white on white. */
@Composable
internal fun Wq183QuizPrompt(prompt: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFFAFF),
        border = BorderStroke(1.dp, Color(0xFFD9D0EC))
    ) {
        Text(
            text = prompt.trim().ifBlank { "Soru metni yüklenemedi." },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            color = Color(0xFF241C35),
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            softWrap = true
        )
    }
}

@Composable
internal fun Wq183QuizUnavailableQuestion() {
    Surface(
        color = Color(0xFFFFF2E8),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Bu soru eksik veya geçersiz. Puan verilmedi. Sonraki soruya geçin.",
            modifier = Modifier.padding(14.dp),
            color = Color(0xFF783A20),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
