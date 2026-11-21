package com.kelimebulmacasi.a1

import android.content.Context

/** Persistent per-player shuffle bags, independent for each daily minigame.
 * Every index is seen exactly once before a new bag is generated. A restarted
 * session cannot return to the first sample just because its screen index did.
 */
internal object Wq177DailyRotation {
    fun next(context:Context,profile:String,kind:PracticeKind,size:Int):Int {
        if(size<=1)return 0
        val prefs=context.getSharedPreferences("wq177_daily_rotation",Context.MODE_PRIVATE)
        val suffix=profile.hashCode().toString()+"."+kind.name
        val key="bag.$suffix"
        val lastKey="last.$suffix"
        val remaining=prefs.getString(key,"").orEmpty().split(",")
            .mapNotNull { it.toIntOrNull()?.takeIf { n-> n in 0 until size } }
            .distinct()
        val bag=if(remaining.isNotEmpty())remaining else (0 until size).shuffled().toMutableList().also {
            val previous=prefs.getInt(lastKey,-1)
            if(it.firstOrNull()==previous && it.size>1) {
                val swap=it[0]; it[0]=it[1]; it[1]=swap
            }
        }
        val index=bag.first()
        prefs.edit()
            .putString(key,bag.drop(1).joinToString(","))
            .putInt(lastKey,index)
            .apply()
        return index
    }
}
