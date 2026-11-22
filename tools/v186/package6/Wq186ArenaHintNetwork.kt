package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

internal data class Wq186HintClaim(val word: Int,val position: Int)

/**
 * Each hint is a separately authorized, append-only Firebase claim. No per-word
 * "hint already used" restriction: the same word may consume the entire budget.
 */
internal object Wq186ArenaHintNetwork {
    private fun parse(snapshot: DataSnapshot): List<Wq186HintClaim> =
        snapshot.children.mapNotNull { item ->
            val w=(item.child("word").value as? Number)?.toInt()
            val p=(item.child("position").value as? Number)?.toInt()
            if(w != null && p != null) Wq186HintClaim(w,p) else null
        }

    @Composable
    fun claims(session: AppSession, room: String, uid: String): List<Wq186HintClaim> {
        val database=session.accounts.socialDatabase()
        val ref=remember(database,room,uid) {
            if(room.isBlank() || uid.isBlank()) null else
                database?.reference?.child("premiumRooms")?.child(room)
                    ?.child("package6")?.child("hintClaims")?.child(uid)
        }
        var claims by remember(room,uid) { mutableStateOf(emptyList<Wq186HintClaim>()) }
        DisposableEffect(ref) {
            if (ref == null) {
                onDispose {}
            } else {
                val listener=object:ValueEventListener {
                    override fun onDataChange(snapshot:DataSnapshot) {
                        claims=parse(snapshot)
                    }
                    override fun onCancelled(error:DatabaseError) {}
                }
                ref.addValueEventListener(listener)
                onDispose { ref.removeEventListener(listener) }
            }
        }
        return claims
    }

    fun reveal(
        session:AppSession,room:String,uid:String,word:Int,
        pattern:String,limit:Int,done:(String?)->Unit
    ) {
        if(room.isBlank() || uid.isBlank() || word<0 || limit !in 1..20) {
            done("İpucu ayarları geçersiz.");return
        }
        val ref=session.accounts.socialDatabase()?.reference
            ?.child("premiumRooms")?.child(room)
            ?.child("package6")?.child("hintClaims")?.child(uid)
        if(ref==null){done("Firebase bağlantısı yok.");return}
        ref.get().addOnSuccessListener { data ->
            val prior=parse(data)
            if(prior.size>=limit) {
                done("İpucu hakkın kalmadı.");return@addOnSuccessListener
            }
            val used=prior.filter { it.word==word }.map{it.position}.toSet()
            val position=Wq186ArenaFairPlay.hintPosition(room,word,used,pattern)
            if(position==null) {
                done("Bu kelimede açılabilecek harf kalmadı.");return@addOnSuccessListener
            }
            val next=data.children.mapNotNull{it.key?.toIntOrNull()}.maxOrNull()?.plus(1) ?: 0
            if(next !in 0..19) { done("İpucu hakkın kalmadı.");return@addOnSuccessListener }
            ref.child(next.toString()).runTransaction(object:Transaction.Handler {
                override fun doTransaction(current:MutableData):Transaction.Result {
                    if(current.value!=null)return Transaction.abort()
                    current.value=mapOf("word" to word,"position" to position)
                    return Transaction.success(current)
                }
                override fun onComplete(error:DatabaseError?,committed:Boolean,snapshot:DataSnapshot?) {
                    done(if(error!=null) "İpucu Firebase tarafından reddedildi: ${error.message}"
                         else if(!committed) "İpucu eşzamanlı kullanıldı; yeniden dene."
                         else null)
                }
            },false)
        }.addOnFailureListener { done("İpucu kaydı okunamadı. Bağlantını kontrol et.") }
    }
}

@Composable
internal fun Wq186ArenaHintAction(
    left:Int, revealed:Map<Int,Char>, canUse:Boolean, onHint:()->Unit
) {
    Surface(
        shape=RoundedCornerShape(14.dp),
        color=Color(0xFFFFF8EA),
        border=BorderStroke(1.dp,Color(0xFFF2D9AB)),
        modifier=Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal=9.dp,vertical=4.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("💡 İpucu · $left hak",
                    color=Color(0xFF49345C),fontSize=12.sp,fontWeight=FontWeight.Bold)
                if(revealed.isNotEmpty()) {
                    Text(revealed.toSortedMap().entries.joinToString(" · ") {
                        "${it.key+1}. ${it.value}"
                    },color=Color(0xFF5C4777),fontSize=11.sp,maxLines=2)
                }
            }
            Button(onClick=onHint,enabled=canUse && left>0,
                shape=RoundedCornerShape(12.dp),
                colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF70419B)),
                contentPadding=PaddingValues(horizontal=12.dp,vertical=7.dp)) {
                Text("Harf aç",color=Color.White,fontSize=12.sp)
            }
        }
    }
}
