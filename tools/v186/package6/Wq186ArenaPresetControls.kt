package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot

/** Persisted per profile; actual match rules are copied into its Firebase room. */
internal data class Wq186MatchPreset(
    val levels: List<String>, val hints: Int = 3, val retries: Int = 1
) {
    fun sanitized(primary: String): Wq186MatchPreset {
        val pool = levels.filter { it in Wq186ArenaPresetStore.LEVELS }
            .distinct().take(6).ifEmpty { listOf(primary.takeIf {
                it in Wq186ArenaPresetStore.LEVELS
            } ?: "A1") }
        return Wq186MatchPreset(pool, hints.coerceIn(0,20), retries.coerceIn(0,3))
    }

    companion object {
        fun fromRoom(snapshot: DataSnapshot, primary: String): Wq186MatchPreset {
            val pool = snapshot.child("levels").getValue(String::class.java).orEmpty()
                .split(',').filter(String::isNotBlank)
            val h = (snapshot.child("hints").value as? Number)?.toInt() ?: 3
            val r = (snapshot.child("retries").value as? Number)?.toInt() ?: 1
            return Wq186MatchPreset(pool,h,r).sanitized(primary)
        }
    }
}

internal object Wq186ArenaPresetStore {
    val LEVELS = listOf("A1","A2","B1","B2","C1","C2")
    private fun key(profile: String) = "arena6." + profile
    private fun prefs(context: Context) = context.getSharedPreferences(
        "wordquest_arena_p6_prefs", Context.MODE_PRIVATE
    )
    fun read(context: Context, profile: String, primary: String): Wq186MatchPreset {
        val p = prefs(context)
        val base = key(profile)
        val raw = p.getString("$base.levels",null)
        val levels = raw?.split(',')?.filter(String::isNotBlank)
            ?: if (primary == "A1–A2") listOf("A1","A2") else listOf(primary)
        return Wq186MatchPreset(levels,
            p.getInt("$base.hints",3),p.getInt("$base.retries",1)).sanitized(primary)
    }
    fun write(context: Context, profile: String, preset: Wq186MatchPreset) {
        val safe = preset.sanitized("A1")
        prefs(context).edit()
            .putString("${key(profile)}.levels",safe.levels.joinToString(","))
            .putInt("${key(profile)}.hints",safe.hints)
            .putInt("${key(profile)}.retries",safe.retries)
            .apply()
    }
}

/** Compact in-place settings, not another Arena navigation page. */
@Composable
internal fun Wq186ArenaPresetControls(
    profile: String, selectedLevel: String, onLevel: (String) -> Unit
) {
    val context = LocalContext.current
    var rules by remember(profile) {
        mutableStateOf(Wq186ArenaPresetStore.read(context,profile,selectedLevel))
    }
    fun change(next: Wq186MatchPreset) {
        rules = next.sanitized(selectedLevel)
        Wq186ArenaPresetStore.write(context,profile,rules)
    }
    Column(Modifier.fillMaxWidth(),
        verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Text("Maç seviyesi · birden fazla seçilebilir",
            fontWeight=FontWeight.Bold)
        Wq186ArenaPresetStore.LEVELS.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                row.forEach { level ->
                    val checked = level in rules.levels
                    FilterChip(
                        selected=checked,
                        onClick={
                            if(!checked) {
                                change(rules.copy(levels=rules.levels+level))
                                onLevel(level)
                            } else if(rules.levels.size>1) {
                                change(rules.copy(levels=rules.levels-level))
                                if(selectedLevel==level)onLevel(
                                    (rules.levels-level).first())
                            }
                        },
                        label={Text(level)},
                        modifier=Modifier.weight(1f),
                        shape=RoundedCornerShape(13.dp)
                    )
                }
            }
        }
        Text("İpucu hakkı · hepsi aynı kelimede kullanılabilir",
            fontWeight=FontWeight.Bold)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            listOf(0,3,5,8,12,20).forEach { number ->
                FilterChip(selected=rules.hints==number,
                    onClick={change(rules.copy(hints=number))},
                    label={Text("$number")},modifier=Modifier.weight(1f))
            }
        }
        Text("Sonda tekrar deneme hakkı",fontWeight=FontWeight.Bold)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf(0,1,2,3).forEach { number ->
                FilterChip(selected=rules.retries==number,
                    onClick={change(rules.copy(retries=number))},
                    label={Text("$number")},modifier=Modifier.weight(1f))
            }
        }
        Text("Maç kurulduğunda seçilen ayarlar iki oyuncuya da Firebase üzerinden iletilir.",
            style=MaterialTheme.typography.bodySmall)
    }
}
