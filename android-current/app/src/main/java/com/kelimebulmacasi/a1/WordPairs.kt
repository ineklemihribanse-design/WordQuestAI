package com.kelimebulmacasi.a1

import kotlin.math.abs
import kotlin.math.roundToInt

data class ScanLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val height get() = (bottom - top).coerceAtLeast(1)
    val centerY get() = (top + bottom) / 2.0
}

internal fun joinTableFragments(lines: List<ScanLine>): List<ScanLine> {
    val joined = mutableListOf<ScanLine>()
    for (line in lines.sortedBy(ScanLine::left)) {
        val preceding = joined.withIndex().filter { (_, prior) ->
            val distance = line.left - prior.right
            val size = minOf(prior.height, line.height)
            distance in -size / 3..size * 3 / 4 && abs(line.centerY - prior.centerY) <= size * .46 &&
                prior.text.any(Char::isLetter) && line.text.any(Char::isLetter)
        }.minByOrNull { abs(line.left - it.value.right) }
        if (preceding == null) joined.add(line) else {
            val prior = preceding.value
            joined[preceding.index] = ScanLine(
                prior.text.trimEnd() + " " + line.text.trimStart(), prior.left, minOf(prior.top, line.top),
                maxOf(prior.right, line.right), maxOf(prior.bottom, line.bottom),
            )
        }
    }
    return joined
}

data class WordPair(val answer: String, val clue: String, val review: Boolean = false) {
    val valid get() = isEnglishAnswer(answer) && clue.any(Char::isLetter)
}

internal data class PairReading(val pairs: List<WordPair>, val tableScore: Int)

internal fun pageNumbers(selection: String, count: Int): List<Int> {
    require(count > 0) { "Bu PDF’de sayfa bulunamadı." }
    if (selection.isBlank()) return (1..count).toList()
    val numbers = linkedSetOf<Int>()
    for (part in selection.split(',')) {
        val chunks = part.trim().replace('–', '-').split('-')
        val start = chunks.firstOrNull()?.trim()?.toIntOrNull()
        val end = chunks.lastOrNull()?.trim()?.toIntOrNull()
        require(chunks.size in 1..2 && start != null && end != null && start in 1..count && end in start..count) {
            "Bu PDF $count sayfa. Örnekteki gibi bir aralık gir. Örnek 2–4, 7."
        }
        numbers.addAll(start..end)
    }
    return numbers.sorted()
}

private val englishPattern = Regex("[A-Za-z]+(?:[ '-][A-Za-z]+){0,5}")
private val spaces = Regex("\\s+")
private val numbered = Regex("^\\s*\\d+[.)]\\s*")
private val beforeMeaning = Regex("^[\\s:;=|•·]+")
private val separation = Regex("\\s*[:=|→]\\s*|\\s+[–—-]\\s+|\\t+")
private val dashedSeparation = Regex("^([A-Za-z][A-Za-z '-]{1,35})\\s*[-–—]\\s*([\\p{L}].*)$")
private val turkish = "çğıöşüÇĞİÖŞÜ"
private val ignoredHeadings = setOf("english", "turkish", "meaning", "word", "vocabulary", "answer", "translation")
private val sentenceOpeners = setOf("a", "an", "the", "he", "she", "they", "we", "you", "i", "there", "these", "those")
private val turkishHeadings = Regex("(?:hedef\\s+kelime|kelime\\s+listesi|türkçe\\s+(?:anlam|karşılık)|vocabulary)", RegexOption.IGNORE_CASE)
private val tableEndHeading = Regex("(?:örnek\\s+cümle|olum(?:lu|suz)\\s+cümle|soru\\s+cümle|dil\\s+bilgisi|exercise\\b|practice\\b)", RegexOption.IGNORE_CASE)
private val ambiguousScan = Regex("\\b(?:ii|ll|[ıİ])\\b|[�¤]", RegexOption.IGNORE_CASE)

internal fun cleanAnswer(raw: String) = raw.trim().replace(numbered, "").trim()
    .replace('’', '\'').replace('‘', '\'').replace('–', '-').replace('—', '-')
    .trim { it in " \t:;.,·•|_\"“”()[]/\\\u00a0" }.replace(spaces, " ")

internal fun isEnglishAnswer(raw: String): Boolean {
    val answer = cleanAnswer(raw)
    return answer.matches(englishPattern) && englishLetters(answer).length in 2..22
}

private fun cleanClue(raw: String) = raw.trim().replace(beforeMeaning, "").trim()
    .trimEnd(' ', '|', '•', '·', '—', '_').replace(spaces, " ").take(125)

