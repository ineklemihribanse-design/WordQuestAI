package com.kelimebulmacasi.a1

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

internal object Wq179ArenaReplayWords {
    fun pending(match:PremiumOnlineMatch):List<Int> =
        (0 until match.size).filter{index->
            index !in match.solvedBy &&
            PremiumOnlineLimits.gaveUpOrFailed(match,index,match.hostUid) &&
            PremiumOnlineLimits.gaveUpOrFailed(match,index,match.guestUid)
        }
    fun available(match:PremiumOnlineMatch,uid:String):List<Int> =
        if(!PremiumOnlineLimits.replayRound(match))emptyList()
        else pending(match).filter{PremiumOnlineLimits.canAttempt(match,it,uid)}
}

@Composable
internal fun Wq179ArenaReplayPanel(
    match:PremiumOnlineMatch,puzzle:Puzzle,uid:String,choose:(Int)->Unit
){
    val pending=Wq179ArenaReplayWords.pending(match)
    if(pending.isEmpty())return
    val available=Wq179ArenaReplayWords.available(match,uid)
    val active=PremiumOnlineLimits.replayRound(match)
    var open by rememberSaveable(match.code,uid){mutableStateOf(false)}
    val shape=RoundedCornerShape(17.dp)
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFFFF1DE),shape)
            .border(1.dp,Color(0xFFE7BE7C),shape)
            .padding(12.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp)){
        Text(if(active)"🏆  SON ŞANS TURU AÇILDI" else
            "⏳  SON ŞANSA KALANLAR · "+pending.size,
            fontSize=14.sp,fontWeight=FontWeight.ExtraBold,color=Color(0xFF815526))
        Text(if(active)
            "İkinizin de pas geçtiği ya da yanlış cevapladığı kelimeler yeniden seçilebilir."
            else "İkinizin de bilemediği "+pending.size+
                " kelime kaydedildi. Diğer kelimeler de denendiğinde son şans turunda açılacak.",
            fontSize=12.sp,lineHeight=17.sp,color=Color(0xFF674F36))
        Button(onClick={open=true},enabled=active && available.isNotEmpty(),
            modifier=Modifier.fillMaxWidth(),
            colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF7747B4))){
            Text(if(active)"Son Şans Kelimeleri ("+available.size+")"
                else "Son Şans Turunu Bekle",fontWeight=FontWeight.Bold)
        }
        if(active && available.isEmpty())
            Text("Bu kelimelerde son şansını kullandın. Rakibin cevabı bekleniyor.",
                color=Color(0xFF785840),fontSize=11.sp)
    }
    if(open)Dialog(onDismissRequest={open=false}){
        Surface(color=Color(0xFFFFFBF4),shape=RoundedCornerShape(24.dp)){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Text("Son Şans Kelimeleri",fontWeight=FontWeight.ExtraBold,
                    fontSize=21.sp,color=Color(0xFF443060))
                Text("Bir kelimeyi seçip son kez cevaplayabilirsin.",
                    color=Color(0xFF766782),fontSize=12.sp)
                Column(Modifier.heightIn(max=430.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(6.dp)){
                    available.forEach{index->
                        val w=puzzle.words.getOrNull(index)?:return@forEach
                        Surface(onClick={choose(index);open=false},
                            shape=RoundedCornerShape(13.dp),color=Color.White,
                            border=BorderStroke(1.dp,Color(0xFFE6D7F8))){
                            Column(Modifier.fillMaxWidth()
                                .padding(horizontal=12.dp,vertical=9.dp)){
                                Text(w.number.toString()+" · "+
                                    (if(w.direction==Direction.Across)"YATAY" else "DİKEY"),
                                    color=Color(0xFF8F60B6),fontSize=11.sp,
                                    fontWeight=FontWeight.Bold)
                                Text(w.entry.clue,color=Color(0xFF302A42),
                                    fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                            }
                        }
                    }
                }
                TextButton(onClick={open=false},modifier=Modifier.align(Alignment.End)){
                    Text("Kapat")
                }
            }
        }
    }
}
