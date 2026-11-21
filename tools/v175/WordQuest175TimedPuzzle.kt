package com.kelimebulmacasi.a1

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Deadline math is monotonic and does not use a Composition-driven counter. */
internal object Wq175TimerMath {
    fun remainingMillis(
        totalMillis:Long,elapsedStart:Long,wallStart:Long,
        nowElapsed:Long,nowWall:Long
    ):Long {
        if(totalMillis<=0)return 0L
        val passed=if(nowElapsed>=elapsedStart)
            nowElapsed-elapsedStart
        else (nowWall-wallStart).coerceAtLeast(0L) // device reboot
        return (totalMillis-passed.coerceAtLeast(0L)).coerceAtLeast(0L)
    }
    fun secondsRoundedUp(ms:Long)=((ms.coerceAtLeast(0L)+999L)/1000L)
}

/**
 * Profile and puzzle scoped opt-in timer. Only generated puzzles selected in
 * New Game receive this data. Existing untimed saves are never mutated.
 */
internal object Wq175TimedPuzzle {
    private const val STORE="wordquest_175_timed_puzzles"
    private fun key(profile:String,gameId:String):String =
        stableHash(profile+"\u001e"+gameId).take(36)
    data class Config(val durationMillis:Long,val elapsedStart:Long,val wallStart:Long) {
        fun remaining():Long=Wq175TimerMath.remainingMillis(durationMillis,
            elapsedStart,wallStart,SystemClock.elapsedRealtime(),
            System.currentTimeMillis())
    }
    fun configure(context:Context,profile:String,gameId:String,minutes:Int) {
        require(minutes in setOf(0,3,5,10))
        val prefs=context.getSharedPreferences(STORE,Context.MODE_PRIVATE)
        val k=key(profile,gameId)
        if(minutes==0) {
            prefs.edit().remove(k+"_length").remove(k+"_elapsed")
                .remove(k+"_wall").commit()
            return
        }
        val ok=prefs.edit()
            .putLong(k+"_length",minutes*60_000L)
            .putLong(k+"_elapsed",SystemClock.elapsedRealtime())
            .putLong(k+"_wall",System.currentTimeMillis())
            .commit()
        require(ok){"Süreye karşı oyun ayarları kaydedilemedi."}
    }
    fun read(context:Context,profile:String,gameId:String):Config? {
        val p=context.getSharedPreferences(STORE,Context.MODE_PRIVATE)
        val k=key(profile,gameId)
        val duration=p.getLong(k+"_length",0L)
        val elapsed=p.getLong(k+"_elapsed",0L)
        val wall=p.getLong(k+"_wall",0L)
        if(duration !in listOf(180_000L,300_000L,600_000L)
            || wall<=0L || elapsed<=0L)return null
        return Config(duration,elapsed,wall)
    }
}

@Composable
internal fun Wq175TimeRemainingBadge(remainingMillis:Long) {
    val seconds=Wq175TimerMath.secondsRoundedUp(remainingMillis)
    val critical=seconds<=30L
    val bg=if(critical)
        listOf(Color(0xFFFFD9DD),Color(0xFFFFEEF0))
        else listOf(Color(0xFFE2F5EF),Color(0xFFF7FCF9))
    val minutes=seconds/60L
    val s=seconds%60L
    Row(Modifier.fillMaxWidth()
        .background(Brush.horizontalGradient(bg),RoundedCornerShape(15.dp))
        .padding(horizontal=15.dp,vertical=10.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween){
        Row(verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Wq175GlossIcon("timer",Modifier.size(43.dp))
            Text("SÜREYE KARŞI",fontSize=12.sp,
                fontWeight=FontWeight.ExtraBold,
                color=if(critical)Color(0xFFA33E52) else Color(0xFF2E6B58))
        }
        Text("%02d:%02d".format(minutes,s),fontSize=21.sp,
            fontWeight=FontWeight.Black,
            color=if(critical)Color(0xFFB52E4B) else Color(0xFF20493A))
    }
}