private fun isHeading(raw: String) = turkishHeadings.containsMatchIn(raw) || tableEndHeading.containsMatchIn(raw) ||
    cleanAnswer(raw).lowercase() in ignoredHeadings

private fun plausibleClue(text: String): Boolean =
    text.length in 2..125 && text.any(Char::isLetter) && text.count { it == '.' } < 3 && !isHeading(text) &&
        text.split(' ').size <= 16

private fun readPair(line: String): WordPair? {
    val index = separation.find(line)
    val parts = if (index != null) {
        line.substring(0, index.range.first) to line.substring(index.range.last + 1)
    } else {
        val match = dashedSeparation.matchEntire(line.trim()) ?: return null
        match.groupValues[1] to match.groupValues[2]
    }
    val original = cleanAnswer(parts.first)
    val answer = preserveEnglishPhraseSpacing(original)
    val clue = cleanClue(parts.second)
    if (isHeading(answer) || !isEnglishAnswer(answer) || !plausibleClue(clue)) return null
    return WordPair(answer.uppercase(), clue, original != answer || isUncertain(answer, clue))
}

internal fun pastedPairs(text: String): List<WordPair> = text.lineSequence().mapNotNull(::readPair)
    .distinctBy { englishLetters(it.answer) }.toList()

private fun isUncertain(answer: String, clue: String): Boolean {
    val words = answer.split(' ')
    return clue.count(Char::isLetter) <= 2 || clue.any(Char::isDigit) || ambiguousScan.containsMatchIn(answer) ||
        (words.size >= 4 && words.first().lowercase() in sentenceOpeners) ||
        answer.equals(clue, ignoreCase = true) || clue.contains("�")
}

internal fun recognizePairs(lines: List<ScanLine>): PairReading {
    val clean = lines.filter { it.text.isNotBlank() && it.right > it.left && it.bottom > it.top }
    if (clean.isEmpty()) return PairReading(emptyList(), 0)
    val ordered = clean.sortedWith(compareBy<ScanLine> { it.centerY }.thenBy { it.left })
    val scale = ordered.map(ScanLine::height).sorted()[ordered.size / 2].toDouble().coerceAtLeast(9.0)
    val explicit = ordered.mapNotNull { readPair(it.text) }
    val table = columnPairs(ordered, scale)
    if (table.pairs.isEmpty()) return PairReading(explicit.distinctBy { englishLetters(it.answer) }, explicit.size * 3)
    val pairedByAnswer = (table.pairs + explicit).associateBy { englishLetters(it.answer) }
    val result = (table.pairs + explicit).distinctBy { englishLetters(it.answer) }
        .map { pairedByAnswer.getValue(englishLetters(it.answer)) }
    return PairReading(result, table.tableScore + explicit.size * 2)
}

private data class Track(val lines: MutableList<ScanLine> = mutableListOf()) {
    val centerX get() = lines.map(ScanLine::left).average()
}

