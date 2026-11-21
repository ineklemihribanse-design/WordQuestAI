package com.kelimebulmacasi.a1

import android.content.Context
import java.util.Locale

/**
 * Daily-plan word drills are read from the SAME bundled bilingual corpus as
 * vocabulary targets. No fabricated CEFR data or external AI/API is required.
 * Missing C2 translations do not become questions.
 *
 * A word is accepted once per level, even if it exists in both old puzzles
 * and the supplementary TSV. Wrong answers are distinct Turkish meanings
 * drawn preferentially from the same grammatical category and tier.
 */
internal object Wq176VocabularyPractice {
    internal data class Word(
        val level: String,
        val kind: String,
        val english: String,
        val turkish: String
    )

    private val LEVELS=setOf("A1","A2","B1","B2","C1","C2")
    private val FILES=listOf(
        "vocabulary-extra.tsv",
        "vocabulary-addendum-v175.tsv",
        "vocabulary-addendum-v176-approved.tsv"
    )

    private fun parse(context:Context):List<Word> {
        val all=ArrayList<Word>()
        val seen=HashSet<String>()
        FILES.forEachIndexed { index,filename ->
            // Only the reviewed 1.0.76 extension file is optional.
            if(index==2 && !context.assets.list("")!!.contains(filename)) return@forEachIndexed
            context.assets.open(filename).bufferedReader(Charsets.UTF_8).use { reader ->
                reader.forEachLine { raw ->
                    if(raw.isBlank() || raw.startsWith("#")) return@forEachLine
                    val fields=raw.split(9.toChar())
                    if(fields.size!=5 || fields[0] !in LEVELS) return@forEachLine
                    val en=fields[3].trim().uppercase(Locale.ROOT)
                    if(!en.matches(Regex("[A-Z][A-Z '-]{1,30}"))) return@forEachLine
                    val tr=fields[4].substringBefore(";").substringBefore(",")
                        .trim().take(68)
                    if(tr.length<2 || fields[2].isBlank()) return@forEachLine
                    if(seen.add(fields[0]+":"+en))
                        all.add(Word(fields[0],fields[2],en,tr))
                }
            }
        }
        return all
    }

    fun questions(context:Context,level:String):List<YdsQuestion> {
        require(level in LEVELS)
        val words=parse(context).filter { it.level==level }
        val unique=words.distinctBy { it.english }
        val byKind=unique.groupBy { it.kind }
        return unique.mapNotNull { item ->
            // O(n) total construction, not an O(n² log n) per-word sort on UI.
            val wrong=LinkedHashSet<String>()
            fun appendFrom(deck:List<Word>) {
                if(deck.isEmpty() || wrong.size>=3) return
                val start=(item.english.hashCode().toLong() and 0x7FFFFFFF)
                    .rem(deck.size.toLong()).toInt()
                for(offset in deck.indices) {
                    val candidate=deck[(start+offset)%deck.size]
                    if(candidate.english!=item.english &&
                       !candidate.turkish.equals(item.turkish,true) &&
                       wrong.none { it.equals(candidate.turkish,true) }) {
                        wrong.add(candidate.turkish)
                        if(wrong.size==3) break
                    }
                }
            }
            appendFrom(byKind[item.kind].orEmpty())
            appendFrom(unique)
            if(wrong.size!=3) null else {
                val difficulty=when(level) {
                    "A1","A2" -> YdsDifficulty.Easy
                    "B1","B2" -> YdsDifficulty.Medium
                    else -> YdsDifficulty.Hard
                }
                YdsQuestion(
                    id="wq176_word_${level}_${item.english.replace(' ','_')}",
                    topic=YdsTopic.Vocabulary,
                    difficulty=difficulty,
                    question="‘${item.english.lowercase(Locale.ROOT)}’ kelimesinin Türkçe karşılığı hangisidir?",
                    options=listOf(item.turkish)+wrong,
                    answer=0,
                    explanation="Kelime: ${item.english.lowercase(Locale.ROOT)}. Türkçe karşılığı: ${item.turkish}."
                )
            }
        }
    }
}
