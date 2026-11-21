package com.kelimebulmacasi.a1

import android.content.Context

/**
 * Preserve the exact letter position of a paid Firebase hint when later
 * crossword intersections change the blank-cell pattern.
 * Hint permission and quotas remain authoritative in the Realtime Database.
 */
internal object Wq181ArenaHintSlot {
    private const val PREFS = "wordquest_arena_hint_slots"
    private fun key(room:String,uid:String,word:Int) = "$room|$uid|$word"

    /** Pure helper allows JVM regression tests. */
    fun lockedPosition(previous:Int,suggested:Int,size:Int):Int =
        if(previous in 0 until size) previous else suggested

    fun reserve(context:Context,room:String,uid:String,word:Int,pattern:String):Int {
        val store=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val key=key(room,uid,word)
        val previous=store.getInt(key,-1)
        val suggested=Wq179ArenaOrder.hintPosition(room,word,pattern)
        val position=lockedPosition(previous,suggested,pattern.length)
        if(position>=0 && previous!=position) store.edit().putInt(key,position).apply()
        return position
    }
}