private fun columnPairs(ordered: List<ScanLine>, scale: Double): PairReading {
    val heading = ordered.filter { turkishHeadings.containsMatchIn(it.text) }
    val words = ordered.filter { line ->
        val candidate = cleanAnswer(line.text)
        !isHeading(line.text) && isEnglishAnswer(candidate) && !line.text.trimStart().startsWith(":") &&
            (candidate.split(' ').size <= 3 || candidate.split(' ').first().lowercase() !in sentenceOpeners)
    }.sortedBy { it.left }
    val leftTracks = tracks(words, scale * 1.5).filter { it.lines.size >= 3 }
    val allMeaning = ordered.filter { line ->
        val text = cleanClue(line.text)
        plausibleClue(text)
    }.sortedBy { it.left }
    val rightTracks = tracks(allMeaning, scale * 1.9).filter { it.lines.size >= 3 }
    val minGap = scale * 2.5
    var best = PairReading(emptyList(), 0)

    for (left in leftTracks) {
        for (right in rightTracks) {
            val gap = right.centerX - left.centerX
            if (gap < minGap || gap > scale * 26) continue
            val english = left.lines.filter { it.right < right.centerX + scale * .32 }
            val meanings = right.lines.sortedBy { it.centerY }
            val relatedHeading = heading.filter {
                it.left.toDouble() in (left.centerX - scale * 2.4)..(left.centerX + scale * 3.2) &&
                    english.any { w -> w.top >= it.bottom && w.top - it.bottom < scale * 10 }
            }.minByOrNull { abs(it.left - left.centerX) }
            val end = if (relatedHeading == null) null else ordered.firstOrNull {
                it.top > relatedHeading.bottom && it.left.toDouble() in (left.centerX - scale * 2.4)..(right.centerX + scale) &&
                    (turkishHeadings.containsMatchIn(it.text) || tableEndHeading.containsMatchIn(it.text))
            }
            val eligible = english.filter {
                (relatedHeading == null || it.centerY > relatedHeading.centerY) &&
                    (end == null || it.centerY < end.centerY)
            }.sortedBy { it.centerY }
            if (eligible.size < 3) continue

            val candidate = alignColumns(eligible, meanings, scale)
            val linked = candidate.count { it.second != null }
            if (linked < 3) continue
            val evidence = candidate.count { (_, clue) ->
                clue != null && (clue.text.any { it in turkish } || clue.text.trimStart().startsWith(':'))
            }
            if (evidence < 2 && relatedHeading == null) continue
            val score = linked * 6 + evidence * 4 + (if (relatedHeading != null) 20 else 0) -
                (candidate.size - linked) * 2
            if (score <= best.tableScore) continue

            val start = candidate.indexOfFirst { it.second != null }
            val last = candidate.indexOfLast { it.second != null }
            val range = if (relatedHeading != null) (start - 1).coerceAtLeast(0)..(last + 2).coerceAtMost(candidate.lastIndex)
                else start..last
            val selected = candidate.slice(range).dropWhile { (word, clue) ->
                clue == null || (
                    word.text.trim().lowercase() in sentenceOpeners &&
                        !clue.text.trimStart().startsWith(':') && clue.text.none { it in turkish }
                )
            }
            if (selected.isEmpty()) continue
            val small = selected.map { it.first.height }.sorted().let { it[it.size / 2] } * .57
            best = PairReading(selected.filter { it.first.height >= small }.map { (en, tr) ->
                val answer = cleanAnswer(en.text)
                val clue = tr?.let { cleanClue(it.text) }.orEmpty()
                WordPair(preserveEnglishPhraseSpacing(answer).uppercase(), clue,
                    answer != preserveEnglishPhraseSpacing(answer) || clue.isBlank() || isUncertain(answer, clue))
            }, score)
        }
    }
    return best
}

private fun tracks(lines: List<ScanLine>, radius: Double): List<Track> {
    val result = mutableListOf<Track>()
    for (line in lines) {
        val existing = result.lastOrNull()?.takeIf { abs(it.centerX - line.left) <= radius }
        if (existing != null) existing.lines.add(line) else result.add(Track(mutableListOf(line)))
    }
    return result
}

private fun alignColumns(english: List<ScanLine>, meanings: List<ScanLine>, scale: Double): List<Pair<ScanLine, ScanLine?>> {
    val slopes = listOf(0.0, .035, .065, .105, -.035, -.065, -.105)
    val base = english.map { it.centerY }.average()
    var bestScore = Int.MIN_VALUE
    var best = emptyList<Pair<ScanLine, ScanLine?>>()
    for (slope in slopes) {
        for (delta in (-2..2).map { it * scale * .39 }) {
            val assignments = mutableListOf<Pair<ScanLine, ScanLine?>>()
            var last = -1
            var score = 0
            for ((index, line) in english.withIndex()) {
                val baseline = line.centerY + delta + slope * (line.centerY - base)
                val previous = english.getOrNull(index - 1)?.let { line.centerY - it.centerY } ?: Double.MAX_VALUE
                val following = english.getOrNull(index + 1)?.let { it.centerY - line.centerY } ?: Double.MAX_VALUE
                val tolerance = minOf(scale * 1.42, minOf(previous, following) * .68).coerceAtLeast(scale * .60)
                val near = meanings.withIndex().asSequence().drop(last + 1)
                    .takeWhile { it.value.centerY <= baseline + tolerance }
                    .filter { abs(it.value.centerY - baseline) <= tolerance }
                    .maxByOrNull { item ->
                        val value = item.value
                        val evidence = if (value.text.any { it in turkish }) .25 else 0.0
                        val colon = if (value.text.trimStart().startsWith(':')) .20 else 0.0
                        evidence + colon - abs(value.centerY - baseline) / scale
                    }
                if (near == null) {
                    assignments.add(line to null)
                } else {
                    last = near.index
                    assignments.add(line to near.value)
                    score += 18 - (abs(near.value.centerY - baseline) / scale * 11).roundToInt()
                    if (near.value.text.trimStart().startsWith(':')) score += 2
                }
            }
            if (score > bestScore) {
                bestScore = score
                best = assignments
            }
        }
    }
    return best
}
