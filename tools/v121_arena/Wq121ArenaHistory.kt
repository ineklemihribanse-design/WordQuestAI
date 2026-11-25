package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

/** Additive, private, profile-specific finished online match archive. No game rule changes. */
internal object Wq121ArenaHistory {
    private fun prefs(c:Context)=c.getSharedPreferences("wq_arena_finished_v121",Context.MODE_PRIVATE)
    fun read(c:Context,profile:String):List<JSONObject> {
        val a=runCatching{JSONArray(prefs(c).getString(profile,"[]") ?: "[]")}.getOrElse{JSONArray()}
        return (0 until a.length()).mapNotNull{a.optJSONObject(it)}
    }
    fun record(c:Context,profile:String,code:String,mode:String,hostUid:String,guestUid:String,host:String,guest:String,h:Int,g:Int) {
        if(code.isBlank() || hostUid.isBlank() || guestUid.isBlank())return
        val old=read(c,profile)
        if(old.any{it.optString("code")==code})return
        val entry=JSONObject().put("code",code).put("mode",mode).put("hostUid",hostUid).put("guestUid",guestUid).put("host",host).put("guest",guest).put("h",h).put("g",g).put("at",System.currentTimeMillis())
        val all=JSONArray()
        (listOf(entry)+old).take(100).forEach{all.put(it)}
        prefs(c).edit().putString(profile,all.toString()).apply()
    }
}

@Composable
internal fun Wq121ArenaRecords(context:Context,profile:String,ranking:Boolean) {
    val records=remember(context,profile){Wq121ArenaHistory.read(context,profile)}
    Text(if(ranking)"Bu cihazda kayıtlı son 100 çevrimiçi maçın puan sıralaması (genel çevrimiçi sıralama değildir)." else "Bu cihazda 1.0.121 ve sonrası tamamlanan çevrimiçi maçlar.")
    if(records.isEmpty())Text("Henüz kayıtlı tamamlanmış maç yok.")
    else if(ranking) {
        val totals=mutableMapOf<String,Pair<String,Int>>()
        records.forEach{r->
            listOf(Triple(r.optString("hostUid"),r.optString("host"),r.optInt("h")),Triple(r.optString("guestUid"),r.optString("guest"),r.optInt("g"))).forEach{(uid,name,score)->
                totals[uid]=Pair(name,(totals[uid]?.second?:0)+score)
            }
        }
        totals.values.sortedByDescending{it.second}.forEachIndexed{i,p->Text("${i+1}. ${p.first} — ${p.second} puan")}
    } else records.forEach{r->
        val mode=when(r.optString("mode")){"team"->"Ortak Bulmaca";"quiz"->"5 Şıklı Düello";else->"Bulmaca Düellosu"}
        Text(mode,fontWeight=FontWeight.Bold)
        Text("${r.optString("host")} ${r.optInt("h")} – ${r.optInt("g")} ${r.optString("guest")}")
        Text(DateFormat.getDateTimeInstance().format(Date(r.optLong("at"))))
        HorizontalDivider()
    }
}
