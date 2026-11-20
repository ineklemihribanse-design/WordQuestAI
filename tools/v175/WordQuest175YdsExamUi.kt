package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val YdsNavy = Color(0xFF183365)
private val YdsBlue = Color(0xFF416AB4)

@Composable
internal fun Wq175YdsStudyHero(profile: String, attempts: Int, percent: Int, available: Int) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(Brush.horizontalGradient(listOf(
            YdsNavy, Color(0xFF416BB7), Color(0xFF7797D4))))
        .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("YDS SINAV MERKEZİ", fontWeight = FontWeight.Black,
                fontSize = 12.sp, color = Color(0xFFFFD985))
            Text("Hedefine bir adım daha yaklaş", fontSize = 23.sp,
                lineHeight = 27.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(profile + " · " + attempts + " çözülen soru",
                color = Color(0xFFE8F1FF), fontSize = 12.sp)
            Text("%" + percent + " başarı · " + available + " özgün soru",
                color = Color(0xFFFFEAB8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Wq175GlossIcon("yds", Modifier.size(93.dp))
    }
}

@Composable
internal fun Wq175YdsQuestionPanel(
    position: Int, count: Int, topic: String, level: String,
    remainingSeconds: Int?, question: String
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp))
        .background(Color.White).border(1.dp,Color(0xFFD8E4F9),RoundedCornerShape(21.dp))
        .padding(16.dp),verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SORU " + position + "/" + count,
                fontSize = 12.sp, fontWeight = FontWeight.Black, color = YdsBlue)
            Text(topic + " · " + level, color = YdsNavy, fontSize = 11.sp)
        }
        LinearProgressIndicator(progress = {
            (position.toFloat() / count.coerceAtLeast(1)).coerceIn(0f,1f)
        }, modifier = Modifier.fillMaxWidth().height(7.dp)
            .clip(RoundedCornerShape(5.dp)),
            color = YdsBlue, trackColor = Color(0xFFE7EFFF))
        if(remainingSeconds != null) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                .background(if(remainingSeconds <= 30) Color(0xFFFFE3E6)
                    else Color(0xFFE5F1FF))
                .padding(11.dp),horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if(remainingSeconds <= 30) "Son saniyeler" else "Kalan süre",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if(remainingSeconds <= 30) Color(0xFFB0334B) else YdsNavy)
                Text(if (remainingSeconds >= 3600)
                    "%02d:%02d:%02d".format(remainingSeconds / 3600,
                        (remainingSeconds % 3600) / 60, remainingSeconds % 60)
                    else "%02d:%02d".format(remainingSeconds / 60,remainingSeconds % 60),
                    color = if(remainingSeconds <= 30) Color(0xFFB0334B) else YdsNavy,
                    fontWeight = FontWeight.Black, fontSize = 19.sp)
            }
        }
        Text(question, fontSize = 18.sp, lineHeight = 26.sp,
            color = YdsNavy, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun Wq175YdsOption(
    number: Int, label: String, selected: Boolean,
    answered: Boolean, correct: Boolean, locked: Boolean = answered, onClick: () -> Unit
) {
    val selectedBg = when {
        answered && correct -> Color(0xFFE4F7ED)
        answered && selected -> Color(0xFFFFE6E3)
        else -> Color(0xFFF7F9FF)
    }
    val rim = when {
        answered && correct -> Color(0xFF63BA8C)
        answered && selected -> Color(0xFFD97B7B)
        else -> Color(0xFFD5E0F3)
    }
    Surface(onClick = onClick, enabled = !locked,
        modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp),
        color = selectedBg, shape = RoundedCornerShape(17.dp),
        border = BorderStroke(if(answered && (correct || selected)) 2.dp else 1.dp,rim)) {
        Row(Modifier.padding(horizontal = 12.dp,vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(34.dp)
                .background(Color(0xFFE0E8FA),RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center) {
                Text(('A'.code+number).toChar().toString(),
                    color = YdsNavy,fontWeight = FontWeight.Black)
            }
            Text(label,modifier = Modifier.weight(1f), color = YdsNavy,
                fontSize = 14.sp,lineHeight = 19.sp)
            if(answered && correct) Text("✓",color = Color(0xFF19845A))
        }
    }
}

@Composable
internal fun Wq175YdsAnswerFeedback(correct: Boolean, right: String, explanation: String) {
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().clip(shape)
        .background(if(correct) Color(0xFFE3F7ED) else Color(0xFFFFF2E9))
        .border(1.dp,if(correct)Color(0xFF9BD5B5) else Color(0xFFE8C0A5),shape)
        .padding(15.dp),verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if(correct) "Doğru cevap!" else "Doğru cevap: " + right,
            color = if(correct)Color(0xFF266B4C) else Color(0xFF9C5339),
            fontWeight = FontWeight.ExtraBold,fontSize = 15.sp)
        Text(explanation,color = YdsNavy,fontSize = 13.sp,lineHeight = 19.sp)
    }
}

@Composable
internal fun Wq175YdsResult(score: Int, answered: Int, total: Int) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
        .background(Brush.verticalGradient(listOf(
            Color(0xFFE8F0FF),Color(0xFFFFF4DB))))
        .padding(18.dp),horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Wq175GlossIcon("trophy",Modifier.size(94.dp))
        Text("Çalışma Özeti",color = YdsNavy,fontSize = 22.sp,
            fontWeight = FontWeight.Black)
        Text(score.toString()+"/"+answered.toString()+" doğru",
            color = YdsBlue,fontSize = 19.sp,fontWeight = FontWeight.Black)
        Text((total-answered).coerceAtLeast(0).toString() + " yanıtsız soru",
            color = Color(0xFF647493),fontSize = 12.sp)
        Text("Yanıtların kişisel çalışma istatistiklerine kaydedildi.",
            color = Color(0xFF647493),fontSize = 12.sp)
    }
}


@Composable
internal fun Wq175YdsPrimaryAction(label: String, action: () -> Unit) {
    Button(onClick = action,
        modifier = Modifier.fillMaxWidth().heightIn(min = 53.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = YdsNavy, contentColor = Color.White)) {
        Text(label, fontWeight = FontWeight.ExtraBold)
    }
}
