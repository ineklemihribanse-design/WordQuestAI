package com.kelimebulmacasi.a1

/** Only high-confidence phrasal verbs: never split a legitimate unknown compound by guessing. */
internal val commonPhrasalVerbs = listOf(
    "give up", "look up", "look after", "take off", "put off", "carry out", "come across",
    "run into", "turn down", "turn up", "find out", "set off", "get over", "bring about",
    "look forward to", "come up with", "put up with", "get along with", "run out of",
    "make up for", "get rid of", "keep up with", "take care of", "break down",
    "take over", "put away", "pick up", "give in", "hold on", "take after",
    "show up", "back up", "call off", "work out", "sort out", "fill in", "go through",
    "get by", "get away with", "account for", "deal with", "depend on",
    "as a result", "due to", "in spite of", "according to", "in addition to",
)
internal val fusedPhrasals: Map<String,String> = commonPhrasalVerbs.associateBy {
    it.filter(Char::isLetter).lowercase()
}

/** Preserve the *linguistic* answer; Crossword.Entry converts letters for grid drawing only. */
internal fun preserveEnglishPhraseSpacing(value: String): String {
    val clean = cleanAnswer(value)
    if (clean.isBlank()) return clean
    val glued = clean.filter(Char::isLetter).lowercase()
    return if (clean.none(Char::isWhitespace) && glued in fusedPhrasals) fusedPhrasals.getValue(glued) else clean
}

internal fun requireVisibleWordBoundaries(candidate: String): Boolean {
    val answer = preserveEnglishPhraseSpacing(candidate)
    return isEnglishAnswer(answer) && answer.length <= 32
}
