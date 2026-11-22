package com.kelimebulmacasi.a1

import androidx.compose.runtime.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

/**
 * Two skips belong to a participant in a Firebase room, not to one device.
 * A transaction and a per-user RTDB rule prevent simultaneous double spending.
 */
internal object Wq186ArenaSkipNetwork {
    @Composable
    fun count(session: AppSession, room: String, uid: String): Int {
        val db = session.accounts.socialDatabase()
        val ref = remember(db, room, uid) {
            if (uid.isBlank() || room.isBlank()) null
            else db?.reference?.child("premiumRooms")?.child(room)
                ?.child("package6")?.child("skips")?.child(uid)
        }
        var value by remember(room, uid) { mutableIntStateOf(0) }
        DisposableEffect(ref) {
            if (ref == null) {
                onDispose {}
            } else {
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        value = ((snapshot.value as? Number)?.toInt() ?: 0).coerceIn(0, 2)
                    }
                    override fun onCancelled(error: DatabaseError) { }
                }
                ref.addValueEventListener(listener)
                onDispose { ref.removeEventListener(listener) }
            }
        }
        return value
    }

    fun consume(session: AppSession, room: String, uid: String, done: (Boolean) -> Unit) {
        if (room.isBlank() || uid.isBlank()) { done(false); return }
        val ref = session.accounts.socialDatabase()?.reference
            ?.child("premiumRooms")?.child(room)?.child("package6")
            ?.child("skips")?.child(uid)
        if (ref == null) { done(false); return }
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                val old = (data.value as? Number)?.toInt() ?: 0
                if (old !in 0..1) return Transaction.abort()
                data.value = old + 1
                return Transaction.success(data)
            }
            override fun onComplete(
                error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?
            ) { done(error == null && committed) }
        }, false)
    }
}
