package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RuleInk = Color(0xFF322950)
private val RulePurple = Color(0xFF8051CC)
private val RuleMuted = Color(0xFF746E8F)

/**
 * Small expandable summary for the current multiplayer rules.
 * Keep the game and ready controls above the fold on typical Android screens.
 */
@Composable
internal fun ArenaRulesCardV3(
    mode: String,
    wordCount: Int,
    seconds: Int,
    initiallyExpanded: Boolean = false
) {
    var expanded by rememberSaveable(mode) { mutableStateOf(initiallyExpanded) }
    Column(
        Modifier.fillMaxWidth()
            .border(1.dp,Color(0xFFE6DDF3),RoundedCornerShape(16.dp))
            .background(Color(0xFFFCFAFF),RoundedCornerShape(16.dp))
            .padding(horizontal=13.dp,vertical=11.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp)
    ) {
        Row(Modifier.fillMaxWidth().clickable{expanded=!expanded},
            horizontalArrangement=Arrangement.SpaceBetween) {
            Text("ⓘ  Yarışma Kuralları",color=RuleInk,
                fontWeight=FontWeight.Bold,fontSize=14.sp)
            Text(if(expanded)"Gizle  ⌃" else "Göster  ⌄",color=RulePurple,fontSize=12.sp)
        }
        if(!expanded) {
            Text(when(mode) {
                "quiz" -> "12 soru · Doğru +10 · Yanlış −3"
                "team" -> "Tek bulmaca · Ortak hedef · Birlikte çözüm"
                else -> "Doğru +100 · Yanlış −10 · Pas ile sıra değişir"
            },color=RuleMuted,fontSize=11.sp)
        } else {
            when(mode) {
                "quiz" -> {
                    RuleLineV3("• Her iki oyuncu aynı 12 soruyu çözer.")
                    RuleLineV3("• Doğru yanıt +10, yanlış yanıt −3 puandır.")
                    RuleLineV3("• Cevaplar sonuç açıklanana kadar gizli kalır.")
                }
                "team" -> {
                    RuleLineV3("• İki oyuncu aynı bulmacayı birlikte tamamlar.")
                    RuleLineV3("• Kesişen doğru harfler her iki oyuncuya görünür.")
                    RuleLineV3("• Amaç birbirini yenmek değil, bulmacayı bitirmektir.")
                }
                else -> {
                    RuleLineV3("• Doğru kelime +100 puan. Rakibin bıraktığı kelimeyi çözmek +10 ek puan.")
                    RuleLineV3("• Yanlış cevap −10 puan ve sıra rakibe geçer. Aynı kelimede ikinci anlık tahmin yok.")
                    RuleLineV3("• Pas cezasızdır; sıra değişir. İki oyuncu da bilemezse kelime sona kalır.")
                    RuleLineV3("• Diğer kelimelerden sonra bekleyen kelimeler son kez denenir.")
                    RuleLineV3("• Her oyuncu aynı kelimede bir ipucu kullanabilir. İpucuyla kazanılan kelimeden 20 puan düşülür.")
                    RuleLineV3("• $wordCount kelimelik maçta kişi başı ${ArenaDuelEngine.hintsFor(wordCount)} ipucu.")
                }
            }
            RuleLineV3(if(seconds==0)"• Süre sınırı yok."
                else "• Kelime/soru süresi: $seconds saniye.")
            RuleLineV3("• Oda sahibi ayarları değiştirirse iki oyuncu da yeniden hazır olmalıdır.")
        }
    }
}

@Composable
private fun RuleLineV3(text: String) {
    Text(text,color=RuleMuted,fontSize=12.sp,lineHeight=17.sp)
}
