package com.kelimebulmacasi.a1

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

internal data class Wq186MatchFolder(
    val id: String,
    val name: String,
    val englishDefinitions: Boolean,
    val words: List<Wq175MatchPair>,
)

/**
 * Matching collections are independent from Kelime Hafızam, puzzles and Arena.
 * Everything is stored locally in separate profile-scoped preferences.
 */
internal class Wq186MatchingFolders(context: Context, profile: String) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "wq186_match_folders_" + stableHash(profile).take(24), Context.MODE_PRIVATE
    )
    @Synchronized fun all(): List<Wq186MatchFolder> {
        val data=runCatching { JSONArray(prefs.getString("folders","[]")) }.getOrNull()
            ?: return emptyList()
        return (0 until minOf(120,data.length())).mapNotNull { i ->
            runCatching {
                val raw=data.getJSONObject(i)
                val words=raw.optJSONArray("words") ?: JSONArray()
                val pairs=(0 until minOf(300,words.length())).mapNotNull { j ->
                    runCatching {
                        val item=words.getJSONObject(j)
                        Wq175MatchPair(
                            item.optString("level").take(12),
                            item.optString("kind").take(40),
                            item.getString("english").take(75),
                            item.getString("meaning").take(200),
                        )
                    }.getOrNull()
                }
                Wq186MatchFolder(
                    raw.getString("id").take(80),
                    raw.getString("name").take(60),
                    raw.optBoolean("englishDefinitions",false),
                    pairs,
                )
            }.getOrNull()
        }
    }
    private fun persist(folders: List<Wq186MatchFolder>) {
        val arr=JSONArray()
        folders.take(120).forEach { folder ->
            val words=JSONArray()
            folder.words.take(300).forEach { word ->
                words.put(JSONObject().put("level",word.level).put("kind",word.kind)
                    .put("english",word.english).put("meaning",word.turkish))
            }
            arr.put(JSONObject().put("id",folder.id).put("name",folder.name)
                .put("englishDefinitions",folder.englishDefinitions)
                .put("words",words))
        }
        check(prefs.edit().putString("folders",arr.toString()).commit()) {
            "Eşleştirme klasörü kaydedilemedi."
        }
    }
    /** Returns null for invalid data. Never deletes or overwrites another folder. */
    @Synchronized fun save(
        name: String, englishDefinitions: Boolean,
        candidates: List<Wq175MatchPair>, folderId: String? = null,
    ): Wq186MatchFolder? {
        val title=name.trim().replace(Regex("\\s+")," ").take(60)
        if(title.isBlank() || candidates.isEmpty()) return null
        val existing=all()
        val original=existing.firstOrNull { it.id==folderId }
        if(original != null && original.englishDefinitions != englishDefinitions) return null
        if(original == null && existing.size>=120) return null
        val valid=candidates.filter { item ->
            item.english.trim().length in 2..75 &&
                item.turkish.trim().length in 2..200 &&
                item.english.any(Char::isLetter) && item.turkish.any(Char::isLetter)
        }
        if(valid.isEmpty()) return null
        val previous=original?.words.orEmpty()
        val merged=(previous+valid).distinctBy {
            it.english.lowercase(Locale.ROOT).trim()
        }.distinctBy { it.turkish.lowercase(Locale.ROOT).trim() }.take(300)
        val folder=Wq186MatchFolder(original?.id?:UUID.randomUUID().toString(),
            title,englishDefinitions,merged)
        persist(existing.filterNot { it.id==folder.id }+folder)
        return folder
    }
    @Synchronized fun rename(folderId:String,newName:String):Boolean {
        val title=newName.trim().take(60)
        if(title.isBlank())return false
        val folders=all()
        if(folders.none{it.id==folderId})return false
        persist(folders.map{if(it.id==folderId)it.copy(name=title)else it})
        return true
    }
    @Synchronized fun remove(folderId:String):Boolean {
        val folders=all()
        if(folders.none { it.id==folderId })return false
        persist(folders.filterNot { it.id==folderId })
        return true
    }
}
