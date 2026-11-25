package com.kelimebulmacasi.a1

import android.content.Context
import org.json.JSONObject

/** Bounded, data-only design settings. No scripts, callbacks, URLs or executable code. */
internal data class Wq128LobbyDesign(
    val phoneCardPercent:Int=47, val tabletCardPercent:Int=49,
    val cardPadding:Int=7, val portraitBottom:Int=9, val nameSp:Int=15,
    val actionHeight:Int=44, val gap:Int=8, val iconSize:Int=34, val radius:Int=20,
    val palette:String="royal", val sectionOrder:String="code_settings_players"
) {
    val sections:List<String> get()=when(sectionOrder) {
        "players_code_settings" -> listOf("players","code","settings")
        "settings_code_players" -> listOf("settings","code","players")
        else -> listOf("code","settings","players")
    }
    companion object {
        const val PATH="design/arena_lobby.json"
        const val MAX_BYTES=4096
        private val keys=setOf("schema","phoneCardPercent","tabletCardPercent","cardPadding","portraitBottom",
            "nameSp","actionHeight","gap","iconSize","radius","palette","sectionOrder")
        fun parse(bytes:ByteArray):Wq128LobbyDesign {
            require(bytes.size in 2..MAX_BYTES)
            val o=JSONObject(String(bytes,Charsets.UTF_8))
            require(o.keys().asSequence().toSet()==keys)
            fun n(k:String,range:IntRange):Int {
                val v=o.get(k)
                require(v is Int || v is Long)
                val x=(v as Number).toLong()
                require(x in range.first.toLong()..range.last.toLong())
                return x.toInt()
            }
            fun choice(k:String,allowed:Set<String>):String {
                val v=o.get(k)
                require(v is String && v in allowed)
                return v
            }
            n("schema",1..1)
            return Wq128LobbyDesign(n("phoneCardPercent",40..60),n("tabletCardPercent",40..65),
                n("cardPadding",4..14),n("portraitBottom",0..16),n("nameSp",13..20),
                n("actionHeight",44..58),n("gap",4..12),n("iconSize",24..44),n("radius",12..28),
                choice("palette",setOf("royal","midnight","amethyst")),
                choice("sectionOrder",setOf("code_settings_players","players_code_settings","settings_code_players")))
        }
        fun load(c:Context):Wq128LobbyDesign = runCatching {
            WqContentUpdates.open(c,PATH).use { input ->
                val b=java.io.ByteArrayOutputStream()
                val buf=ByteArray(1024)
                while(true) {
                    val n=input.read(buf)
                    if(n<0)break
                    require(b.size()+n<=MAX_BYTES)
                    b.write(buf,0,n)
                }
                parse(b.toByteArray())
            }
        }.getOrElse { Wq128LobbyDesign() }
    }
}
